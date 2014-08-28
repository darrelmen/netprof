package mitll.langtest.server.database.connection;

import org.apache.log4j.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.sql.Connection;
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
  public static final int MAX = 10;
  private static final Logger logger = Logger.getLogger(H2Connection.class);
  private static final int QUERY_CACHE_SIZE = 1024;
  public static final int CACHE_SIZE_KB = 500000;

  private Connection conn;
  private final int cacheSizeKB;
  private final int queryCacheSize;
  private static final int MAX_MEMORY_ROWS = 1000000;
  private static final int maxMemoryRows = MAX_MEMORY_ROWS;
  private static final boolean USE_MVCC = false;
  private JdbcConnectionPool cp;

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
        "MAX_MEMORY_ROWS="  + maxMemoryRows + ";" +
        "AUTOCOMMIT=ON"+ ";" +
        (USE_MVCC ? "MVCC=true" : "")
      ;

    File test = new File(h2FilePath + ".h2.db");
    if (!test.exists()) {
      logger.error("no h2 db file at  " + test.getAbsolutePath() + "");
    } else {
      logger.debug("connecting to " + url);
      org.h2.Driver.load();


      //  try {
      //conn = DriverManager.getConnection(url, "", "");
      long then = System.currentTimeMillis();
      cp = JdbcConnectionPool.create(url, "", "");
      long now = System.currentTimeMillis();
      if (now-then > 200) {
        logger.debug("took " + (now - then) + " to create connection pool");
      }

      cp.setMaxConnections(MAX);
    //  cp.setLoginTimeout(2);
      logger.debug("made cp " + cp);
      //try {
        //Connection connection = cp.getConnection();
       // logger.debug("made connection " + connection);
        logger.debug("max connections " + cp.getMaxConnections());
     // } catch (SQLException e) {
     //   logger.error("couldn't get connection");
     // }
      // cpconn.setAutoCommit(true);
      //    } catch (SQLException e) {
      //     conn = null;
      //     logger.error("got error trying to create h2 connection with URL '" + url + "', exception = " + e, e);
      //   }
    }
  }

  @Override
  public void contextDestroyed() {
    if (conn == null) {
      //logger.info("not never successfully created h2 connection ");
    } //else {
      sendShutdown();
      cleanup();
      //org.h2.Driver.unload();
    //}
  }

  @Override
  public int connectionsOpen() {
    if (cp != null) return cp.getActiveConnections();
    else return 1;
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
      e.printStackTrace();
    }
  }

  private void cleanup() {
    try {
      logger.info("closing connection " + conn);
      if (conn != null) {
        conn.close();
      }
      if (cp != null) {
        cp.dispose();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Connection getConnection(String who) {
    if (cp.getActiveConnections() > 3) {
      logger.debug("get for " + who + " connection #" + cp.getActiveConnections());
    }
//    new Exception().printStackTrace();

    try {
      //if (cp.getActiveConnections() > 3) {
     // }
      long then = System.currentTimeMillis();
      Connection connection = conn != null ? conn : cp.getConnection();
      long now = System.currentTimeMillis();
      if (now-then > 200) {
        logger.debug("took " + (now - then) + " getConnection, currently " +cp.getActiveConnections() + " open.");
      }
      return connection;
    } catch (SQLException e) {
      logger.error("huh? couldn't get connection with cp",e);
      return null;
    }
  }
  public boolean isValid() { return conn != null; }
}
