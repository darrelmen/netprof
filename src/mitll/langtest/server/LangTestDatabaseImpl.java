package mitll.langtest.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.*;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
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
  private DatabaseImpl db;
  private AudioCheck audioCheck = new AudioCheck();

  private static final String LAME_PATH_WINDOWS = "C:\\Users\\go22670\\lame\\lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";

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

  public List<Exercise> getExercises() {
    return db.getExercises();
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   * @param user
   * @return
   */
  public Exercise getNextUngradedExercise(String user) {
    synchronized (this) {
      ConcurrentMap<String,String> stringStringConcurrentMap = userToExerciseID.asMap();
      Collection<String> values = stringStringConcurrentMap.values();
      String currentExerciseForUser = userToExerciseID.getIfPresent(user);
      System.out.println("for " + user + " current " + currentExerciseForUser);

      Collection<String> currentActiveExercises = new HashSet<String>(values);

      if (currentExerciseForUser != null) {
        currentActiveExercises.remove(currentExerciseForUser); // it's OK to include the one the user is working on now...
      }
      System.out.println("current set minus " + user + " is " + currentActiveExercises);

      return db.getNextUngradedExercise(currentActiveExercises);
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
   * @see mitll.langtest.client.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.ExerciseController, int)
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
   * @see mitll.langtest.client.ExercisePanel#postAnswers
   */
  public void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile) {
    db.addAnswer(userID, exercise, questionID, answer, audioFile);
  }

  public int addGrade(int resultID, String exerciseID, int grade, boolean correct) {
    return db.addGrade(resultID, exerciseID, grade, correct);
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
   // ensureMP3(results);
    return results;
  }

  /**
   * Decode Base64 string
   *
   * @param base64EncodedByteArray
   * @return
   */
  private byte[] getBytesFromBase64String(String base64EncodedByteArray) {
    Base64 decoder = new Base64();
    byte[] decoded = null;
    //System.out.println("postArray : got " + base64EncodedByteArray.substring(0,Math.min(base64EncodedByteArray.length(), 20)) +"...");
    // decoded = (byte[])decoder.decode(base64EncodedByteArray);

   try {
      decoded = (byte[]) decoder.decode(base64EncodedByteArray);
    } catch (DecoderException e1) {   // just b/c eclipse seems to insist
      e1.printStackTrace();
    }
    return decoded;
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

    File parentFile = file.getParentFile();
    // System.out.println("making dir " + parentFile.getAbsolutePath());

    parentFile.mkdirs();

    byte[] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray, file);

    if (!file.exists()) {
      System.err.println("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    boolean valid = isValid(file);

    writeMP3(file.getAbsolutePath());
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
    if (appName == null) appName = "netPron2";
    realContextPath = realContextPath.replace(appName +"/" + appName, appName);
    return new File(realContextPath, wavPath);
  }

  /**
   * Checks if file exists already...
   * @param pathToWav
   */
  private void ensureWriteMP3(String pathToWav) {
    File absolutePathToWav = getAbsolutePathToWav(pathToWav);
    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav",".mp3");
    if (new File(mp3File).exists()) return;
    writeMP3(absolutePathToWav.getAbsolutePath());
  }

  /**
   * Use lame to write an mp3 file.
   * @param pathToWav
   */
  private void writeMP3(String pathToWav) {
    String mp3File = pathToWav.replace(".wav",".mp3");
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      System.err.println("no lame installed at " + lamePath + " or " +LAME_PATH_WINDOWS);
    }

/*    System.out.println("using " +lamePath +" audio :'" +pathToWav +
        "' mp3 '" +mp3File+
        "'");*/
    writeMP3(lamePath, pathToWav, mp3File);
  }

  private void writeMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File);
    try {
  //    System.out.println("writeMP3 running lame" + lameProc.command());
      runProcess(lameProc);
 //     System.out.println("writeMP3 exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
   //   System.out.println("Wrote to " + testMP3);
    }
  }

  private void runProcess(ProcessBuilder shellProc) throws IOException {
    //System.out.println(new Date() + " : proc " + shellProc.command() + " started...");

    shellProc.redirectErrorStream(true);
    Process process2 = shellProc.start();

    // read the output
    InputStream stdout = process2.getInputStream();
    readFromStream(stdout, false);
    InputStream errorStream = process2.getErrorStream();
    readFromStream(errorStream, true);

    process2.destroy();
    //System.out.println(new Date() + " : proc " + shellProc.command() + " finished");
  }

  private void readFromStream(InputStream is2, boolean showOutput) throws IOException {
    InputStreamReader isr2 = new InputStreamReader(is2);
    BufferedReader br2 = new BufferedReader(isr2);
    String line2;
    while ((line2 = br2.readLine()) != null) {
      if (showOutput) System.err.println(line2);
    }
    br2.close();
  }

/*  public String getPathToAnswer(String plan, String exercise, String question, String user) {
    return getLocalPathToAnswer(plan,exercise,question,user).replaceAll("\\\\","/");
  }*/

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

  private void writeToFile(byte[] byteArray, File file) {
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

  public static void main(String[] arg) {
    //System.out.println("x\\y\\z".replaceAll("\\\\", "/"));

    LangTestDatabaseImpl langTestDatabase = new LangTestDatabaseImpl();
    //langTestDatabase.init();

    langTestDatabase.writeMP3("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");
    //langTestDatabase.writeMP3("C:\Users\go22670\DLITest\LangTest\war\answers\test\ac-LC1-001\1\subject-460\answer_1345134729569.wav");

    if (true) return;
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
