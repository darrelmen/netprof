package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRelatedResult;
import mitll.npdata.dao.dialog.RelatedResultDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;

public class RelatedResultDAO extends DAO implements IRelatedResultDAO {
 // private static final Logger logger = LogManager.getLogger(RelatedResultDAO.class);
  private final RelatedResultDAOWrapper dao;


  public RelatedResultDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new RelatedResultDAOWrapper(dbConnection);
  }

  @Override
  public int add(int resultid, int dialogsessionid) {
    return dao.insert(new SlickRelatedResult(
        -1,
        resultid,
        dialogsessionid, DialogStatus.DEFAULT.toString(), new Timestamp(System.currentTimeMillis())
    ));
  }

  // only marks status as deleted
  @Override
  public void removeForProject(int id) {
    dao.deleteForProject(id);
  }

 @Override public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }
}
