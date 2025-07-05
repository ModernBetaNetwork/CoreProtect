package net.coreprotect.utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import net.coreprotect.CoreProtect;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.language.Phrase;
import net.coreprotect.model.BlockGroup;
import net.coreprotect.paper.PaperAdapter;
import net.coreprotect.thread.Scheduler;
import net.coreprotect.utility.BlockUtils;

public class Teleport {

    private Teleport() {
        throw new IllegalStateException("Utility class");
    }

    public static ConcurrentHashMap<Location, BlockData> revertBlocks = new ConcurrentHashMap<>();

    public static void performSafeTeleport(Player player, Location location, boolean enforceTeleport) {
        // use admin tool box's admin mode for teleports
        player.performCommand("admin " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + " " + location.getWorld().getName());
    }

}
