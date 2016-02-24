/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.connection;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.log4j.Logger;

import java.sql.*;

/**
 * Makes PostgreSQL Connections.
 * <p>
 * User: GO22670
 * Date: 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostgreSQLConnection implements DatabaseConnection {
  private static final Logger logger = Logger.getLogger(PostgreSQLConnection.class);
  public static final String DEFAULT_USER = "postgres";

  private Connection conn;
  LogAndNotify logAndNotify;

  /**
   * @param dbName
   * @paramx configDir
   * @paramx mustAlreadyExist
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(String, String, String, ServerProperties, PathHelper, boolean, LogAndNotify)
   */
  public PostgreSQLConnection(String dbName, LogAndNotify logAndNotify) {
    this.logAndNotify = logAndNotify;

    try {
      Class.forName("org.postgresql.Driver").newInstance();
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
    try {
      String host = "localhost";

      Connection conn = DriverManager.getConnection(
          "jdbc:postgresql://" +
              host +
              ":5432/", DEFAULT_USER,
          "");

      String dbName1 = dbName.toLowerCase();
      createDatabaseForgiving(dbName1, conn);

      this.conn =
          DriverManager.getConnection("jdbc:postgresql://" +
              host +
              "/" + dbName1, DEFAULT_USER, ""
          );

    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  void createDatabaseForgiving(String dbName, Connection conn) throws SQLException {
    try {
      createDatabase(dbName, conn);
    } catch (SQLException e) {
      if (!e.getMessage().contains("already exists")) {
        throw e;
      }
      //else logger.info("Got " +e.getMessage());
    }
  }

  void createDatabase(String dbName, Connection conn) throws SQLException {
    Statement s2 = conn.createStatement();
    int myResult = s2.executeUpdate("CREATE DATABASE " + dbName);
    s2.close();
//    logger.info("Got " + myResult);
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
