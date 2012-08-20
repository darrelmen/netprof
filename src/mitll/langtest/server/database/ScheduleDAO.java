package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
    Connection connection;
   // SortedSet<String> ids = new TreeSet<String>();

    List<Schedule> schedules = new ArrayList<Schedule>();
    try {
      connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM schedules");

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

/*  public Map<Long,List<Schedule>> getScheduleForUserAndExercise(boolean getAll, long userid, String exid) {
    Connection connection;
    // SortedSet<String> ids = new TreeSet<String>();

    List<Schedule> schedules = new ArrayList<Schedule>();
    try {
      connection = database.getConnection();
      String sql = "SELECT * FROM schedules";
      if (!getAll) {
        sql += " where USERID = '" + userid +
            "' and EXID='" + exid + "'";
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
  }*/
}