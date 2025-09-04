package net.coreprotect.listener.player.inspector;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import net.coreprotect.database.lookup.DoubleChestTransactionLookup;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.coreprotect.database.lookup.ChestTransactionLookup;
import net.coreprotect.utility.Chat;

public class ContainerInspector extends BaseInspector {

    public void performContainerLookup(final Player player, final Location finalLocation, final Location finalLocation2) {
        class BasicThread implements Runnable {
            @Override
            public void run() {
                try {
                    checkPreconditions(player);

                    try (Connection connection = getDatabaseConnection(player)) {
                        Statement statement = connection.createStatement();
                        List<String> blockData;
                        if ( finalLocation2 == null ) {
                            blockData = ChestTransactionLookup.performLookup(null, statement, finalLocation, player, 1, 7);
                        } else {
                            blockData = DoubleChestTransactionLookup.performLookup(null, statement, finalLocation, finalLocation2, player, 1, 7);
                        }
                        for (String data : blockData) {
                            Chat.sendComponent(player, data);
                        }

                        statement.close();
                    }
                }
                catch (InspectionException e) {
                    Chat.sendMessage(player, e.getMessage());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    finishInspection(player);
                }
            }
        }

        Runnable runnable = new BasicThread();
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
