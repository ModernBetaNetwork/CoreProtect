package net.coreprotect.consumer.process;

import java.sql.BatchUpdateException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import net.coreprotect.config.Config;
import net.coreprotect.consumer.Consumer;
import net.coreprotect.database.Database;
import net.coreprotect.utility.MaterialUtils;

@Slf4j
class RollbackUpdateProcess {

    static void process(Statement statement, int processId, int id, int action, int table) {
        Map<Integer, List<Object[]>> updateLists = Consumer.consumerObjectArrayList.get(processId);
        if (updateLists.get(id) != null) {
            List<Object[]> list = updateLists.get(id);
            long batchSize = 0;
            int batchStart = 0;
            int totalProcessed = 0;
            for ( int i = 0; i < list.size(); ++i ) {
                Object[] listRow = list.get(i);
                long rowid = (Long) listRow[0];
                int rolledBack = (Integer) listRow[9];
                if (MaterialUtils.rolledBack(rolledBack, (table == 2 || table == 3 || table == 4)) == action) { // 1 = restore, 0 = rollback
                    Database.performUpdate(statement, rowid, rolledBack, table);
                    if ( Config.getGlobal().BATCH_DB_UPDATES && ++batchSize > Config.getGlobal().MAX_DB_BATCH_SIZE ) {
                        totalProcessed += executeBatch(statement, list, batchStart);
                        batchSize = 0;
                        batchStart = i+1;
                    }
                }
            }

            if ( Config.getGlobal().BATCH_DB_UPDATES ) {
                if ( batchSize > 0 )
                    totalProcessed += executeBatch(statement, list, batchStart);
                if ( totalProcessed != list.size() )
                    log.warn("BATCH MISMATCH. Unexpected number of updated records (expected: {}, actual: {})", list.size(), totalProcessed);
            }

            updateLists.remove(id);
        }
    }

    private static int executeBatch(Statement statement, List<Object[]> data, int batchStart) {
        int [] counts;
        try {
            counts = statement.executeBatch();
        } catch(BatchUpdateException e) {
            log.warn("BATCH ERROR", e);
            counts = e.getUpdateCounts();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        int total = 0;
        for ( int i = 0 ; i < counts.length; ++i ) {
            int count = counts[i];
            if ( count >= 0 ) {
                total += count;
            } else if ( count == Statement.SUCCESS_NO_INFO ) {
                total += 1; // we are updating single records
            } else {
                Object[] rowdata = data.get(batchStart+i);
                if (count == Statement.EXECUTE_FAILED) {
                    log.warn("BATCH ERROR (EXECUTE_FAILED): list[{}] = {}", batchStart+i, rowdata);
                } else {
                    log.warn("BATCH ERROR (UNKNOWN:{}): list[{}] = {}", count, batchStart+i, rowdata);
                }
            }
        }
        return total;
    }
}
