package pl.axon.structure;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.axon.structure.command.StrukturyCommand;
import pl.axon.structure.loot.AirdropService;
import pl.axon.structure.loot.CrateService;
import pl.axon.structure.loot.LootConfig;
import pl.axon.structure.protect.ProtectionListener;
import pl.axon.structure.protect.ProtectionService;
import pl.axon.structure.world.AutoGenerator;
import pl.axon.structure.world.InstanceStore;
import pl.axon.structure.world.PasteService;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AxonStructurePlugin extends JavaPlugin {

    private StructureLibrary library;
    private InstanceStore store;
    private CrateService crates;
    private AirdropService airdrops;
    private PasteService paster;
    private AutoGenerator autoGenerator;
    private ProtectionService protection;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        library = new StructureLibrary(new File(getDataFolder(), "struktury"), getLogger());
        store = new InstanceStore(new File(getDataFolder(), "dane.yml"), getLogger());
        crates = new CrateService(this, library, store);
        airdrops = new AirdropService(this, library, store, crates);
        protection = new ProtectionService(this, library, store);
        paster = new PasteService(this, store, crates, protection);
        autoGenerator = new AutoGenerator(this, library, store, paster, protection);

        reload();
        store.load();
        protection.hookWorldGuard();
        protection.syncAll();
        crates.start();
        airdrops.start();
        getServer().getPluginManager().registerEvents(autoGenerator, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(protection), this);

        StrukturyCommand command = new StrukturyCommand(this);
        PluginCommand pluginCommand = getCommand("struktury");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getLogger().info("Struktur: " + library.all().size() + ", postawionych: " + store.all().size()
                + ", klas skrzyń: " + crates.loot().classes().size());
    }

    @Override
    public void onDisable() {
        if (airdrops != null) {
            airdrops.stop();
        }
        if (crates != null) {
            crates.stop();
        }
        if (store != null) {
            store.save();
        }
    }

    /** Wczytuje config.yml i pliki struktur od nowa. */
    public void reload() {
        reloadConfig();
        FileConfiguration config = getConfig();
        library.reload();

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
                config.getBoolean("odswiezanie.zastepuj-pelne", false)));

        airdrops.configure(new AirdropService.Settings(
                config.getBoolean("zrzuty.wlaczone", true),
                config.getInt("zrzuty.co-ile-minut-od", 45),
                config.getInt("zrzuty.co-ile-minut-do", 90),
                Math.max(0, config.getInt("zrzuty.min-graczy", 2)),
                config.getString("zrzuty.pula", "zrzut").toLowerCase(Locale.ROOT),
                config.getInt("zrzuty.znika-po-minutach", 15),
                config.getInt("zrzuty.wysokosc-spadania", 40),
                config.getInt("zrzuty.czas-spadania-sekund", 10),
                config.getString("zrzuty.nazwa-skrzyni", "&4&lZrzut zaopatrzenia"),
                config.getString("zrzuty.komunikat", ""),
                config.getString("zrzuty.komunikat-wyladowal", ""),
                config.getString("zrzuty.komunikat-zniknal", "")));

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
                weights));
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

    public AirdropService airdrops() {
        return airdrops;
    }

    public PasteService paster() {
        return paster;
    }

    public ProtectionService protection() {
        return protection;
    }

    public AutoGenerator autoGenerator() {
        return autoGenerator;
    }
}
