package me.sheidy.autorevoker;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
//import java.util.Set;
import java.util.TreeMap;
//import java.util.stream.Collectors;
import me.leoko.advancedban.manager.DatabaseManager;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.TimeManager;
import me.leoko.advancedban.utils.Punishment;
import java.util.logging.Logger;

public class AutoRevoker extends Thread {

    private final Object lock = new Object();

    private final Map<Long, Punishment> map;
    private Method executeStatementMethod;
    private int currentId = -1;

    private final Logger logger;

    public AutoRevoker(Logger logger) {
        super("AutoRevoker Thread");
        this.logger = logger;
        this.map = Collections.synchronizedSortedMap(new TreeMap<Long, Punishment>());

        try {
            executeStatementMethod = DatabaseManager.class.getDeclaredMethod("executeStatement", String.class, boolean.class, Object[].class);
            executeStatementMethod.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        loadPunishments();
    }

    @Override
    public void run() {
        try {

            // thread loop
            while (true) {

                currentId = -1;

                // wait until a new punishment is added
                if (map.isEmpty()) waitFor(0);

                // get the punishment that will expire sooner
                Punishment pun = map.values().iterator().next();

                currentId = pun.getId();

                long expireInMillis = pun.getEnd() - TimeManager.getTime();

                if (expireInMillis > 0) {
                    waitFor(expireInMillis); // wait until it expires or a new punishment is added

                    if (pun.isExpired()) {
                        String message = String.format("Expired %s for %s", pun.getType().getName(), pun.getName());
                        logger.info(message);
                        pun.delete();
                    }

                } else {
                    pun.delete();
                }
            }

        } catch (InterruptedException e) {
            map.clear();
        }
    }

    public void addPunishment(Punishment punishment) {
        long end = punishment.getEnd();
        while (map.containsKey(end)) end++; // increase +1ms to avoid issues, this will never happen but who knows...
        map.put(end, punishment);
        unlock();
    }

    public void removePunishment(Punishment punishment) {
        if (!punishment.isExpired()) {
            // manually revoked
        }

        for (Punishment pun : map.values()) {
            if (pun.getId() == punishment.getId()) {
                punishment = pun; break;
            }
        }

        map.values().remove(punishment);

        if (punishment.getId() == currentId) unlock();
    }

    public String getStatus() {
        return String.format("Currently watching %d punishments", map.size());
    }

    private void waitFor(long timeout) throws InterruptedException {
        synchronized (lock) {
            lock.wait(timeout);
        }
    }

    private void unlock() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void loadPunishments() {
        PunishmentManager punishmentManager = PunishmentManager.get();

        try (ResultSet rs = executeQuery("SELECT * FROM Punishments WHERE end != -1")) {
            while (rs.next()) {
                Punishment punishment = punishmentManager.getPunishmentFromResultSet(rs);
                addPunishment(punishment);
            }
        } catch (SQLException e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        // remove temp. punishments from AB cache
        //Set<Integer> ids = map.values().stream().map(Punishment::getId).collect(Collectors.toSet());
        //punishmentManager.getLoadedPunishments(false).removeIf(p -> ids.contains(p.getId()));
    }

    private ResultSet executeQuery(String sql, Object... args) {
        try {
            return (ResultSet) executeStatementMethod.invoke(DatabaseManager.get(), sql, true, args);
        } catch (ReflectiveOperationException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }
}
