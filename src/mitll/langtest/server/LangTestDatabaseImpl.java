package mitll.langtest.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.ServletContext;
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
  private static final String ANSWERS = "answers";
  private DatabaseImpl db;
  private AudioCheck audioCheck = new AudioCheck();

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
  public List<Result> getResults() {
    List<Result> results = db.getResults();
    for (Result r : results) {
      int answer = r.answer.indexOf(ANSWERS);
      if (answer == -1) continue;
      r.answer = r.answer.substring(answer);
    }
    return results;
  }

  /**
   * Decode Base64 string
   * @param base64EncodedByteArray
   * @return
   */
  private byte[] getBytesFromBase64String(String base64EncodedByteArray) {
    Base64 decoder = new Base64();
    byte[] decoded = null;
    //System.out.println("postArray : got " + base64EncodedByteArray.substring(0,Math.min(base64EncodedByteArray.length(), 20)) +"...");
   // decoded = (byte[])decoder.decode(base64EncodedByteArray);

    try {
      decoded = (byte[])decoder.decode(base64EncodedByteArray);
    } catch (DecoderException e1) {   // just b/c eclipse seems to insist
      e1.printStackTrace();
    }
    return decoded;
  }

  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user) {
    ServletContext context = getServletContext();
    String realContextPath = context.getRealPath(getThreadLocalRequest().getContextPath());

    realContextPath = realContextPath.replace("netPron2/netPron2","netPron2"); // hack for mtex!!!
    System.out.println("Deployed context is " + realContextPath);
    String wavPath = getLocalPathToAnswer(plan, exercise, question, user);
    File file = new File(realContextPath, wavPath);   // relative to deploy

    File parentFile = file.getParentFile();
   // System.out.println("making dir " + parentFile.getAbsolutePath());

    parentFile.mkdirs();

    byte [] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray,file);

    if (!file.exists()) {
      System.err.println("huh? can't find " + file.getAbsolutePath());
    }
    boolean valid = isValid(file);
    /*    if (!valid) {
    System.err.println("audio file " + file.getAbsolutePath() + " is *not* valid");
  }
  else {
    System.out.println("audio file " + file.getAbsolutePath() + " is valid");
  }*/
    db.answerDAO.addAnswer(Integer.parseInt(user), plan,exercise,Integer.parseInt(question),"",file.getPath(), valid, db);
    return new AudioAnswer(wavPath.replaceAll("\\\\","/"), valid);
  }

/*  public String getPathToAnswer(String plan, String exercise, String question, String user) {
    return getLocalPathToAnswer(plan,exercise,question,user).replaceAll("\\\\","/");
  }*/

  private String getLocalPathToAnswer(String plan, String exercise, String question, String user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-"+user;
    String currentTestDir = tomcatWriteDirectory + File.separator  + planAndTestPath;
    String wavPath = currentTestDir + File.separator + "answer_" + System.currentTimeMillis() + ".wav";
    File audioFilePath = new File(currentTestDir);
    boolean mkdirs = audioFilePath.mkdirs();

    return wavPath;
  }

  private String getTomcatDir() {
    String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = ANSWERS;

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = ANSWERS;
    }
    return tomcatWriteDirectory;
  }

  private void writeToFile(byte [] byteArray, File file) {
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      outputStream.write(byteArray);
//      System.out.println("wrote " + file.getAbsolutePath());
      outputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isValid(File file) {
    try {
      return audioCheck.checkWavFile(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }

  public static void main(String [] arg) {
    System.out.println("x\\y\\z".replaceAll("\\\\","/"));

    LangTestDatabaseImpl langTestDatabase = new LangTestDatabaseImpl();
    langTestDatabase.init();
    langTestDatabase.getResults();
  }
}
