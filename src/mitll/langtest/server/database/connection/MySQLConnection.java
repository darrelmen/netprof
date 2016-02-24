/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.connection;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes H2 Connections.
 * <p>
 * User: GO22670
 * Date: 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MySQLConnection implements DatabaseConnection {
  private static final Logger logger = Logger.getLogger(MySQLConnection.class);

  private Connection conn;
  private static final int MAX_MEMORY_ROWS = 1000000;
  LogAndNotify logAndNotify;

  /**
   * @param dbName
   * @paramx configDir
   * @paramx mustAlreadyExist
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(String, String, String, ServerProperties, PathHelper, boolean, LogAndNotify)
   */
  public MySQLConnection(String dbName, LogAndNotify logAndNotify) {
    this.logAndNotify = logAndNotify;

    try {
      // The newInstance() call is a work around for some
      // broken Java implementations

      Class.forName("com.mysql.jdbc.Driver").newInstance();
    } catch (Exception ex) {
      // handle the error
    }
    try {
      String host = "localhost";
      Connection conn = DriverManager.getConnection("jdbc:mysql://" +
          host +
          "/?user=root&password=");

      Statement s = conn.createStatement();
      int myResult = s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
      logger.info("Got " + myResult);
      s.close();

      this.conn =
          DriverManager.getConnection("jdbc:mysql://" +
                  host +
                  "/" + dbName +
                  "?" +
                  "user=" +
                  "root" +
                  "&" +
                  "password=" //+
              //"greatsqldb"
          );


    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  @Override
  public void contextDestroyed() {
    cleanup();
  }

  @Override
  public int connectionsOpen() {
    return 1;
  }

  private void cleanup() {
    try {
      logger.info("closing connection " + conn);
      if (conn != null) {
        conn.close();
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public Connection getConnection(String who) {
    return conn;
  }

  public boolean isValid() {
    return conn != null;
  }

  @Override
  public boolean usingCP() {
    return false;
  }
}
