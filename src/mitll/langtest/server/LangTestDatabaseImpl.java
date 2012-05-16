package mitll.langtest.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.Exercise;

import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase {
  private DatabaseImpl db;

  public LangTestDatabaseImpl() {
    db = new DatabaseImpl(this);
  }

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad
   * @return
   * @param userID
   */
  public List<Exercise> getExercises(long userID) {
    return db.getExercises(userID);
  }

  public void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile) {
    db.addAnswer(userID, exercise,questionID,answer,audioFile);
  }

  public long addUser(int age, String gender, int experience) {
    String ip = getThreadLocalRequest().getRemoteAddr();
    return db.addUser(age,gender,experience, ip);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID) {
    return db.isAnswerValid(userID, exercise, questionID);
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }
}
