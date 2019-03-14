/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class H2DatabaseImpl extends DatabaseImpl {
  private static final Logger logger = LogManager.getLogger(DatabaseImpl.class);
  private static final int LOG_THRESHOLD = 10;
  /**
   * Only for h2
   */
  @Deprecated
  private DatabaseConnection connection;

  /**
   * @see mitll.langtest.server.database.copy.CopyToPostgres#getDatabaseLight
   * @param configDir
   * @param dbName
   * @param serverProps
   * @param pathHelper
   * @param mustAlreadyExist
   * @param logAndNotify
   * @param readOnly
   */
  public H2DatabaseImpl(String configDir,
                        String dbName,
                        ServerProperties serverProps,
                        PathHelper pathHelper,
                        boolean mustAlreadyExist,
                        LogAndNotify logAndNotify,
                        boolean readOnly) {
    this.serverProps = serverProps;
    this.logAndNotify = logAndNotify;
    this.pathHelper = pathHelper;
    this.connection = new H2Connection(configDir, dbName, mustAlreadyExist, logAndNotify, readOnly);
    connectToDatabases(pathHelper,null);
  }

  @Override
  public Connection getConnection(String who) {
    if (connection == null) {
      if (serverProps.useH2()) {
        logger.warn("getConnection no connection created " + who + " use h2 property = " + serverProps.useH2());
      }
      return null;
    } else {
      return connection.getConnection(who);
    }
  }

  /**
   * It seems like this isn't required?
   *
   * @param conn
   * @throws SQLException
   */
  public void closeConnection(Connection conn) {
    try {
      if (connection != null) {
        int before = connection.connectionsOpen();
        if (conn != null && !conn.isClosed()) {
          if (connection.usingCP()) {
            conn.close();
          }
        }
        //else {
        //logger.warn("trying to close a null connection...");
        // }
        if (connection.connectionsOpen() > LOG_THRESHOLD) {
          logger.debug("closeConnection : now " + connection.connectionsOpen() + " open vs before " + before);
        }
      }
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * @see LangTestDatabaseImpl#destroy()
   */
  public void close() {
    super.close();
    try {
      if (connection != null) {
        connection.contextDestroyed();
      }
    } catch (Exception e) {
      logger.error("close got " + e, e);
    }
  }
}
