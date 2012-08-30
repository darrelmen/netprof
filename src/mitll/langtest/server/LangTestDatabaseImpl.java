package mitll.langtest.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


/**
 * Supports all the database interactions.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase {
  public static final String ANSWERS = "answers";
  public static final int TIMEOUT = 30;
  public static final String DEFAULT_APP_NAME = "netPron2";
  private DatabaseImpl db;

  private Cache<String, String> userToExerciseID = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .maximumSize(10000)
      .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES).build();

  @Override
  public void init() {
    db = new DatabaseImpl(this);
  }

  /**
   * @param userID
   * @return
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
  public List<Exercise> getExercises(long userID) {
    return db.getExercises(userID);
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#loadGradingExercises()
   * @return
   */
  public List<Exercise> getExercises() {
    return db.getExercises();
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   *
   * @see mitll.langtest.client.exercise.ExerciseList#getNextUngraded()
   * @param user
   * @param expectedGrades
   * @return
   */
  public Exercise getNextUngradedExercise(String user, int expectedGrades) {
    synchronized (this) {
      ConcurrentMap<String,String> stringStringConcurrentMap = userToExerciseID.asMap();
      Collection<String> values = stringStringConcurrentMap.values();
      String currentExerciseForUser = userToExerciseID.getIfPresent(user);
      //System.out.println("for " + user + " current " + currentExerciseForUser);

      Collection<String> currentActiveExercises = new HashSet<String>(values);

      if (currentExerciseForUser != null) {
        currentActiveExercises.remove(currentExerciseForUser); // it's OK to include the one the user is working on now...
      }
      //System.out.println("current set minus " + user + " is " + currentActiveExercises);

      return db.getNextUngradedExercise(currentActiveExercises, expectedGrades);
    }
  }

  public void checkoutExerciseID(String user, String id) {
    synchronized (this) {
      userToExerciseID.put(user, id);
      System.out.println("after adding " + user + "->" + id + " active exercise map now " + userToExerciseID.asMap());
    }
  }

  /**
   * @param exid
   * @return
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public ResultsAndGrades getResultsForExercise(String exid) {
    ResultsAndGrades resultsForExercise = db.getResultsForExercise(exid);
    ensureMP3(resultsForExercise.results);
    return resultsForExercise;
  }

  /**
   * Make sure we have mp3 files in results.
   * @param results
   */
  private void ensureMP3(Collection<Result> results) {
    for (Result r : results) {
      if (r.answer.endsWith(".wav")) {
        ensureWriteMP3(r.answer);
      }
    }
  }

  /**
   * @param userID
   * @param exercise
   * @param questionID
   * @param answer
   * @param audioFile
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   */
  public void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile) {
    db.addAnswer(userID, exercise, questionID, answer, audioFile);
  }

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#addGrade
   * @param exerciseID
   * @return
   */
/*  public CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType) {
    return db.addGrade(resultID, exerciseID, grade, gradeID, correct, grader, gradeType);
  }*/

  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {
    return db.addGrade(exerciseID, toAdd);
  }

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#changeGrade(mitll.langtest.shared.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) {
    db.changeGrade(toChange);
  }


  public void addGrader(String login) {
    db.addGrader(login);
  }

  public boolean graderExists(String login) {
    return db.graderExists(login);
  }

  public long addUser(int age, String gender, int experience) {
    HttpServletRequest request = getThreadLocalRequest();
    // String header = request.getHeader("X-FORWARDED-FOR");
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    String ip = request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
    return db.addUser(age, gender, experience, ip);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID) {
    return db.isAnswerValid(userID, exercise, questionID, db);
  }

  public List<User> getUsers() {
    return db.getUsers();
  }

  /**
   * @return
   * @see mitll.langtest.client.ResultManager#showResults()
   */
  public List<Result> getResults() {
    List<Result> results = db.getResults();
    return results;
  }

  /**
   * Writes an mp3 equivalent as well.
   *
   * @param base64EncodedString
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return
   */
  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user) {
    String wavPath = getLocalPathToAnswer(plan, exercise, question, user);

    File file = getAbsolutePathToWav(wavPath);

    boolean valid = new AudioConversion().convertBase64ToAudioFiles(base64EncodedString, file);
    /*    if (!valid) {
    System.err.println("audio file " + file.getAbsolutePath() + " is *not* valid");
  }
  else {
    System.out.println("audio file " + file.getAbsolutePath() + " is valid");
  }*/
    db.answerDAO.addAnswer(Integer.parseInt(user), plan, exercise, Integer.parseInt(question), "", file.getPath(), valid, db);
    String wavPathWithForwardSlashSeparators = wavPath.replaceAll("\\\\", "/");
    return new AudioAnswer(wavPathWithForwardSlashSeparators, valid);
  }

  private File getAbsolutePathToWav(String wavPath) {
    ServletContext context = getServletContext();
    String realContextPath = context.getRealPath(getThreadLocalRequest().getContextPath());

    String appName = getServletContext().getInitParameter("appName");
    if (appName == null) appName = DEFAULT_APP_NAME;
    realContextPath = realContextPath.replace(appName +"/" + appName, appName);
    return new File(realContextPath, wavPath);
  }

  /**
   * Checks if file exists already...
   * @param pathToWav
   */
  private void ensureWriteMP3(String pathToWav) {
    AudioConversion audioConversion = new AudioConversion();
    File absolutePathToWav = getAbsolutePathToWav(pathToWav);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav",".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists()) {
      audioConversion.writeMP3(absolutePathToWav.getAbsolutePath());
    }

    if (WRITE_ALTERNATE_COMPRESSED_AUDIO) {
       audioConversion.writeCompressed(absolutePathToWav.getAbsolutePath());
    }
  }

  private String getLocalPathToAnswer(String plan, String exercise, String question, String user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-" + user;
    String currentTestDir = tomcatWriteDirectory + File.separator + planAndTestPath;
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

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }

  public static void main(String[] arg) {
    //System.out.println("x\\y\\z".replaceAll("\\\\", "/"));

    LangTestDatabaseImpl langTestDatabase = new LangTestDatabaseImpl();
    langTestDatabase.init();

    String fred = langTestDatabase.userToExerciseID.getIfPresent("fred");
    System.out.println("Val " + fred);
    langTestDatabase.userToExerciseID.put("fred","Barney");
    fred = langTestDatabase.userToExerciseID.getIfPresent("fred");
    System.out.println("Val " + fred);
    try {
      Object o = new Object();
      synchronized (o) {
        o.wait(6000);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    fred = langTestDatabase.userToExerciseID.getIfPresent("fred");
    System.out.println("Val " + fred);
  }


}
