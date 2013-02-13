package mitll.langtest.server.database;

import mitll.langtest.shared.Site;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

  public Site addSite(Site site) {
    return addSite(site.creatorID,site.name,site.language,site.notes,site.exerciseFile);
  }
  public Site addSite(long creatorID, String name, String language, String notes, String file) {
    long id = 0;
    try {
      Connection connection = database.getConnection();
      String sql = "INSERT INTO site(creatorID,name,language,notes,file) VALUES(?,?,?,?,?)";


      PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setLong(i++, creatorID);
      statement.setString(i++, name);
      statement.setString(i++, language);
      statement.setString(i++, notes);
      statement.setString(i++, file);
      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + name);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        System.err.println("huh? no key was generated?");
      }
      Site site = new Site(id, creatorID, name, language, notes, file);
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
          sites.add(new Site(id, creatorID, name,language, notes,file));
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
      logger.info("checking " + s + " against " + id);
      if (s.id == id) return s;
    }
    logger.error("couldn't find site with id " +id);
    return null;
  }

  /**
   *       String sql = "INSERT INTO site(creatorID,name,language,notes,file) VALUES(?,?,?,?,?)";

   * @param connection
   * @throws SQLException
   */
  public void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
        "site (id IDENTITY, " +
        "creatorID INT, " +
        "name VARCHAR, " +
        "language VARCHAR, " +
        "notes VARCHAR, " +
        "file VARCHAR" +
        ")");
    boolean execute = statement.execute();
//    if (!execute) logger.error("huh? didn't do create table?");
    statement.close();
  }
}
