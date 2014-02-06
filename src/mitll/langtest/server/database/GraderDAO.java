package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mitll.langtest.shared.grade.Grader;

import org.apache.log4j.Logger;

/**
 * @deprecated there are no graders, just users
 */
public class GraderDAO {
  private static Logger logger = Logger.getLogger(GraderDAO.class);

  private final Database database;

  public GraderDAO(Database database) {
    this.database = database;
  }

  public Collection<Grader> getGraders() {
    List<Grader> graders = new ArrayList<Grader>();
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM grader ");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int i = 1;
        graders.add(new Grader((long) rs.getInt(i++), rs.getString(i), "", 0));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      if (e.getMessage().contains("not found")) {
        //logger.debug("note there is no grader table.");
      } else {
        logger.error("got " + e, e);
      }
    }
    return graders;
  }
}