package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Pieniądze z pluginu ekonomii przez Vault, wołane przez refleksję: bez Vaulta (albo bez pluginu ekonomii)
 * {@link #available()} zwraca false i płatne rzeczy w oazie grzecznie odmawiają.
 */
public final class Vault {

    private static final String ECONOMY = "net.milkbowl.vault.economy.Economy";

    private Object economy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        try {
            Class<?> type = Class.forName(ECONOMY);
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(type);
            return provider == null ? null : provider.getProvider();
        } catch (ClassNotFoundException | RuntimeException e) {
            return null;
        }
    }

    public boolean available() {
        return economy() != null;
    }

    public double balance(OfflinePlayer player) {
        Object eco = economy();
        if (eco == null) {
            return 0;
        }
        try {
            return ((Number) method(eco, "getBalance").invoke(eco, player)).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** Pobiera kwotę; false, gdy gracz ma za mało albo ekonomia nie działa. */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        Object eco = economy();
        if (eco == null) {
            return false;
        }
        try {
            if (!Boolean.TRUE.equals(method(eco, "has").invoke(eco, player, amount))) {
                return false;
            }
            return success(method(eco, "withdrawPlayer").invoke(eco, player, amount));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        Object eco = economy();
        if (eco == null) {
            return false;
        }
        try {
            return success(method(eco, "depositPlayer").invoke(eco, player, amount));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public String format(double amount) {
        Object eco = economy();
        if (eco != null) {
            try {
                Class<?> type = Class.forName(ECONOMY, false, eco.getClass().getClassLoader());
                return String.valueOf(type.getMethod("format", double.class).invoke(eco, amount));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // niżej zwykła liczba
            }
        }
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.format("%.2f", amount);
    }

    /**
     * Metoda interfejsu Economy z parametrami (OfflinePlayer[, double]). Z interfejsu, nie z klasy pluginu ekonomii,
     * bo ta bywa niepubliczna.
     */
    private static Method method(Object eco, String name) throws ReflectiveOperationException {
        Class<?> type = Class.forName(ECONOMY, false, eco.getClass().getClassLoader());
        try {
            return type.getMethod(name, OfflinePlayer.class, double.class);
        } catch (NoSuchMethodException e) {
            return type.getMethod(name, OfflinePlayer.class);
        }
    }

    private static boolean success(Object response) throws ReflectiveOperationException {
        if (response == null) {
            return false;
        }
        Method ok = response.getClass().getMethod("transactionSuccess");
        ok.setAccessible(true);
        return Boolean.TRUE.equals(ok.invoke(response));
    }
}
