package mitll.langtest.server.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 6/26/12
* Time: 3:45 PM
* To change this template use File | Settings | File Templates.
*/
class Schedule {
  public long id;
  public String plan;
  public long userid;
  public String exid;
  public boolean flQ;
  public boolean spoken;

  /**
   * @see mitll.langtest.server.database.ScheduleDAO#getSchedule()
   * @param rs
   * @param stringRefs
   */
  public Schedule(ResultSet rs, Map<String,String> stringRefs) {
    int i = 1;
    try {
      id = rs.getLong(i++);

      String planRef = rs.getString(i++);
      if (!stringRefs.containsKey(planRef)) stringRefs.put(planRef, planRef);
      plan = stringRefs.get(planRef);

      userid = rs.getLong(i++);

      String exRef = rs.getString(i++);
      if (!stringRefs.containsKey(exRef)) stringRefs.put(exRef,exRef);
      exid = stringRefs.get(exRef);

      flQ = rs.getBoolean(i++);
      spoken = rs.getBoolean(i++);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return "Schedule " + id + " user " + userid + " exid " + exid + " flq " + flQ + " spoken " + spoken;
  }
}
