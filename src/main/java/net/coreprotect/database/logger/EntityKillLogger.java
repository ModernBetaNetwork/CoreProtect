package net.coreprotect.database.logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;

import net.coreprotect.CoreProtect;
import net.coreprotect.config.Config;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Database;
import net.coreprotect.database.statement.BlockStatement;
import net.coreprotect.database.statement.EntityStatement;
import net.coreprotect.database.statement.UserStatement;
import net.coreprotect.event.CoreProtectPreLogEvent;
import net.coreprotect.utility.WorldUtils;
import net.coreprotect.utility.entity.EntityUtil;

public class EntityKillLogger {

    private EntityKillLogger() {
        throw new IllegalStateException("Database class");
    }

    public static void log(PreparedStatement preparedStmt, PreparedStatement preparedStmt2, int batchCount, String user, BlockState block, List<Object> data, int type) {
        try {
            if (ConfigHandler.blacklist.get(user.toLowerCase(Locale.ROOT)) != null) {
                return;
            }

            CoreProtectPreLogEvent event = new CoreProtectPreLogEvent(user);
            if (Config.getGlobal().API_ENABLED && !Bukkit.isPrimaryThread()) {
                CoreProtect.getInstance().getServer().getPluginManager().callEvent(event);
            }

            if (event.isCancelled()) {
                return;
            }

            int userId = UserStatement.getId(preparedStmt, event.getUser(), true);
            int wid = WorldUtils.getWorldId(block.getWorld().getName());
            int time = (int) (System.currentTimeMillis() / 1000L);
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            int entity_key = 0;

            /* START MODERNBETA: NON-ROLLBACKABLE ENTITY DEATHS */
            if ( EntityUtil.isRollbackable(type) && ! data.isEmpty() ) { // wild wolves will have empty data
            /* END MODERNBETA: NON-ROLLBACKABLE ENTITY DEATHS */
                ResultSet resultSet = EntityStatement.insert(preparedStmt2, time, data);
                if (Database.hasReturningKeys()) {
                    resultSet.next();
                    entity_key = resultSet.getInt(1);
                    resultSet.close();
                } else {
                    ResultSet keys = preparedStmt2.getGeneratedKeys();
                    keys.next();
                    entity_key = keys.getInt(1);
                    keys.close();
                }
            /* START MODERNBETA: NON-ROLLBACKABLE ENTITY DEATHS */
            } else {
                entity_key = -1; // not rollbackable, data not persisted
            /* END MODERNBETA: NON-ROLLBACKABLE ENTITY DEATHS */
            }

            BlockStatement.insert(preparedStmt, batchCount, time, userId, wid, x, y, z, type, entity_key, null, null, 3, 0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
