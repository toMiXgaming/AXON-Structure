package pl.axon.structure;

import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import pl.axon.structure.api.AxonStructureApi;
import pl.axon.structure.command.StrukturyCommand;
import pl.axon.structure.gui.ShaderGui;
import pl.axon.structure.loot.AirdropService;
import pl.axon.structure.loot.CrateService;
import pl.axon.structure.loot.LootConfig;
import pl.axon.structure.npc.GuardService;
import pl.axon.structure.oasis.OasisService;
import pl.axon.structure.command.OazaCommand;
import pl.axon.structure.life.AmbientService;
import pl.axon.structure.life.AnimalService;
import pl.axon.structure.protect.ProtectionListener;
import pl.axon.structure.protect.ProtectionService;
import pl.axon.structure.world.AutoGenerator;
import pl.axon.structure.world.InstanceStore;
import pl.axon.structure.world.OreVeinService;
import pl.axon.structure.world.PasteService;
import pl.axon.structure.world.PropService;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AxonStructurePlugin extends JavaPlugin {

    private StructureLibrary library;
    private InstanceStore store;
    private CrateService crates;
    private AirdropService airdrops;
    private GuardService guards;
    private OasisService oasis;
    private ShaderGui gui;
    private PasteService paster;
    private PropService props;
    private AutoGenerator autoGenerator;
    private ProtectionService protection;
    private OreVeinService veins;
    private AnimalService animals;
    private AmbientService ambient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        library = new StructureLibrary(new File(getDataFolder(), "struktury"), getLogger());
        store = new InstanceStore(new File(getDataFolder(), "dane.yml"), getLogger());
        crates = new CrateService(this, library, store);
        airdrops = new AirdropService(this, library, store, crates);
        guards = new GuardService(this, library, store, crates);
        oasis = new OasisService(this, library, store, guards, airdrops);
        gui = new ShaderGui(this);
        oasis.gui(gui);
        protection = new ProtectionService(this, library, store);
        veins = new OreVeinService(this, library, store);
        protection.veins(veins);
        animals = new AnimalService(this, library, store, oasis);
        ambient = new AmbientService(this, store);
        props = new PropService(this);
        paster = new PasteService(this, store, crates, protection, props);
        autoGenerator = new AutoGenerator(this, library, store, paster, protection);

        reload();
        store.load();
        // Przy load: STARTUP świat i pluginy POSTWORLD (WorldGuard, Citizens, QualityArmory, PacketEvents…)
        // jeszcze nie działają. Usługi startują na pierwszym ticku, gdy wszystko już jest włączone.
        getServer().getScheduler().runTask(this, this::startServices);
        getServer().getServicesManager().register(AxonStructureApi.class, new AxonStructureApiImpl(this), this,
                ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(guards, this);
        getServer().getPluginManager().registerEvents(veins, this);
        getServer().getPluginManager().registerEvents(animals, this);
        getServer().getPluginManager().registerEvents(oasis, this);
        getServer().getPluginManager().registerEvents(autoGenerator, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(protection), this);
        getServer().getPluginManager().registerEvents(props, this);

        StrukturyCommand command = new StrukturyCommand(this);
        PluginCommand pluginCommand = getCommand("struktury");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        OazaCommand oazaCommand = new OazaCommand(this);
        PluginCommand oaza = getCommand("oaza");
        if (oaza != null) {
            oaza.setExecutor(oazaCommand);
            oaza.setTabCompleter(oazaCommand);
        }
        getLogger().info("Struktur: " + library.all().size() + ", postawionych: " + store.all().size()
                + ", klas skrzyń: " + crates.loot().classes().size());
    }

    /** Start usług, które dotykają światów albo soft-zależności. Wywoływane z pierwszego ticka serwera. */
    private void startServices() {
        protection.hookWorldGuard();
        protection.syncAll();
        crates.start();
        airdrops.start();
        guards.start();
        oasis.start();
        gui.start();
        veins.start();
        animals.start();
        ambient.start();
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        // każda usługa osobno: błąd jednej nie może zablokować zapisu reszty (depozyty, wymiany, dane.yml)
        safely("odgłosy", ambient == null ? null : ambient::stop);
        safely("zwierzęta", animals == null ? null : animals::stop);
        safely("kopalnia", veins == null ? null : veins::stop);
        safely("oaza", oasis == null ? null : oasis::stop);
        // po oazie: wymiany już oddały towar, zostaje zsadzić graczy sprzed ekranów
        safely("gui", gui == null ? null : gui::stop);
        safely("strażnicy", guards == null ? null : guards::stop);
        safely("zrzuty", airdrops == null ? null : airdrops::stop);
        safely("skrzynie", crates == null ? null : crates::stop);
        safely("dane.yml", store == null ? null : store::save);
    }

    private void safely(String what, Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Błąd przy wyłączaniu (" + what + ")", e);
        }
    }

    /**
     * Dopisuje do config.yml na serwerze ustawienia, których w nim nie ma, a są w domyślnym configu z nowszej
     * wersji pluginu (np. nowe zestawy strażników albo sekcje oazy). Istniejących wartości nie zmienia.
     */
    private void addMissingDefaults(FileConfiguration config) {
        org.bukkit.configuration.Configuration defaults = config.getDefaults();
        if (defaults == null) {
            return;
        }
        // Tylko całe brakujące sekcje główne i całe nowe zestawy strażników. Nie scalamy głębiej: admin mógł
        // celowo usunąć wpis z mapy (np. rudę z kopalnia.rudy albo odgłos), a copyDefaults dopisałby go z powrotem.
        java.util.List<String> added = new java.util.ArrayList<>();
        for (String key : defaults.getKeys(false)) {
            if (!config.isSet(key)) {
                config.set(key, defaults.get(key));
                added.add(key);
            }
        }
        ConfigurationSection defaultKits = defaults.getConfigurationSection("straznicy.zestawy");
        ConfigurationSection kits = config.getConfigurationSection("straznicy.zestawy");
        if (defaultKits != null && kits != null) {
            java.util.List<String> known = config.getStringList("straznicy.znane-zestawy");
            boolean knownChanged = false;
            for (String kit : defaultKits.getKeys(false)) {
                if (!known.contains(kit)) {
                    // zestaw nowy w tej wersji pluginu: dopisujemy raz; usunięty później przez admina nie wróci
                    if (!kits.isSet(kit)) {
                        kits.set(kit, defaultKits.get(kit));
                        added.add("straznicy.zestawy." + kit);
                    }
                    known.add(kit);
                    knownChanged = true;
                }
            }
            if (knownChanged) {
                config.set("straznicy.znane-zestawy", known);
            }
            if (knownChanged && added.isEmpty()) {
                saveConfig();
            }
        }
        if (added.isEmpty()) {
            return;
        }
        saveConfig();
        getLogger().info("Uzupełniono config.yml o nowe ustawienia (" + added.size() + "): " + String.join(", ", added));
    }

    /** Wczytuje config.yml i pliki struktur od nowa. */
    public void reload() {
        reloadConfig();
        FileConfiguration config = getConfig();
        addMissingDefaults(config);
        library.reload();

        // przed ochroną: regiony WorldGuard kopalni zależą od tego, czy żyły rudy są włączone
        configureVeins(config);

        Map<String, String> flags = new LinkedHashMap<>();
        ConfigurationSection flagSection = config.getConfigurationSection("ochrona.flagi-worldguard");
        if (flagSection != null) {
            for (String key : flagSection.getKeys(false)) {
                flags.put(key.toLowerCase(Locale.ROOT), String.valueOf(flagSection.get(key)));
            }
        }
        boolean firstLoad = protection.settings() == null;
        protection.configure(new ProtectionService.Settings(
                config.getBoolean("ochrona.wlaczona", true),
                Math.max(0, config.getInt("ochrona.margines", 2)),
                Math.max(0, config.getInt("ochrona.odstep-od-dzialek", 50)),
                Math.max(0, config.getInt("ochrona.promien-dzialki", 25)),
                config.getString("ochrona.komunikat", "&cTej struktury nie można niszczyć ani rozbudowywać."),
                flags));
        if (!firstLoad) {
            protection.syncAll();
        }

        crates.configure(LootConfig.load(config, getLogger()), new CrateService.Settings(
                Math.max(1, config.getLong("odswiezanie.co-ile-minut", 20)) * 60_000L,
                config.getInt("odswiezanie.blokada-gdy-gracz-blizej-niz", 32),
                config.getBoolean("odswiezanie.zastepuj-pelne", false),
                config.getBoolean("loot.puste", true)));

        airdrops.configure(new AirdropService.Settings(
                config.getBoolean("zrzuty.wlaczone", true),
                Math.max(1, config.getInt("zrzuty.dziennie", 2)),
                config.getInt("zrzuty.godziny-od", 12),
                config.getInt("zrzuty.godziny-do", 22),
                zone(config.getString("zrzuty.strefa-czasowa", "Europe/Warsaw")),
                Math.max(0, config.getInt("zrzuty.min-graczy", 2)),
                config.getString("zrzuty.pula", "zrzut").toLowerCase(Locale.ROOT),
                config.getInt("zrzuty.znika-po-minutach", 15),
                config.getInt("zrzuty.wysokosc-spadania", 60),
                config.getInt("zrzuty.czas-spadania-sekund", 45),
                config.getString("zrzuty.nazwa-skrzyni", "&4&lZrzut zaopatrzenia"),
                config.getString("zrzuty.komunikat", ""),
                config.getString("zrzuty.komunikat-wyladowal", ""),
                config.getString("zrzuty.komunikat-zniknal", ""),
                config.getBoolean("zrzuty.dym-sygnalowy", true),
                Math.max(0, config.getInt("zrzuty.flara-co-sekund", 5)),
                config.getBoolean("zrzuty.swiecenie", true)));

        Map<String, GuardService.Kit> kits = new LinkedHashMap<>();
        ConfigurationSection kitSection = config.getConfigurationSection("straznicy.zestawy");
        if (kitSection != null) {
            for (String key : kitSection.getKeys(false)) {
                ConfigurationSection k = kitSection.getConfigurationSection(key);
                if (k == null) {
                    continue;
                }
                String id = key.toLowerCase(Locale.ROOT);
                kits.put(id, new GuardService.Kit(id,
                        k.getString("nazwa", key),
                        k.getString("skin-tekstura", ""),
                        k.getString("skin-podpis", ""),
                        k.getString("skin-gracza", ""),
                        k.getString("bron", "crossbow"),
                        k.getStringList("zbroja"),
                        k.getDouble("zycie", 20),
                        k.getDouble("obrazenia", -1),
                        k.getDouble("zasieg", 20),
                        k.getDouble("celnosc", 0),
                        k.getString("loot", ""),
                        k.isList("cele") ? k.getStringList("cele") : java.util.List.of("players"),
                        // handlarze stoją za ladą; reszta domyślnie chodzi po okolicy
                        k.getBoolean("chodzi", !id.equals(GuardService.TRADER_KIT) && !id.equals(GuardService.FENCE_KIT)
                                && !k.getBoolean("spi", false)),
                        Math.max(1, Math.min(32, k.getInt("promien-chodzenia", 8))),
                        k.getBoolean("uprawia", false),
                        k.getBoolean("spi", false)));
            }
        }
        guards.configure(new GuardService.Settings(
                config.getBoolean("straznicy.wlaczeni", true),
                Math.max(16, config.getInt("straznicy.promien-aktywacji", 64)),
                Math.max(1, config.getInt("straznicy.odradzanie-minut", 10)),
                kits));

        oasis.configure(config.getConfigurationSection("oaza"), zone(config.getString("zrzuty.strefa-czasowa", "Europe/Warsaw")));
        oasis.configureBlackMarket(config.getConfigurationSection("czarny-rynek"));
        gui.configure(config.getConfigurationSection("gui"));

        animals.configure(new AnimalService.Settings(
                config.getBoolean("zwierzeta.wlaczone", true),
                Math.max(16, config.getInt("zwierzeta.promien-aktywacji", 64)),
                Math.max(1, config.getInt("zwierzeta.odradzanie-minut", 10)),
                Math.max(0, config.getInt("zwierzeta.maks-odejscie", 24))));

        Map<String, List<AmbientService.SoundSpec>> sounds = new LinkedHashMap<>();
        ConfigurationSection soundSection = config.getConfigurationSection("zycie.odglosy");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                List<AmbientService.SoundSpec> list = new ArrayList<>();
                for (String line : soundSection.getStringList(key)) {
                    AmbientService.SoundSpec spec = AmbientService.SoundSpec.parse(line);
                    if (spec == null) {
                        getLogger().warning("zycie.odglosy." + key + ": zły wpis '" + line + "' (nazwa [wysokość] [głośność])");
                    } else {
                        list.add(spec);
                    }
                }
                sounds.put(key.toLowerCase(Locale.ROOT), list);
            }
        }
        ambient.configure(new AmbientService.Settings(
                config.getBoolean("zycie.wlaczone", true),
                Math.max(2, config.getInt("zycie.co-ile-sekund", 12)),
                Math.max(0, config.getDouble("zycie.szansa-procent", 35)),
                (float) Math.max(0.05, config.getDouble("zycie.glosnosc", 0.6)),
                sounds));

        Set<String> skipFurniture = new HashSet<>();
        for (String id : config.getStringList("meble.bez-zamiany")) {
            skipFurniture.add(id.toLowerCase(Locale.ROOT));
        }
        props.configure(new PropService.Settings(config.getString("meble.lozko", ""),
                blockModels(config, "meble.bloki"), blockModels(config, "meble.drzwi"), skipFurniture));

        paster.configure(new PasteService.Settings(
                Math.max(100, config.getInt("stawianie.bloki-na-tick", 4000)),
                Math.max(0, config.getInt("stawianie.wygladzanie-krawedzi", 6)),
                Math.max(0, config.getInt("stawianie.maks-fundament", 24)),
                Math.max(0, config.getInt("stawianie.czyszczenie-nad", 24))));

        Map<String, Integer> weights = new LinkedHashMap<>();
        ConfigurationSection weightSection = config.getConfigurationSection("auto-generowanie.struktury");
        if (weightSection != null) {
            for (String key : weightSection.getKeys(false)) {
                weights.put(key.toLowerCase(Locale.ROOT), weightSection.getInt(key));
            }
        }
        autoGenerator.configure(new AutoGenerator.Settings(
                config.getBoolean("auto-generowanie.wlaczone", true),
                new HashSet<>(config.getStringList("auto-generowanie.swiaty")),
                config.getDouble("auto-generowanie.szansa-procent", 0.5),
                config.getInt("auto-generowanie.min-odleglosc-miedzy", 1000),
                config.getInt("auto-generowanie.min-odleglosc-od-spawnu", 300),
                config.getInt("auto-generowanie.maks-roznica-wysokosci", 6),
                config.getDouble("auto-generowanie.maks-woda-procent", 3),
                weights,
                Math.max(0, config.getLong("auto-generowanie.maks-czas-graczy-w-chunku", 1200))));
    }

    private void configureVeins(FileConfiguration config) {
        Map<OreVeinService.Ore, Integer> weights = new EnumMap<>(OreVeinService.Ore.class);
        ConfigurationSection oreSection = config.getConfigurationSection("kopalnia.rudy");
        if (oreSection != null) {
            for (String key : oreSection.getKeys(false)) {
                OreVeinService.Ore ore = OreVeinService.Ore.parse(key);
                if (ore == null) {
                    getLogger().warning("kopalnia.rudy: nieznana ruda '" + key
                            + "' (coal, iron, copper, gold, redstone, lapis, diamond, emerald)");
                } else {
                    weights.put(ore, Math.max(0, oreSection.getInt(key)));
                }
            }
        }
        veins.configure(new OreVeinService.Settings(
                config.getBoolean("kopalnia.wlaczone", true),
                Math.max(1, config.getLong("kopalnia.odnawianie-minut", 15)) * 60_000L,
                OreVeinService.Leftover.parse(config.getString("kopalnia.po-wykopaniu", "kamien")),
                Math.max(0, config.getInt("kopalnia.blokada-gdy-gracz-blizej-niz", 3)),
                config.getBoolean("kopalnia.doswiadczenie", true),
                weights,
                config.getBoolean("kopalnia.siarka.wlaczona", true),
                config.getString("kopalnia.siarka.nazwa", "<#FDE047>&lSurowa Siarka"),
                config.getStringList("kopalnia.siarka.opis"),
                config.getInt("kopalnia.siarka.custom-model-data", 10001),
                config.getInt("kopalnia.siarka.min", 1),
                config.getInt("kopalnia.siarka.max", 2)));
    }

    public StructureLibrary library() {
        return library;
    }

    public InstanceStore store() {
        return store;
    }

    public CrateService crates() {
        return crates;
    }

    private java.time.ZoneId zone(String id) {
        try {
            return java.time.ZoneId.of(id);
        } catch (RuntimeException e) {
            getLogger().warning("Zła strefa czasowa '" + id + "', używam strefy serwera.");
            return java.time.ZoneId.systemDefault();
        }
    }

    public GuardService guards() {
        return guards;
    }

    public ShaderGui gui() {
        return gui;
    }

    public OasisService oasis() {
        return oasis;
    }

    public AirdropService airdrops() {
        return airdrops;
    }

    public PasteService paster() {
        return paster;
    }

    public OreVeinService veins() {
        return veins;
    }

    public AnimalService animals() {
        return animals;
    }

    public AmbientService ambient() {
        return ambient;
    }

    public ProtectionService protection() {
        return protection;
    }

    public AutoGenerator autoGenerator() {
        return autoGenerator;
    }

    /** Sekcja {@code blok: "model"} z configu. */
    private Map<Material, String> blockModels(FileConfiguration config, String path) {
        Map<Material, String> models = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                String model = section.getString(key, "");
                if (material == null || !material.isBlock()) {
                    getLogger().warning(path + ": nieznany blok '" + key + "', pomijam");
                } else if (!model.isBlank()) {
                    models.put(material, model);
                }
            }
        }
        return models;
    }
}
