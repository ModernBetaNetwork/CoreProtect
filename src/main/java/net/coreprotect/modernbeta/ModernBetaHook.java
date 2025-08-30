package net.coreprotect.modernbeta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import net.coreprotect.config.Config;
import net.coreprotect.consumer.Queue;
import net.coreprotect.utility.BlockUtils;
import net.coreprotect.utility.EntityUtils;
import net.coreprotect.worldedit.WorldEditBlockState;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.modernbeta.plugins.betaify.Betaify;

import java.util.Locale;

public class ModernBetaHook {

    private static Betaify betaify = null;

    public static Betaify getInstance() {
        if (betaify != null) {
            return betaify;
        }

        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("ModernBeta-Betaify");
        if (!(plugin instanceof Betaify)) {
            return null;
        }

        betaify = Betaify.getInstance();
        return betaify;
    }
}
