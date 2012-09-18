package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.DTWScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.User;
import mitll.langtest.shared.scoring.PretestScore;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
  public static final String IMAGE_WRITER_IMAGES = "audioimages";
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
   * @see mitll.langtest.client.exercise.ExerciseList#getExercisesInOrder()
   * @return
   */
  public List<Exercise> getExercises() {
    return db.getExercises();
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   *
   * @seex mitll.langtest.client.exercise.ExerciseList#getNextUngraded
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
   * Get an image of desired dimensions for the audio file
   *
   * @see mitll.langtest.client.goodwave.AudioPanel#getImageURLForAudio
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height     @return path to an image file
   * */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height) {
    ImageWriter imageWriter = new ImageWriter();
    //System.out.println("getImageForAudioFile : for " + audioFile);

    if (audioFile.endsWith(".mp3")) {
      String wavFile = removeSuffix(audioFile) +".wav";
      File test = getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : getWavForMP3(audioFile);
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM :
                imageType.equalsIgnoreCase(ImageType.WORD_TRANSCRIPT.toString()) ? ImageType.WORD_TRANSCRIPT :
                    imageType.equalsIgnoreCase(ImageType.PHONE_TRANSCRIPT.toString()) ? ImageType.PHONE_TRANSCRIPT :
                        imageType.equalsIgnoreCase(ImageType.SPEECH_TRANSCRIPT.toString()) ? ImageType.SPEECH_TRANSCRIPT : null;

    String imageOutDir = getImageOutDir();
    String absolutePathToImage = imageWriter.writeImageSimple(audioFile, getAbsoluteFile(imageOutDir).getAbsolutePath(), width, height, imageType1);
    String installPath = getInstallPath();
    assert (absolutePathToImage.startsWith(installPath));

    String relativeImagePath = ensureForwardSlashes(absolutePathToImage.substring(installPath.length()));
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    // +1 removes initial slash!
    System.out.println("rel path is " + relativeImagePath);
    return new ImageResponse(reqid,relativeImagePath);
  }

  /**
   * @see mitll.langtest.client.goodwave.AudioPanel#getTranscriptImageURLForAudio(String, String, int, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck)
   * @param reqid
   *@param audioFile
   * @param refs
   * @param width
   * @param height
   * @return
   */
  public PretestScore getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height) {
    System.out.println("DTW getScoreForAudioFile " + audioFile + " against " +refs);
    if (refs.isEmpty()) {
      System.err.println("DTW getScoreForAudioFile no refs? ");
      PretestScore pretestScore = new PretestScore();
      pretestScore.setReqid(reqid);
      return pretestScore;
    }
    String installPath = getInstallPath();
    File testAudioFile = getProperAudioFile(audioFile, installPath);

    System.out.println("DTW scoring after conversion " + testAudioFile.getAbsolutePath());
    String name = testAudioFile.getName();

    String imageOutDir = getImageOutDir();
    String testAudioDir = testAudioFile.getParent();

    System.out.println("DTW scoring " + name + " in dir " +testAudioDir);

    Collection<String> names = new ArrayList<String>();
    String refAudioDir = null;

    for (String ref : refs) {
      File properAudioFile = getProperAudioFile(ref, installPath);
      if (refAudioDir == null) refAudioDir = properAudioFile.getParent();
      names.add(removeSuffix(properAudioFile.getName()));
    }
    System.out.println("converted refs " + refs +" into " + names);

    if (names.isEmpty()) {
      System.err.println("no valid ref files");
      return new PretestScore();
    } else {
      DTWScoring dtwScoring = new DTWScoring(installPath);
      PretestScore pretestScore =
          dtwScoring.score(testAudioDir, removeSuffix(name), refAudioDir, names, imageOutDir, width, height);
      pretestScore.setReqid(reqid);
      System.out.println("score " + pretestScore);
      return pretestScore;
    }
  }

  private File getProperAudioFile(String audioFile, String installPath) {
    // check the path of the audio file!
    File t = new File(audioFile);
    if (!t.exists()) {
      System.out.println("DTW getProperAudioFile " + t.getAbsolutePath() + " doesn't exist");
    }
    // make sure it's under the deploy location/install path
    if (!audioFile.startsWith(installPath)) {
      audioFile = installPath + File.separator + audioFile;
    }
    String noSuffix = removeSuffix(audioFile);

    // convert it to wav, if needed
    if (audioFile.endsWith(".mp3")) {
      System.out.println("converting " +audioFile + " to wav ");
      String wavFile = noSuffix +".wav";
      File test = new File(wavFile);

    //  File test = getAbsoluteFile(wavFile);
      //File file = new AudioConversion().convertMP3ToWav(audioFile);

      audioFile = test.exists() ? test.getAbsolutePath() : new AudioConversion().convertMP3ToWav(audioFile).getAbsolutePath();
    }

    File testAudioFile = new File(audioFile);

    //  String parent1 = testAudioFile.getParent();
    //System.out.println("DTW test audio is " + testAudioFile + " parent " + parent1);

    // convert it to 16K sample rate, if needed
    try {
      File converted = new AudioConversion().convertTo16Khz(testAudioFile);
      System.out.println("DTW test audio is " + testAudioFile.getAbsolutePath() + " converted " + converted.getAbsolutePath());
      testAudioFile = converted;
    } catch (UnsupportedAudioFileException e) {
      System.err.println("DTW couldn't convert " + testAudioFile.getAbsolutePath() + " : " + e.getMessage());
    }

    if (!testAudioFile.exists()) {
      System.err.println("DTW getProperAudioFile " + testAudioFile.getAbsolutePath() + " doesn't exist????");

    }
    return testAudioFile;
  }

  /**
   * @see mitll.langtest.client.goodwave.AudioPanel#getTranscriptImageURLForAudio(String, String, int, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck, mitll.langtest.client.goodwave.AudioPanel.ImageAndCheck)
   * @param reqid
   * @param audioFile
   * @param width
   * @param height
   * @return
   */
  public PretestScore getScoreForAudioFile(int reqid, String audioFile, int width, int height) {
    String noSuffix = removeSuffix(audioFile);
    if (audioFile.endsWith(".mp3")) {
      String wavFile = noSuffix +".wav";
      File test = getAbsoluteFile(wavFile);
        audioFile = test.exists() ? test.getAbsolutePath() :  getWavForMP3(audioFile);
    }

    File testAudioFile = new File(audioFile);
    String name = testAudioFile.getName();


    String installPath = getInstallPath();
    String imageOutDir = getImageOutDir();
    String testAudioDir = testAudioFile.getParent().substring(installPath.length());

    System.out.println("scoring " + name + " in dir " +testAudioDir);
    // TODO : pass in reference audio and reference audio directory!
    PretestScore pretestScore = new ASRScoring(installPath).scoreRepeat(testAudioDir, removeSuffix(name), testAudioDir, removeSuffix(name), imageOutDir, width, height);
    pretestScore.setReqid(reqid);

    System.out.println("score " + pretestScore);
    return pretestScore;
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - ".mp3".length());
  }

  private String getWavForMP3(String audioFile) {
    return getWavForMP3(audioFile, getInstallPath());
  }

  /**
    * Ultimately does lame --decode from.mp3 to.wav
    * @param audioFile to convert
    * @return
    */
  private String getWavForMP3(String audioFile, String installPath) {
    assert(audioFile.endsWith(".mp3"));
    AudioConversion audioConversion = new AudioConversion();
    String absolutePath = getAbsolute(audioFile,installPath).getAbsolutePath();

    if (!new File(absolutePath).exists())
      System.err.println("expecting file at " + absolutePath);
    else {
      File file = audioConversion.convertMP3ToWav(absolutePath);
      if (file.exists()) {
       // String orig = audioFile;
        audioFile = file.getAbsolutePath();
        //System.out.println("getImageForAudioFile : from " + orig + " wrote to " + file + " or " + audioFile);
      }
      else {
        System.err.println("getImageForAudioFile : can't find " +  file.getAbsolutePath());
      }
    }
    assert(audioFile.endsWith(".wav"));
    return audioFile;
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
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param base64EncodedString
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return relative path with forward slashes
   */
  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user) {
    String wavPath = getLocalPathToAnswer(plan, exercise, question, user);

    File file = getAbsoluteFile(wavPath);

    boolean valid = new AudioConversion().convertBase64ToAudioFiles(base64EncodedString, file);
    /*    if (!valid) {
    System.err.println("audio file " + file.getAbsolutePath() + " is *not* valid");
  }
  else {
    System.out.println("audio file " + file.getAbsolutePath() + " is valid");
  }*/
    db.answerDAO.addAnswer(Integer.parseInt(user), plan, exercise, Integer.parseInt(question), "", file.getPath(), valid, db);
    String wavPathWithForwardSlashSeparators = ensureForwardSlashes(wavPath);
    return new AudioAnswer(wavPathWithForwardSlashSeparators, valid);
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private File getAbsoluteFile(String filePath) {
    String realContextPath = getInstallPath();
    return getAbsolute(filePath, realContextPath);
  }

  private File getAbsolute(String filePath, String realContextPath) {
    File file = new File(realContextPath, filePath);
    assert(file.exists());
    return file;
  }

  private String getInstallPath() {
    ServletContext context = getServletContext();
    String realContextPath = context.getRealPath(getThreadLocalRequest().getContextPath());

    String appName = getServletContext().getInitParameter("appName");
    if (appName == null) appName = DEFAULT_APP_NAME;
    String dupPath = appName + File.separator + appName;
    realContextPath = realContextPath.replace(dupPath, appName);// hack to deal with path duplication issue
    return realContextPath;
  }

  /**
   * Checks if file exists already...
   * @param pathToWav
   */
  private void ensureWriteMP3(String pathToWav) {
    AudioConversion audioConversion = new AudioConversion();
    File absolutePathToWav = getAbsoluteFile(pathToWav);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav",".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists()) {
      audioConversion.writeMP3(absolutePathToWav.getAbsolutePath());
    }

    if (WRITE_ALTERNATE_COMPRESSED_AUDIO) {
       audioConversion.writeCompressed(absolutePathToWav.getAbsolutePath());
    }
  }

  /**
   *
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return a path relative to the install dir
   */
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

  private String getImageOutDir() {
    String imageOutdir = getServletContext().getInitParameter("imageOutdir");
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      imageOutdir = IMAGE_WRITER_IMAGES;
    }
    return imageOutdir;
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }

  public static void main(String[] arg) {
    //System.out.println("x\\y\\z".replaceAll("\\\\", "/"));

    LangTestDatabaseImpl langTestDatabase = new LangTestDatabaseImpl();

    String audioFile = "answers/test/ac-L0P-001/1/subject--1/answer_1347645428580.mp3";
    File properAudioFile = langTestDatabase.getProperAudioFile(audioFile, "C:\\Users\\go22670\\DLITest\\LangTest\\war");
    System.out.println("From " +audioFile + " to " + properAudioFile);


    if (true) return;

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
