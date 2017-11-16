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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes H2 Connections.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class H2Connection implements DatabaseConnection {
  private static final Logger logger = LogManager.getLogger(H2Connection.class);
  private static final int QUERY_CACHE_SIZE = 1024;
  private static final int CACHE_SIZE_KB = 500000;

  private Connection conn;
  private final int cacheSizeKB;
  private final int queryCacheSize;
  private static final int MAX_MEMORY_ROWS = 1000000;
  private static final int maxMemoryRows = MAX_MEMORY_ROWS;
  private static final boolean USE_MVCC = false;
  private LogAndNotify logAndNotify;

  /**
   *
   *
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(ServerProperties, PathHelper, LogAndNotify)
   * @param configDir
   * @param dbName
   * @param mustAlreadyExist
   * @param readOnly
   */
  public H2Connection(String configDir, String dbName, boolean mustAlreadyExist, LogAndNotify logAndNotify, boolean readOnly) {
    this(configDir, dbName, CACHE_SIZE_KB, QUERY_CACHE_SIZE, mustAlreadyExist, logAndNotify, readOnly);
  }

  /**
   * @param configDir
   * @param dbName
   * @param readOnly
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl
   */
  private H2Connection(String configDir, String dbName, int cacheSizeKB, int queryCacheSize, boolean mustAlreadyExist,
                       LogAndNotify logAndNotify, boolean readOnly) {
    this.cacheSizeKB = cacheSizeKB;
    this.queryCacheSize = queryCacheSize;
    this.logAndNotify = logAndNotify;
    connect(configDir + File.separator + dbName, mustAlreadyExist, readOnly);
  }

  /**
   * h2 db must exist - don't try to make an empty one if it's not there.
   * <p>
   * //jdbc:h2:file:/Users/go22670/DLITest/clean/netPron2/war/config/urdu/vlr-parle;IFEXISTS=TRUE;CACHE_SIZE=30000
   *  @param h2FilePath
   * @param mustAlreadyExist
   * @param readOnly
   */
  private void connect(String h2FilePath, boolean mustAlreadyExist, boolean readOnly) {
    String url = "jdbc:h2:file:" + h2FilePath +
        ";" +
        (mustAlreadyExist ? "IFEXISTS=TRUE;" : "") +
        "QUERY_CACHE_SIZE=" + queryCacheSize + ";" +
        "CACHE_SIZE=" + cacheSizeKB + ";" +
        "MAX_MEMORY_ROWS=" + maxMemoryRows + ";" +
        "AUTOCOMMIT=ON" + ";" +
        (readOnly ? "ACCESS_MODE_DATA=r;" : "")+
        (USE_MVCC ? "MVCC=true" : "");

    File test = new File(h2FilePath + ".h2.db");
    if (!test.exists()) {
      logger.error("\n\n\nconnect : no h2 db file at  " + test.getAbsolutePath() + " : check config file?\n\n\n");
    } else {
      logger.debug("H2 : connecting to " + url);
      org.h2.Driver.load();
      try {
        conn = DriverManager.getConnection(url, "", "");
      } catch (SQLException e) {
        String message = "couldn't get connection to " + url;
        logger.error(message, e);
        logAndNotify.logAndNotifyServerException(e, message);

        contextDestroyed();
      }
    }
  }

  @Override
  public void contextDestroyed() {
    sendShutdown();
    cleanup();
  }

  @Override
  public int connectionsOpen() {
    return 1;
  }

  private void sendShutdown() {
    logger.info("send shutdown on connection " + conn);
    try {
      Connection connection = getConnection(this.getClass().toString());
      if (connection != null) {
        Statement stat = connection.createStatement();
        stat.execute("SHUTDOWN");
        stat.close();
        connection.close();
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
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
