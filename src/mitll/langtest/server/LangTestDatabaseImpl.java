package mitll.langtest.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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

  @Override
  public void init() {
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

  /**
   * @see mitll.langtest.client.ExercisePanel#postAnswers
   * @param userID
   * @param exercise
   * @param questionID
   * @param answer
   * @param audioFile
   */
  public void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile) {
    db.addAnswer(userID, exercise,questionID,answer,audioFile);
  }

  public long addUser(int age, String gender, int experience) {
    String ip = getThreadLocalRequest().getRemoteAddr();
    return db.addUser(age,gender,experience, ip);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID) {
    return db.isAnswerValid(userID, exercise, questionID, db);
  }

  public List<User> getUsers() { return db.getUsers(); }
  public List<Result> getResults() { return db.getResults(); }

  public void postArray(String base64EncodedByteArray) {
    Base64 decoder = new Base64();
    byte[] decoded = null;
    System.out.println("postArray : got " + base64EncodedByteArray.substring(0,Math.min(base64EncodedByteArray.length(), 20)) +"...");
	try {
		decoded = (byte[])decoder.decode(base64EncodedByteArray);
	} catch (DecoderException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    /*System.out.println("got " + base64EncodedByteArray.size());
    for (Integer b : base64EncodedByteArray) {
      System.out.println("got " + b);
    }*/
    File file = new File("test.wav");
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      outputStream.write(decoded);
   /*   for (Integer b : base64EncodedByteArray) {
        outputStream.write(b);
      }*/
      System.out.println("wrote " + file.getAbsolutePath());
      outputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }
}
