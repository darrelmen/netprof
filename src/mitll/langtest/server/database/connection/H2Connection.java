package mitll.langtest.server.database.connection;

import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes H2 Connections.
 *
 * User: GO22670
 * Date: 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class H2Connection implements DatabaseConnection {
  private static final Logger logger = Logger.getLogger(H2Connection.class);
  private static final int QUERY_CACHE_SIZE = 8;

  private Connection conn;
  private final int cacheSizeKB;
  private final int queryCacheSize;
  private static final int MAX_MEMORY_ROWS = 50000;
  private static final int maxMemoryRows = MAX_MEMORY_ROWS;

  public H2Connection(String configDir, String dbName, boolean mustAlreadyExist) {
    this(configDir, dbName, 50000, QUERY_CACHE_SIZE, mustAlreadyExist);
  }

  /**
   * @param configDir
   * @param dbName
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl
   */
  private H2Connection(String configDir, String dbName, int cacheSizeKB, int queryCacheSize, boolean mustAlreadyExist) {
    this.cacheSizeKB = cacheSizeKB;
    this.queryCacheSize = queryCacheSize;
    connect(configDir, dbName, mustAlreadyExist);
  }

  private void connect(String configDir, String database, boolean mustAlreadyExist) {
    String h2FilePath = configDir + File.separator + database;
    connect(h2FilePath, mustAlreadyExist);
  }

  /**
   * h2 db must exist - don't try to make an empty one if it's not there.
   *
   *   //jdbc:h2:file:/Users/go22670/DLITest/clean/netPron2/war/config/urdu/vlr-parle;IFEXISTS=TRUE;CACHE_SIZE=30000
   * @param h2FilePath
   * @param mustAlreadyExist
   */
  private void connect(String h2FilePath, boolean mustAlreadyExist) {
    String url = "jdbc:h2:file:" + h2FilePath +
      ";" +
      (mustAlreadyExist ? "IFEXISTS=TRUE;" :"") +
      "QUERY_CACHE_SIZE=" + queryCacheSize + ";" +
      "CACHE_SIZE="       + cacheSizeKB + ";" +
      "MAX_MEMORY_ROWS="  + maxMemoryRows
      ;

    File test = new File(h2FilePath + ".h2.db");
    if (!test.exists()) {
      logger.error("no h2 db file at  " + test.getAbsolutePath() + "");
    } else {
      logger.debug("connecting to " + url);
      org.h2.Driver.load();
      try {
        conn = DriverManager.getConnection(url, "", "");
        conn.setAutoCommit(true);

      } catch (SQLException e) {
        conn = null;
        logger.error("got error trying to create h2 connection with URL '" + url + "', exception = " + e, e);
      }
    }
  }

  @Override
  public void contextDestroyed() {
    if (conn == null) {
      logger.info("not never successfully created h2 connection ");
    } else {
      sendShutdown();
      closeConnection();
      //org.h2.Driver.unload();
    }
  }

  private void sendShutdown() {
    logger.info("send shutdown on connection " + conn);

    try {
      Statement stat = conn.createStatement();
      stat.execute("SHUTDOWN");
      stat.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void closeConnection() {
    try {
      logger.info("closing connection " + conn);
      if (conn != null) {
        conn.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Connection getConnection() { return conn; }
  public boolean isValid() { return conn != null; }
}
