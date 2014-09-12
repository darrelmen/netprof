package testing;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Note with H2 that :  <br></br>
 * * you can corrupt the database if you try to copy a file that's in use by another process. <br></br>
 * * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 * more easily when H2 lives inside a servlet container (e.g. tomcat). <br></br>
 * * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 * H2 will return "new" connections that have already been closed.   <br></br>
 * * it's not a good idea to reuse one connection...?  <br></br>
 * <p/>
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallDatabaseImpl implements Database {
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);
  private static final boolean TESTING = false;

  //private static final boolean DROP_USER = false;
  private static final String H2_DB_NAME = TESTING ? "vlr-parle" : "/services/apache-tomcat-7.0.27/webapps/langTest/vlr-parle";
  // h2 config info
  private String url = "jdbc:h2:" + H2_DB_NAME + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;";
  private final String dbOptions = "";//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
      private final String driver = "org.h2.Driver";

  private HttpServlet servlet;
  private String h2DbName = H2_DB_NAME;

  public SmallDatabaseImpl(String dburl) {
    this.h2DbName = dburl;
    this.url = "jdbc:h2:" + h2DbName + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;";
    try {
      getConnection(this.getClass().toString());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }


  @Override
  public void logEvent(String exid, String context, long userid) {

  }

  /**
   * @param s
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public SmallDatabaseImpl(HttpServlet s) {
    this.servlet = s;
    try {
      boolean open = getConnection(this.getClass().toString()) != null;
      if (!open) {
        logger.warn("couldn't open connection to database");
        return;
      }
    } catch (Exception e) {
      logger.warn("couldn't open connection to database, got " + e.getMessage());
      logger.error("got " + e, e);
      return;
    }

/*
    if (DROP_USER) {
      try {
        UserDAO userDAO = new UserDAO(this);
        userDAO.dropUserTable();
        userDAO.createUserTable(this);
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
    }
*/

    try {
      UserDAO userDAO = new UserDAO(this);
      userDAO.createUserTable(this);
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }
  }

    /**
     * Just for dbLogin
     */
  private Connection localConnection;
  /**
   * Not necessary if we use the h2 DBStarter service -- see web.xml reference
   *
   * @return
   * @throws Exception
   */
  private Connection dbLogin() /*throws Exception*/ {
    if (localConnection != null) return localConnection;
    try {
      Class.forName(driver).newInstance();
      try {
        url = servlet.getServletContext().getInitParameter("db.url"); // from web.xml
      } catch (Exception e) {
        logger.warn("no servlet context?");
      }
      logger.info("connecting to " + url);

     // GWT.log("connecting to " + url);
      File f = new java.io.File(h2DbName + ".h2.db");
      if (!f.exists()) {
        String s = "huh? no file at " + f.getAbsolutePath();
        logger.warn(s);

       // GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url + dbOptions);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      if (closed) {
        logger.warn("connection is closed to : " + url);
      }
      this.localConnection = connection;
      return connection;
    } catch (Exception ex) {
      logger.error("Got " +ex,ex);
    }
    return null;
  }

  public Connection getConnection(String who) {
    Connection c;
    try {
      if (servlet == null) {
        c = this.dbLogin();
      } else {
        ServletContext servletContext = servlet.getServletContext();
        c = (Connection) servletContext.getAttribute("connection");
      }
    } catch (Exception e) {  // for standalone testing
      logger.warn("The context DBStarter is not working : " + e.getMessage(), e);
      c = this.dbLogin();
    }
    if (c == null) {
      return c;
    }
    try {
      c.setAutoCommit(true);
      if (c.isClosed()) {
        logger.warn("getConnection : conn " + c + " is closed!");
      }
    } catch (SQLException e) {
      logger.error("got " +e,e);
    }
    return c;
  }


  public void closeConnection(Connection connection)  {}
}
