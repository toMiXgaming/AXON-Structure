package pl.axon.structure.npc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Resolve the bundled PNG once, then reuse the signed property on subsequent starts. */
final class BundledSkin {
    String value = "";
    String signature = "";
    boolean loading;

    void start(JavaPlugin plugin) {
        String path = "skins/zlomiarz.png";
        File image = new File(plugin.getDataFolder(), path);
        if (!image.exists()) plugin.saveResource(path, false);
        var signer = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        File cache = new File(image.getParentFile(), "zlomiarz.properties");
        loading = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Properties data = new Properties();
            try {
                String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(image.toPath())));
                if (cache.isFile()) try (var in = Files.newInputStream(cache.toPath())) { data.load(in); }
                if (!hash.equals(data.getProperty("hash")) || data.getProperty("signature", "").isBlank()
                        || data.getProperty("value", "").isBlank()) {
                    data.clear();
                    if (signer == null) throw new IllegalStateException("Potrzebny SkinsRestorer lub podpis w zestawie");
                    ClassLoader loader = signer.getClass().getClassLoader();
                    Class<?> provider = loader.loadClass("net.skinsrestorer.api.SkinsRestorerProvider");
                    Object api = provider.getMethod("get").invoke(null);
                    Object service = loader.loadClass("net.skinsrestorer.api.SkinsRestorer")
                            .getMethod("getMineSkinAPI").invoke(api);
                    Class<?> mineSkin = loader.loadClass("net.skinsrestorer.api.connections.MineSkinAPI");
                    var generate = java.util.Arrays.stream(mineSkin.getMethods()).filter(m -> m.getName().equals("genSkin")
                            && m.getParameterCount() == 2 && m.getParameterTypes()[0] == File.class).findFirst().orElseThrow();
                    Object response = generate.invoke(service, image, null);
                    Object skin = response.getClass().getMethod("getProperty").invoke(response);
                    Class<?> property = loader.loadClass("net.skinsrestorer.api.property.SkinProperty");
                    data.setProperty("value", (String) property.getMethod("getValue").invoke(skin));
                    data.setProperty("signature", (String) property.getMethod("getSignature").invoke(skin));
                    data.setProperty("hash", hash);
                    var tmp = Files.createTempFile(cache.getParentFile().toPath(), "zlomiarz-", ".tmp");
                    try {
                        try (var out = Files.newOutputStream(tmp)) { data.store(out, null); }
                        Files.move(tmp, cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(tmp); }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Skin złomiarza: " + e);
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    value = data.getProperty("value", "");
                    signature = data.getProperty("signature", "");
                    loading = false;
                });
            }
        });
    }
}
