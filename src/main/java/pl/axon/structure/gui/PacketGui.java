package pl.axon.structure.gui;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Silnik ekranów GUI przeniesiony z AxonMap: gracz siada na niewidzialnym stojaku, przed kamerą stają niby-ramki
 * z 15 mapami (5 × 3), a shader z paczki zasobów rozciąga je na cały ekran. Kursor to obrót głowy, klik to
 * uderzenie albo użycie ramki. Wczytywana tylko, gdy na serwerze jest PacketEvents.
 */
final class PacketGui implements GuiBackend, Listener {

    private static final int TICK_PERIOD = 2;
    private static final double RESTORE_DISTANCE = 4.0;

    private final JavaPlugin plugin;
    private final ShaderGui owner;
    private final List<MapView> views = new ArrayList<>();
    private final List<ItemStack> mapItems = new ArrayList<>();
    private final Map<UUID, GuiSession> sessions = new HashMap<>();
    private final AtomicInteger nextEntityId = new AtomicInteger(1_950_000_000);
    private final GuiPackets packets;
    private int taskId = -1;
    private boolean warnedRender;

    private PacketGui(JavaPlugin plugin, ShaderGui owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.packets = new GuiPackets(plugin, this);
    }

    /** Tworzy i włącza silnik; woła to tylko {@link ShaderGui}, gdy PacketEvents jest włączony. */
    static GuiBackend create(JavaPlugin plugin, ShaderGui owner) {
        PacketGui gui = new PacketGui(plugin, owner);
        gui.enable();
        return gui;
    }

