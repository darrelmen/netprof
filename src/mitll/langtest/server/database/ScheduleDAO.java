package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated we're moving away from using schedules
 */
public class ScheduleDAO {
  private final Database database;

  public ScheduleDAO(Database database) {
    this.database = database;
  }

  /**
   * schedules (id LONG, plan VARCHAR, userid LONG, exid VARCHAR, flQ BOOLEAN, spoken BOOLEAN, CONSTRAINT pksched PRIMARY KEY (id, plan, userid))");
   * @see DatabaseImpl#DatabaseImpl(javax.servlet.http.HttpServlet)
   */
  Map<Long, List<Schedule>> getSchedule() {
    Set<Long> objects = Collections.emptySet();
    return getScheduleForUserAndExercise(true, false, objects, "");
  }

  private Map<Long, List<Schedule>> getScheduleForUserAndExercise(boolean getAll, boolean ignoreUsers, Set<Long> users, String exid) {
    Connection connection;
    // SortedSet<String> ids = new TreeSet<String>();

    List<Schedule> schedules = new ArrayList<Schedule>();
    try {
      connection = database.getConnection();
      String sql = "SELECT * FROM schedules";
      if (!getAll) {
        StringBuilder b = new StringBuilder();
        for (Long l : users) b.append(l).append(",");

        String inClause = users.isEmpty() ? "" : b.toString().substring(0, b.length() - 1);
        sql += " where " +
            (ignoreUsers ? "" : "USERID in (" + inClause +") and ")+
            "EXID='" + exid + "'";
      }
      PreparedStatement statement = connection.prepareStatement(sql);

      Map<String,String> stringRefs = new HashMap<String,String>();
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        schedules.add(new Schedule(rs, stringRefs));
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    Map<Long, List<Schedule>> userToSchedule = new HashMap<Long, List<Schedule>>();
    for (Schedule s : schedules) {
      // ids.add(s.exid);
      List<Schedule> forUser = userToSchedule.get(s.userid);
      if (forUser == null) {
        userToSchedule.put(s.userid, forUser = new ArrayList<Schedule>());
      }
      forUser.add(s);
    }
    return userToSchedule;
  }
}