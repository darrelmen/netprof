package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRelatedResult;
import mitll.npdata.dao.dialog.RelatedResultDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class RelatedResultDAO extends DAO implements IRelatedResultDAO {
  // private static final Logger logger = LogManager.getLogger(RelatedResultDAO.class);
  private final RelatedResultDAOWrapper dao;

  /**
   * @param database
   * @param dbConnection
   * @see DatabaseImpl#makeDialogDAOs
   */
  public RelatedResultDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new RelatedResultDAOWrapper(dbConnection);
  }

  /**
   * @param resultid
   * @param dialogsessionid
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#rememberAnswer
   */
  @Override
  public int add(int resultid, int dialogsessionid) {
    return dao.insert(new SlickRelatedResult(
        -1,
        resultid,
        dialogsessionid, DialogStatus.DEFAULT.toString(), new Timestamp(System.currentTimeMillis())
    ));
  }

  @Override
  public Map<Integer, List<SlickRelatedResult>> getByProjectForDialogForUser(int projid, int dialogid, int userid) {
    return dao.byProjectForDialogForUser(projid, dialogid, userid);
  }

  @Override
  public SlickRelatedResult latestByProjectForDialogForUser(int projid, int dialogid, int userid) {
    List<SlickRelatedResult> slickRelatedResults = dao.latestByProjectForDialogForUser(projid, dialogid, userid);
    if (slickRelatedResults.isEmpty()) return null;
    else return slickRelatedResults.iterator().next();
  }

  // only marks status as deleted
  @Override
  public void removeForProject(int id) {
    dao.deleteForProject(id);
  }

  @Override
  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }
}
