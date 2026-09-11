package net.coreprotect.consumer.process;

import java.sql.BatchUpdateException;
import java.sql.Statement;
import java.util.ArrayList;
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
            List<Object[]> batchData = new ArrayList<>(Config.getGlobal().MAX_DB_BATCH_SIZE);
            int expectedUpdates = 0, actualUpdates = 0;
            for ( int i = 0; i < list.size(); ++i ) {
                Object[] listRow = list.get(i);
                long rowid = (Long) listRow[0];
                int time = (int) listRow[1]; // only used for partitioned tables
                int rolledBack = (Integer) listRow[9];
                if (MaterialUtils.rolledBack(rolledBack, (table == 2 || table == 3 || table == 4)) == action) { // 1 = restore, 0 = rollback
                    Database.performUpdate(statement, rowid, time, rolledBack, table);
                    batchData.add(listRow);
                    if ( Config.getGlobal().BATCH_DB_UPDATES && batchData.size() >= Config.getGlobal().MAX_DB_BATCH_SIZE ) {
                        expectedUpdates += batchData.size();
                        actualUpdates += executeBatch(statement, batchData);
                        batchData.clear();
                    }
                }
            }

            if ( Config.getGlobal().BATCH_DB_UPDATES ) {
                if ( ! batchData.isEmpty() ) {
                    expectedUpdates += batchData.size();
                    actualUpdates += executeBatch(statement, batchData);
                }
                if ( expectedUpdates != actualUpdates )
                    log.warn("BATCH MISMATCH. Unexpected number of updated records (expected: {}, actual: {})", expectedUpdates, actualUpdates);
            }

            updateLists.remove(id);
        }
    }

    private static int executeBatch(Statement statement, List<Object[]> batchData) {
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
                if (count == Statement.EXECUTE_FAILED) {
                    log.warn("BATCH ERROR (EXECUTE_FAILED): batch[{}] = {}", i, batchData.get(i));
                } else {
                    log.warn("BATCH ERROR (UNKNOWN:{}): batch[{}] = {}", count, i, batchData.get(i));
                }
            }
        }
        return total;
    }
}
