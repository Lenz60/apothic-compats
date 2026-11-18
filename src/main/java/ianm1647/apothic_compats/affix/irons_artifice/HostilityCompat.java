package ianm1647.apothic_compats.affix.irons_artifice;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Soft (reflection based) bridge to L2 Hostility 1.20.1 so this addon compiles
 * and runs
 * without L2 Hostility on the classpath.
 *
 * L2 Hostility 1.20.1 stores the player difficulty in the capability
 * {@code dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty},
 * reachable through
 * its static {@code HOLDER} (a PlayerCapabilityHolder):
 * {@code PlayerDifficulty.HOLDER.get(player)}.
 * The current difficulty level is then read with {@code getLevel()}.
 *
 * If L2 Hostility is missing, or a name differs in your version, this returns 0
 * (= minimum spell level).
 */
public final class HostilityCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("apothic_compats/hostility");
    private static final String MOD_ID = "l2hostility";
    private static final String CLASS_NAME = "dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty";

    private static boolean initialised = false;
    private static boolean available = false;
    private static Object holder;
    private static Method holderGet;

    private HostilityCompat() {
    }

    /**
     * @return the player's L2 Hostility difficulty level, or 0 if it cannot be
     *         read.
     */
    public static int getPlayerDifficulty(Player player) {
        if (player == null) {
            return 0;
        }
        init();
        if (!available) {
            return 0;
        }
        try {
            Object difficulty = holderGet.invoke(holder, player);
            return readLevel(difficulty);
        } catch (Throwable t) {
            LOGGER.warn("Could not read L2 Hostility player difficulty, using 0", t);
            available = false; // do not spam the log every shot
            return 0;
        }
    }

    private static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(CLASS_NAME);
            Field holderField = clazz.getField("HOLDER");
            holder = holderField.get(null);
            for (Method m : holder.getClass().getMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                    holderGet = m;
                    break;
                }
            }
            available = holder != null && holderGet != null;
            if (!available) {
                LOGGER.warn("L2 Hostility found, but PlayerDifficulty.HOLDER.get(Player) was not found");
            }
        } catch (Throwable t) {
            LOGGER.warn("L2 Hostility found, but PlayerDifficulty could not be resolved", t);
        }
    }

    /**
     * PlayerDifficulty#getLevel() may return an int or a DifficultyLevel-like
     * object that has its own getLevel().
     */
    private static int readLevel(Object difficulty) throws Exception {
        if (difficulty == null) {
            return 0;
        }
        Method getLevel = difficulty.getClass().getMethod("getLevel");
        Object result = getLevel.invoke(difficulty);
        if (result instanceof Number n) {
            return n.intValue();
        }
        if (result != null) {
            Object inner = result.getClass().getMethod("getLevel").invoke(result);
            if (inner instanceof Number n) {
                return n.intValue();
            }
        }
        return 0;
    }
}