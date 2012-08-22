package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

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
    return getScheduleForUserAndExercise(true, objects, "");
  }

  public Map<Long, List<Schedule>> getScheduleForUserAndExercise(Set<Long> users, String exid) {
    return getScheduleForUserAndExercise(false, users, exid);
  }

  private Map<Long, List<Schedule>> getScheduleForUserAndExercise(boolean getAll, Set<Long> users, String exid) {
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
        sql += " where USERID in (" + inClause +
            ") and EXID='" + exid + "'";
      }
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        schedules.add(new Schedule(rs));
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