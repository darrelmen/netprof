/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.connection;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes PostgreSQL Connections.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 * @deprecated
 */
public class PostgreSQLConnection implements DatabaseConnection {
  private static final Logger logger = LogManager.getLogger(PostgreSQLConnection.class);
  public static final String DEFAULT_USER = "postgres";

  private Connection conn;
  LogAndNotify logAndNotify;

  /**
   * @param dbName
   * @paramx configDir
   * @paramx mustAlreadyExist
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(String, String, String, ServerProperties, PathHelper, boolean, LogAndNotify, boolean)
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
