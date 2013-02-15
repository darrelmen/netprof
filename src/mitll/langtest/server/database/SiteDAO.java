package mitll.langtest.server.database;

import mitll.langtest.shared.Site;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/13/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteDAO extends DAO {
  private static Logger logger = Logger.getLogger(SiteDAO.class);

  private boolean debug = false;

  public SiteDAO(Database database) {
    super(database);
  }

  /**
   * @see DatabaseImpl#addSite(mitll.langtest.shared.Site)
   * @param site
   * @return
   */
  public Site addSite(Site site) {
    return addSite(site.creatorID,site.name,site.language,site.notes,site.exerciseFile,site.savedExerciseFile, site.getFeedback(), false);
  }
  public Site addSite(long creatorID, String name, String language, String notes, String file, String filePath, String feedback, boolean deployed) {
    long id = 0;
    try {
      Connection connection = database.getConnection();
      String sql = "INSERT INTO site(creatorID,name,language,notes,file,filepath,feedback,deployed,creationDate) VALUES(?,?,?,?,?,?,?,?,?)";

      PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setLong(i++, creatorID);
      statement.setString(i++, name);
      statement.setString(i++, language);
      statement.setString(i++, notes);
      statement.setString(i++, file);
      statement.setString(i++, filePath);
      statement.setString(i++, feedback);
      statement.setBoolean(i++, deployed);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + name);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        System.err.println("huh? no key was generated?");
      }
      Site site = new Site(id, creatorID, name, language, notes, file,filePath, feedback, false, System.currentTimeMillis());
      statement.close();
      database.closeConnection(connection);

      for (Site s: getSites()) {
        logger.info("now " +s);
      }
      return site;
    } catch (Exception e) {
      logger.error("addSite: got " +e,e);
    }
    return null;
  }


  /**
   *       String sql = "INSERT INTO site(creatorID,name,language,notes,file) VALUES(?,?,?,?,?)";
   * @return
   */
  public Collection<Site> getSites() {
      try {
        Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement("select * from site");

        ResultSet rs = statement.executeQuery();
        List<Site> sites = new ArrayList<Site>();
        while (rs.next()) {
          int i = 1;
          long id = rs.getLong(i++);
          long creatorID = rs.getLong(i++);
          String name = rs.getString(i++);
          String language = rs.getString(i++);
          String notes = rs.getString(i++);
          String file = rs.getString(i++);
          String filePath = rs.getString(i++);
          String feedback = rs.getString(i++);
          boolean deployed = rs.getBoolean(i++);
          long timestamp = rs.getTimestamp(i++).getTime();
          sites.add(new Site(id, creatorID, name,language, notes,file,filePath, feedback, deployed,timestamp));
        }
        rs.close();
        statement.close();
        database.closeConnection(connection);

        return sites;
      } catch (Exception ee) {
        ee.printStackTrace();
      }
      return null;
    }

  public Site getSiteByID(long id) {
    for (Site s : getSites()) {
      //logger.info("checking " + s + " against " + id);
      if (s.id == id) return s;
    }
    logger.error("couldn't find site with id " +id);
    return null;
  }

  public List<Site> getDeployedSites() {
    List<Site> sites = new ArrayList<Site>();

    for (Site s : getSites()) {
      //logger.info("checking " + s + " against " + id);
      if (s.isDeployed()) sites.add(s);
    }
    logger.info("deployed num = " +sites.size());
    return sites;
  }

  public void deploy(Site toChange) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "UPDATE site " +
          "SET deployed=true " +
          "WHERE id=" + toChange.id;
      logger.debug("deploy " + toChange);
      statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (debug) System.out.println("UPDATE " + i);
      if (i == 0) {
        System.err.println("huh? didn't update the grade for " + toChange);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    //return new CountAndGradeID(getCount(), id);
  }

  /**
   *       String sql = "INSERT INTO site(creatorID,name,language,notes,file) VALUES(?,?,?,?,?)";
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs()
   * @param connection
   * @throws SQLException
   */
  public void createTable(Connection connection) throws SQLException {
    //drop(connection);
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
        "site (id IDENTITY, " +
        "creatorID INT, " +
        "name VARCHAR, " +
        "language VARCHAR, " +
        "notes VARCHAR, " +
        "file VARCHAR, " +
        "filepath VARCHAR, " +
        "feedback VARCHAR," +
        "deployed BOOLEAN," +
        "creationDate TIMESTAMP " +
        ")");
    boolean execute = statement.execute();


//    if (!execute) logger.error("huh? didn't do create table?");
    statement.close();

    int numColumns = getNumColumns(connection, "site");
    if (numColumns < 8) {
      addColumnToTable(connection);
    } else if (numColumns < 10) {
      statement = connection.prepareStatement("ALTER TABLE site ADD deployed BOOLEAN");
      statement.execute();
      statement.close();

      statement = connection.prepareStatement("ALTER TABLE site ADD creationDate TIMESTAMP");
      statement.execute();
      statement.close();
    }
  }

  private void drop(Connection connection) {
    try {
      logger.error("----------- dropUserTable -------------------- ");
      PreparedStatement statement;
      statement = connection.prepareStatement("drop TABLE site");
      statement.execute();
      statement.close();
      database.closeConnection(connection);
    } catch (SQLException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE site ADD feedback VARCHAR");
    statement.execute();
    statement.close();
  }
}
