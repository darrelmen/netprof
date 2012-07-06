package mitll.langtest.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

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
  public List<Result> getResults() { return db.getResults(); }

  public void postArray(List<Integer> byteArray) {
    System.out.println("got " + byteArray.size());
    for (Integer b : byteArray) {
      System.out.println("got " + b);
    }
    File file = new File("test.wav");
    writeToFile(byteArray, file);
  }

  public boolean writeAudioFile(List<Integer> byteArray, String plan, String exercise, String question, String user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-"+user;
    String currentTestDir = tomcatWriteDirectory + File.separator  + planAndTestPath;
    File audioFilePath = new File(currentTestDir);
    audioFilePath.mkdirs();

    File file = writeAudioFile(byteArray, "answer", audioFilePath);
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
    return valid;
  }
  //	}
  //	}
//		catch(Exception ex){
//			ex.printStackTrace();
//		}
  //}



  private String getTomcatDir() {
    String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
    //	String pretestFilesRelativePath = getServletContext().getInitParameter("pretestFilesRelativePath");  // likely = pretest_files
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = "answers";

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = "answers";
    }
    return tomcatWriteDirectory;
  }


  private File writeAudioFile( List<Integer> byteArray,String base, File audioFilePath) {
    File file = new File(audioFilePath.getPath() + File.separator + base + ".wav");
    //  item.write(file);
    writeToFile(byteArray,file);
    //isValid(file);
    return file;
  }

  private void writeToFile(List<Integer> byteArray, File file) {
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      for (Integer b : byteArray) {
        outputStream.write(b);
      }
      System.out.println("wrote " + file.getAbsolutePath());
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
}
