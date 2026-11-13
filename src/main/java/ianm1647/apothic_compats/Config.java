package ianm1647.apothic_compats;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static ForgeConfigSpec STARTUP_CONFIG;
    public static ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_CURIOS;

    static {
        ForgeConfigSpec.Builder STARTUP_BUILDER = new ForgeConfigSpec.Builder();
        STARTUP_BUILDER.push("Curios");
        ENABLE_CUSTOM_CURIOS = STARTUP_BUILDER.comment("Should custom loot curios be enabled?").define("enableCustomCurios", true);
        STARTUP_BUILDER.pop();
        STARTUP_CONFIG = STARTUP_BUILDER.build();
    }
}
