package com.BEVL.maid_damage_fix;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;


@Mod(MaidDamageFix.MODID)
public class MaidDamageFix {

    public static final String MODID = "maid_damage_fix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidDamageFix() {
        LOGGER.info("[Maid Damage Fix] 枪械友伤判断已接管");
    }
}