    private void enable() {
        List<Integer> ids = new ArrayList<>(owner.mapIds());
        World defaultWorld = Bukkit.getWorlds().get(0);
        boolean changed = false;
        for (int tile = 0; tile < GuiLayout.TILES; tile++) {
            MapView view = tile < ids.size() ? Bukkit.getMap(ids.get(tile)) : null;
            if (view == null) {
                view = Bukkit.createMap(defaultWorld);
                if (tile < ids.size()) {
                    ids.set(tile, view.getId());
                } else {
                    ids.add(view.getId());
                }
                changed = true;
            }
            for (MapRenderer renderer : new ArrayList<>(view.getRenderers())) {
                view.removeRenderer(renderer);
            }
            view.addRenderer(new GuiMapRenderer(this::pixelsFor, tile));
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
            views.add(view);

            ItemStack item = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapView(view);
            item.setItemMeta(meta);
            mapItems.add(item);
        }
        if (changed) {
            owner.saveMapIds(ids);
        }
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD).getTaskId();
        PacketEvents.getAPI().getEventManager().registerListener(packets);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        closeAll();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        HandlerList.unregisterAll(this);
        PacketEvents.getAPI().getEventManager().unregisterListener(packets);
    }

    private byte[] pixelsFor(Player player) {
        GuiSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.pixels;
    }

    @Override
    public boolean isOpen(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @Override
    public GuiScreen screen(Player player) {
        GuiSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.screen;
    }

    // ------------------------------------------------------------------ otwieranie i zamykanie

    @Override
    public boolean open(Player player, GuiScreen screen) {
        GuiSession current = sessions.get(player.getUniqueId());
        if (current != null) {
            // ekran już stoi: tylko podmieniamy treść
            GuiScreen old = current.screen;
            current.screen = screen;
            current.hover = null;
            screen.redraw();
            if (old != screen) {
                old.closed(player, false);
            }
            return true;
        }
        // Na pojeździe, w powietrzu albo kucając nie da się usiąść (kucanie zsadza w następnym ticku):
        // wtedy zwykłe okno skrzyni.
        if (player.isInsideVehicle() || player.isSneaking() || !isStandingOnGround(player) || player.isDead()
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return false;
        }
        Location location = player.getLocation();
        // shader liczy kursor od yaw 0 / pitch 0, więc od tego zaczynamy
        Location straight = location.clone();
        straight.setYaw(0f);
        straight.setPitch(0f);
        player.teleport(straight);
        ArmorStand seat = player.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
        });
        // inny plugin (limity bytów, ochrona regionów) może anulować postawienie albo posadzenie
        if (!seat.isValid() || !seat.addPassenger(player)) {
            seat.remove();
            player.teleport(location);
            return false;
        }
        GuiSession session = new GuiSession(player, seat, location, screen);
        sessions.put(player.getUniqueId(), session);
        session.packets = packets.open(player);
        render(session, player);
        // ramki stają w bloku z kamerą, gdy gracz już siedzi
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnFrames(player, session), 2L);
        return true;
    }

    private void spawnFrames(Player player, GuiSession session) {
        if (sessions.get(player.getUniqueId()) != session || !player.isOnline()) {
            return;
        }
        Location eye = player.getEyeLocation();
        int x = eye.getBlockX();
        int y = eye.getBlockY();
        int z = eye.getBlockZ();
        // Po dwie ramki na kafelek na przeciwnych ścianach bloku z kamerą: jedna z nich zawsze obejmuje kamerę
        // i nie wypada z pola widzenia.
        for (int tile = 0; tile < GuiLayout.TILES; tile++) {
            int north = nextEntityId.getAndIncrement();
            int south = nextEntityId.getAndIncrement();
            session.entityIds[tile * 2] = north;
            session.entityIds[tile * 2 + 1] = south;
            FakeFrames.spawn(player, north, x, y, z, FakeFrames.FACING_NORTH, mapItems.get(tile));
            FakeFrames.spawn(player, south, x, y, z, FakeFrames.FACING_SOUTH, mapItems.get(tile));
        }
        // Cztery pozostałe ściany zamykają pudełko, więc gdziekolwiek jest kursor, celownik trafia w ramkę i oba
        // przyciski myszy wysyłają kliknięcie. Pokazują znów kafelek 0, który shader rysuje w tym samym miejscu.
        int[] walls = {FakeFrames.FACING_WEST, FakeFrames.FACING_EAST, FakeFrames.FACING_UP, FakeFrames.FACING_DOWN};
        for (int i = 0; i < walls.length; i++) {
            int id = nextEntityId.getAndIncrement();
            session.entityIds[GuiLayout.TILES * 2 + i] = id;
            FakeFrames.spawn(player, id, x, y, z, walls[i], mapItems.get(0));
        }
        for (int id : session.entityIds) {
            session.packets.frameIds.add(id);
        }
        session.framesSpawned = true;
        session.packets.handHidden = true;
        ClientView.hideHand(player);
        ClientView.sendTime(player);
        for (MapView view : views) {
            player.sendMap(view);
        }
    }

    @Override
    public void closeIf(Player player, Predicate<GuiScreen> which) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session != null && which.test(session.screen)) {
            close(player, false);
        }
    }

    @Override
    public void closeAll() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                close(player, false);
            } else {
                GuiSession session = sessions.remove(id);
                packets.close(id);
                session.seat.remove();
                notifyClosed(session.screen, session.player, false);
            }
        }
    }

    /**
     * Zamyka ekran. {@code toChest}: gracz wybrał „Tryb skrzyni”, więc po powrocie na miejsce (następny tick)
     * dostaje to samo okno jako skrzynię. Teleport zamyka otwarte okno, dlatego skrzynia dopiero po nim.
     */
    private void close(Player player, boolean toChest) {
        GuiSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        packets.close(player.getUniqueId());
        if (session.framesSpawned && player.isOnline()) {
            FakeFrames.destroy(player, session.entityIds);
            ClientView.show(player);
            ClientView.sendTime(player);
        }
        session.seat.eject();
        session.seat.remove();
        GuiScreen screen = session.screen;
        if (!toChest) {
            notifyClosed(screen, player, false);
        }
        // Zsiadanie wybiera „bezpieczne” miejsce koło stojaka, czasem po drugiej stronie ściany. W następnym ticku
        // gracz wraca tam, gdzie otworzył ekran, chyba że coś innego (/tp, śmierć, zmiana świata) już go zabrało.
        Runnable restore = () -> {
            boolean here = player.isOnline() && !player.isDead() && !player.isInsideVehicle();
            if (here) {
                Location now = player.getLocation();
                if (now.getWorld() != null && now.getWorld().getName().equals(session.world)
                        && now.distanceSquared(session.origin) <= RESTORE_DISTANCE * RESTORE_DISTANCE) {
                    player.teleport(session.origin);
                }
            }
            if (toChest) {
                boolean chest = here && plugin.isEnabled();
                notifyClosed(screen, player, chest);
                if (chest) {
                    try {
                        screen.openChest(player);
                    } catch (RuntimeException e) {
                        plugin.getLogger().log(Level.WARNING, "GUI: nie udało się otworzyć okna skrzyni", e);
                    }
                }
            }
        };
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, restore);
        } else {
            restore.run(); // plugin się wyłącza i nie może już planować zadań
        }
    }

    private void notifyClosed(GuiScreen screen, Player player, boolean toChest) {
        try {
            screen.closed(player, toChest);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "GUI: błąd przy zamykaniu ekranu", e);
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isStandingOnGround(Player player) {
        return player.isOnGround() && !player.isFlying() && !player.isGliding() && !player.isSwimming();
    }

    // ------------------------------------------------------------------ rysowanie

    private void tick() {
        for (GuiSession session : new ArrayList<>(sessions.values())) {
            Player player = Bukkit.getPlayer(session.playerId);
            if (player == null || !player.isOnline()) {
                sessions.remove(session.playerId);
                packets.close(session.playerId);
                session.seat.remove();
                notifyClosed(session.screen, session.player, false);
                continue;
            }
            keepCursorInside(player);
            double[] c = cursor(player);
            GuiCanvas.Hit hit = hitAt(session, c[0], c[1]);
            String key = hit == null ? null : hit.key();
            boolean dirty = !Objects.equals(key, session.hover);
            session.hover = key;
            GuiScreen screen = session.screen;
            try {
                dirty |= screen.tick();
            } catch (RuntimeException e) {
                failed(player, e);
                continue;
            }
            dirty |= screen.takeDirty();
            if (dirty && sessions.get(session.playerId) == session && session.screen == screen) {
                render(session, player);
            }
        }
    }

    private void render(GuiSession session, Player player) {
        double[] c = cursor(player);
        GuiCanvas canvas = new GuiCanvas(c[0], c[1]);
        GuiScreen screen = session.screen;
        try {
            screen.takeDirty();
            screen.render(canvas, right -> close(player, false), right -> close(player, true));
        } catch (RuntimeException e) {
            failed(player, e);
            return;
        }
        canvas.writeMarkers();
        byte[] old = session.pixels;
        byte[] pixels = canvas.pixels();
        session.pixels = pixels;
        session.hits = canvas.hits();
        if (!session.framesSpawned) {
            return; // ramki wyślą wszystkie mapy, gdy staną
        }
        // tylko kafelki, które się zmieniły (np. podświetlony przycisk), żeby nie słać 15 map przy każdym ruchu myszy
        for (int tile = 0; tile < GuiLayout.TILES; tile++) {
            if (old == null || tileChanged(old, pixels, tile)) {
                player.sendMap(views.get(tile));
            }
        }
    }

    private static boolean tileChanged(byte[] a, byte[] b, int tile) {
        int offsetX = (tile % GuiLayout.COLS) * GuiLayout.TILE;
        int offsetY = (tile / GuiLayout.COLS) * GuiLayout.TILE;
        for (int y = 0; y < GuiLayout.TILE; y++) {
            int from = (offsetY + y) * GuiLayout.WIDTH + offsetX;
            if (!java.util.Arrays.equals(a, from, from + GuiLayout.TILE, b, from, from + GuiLayout.TILE)) {
                return true;
            }
        }
        return false;
    }

    /** Błąd w ekranie: zamykamy go, żeby gracz nie został przyklejony do zepsutego obrazu. */
    private void failed(Player player, RuntimeException e) {
        if (!warnedRender) {
            warnedRender = true;
            plugin.getLogger().log(Level.WARNING, "GUI: błąd ekranu, zamykam go (dalej bez pełnego śladu)", e);
        } else {
            plugin.getLogger().warning("GUI: błąd ekranu: " + e);
        }
        close(player, false);
        player.sendMessage("§cOkno się zacięło. Spróbuj jeszcze raz albo wpisz §f/oaza gui§c, żeby używać skrzyni.");
    }

    private static GuiCanvas.Hit hitAt(GuiSession session, double x, double y) {
        List<GuiCanvas.Hit> hits = session.hits;
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) {
                return hits.get(i);
            }
        }
        return null;
    }

    /** Kursor w pikselach ekranu, z obrotu głowy gracza. */
    private static double[] cursor(Player player) {
        Location look = player.getLocation();
        double u = clamp01(0.5 + Location.normalizeYaw(look.getYaw()) / GuiLayout.CURSOR_YAW_RANGE);
        double v = clamp01(0.5 + look.getPitch() / GuiLayout.CURSOR_PITCH_RANGE);
        return new double[]{u * GuiLayout.WIDTH, v * GuiLayout.HEIGHT};
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Kursor zatrzymuje się na krawędzi ekranu, ale głowa obraca się dalej. Obracamy ją z powrotem do krawędzi,
     * żeby ruch myszy w drugą stronę od razu ruszał kursor.
     */
    private static void keepCursorInside(Player player) {
        Location look = player.getLocation();
        float yaw = Location.normalizeYaw(look.getYaw());
        float pitch = look.getPitch();
        float maxYaw = (float) (GuiLayout.CURSOR_YAW_RANGE / 2.0);
        float maxPitch = (float) (GuiLayout.CURSOR_PITCH_RANGE / 2.0);
        if (Math.abs(yaw) > maxYaw + 0.5f || Math.abs(pitch) > maxPitch + 0.5f) {
            ClientView.rotate(player, Math.max(-maxYaw, Math.min(maxYaw, yaw)),
                    Math.max(-maxPitch, Math.min(maxPitch, pitch)));
        }
    }

    /** Kliknięcie z wątku sieci (już w wątku serwera): to, co jest pod kursorem na ostatnim obrazie. */
    void click(Player player, boolean right) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        double[] c = cursor(player);
        GuiCanvas.Hit hit = hitAt(session, c[0], c[1]);
        if (hit == null) {
            return;
        }
        GuiScreen screen = session.screen;
        try {
            hit.action().run(right);
        } catch (RuntimeException e) {
            failed(player, e);
            return;
        }
        screen.redraw();
    }

    // ------------------------------------------------------------------ zdarzenia

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        GuiSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        int diff = (event.getNewSlot() - event.getPreviousSlot() + 9) % 9;
        if (diff == 0) {
            return;
        }
        try {
            session.screen.scroll(diff <= 4 ? 1 : -1);
        } catch (RuntimeException e) {
            failed(event.getPlayer(), e);
            return;
        }
        session.screen.redraw();
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            GuiSession session = sessions.get(player.getUniqueId());
            if (session != null && event.getDismounted().equals(session.seat)) {
                close(player, false);
            }
        }
    }

    /** Przy otwartym ekranie kliknięcia nie mogą kopać, używać przedmiotów ani bić. */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (isOpen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isOpen(player)) {
            event.setCancelled(true);
        }
    }

    /** Hotbar jest schowany, więc Q i F nie mogą po cichu wyrzucać ani zamieniać przedmiotów. */
    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (isOpen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isOpen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isOpen(player)) {
            close(player, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        close(event.getEntity(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        close(event.getPlayer(), false);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        close(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isOpen(event.getPlayer()) && event.getCause() != PlayerTeleportEvent.TeleportCause.DISMOUNT) {
            close(event.getPlayer(), false);
        }
    }
}
