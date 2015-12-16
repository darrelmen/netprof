/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.exercise.Upload;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadDAO extends DAO {
  private static final Logger logger = Logger.getLogger(UploadDAO.class);

  private static final String UPLOAD = "upload";

  private static final boolean DEBUG = false;
  public static final String USER = "user";
  public static final String SOURCE = "source";
  public static final String PROJECT = "project";
  public static final String TIME = "time";
  public static final String ENABLED = "enabled";
  public static final String NOTE = "note";
  public static final String FILE_REF = "fileRef";

  /**
   * @param database
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public UploadDAO(Database database, ServerProperties serverProperties) {
    super(database);
    try {
      createTable(database);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }


  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws SQLException
   */
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        UPLOAD +
        " (" +
        "ID IDENTITY, " +
        USER + " BIGINT, " +
        SOURCE + " VARCHAR, " +
        FILE_REF + " VARCHAR, " +
        PROJECT + " VARCHAR, " +
        NOTE + " VARCHAR, " +
        TIME + " TIMESTAMP, " +
        ENABLED + " BOOLEAN, " +

        "FOREIGN KEY(" +
        USER +
        ") REFERENCES " +
        UserDAO.USERS +
        "(ID)" +
       ")");

    finish(database, connection, statement);
  }

  /**
   * <p>
   *
   * @param upload
   */
  public boolean addUpload(Upload upload) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + UPLOAD +
              "(" +
              USER + "," +
              SOURCE + "," +
              FILE_REF + "," +
              PROJECT + "," +
              NOTE + "," +
              TIME + ","+
              ENABLED +
              //"," +
              ") " +
              "VALUES(?,?,?,?,?,?,?)");
      int i = 1;

      statement.setLong(i++, upload.getUser());
      statement.setString(i++, upload.getSourceURL());
      statement.setString(i++, upload.getFileRef());
      statement.setString(i++, upload.getProject());
      statement.setString(i++, upload.getNote());
      statement.setTimestamp(i++, new Timestamp(upload.getTimestamp()));
      statement.setBoolean(i++, upload.isEnabled());

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to add upload " + upload + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  public Collection<Upload> getUploads() { return getUploads("SELECT * FROM " + UPLOAD); }
  private Collection<Upload> getUploads(String sql) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      Collection<Upload> users = getUploads(rs);
      finish(connection, statement, rs);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
      database.logEvent("unk", "getUsers: " + ee.toString(), 0, "");
    }
    return new ArrayList<Upload>();
  }

  private Collection<Upload> getUploads(ResultSet rs) throws SQLException {
    List<Upload> users = new ArrayList<Upload>();

    while (rs.next()) {
      long id = rs.getLong("ID");

      users.add(new Upload(id,rs.getLong(USER),
          rs.getString(NOTE),
          rs.getString(FILE_REF),
          rs.getTimestamp(TIME).getTime(),
          rs.getBoolean(ENABLED),
          rs.getString(PROJECT),
          rs.getString(SOURCE)
      ));
    }
    return users;
  }
}