package mitll.langtest.server;

import org.apache.log4j.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/8/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PathHelper {
  private static final Logger logger = Logger.getLogger(PathHelper.class);
  public static final String ANSWERS = "answers";
  private static final String IMAGE_WRITER_IMAGES = "audioimages";

  private String realContextPathTest;
  private final ServletContext context;
  private String configDir;


  public PathHelper(ServletContext context) {
    this.context = context;
  }

  public PathHelper(String realContextPathTest) {
    this((ServletContext)null);
    this.realContextPathTest = realContextPathTest;
  }

  public String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  public File getAbsoluteFile(String filePath) {
    String realContextPath = getInstallPath();
    return getAbsolute(realContextPath, filePath);
  }

  public File getAbsolute(String realContextPath, String filePath) {
    return new File(realContextPath, filePath);
  }

  /**
   * Figure out from the servlet context {@link GenericServlet#getServletContext()} where this instance of the webapp was
   * installed.  This is really important since we use it to convert back and forth between
   * relative and absolute paths to audio and image files.
   * @return path to webapp install location
   */
  public String getInstallPath() {
    if (context == null && realContextPathTest == null) {
      logger.error("no servlet context.");
      return "";
    }

    String realContextPath = context == null ? realContextPathTest : context.getRealPath(context.getContextPath());

    List<String> pathElements = Arrays.asList(realContextPath.split(realContextPath.contains("\\") ? "\\\\" : "/"));

    // hack to deal with the app name being duplicated in the path
    if (pathElements.size() > 1) {
      String last = pathElements.get(pathElements.size() - 1);
      String nextToLast = pathElements.get(pathElements.size() - 2);
      if (last.equals(nextToLast)) {
        realContextPath = realContextPath.substring(0, realContextPath.length() - last.length() -1);
      }
    }

    return realContextPath;
  }

  public File getFileForAnswer(String plan, String exercise, int question, int user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-" + user;
    return getAbsoluteFile(getWavPath(tomcatWriteDirectory, planAndTestPath));
  }

  /**
   * Make a place to store the audio answer, of the form:<br></br>
   *
   * "answers"/plan/exercise/question/"subject-"user/"answer_"timestamp".wav"  <br></br>
   *
   * e.g. <br></br>
   *
   * answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav <br></br>
   *
   * or absolute  <br></br>
   *
   * C:\Users\go22670\apache-tomcat-7.0.25\webapps\netPron2\answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav
   *
   * @see LangTestDatabaseImpl#writeAudioFile
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return a path relative to the install dir
   */
  public String getLocalPathToAnswer(String plan, String exercise, int question, int user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-" + user;
    return getWavPath(tomcatWriteDirectory, planAndTestPath);
  }

  public File getFileForWavPathUnder(String planAndTestPath) { return getAbsoluteFile(getWavPath(getTomcatDir(), planAndTestPath)); }
  public String getWavPathUnder(String planAndTestPath) { return getWavPath(getTomcatDir(), planAndTestPath); }

  private String getWavPath(String tomcatWriteDirectory, String planAndTestPath) {
    String currentTestDir = tomcatWriteDirectory + File.separator + planAndTestPath;
    String wavPath = currentTestDir + File.separator + "answer_" + System.currentTimeMillis() + ".wav";
    //if (mkdirs) logger.debug("getLocalPathToAnswer : making dir at : " + audioFilePath.getAbsolutePath());

    return wavPath;
  }

  private String getTomcatDir() {
    String tomcatWriteDirectory = context.getInitParameter("tomcatWriteDirectoryFullPath");
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = ANSWERS;

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = ANSWERS;
    }
    return tomcatWriteDirectory;
  }

  /**
   * @see LangTestDatabaseImpl#getImageForAudioFile
   * @return path to image output dir
   */
  public String getImageOutDir() {
    String imageOutdir = context.getInitParameter("imageOutdir");
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir);
    if (!test.exists()) {
      test = getAbsoluteFile(imageOutdir);
//      System.out.println("made image out dir at " + test.getAbsolutePath());
      boolean mkdirs = test.mkdirs();
      if (mkdirs) logger.debug("getImageOutDir : making dir at : " + test.getAbsolutePath());

    }
    return imageOutdir;
  }

  public void setConfigDir(String configDir) {
    this.configDir = configDir;
  }

  public String getConfigDir() {
    return configDir;
  }
}
