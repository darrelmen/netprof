package mitll.langtest.server.database;

import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/31/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class H2Connection implements DatabaseConnection {
  private static Logger logger = Logger.getLogger(H2Connection.class);

  private Connection conn;

  public H2Connection(String h2FilePath) {
    connect(h2FilePath);
  }

 public H2Connection(String configDir, String dbName) {
    connect(configDir, dbName);
  }

  private void connect(String configDir, String database) {
    String h2FilePath = configDir +File.separator+ database;
    connect(h2FilePath);
  }

  /**
   *   //jdbc:h2:file:/Users/go22670/DLITest/clean/netPron2/war/config/urdu/vlr-parle;IFEXISTS=TRUE;CACHE_SIZE=30000
   * @param h2FilePath
   */
  private void connect(String h2FilePath) {
    String url = "jdbc:h2:file:" + h2FilePath +
        ";IFEXISTS=TRUE;" +
        "QUERY_CACHE_SIZE=0;" +
        "CACHE_SIZE=20000";

    logger.debug("connecting to " + url);
    org.h2.Driver.load();
    try {
      conn = DriverManager.getConnection(url, "", "");
      conn.setAutoCommit(true);

    } catch (SQLException e) {
      conn = null;
      logger.error("got error trying to create h2 connection with URL '" + url + "', exception = " +e,e);
    }
  }

  @Override
  public void contextDestroyed() {
    if (conn == null) {
      logger.info("not never successfully created h2 connection ");
    } else {
      sendShutdown();
      closeConnection();
      org.h2.Driver.unload();
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

  public Connection getConnection() {
    return conn;
  }

  public boolean isValid() { return conn != null; }
}
