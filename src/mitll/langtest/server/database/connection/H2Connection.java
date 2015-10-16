package mitll.langtest.server.database.connection;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.log4j.Logger;

import java.io.File;
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
public class H2Connection implements DatabaseConnection {
  private static final Logger logger = Logger.getLogger(H2Connection.class);
  private static final int QUERY_CACHE_SIZE = 1024;
  private static final int CACHE_SIZE_KB = 500000;

  private Connection conn;
  private final int cacheSizeKB;
  private final int queryCacheSize;
  private static final int MAX_MEMORY_ROWS = 1000000;
  private static final int maxMemoryRows = MAX_MEMORY_ROWS;
  private static final boolean USE_MVCC = false;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(String, String, String, ServerProperties, PathHelper, boolean, LogAndNotify)
   * @param configDir
   * @param dbName
   * @param mustAlreadyExist
   */
  public H2Connection(String configDir, String dbName, boolean mustAlreadyExist) {
    this(configDir, dbName, CACHE_SIZE_KB, QUERY_CACHE_SIZE, mustAlreadyExist);
  }

  /**
   * @param configDir
   * @param dbName
   * @see mitll.langtest.server.database.DatabaseImpl#DatabaseImpl
   */
  private H2Connection(String configDir, String dbName, int cacheSizeKB, int queryCacheSize, boolean mustAlreadyExist) {
    this.cacheSizeKB = cacheSizeKB;
    this.queryCacheSize = queryCacheSize;
    connect(configDir + File.separator + dbName, mustAlreadyExist);
  }

  /**
   * h2 db must exist - don't try to make an empty one if it's not there.
   * <p>
   * //jdbc:h2:file:/Users/go22670/DLITest/clean/netPron2/war/config/urdu/vlr-parle;IFEXISTS=TRUE;CACHE_SIZE=30000
   *
   * @param h2FilePath
   * @param mustAlreadyExist
   */
  private void connect(String h2FilePath, boolean mustAlreadyExist) {
    String url = "jdbc:h2:file:" + h2FilePath +
        ";" +
        (mustAlreadyExist ? "IFEXISTS=TRUE;" : "") +
        "QUERY_CACHE_SIZE=" + queryCacheSize + ";" +
        "CACHE_SIZE=" + cacheSizeKB + ";" +
        "MAX_MEMORY_ROWS=" + maxMemoryRows + ";" +
        "AUTOCOMMIT=ON" + ";" +
        (USE_MVCC ? "MVCC=true" : "");

    File test = new File(h2FilePath + ".h2.db");
    if (!test.exists()) {
      logger.error("no h2 db file at  " + test.getAbsolutePath() + "");
    } else {
      logger.debug("connecting to " + url);
      org.h2.Driver.load();
      try {
        conn = DriverManager.getConnection(url, "", "");
      } catch (SQLException e) {
        logger.error("couldn't get connection ", e);

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
      Statement stat = connection.createStatement();
      stat.execute("SHUTDOWN");
      stat.close();
      connection.close();
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
