package net.coreprotect.utility;

import java.lang.reflect.Method;

import org.bukkit.command.CommandSender;

import net.coreprotect.language.Phrase;

public class Extensions {

    public static void runDatabaseMigration(String command, CommandSender user, String[] argumentArray) {
        if ( true ) { // ModernBeta: Do not allow migrate-db command (even on accident)
            Chat.sendMessage(user, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Color.RED + Phrase.build(Phrase.COMMAND_IS_DISABLED));
            return;
        }

        try {
            Class<?> patchClass = Class.forName("net.coreprotect.utility.extensions.DatabaseMigration");
            Method patchMethod = patchClass.getDeclaredMethod("runCommand", CommandSender.class, String[].class);
            patchMethod.invoke(null, user, argumentArray);
        }
        catch (Exception e) {
            // plugin not compiled with extension
            Chat.sendMessage(user, Color.DARK_AQUA + "CoreProtect " + Color.WHITE + "- " + Phrase.build(Phrase.COMMAND_NOT_FOUND, Color.WHITE, "/co " + command));
        }
    }

}
