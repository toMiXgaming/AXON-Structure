package pl.axon.structure.protect;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.loot.LootConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gracze nie mogą niszczyć ani rozbudowywać struktur. Wolno otwierać skrzynie, drzwi i przyciski oraz
 * zabierać przedmioty z ramek (karty dostępu). PvP nie jest tu w ogóle ruszane.
 * Priorytet LOWEST: zdarzenie jest anulowane, zanim zobaczą je inne pluginy (np. rdzeń działki AxonPlot,
 * który z ignoreCancelled nie założy wtedy działki).
 */
public final class ProtectionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 2000;

    private final ProtectionService protection;
    private final Map<UUID, Long> lastMessage = new HashMap<>();

    public ProtectionListener(ProtectionService protection) {
        this.protection = protection;
    }

    private boolean denied(Player player, Block block) {
        if (player != null && player.hasPermission(ProtectionService.BYPASS_PERMISSION)) {
            return false;
        }
        return protection.protectedAt(block) != null;
    }

    private void warn(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastMessage.get(player.getUniqueId());
        if (last == null || now - last > MESSAGE_COOLDOWN_MS) {
            lastMessage.put(player.getUniqueId(), now);
            player.sendMessage(LootConfig.color(protection.settings().message()));
        }
    }

    // --- gracze ---

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        boolean blocked = denied(event.getPlayer(), event.getBlockPlaced());
        if (!blocked && event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                if (denied(event.getPlayer(), state.getBlock())) {
                    blocked = true;
                    break;
                }
            }
        }
        if (blocked) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            if (event.getPlayer() != null) {
                warn(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !denied(event.getPlayer(), block)) {
            return;
        }
        if (event.getAction() == Action.PHYSICAL) {
            if (block.getType() == Material.FARMLAND || block.getType() == Material.TURTLE_EGG) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        String type = block.getType().name();
        // Edycja i farbowanie tabliczek, wyjmowanie roślin z doniczek.
        if (type.endsWith("_SIGN") || type.startsWith("POTTED_") || type.equals("FLOWER_POT")) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
        // Ociosywanie pni, ścieżki, oranie, woskowanie i zdrapywanie miedzi. Skrzynie i drzwi dalej się otwierają.
        ItemStack item = event.getItem();
        if (item != null && changesBlocks(item.getType())) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    private static boolean changesBlocks(Material material) {
        String name = material.name();
        return name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || material == Material.HONEYCOMB || material == Material.BONE_MEAL
                || material == Material.FLINT_AND_STEEL || material == Material.FIRE_CHARGE;
    }

    // --- ramki i obrazy ---

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Player player = null;
        if (event instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() instanceof Player p) {
            player = p;
        }
        if (denied(player, event.getEntity().getLocation().getBlock())) {
            event.setCancelled(true);
            if (player != null) {
                warn(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (denied(event.getPlayer(), event.getBlock().getRelative(event.getBlockFace()))) {
            event.setCancelled(true);
            if (event.getPlayer() != null) {
                warn(event.getPlayer());
            }
        }
    }

    /** Wkładanie przedmiotów do ramek i obracanie ich. Zabieranie (uderzenie) zostaje dozwolone. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Hanging && denied(event.getPlayer(), entity.getLocation().getBlock())) {
            event.setCancelled(true);
        }
    }

    // --- świat ---

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeProtected(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeProtected(event.blockList());
    }

    private void removeProtected(List<Block> blocks) {
        blocks.removeIf(block -> protection.protectedAt(block) != null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Player player = event.getEntity() instanceof Player p ? p : null;
        if (denied(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (protection.protectedAt(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (denied(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Woda i lawa wlewające się z zewnątrz. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        StructureInstance target = protection.protectedAt(event.getToBlock());
        if (target != null && protection.protectedAt(event.getBlock()) != target) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (pistonTouchesStructure(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (pistonTouchesStructure(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    private boolean pistonTouchesStructure(Block piston, List<Block> moved, BlockFace direction) {
        StructureInstance own = protection.protectedAt(piston);
        if (differs(own, protection.protectedAt(piston.getRelative(direction)))) {
            return true;
        }
        for (Block block : moved) {
            if (differs(own, protection.protectedAt(block))
                    || differs(own, protection.protectedAt(block.getRelative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean differs(StructureInstance own, StructureInstance other) {
        return other != null && other != own;
    }
}
