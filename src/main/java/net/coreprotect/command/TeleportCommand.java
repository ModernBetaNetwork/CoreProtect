package net.coreprotect.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.coreprotect.CoreProtect;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.language.Phrase;
import net.coreprotect.thread.Scheduler;
import net.coreprotect.utility.Chat;
import net.coreprotect.utility.ChatMessage;
import net.coreprotect.utility.Color;
import net.coreprotect.utility.Teleport;
import net.coreprotect.utility.Util;
import net.coreprotect.utility.WorldUtils;

public class TeleportCommand {

    protected static void runCommand(CommandSender sender, boolean permission, String[] args) {
        int resultc = args.length;

        if (!permission) {
            Chat.sendMessage(sender, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.NO_PERMISSION));
            return;
        }

        if (!(sender instanceof Player)) {
            Chat.sendMessage(sender, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.TELEPORT_PLAYERS));
            return;
        }
        Player player = (Player) sender;

        if (ConfigHandler.teleportThrottle.get(sender.getName()) != null) {
            Object[] lookupThrottle = ConfigHandler.teleportThrottle.get(sender.getName());
            if ((boolean) lookupThrottle[0] || ((System.currentTimeMillis() - (long) lookupThrottle[1])) < 500) {
                Chat.sendMessage(sender, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.COMMAND_THROTTLED));
                return;
            }
        }

        if (resultc < 3) {
            Chat.sendMessage(sender, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.MISSING_PARAMETERS, "/co teleport <world> <x> <y> <z>"));
            return;
        }

        String worldName = args[1];
        int wid = WorldUtils.matchWorld(worldName);
        if (wid == -1 && resultc >= 5) {
            Chat.sendMessage(sender, new ChatMessage(Phrase.build(Phrase.WORLD_NOT_FOUND, worldName)).build());
            return;
        }

        Location location = ((Player) sender).getLocation().clone();
        World world = location.getWorld();
        if (wid > -1) {
            world = Bukkit.getServer().getWorld(WorldUtils.getWorldName(wid));
        }

        String x = null;
        String y = null;
        String z = null;

        for (int i = 1; i < args.length; i++) {
            if (i == 1 && wid > -1) {
                continue;
            }

            if (x == null) {
                x = args[i].replaceAll("[^0-9.\\-]", "");
            }
            else if (z == null) {
                z = args[i].replaceAll("[^0-9.\\-]", "");
            }
            else if (y == null) {
                y = z;
                z = args[i].replaceAll("[^0-9.\\-]", "");
            }
        }

        if (y == null) {
            if (location.getBlockY() > 63) {
                location.setY(63);
            }
            y = Double.toString(location.getY());
        }

        String xValidate = x.replaceAll("[^.\\-]", "");
        String yValidate = y.replaceAll("[^.\\-]", "");
        String zValidate = z.replaceAll("[^.\\-]", "");

        if ((x.length() == 0 || x.length() >= 12 || x.equals(xValidate)) || (y.length() == 0 || y.length() >= 12 || y.equals(yValidate)) || (z.length() == 0 || z.length() >= 12 || z.equals(zValidate))) {
            Chat.sendMessage(sender, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.MISSING_PARAMETERS, "/co teleport <world> <x> <y> <z>"));
            return;
        }

        location.setWorld(world);
        location.setX(Double.parseDouble(x));
        location.setY(Double.parseDouble(y));
        location.setZ(Double.parseDouble(z));

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        Bukkit.getRegionScheduler().run(CoreProtect.getInstance(), location, locTask -> {
            World targetWorld = location.getWorld();
            if (!targetWorld.isChunkLoaded(chunkX, chunkZ)) {
                targetWorld.getChunkAt(location);
            }
            
            Bukkit.getRegionScheduler().run(CoreProtect.getInstance(), player.getLocation(), playerTask -> {
                player.performCommand("admin "
                        + location.getBlockX() + " "
                        + location.getBlockY() + " "
                        + location.getBlockZ() + " "
                        + targetWorld.getName());
            });
        });

        ConfigHandler.teleportThrottle.put(sender.getName(), new Object[] { false, System.currentTimeMillis() });
    }
}
