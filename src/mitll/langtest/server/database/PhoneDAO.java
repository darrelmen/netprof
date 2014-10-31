package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneDAO extends DAO {
  private static final Logger logger = Logger.getLogger(PhoneDAO.class);

  private static final String PHONE = "phone";
  public static final String RID = "rid";
  public static final String WID = "wid";
  public static final String SEQ = "seq";
 // public static final String SEQ = SEQ1;
  public static final String SCORE = "score";
  private LogAndNotify logAndNotify;

  /**
   * @param database
   * @paramx
   * @see DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public PhoneDAO(Database database, LogAndNotify logAndNotify) {
    super(database);
    this.logAndNotify = logAndNotify;
    try {
      createTable(database);
      createIndex(database, RID, PHONE);
      createIndex(database, WID, PHONE);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  public static class Phone {
    long id;
    long wid;
    long rid;
    String phone;
    int seq;
    float score;

    public Phone(long id, long rid, long wid, String phone, int seq, float score) {
      this(rid, wid, phone, seq, score);
      this.id = id;
    }

    public Phone(long rid, long wid, String phone, int seq, float score) {

      this.rid = rid;
      this.wid = wid;
      this.phone = phone;
      this.seq = seq;
      this.score = score;
    }

    public String toString() {
      return "# " + id + " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
    }
  }

  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        PHONE +
        " (" +
        "ID IDENTITY, " +
        RID + " BIGINT, " +
        WID + " BIGINT, " +
        PHONE + " VARCHAR, " +
        SEQ + " INT, " +
        SCORE + " FLOAT, " +

        "FOREIGN KEY(" +
        RID +
        ") REFERENCES " +
        ResultDAO.RESULTS +
        "(ID)," +

        "FOREIGN KEY(" +
        WID +
        ") REFERENCES " +
        WordDAO.WORD +
        "(ID)" +

        ")");

    finish(database, connection, statement);
  }


  /**
   * <p/>
   *
   * @param phone
   */
  public boolean addPhone(Phone phone) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + PHONE +
              "(" +
              RID + "," +
              WID + "," +
              PHONE + "," +
              SEQ + "," +
              SCORE +
              //"," +
              ") " +
              "VALUES(?,?,?,?,?);");
      int i = 1;

      statement.setLong(i++, phone.rid);
      statement.setLong(i++, phone.wid);
      statement.setString(i++, phone.phone);
      statement.setInt(i++, phone.seq);
      statement.setFloat(i++, phone.score);

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to add event " + phone + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  public List<Phone> getAll() {
    try {
      return getPhones("SELECT * from " + PHONE);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
    }
    return Collections.emptyList();
  }

/*  public List<Event> getAllForUserAndExercise(long userid, String exid) {
    try {
      String sql = "SELECT * from " + PHONE + " where " +
        WIDGETTYPE +
        "='qcPlayAudio' AND " +
        CREATORID +"="+userid + " and " +
        EXERCISEID + "='" +exid+
        "'";

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  private List<Phone> getPhones(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Phone> lists = new ArrayList<Phone>();

    while (rs.next()) {
      lists.add(new Phone(
              rs.getLong("ID"),
              rs.getLong("RID"),
              rs.getLong("WID"),
              rs.getString(PHONE),
              rs.getInt(SEQ),
              rs.getFloat(SCORE)
          )
      );
    }

    finish(connection, statement, rs);
    return lists;
  }
}