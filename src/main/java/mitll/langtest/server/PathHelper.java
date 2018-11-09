/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server;

import mitll.langtest.shared.scoring.AudioContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/8/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PathHelper {
  private static final Logger logger = LogManager.getLogger(PathHelper.class);

  public static final String ANSWERS = "answers";
  private static final String IMAGE_WRITER_IMAGES = "audioimages";
  private static final String IMAGE_OUTDIR = "imageOutdir";
  private static final String ANSWER = "answer_";

  // consistent with
  private static final int QUESTION = 1;

  private String realContextPathTest;
  private final ServletContext context;
  private ServerProperties properties;

  public PathHelper(ServletContext context) {
    this.context = context;
  }

  public PathHelper(ServletContext context, ServerProperties properties) {
    this.context = context;
    this.properties = properties;
    if (properties == null) throw new IllegalArgumentException("properties is null??");
  }

  public PathHelper(String realContextPathTest, ServerProperties properties) {
    this((ServletContext) null, properties);
    this.realContextPathTest = realContextPathTest;
  }

  public String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  /**
   * NOT FOR AUDIO????
   *
   * @param filePath
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#getImageForAudioFile
   */
  public File getAbsoluteFile(String filePath) {
    //   String installPath = properties.getAudioBaseDir(); // was install path
    String installPath = getInstallPath();
//    logger.info("getAbsoluteFile " + installPath + "/" +filePath);
    File absolute = getAbsolute(installPath, filePath);
    if (!absolute.exists()) {
      logger.warn("\t getAbsoluteFile doesn't exist: " + absolute.getAbsolutePath());
    }
    return absolute;
  }

  public File getAbsoluteBestAudioFile(String filePath, String language) {
    return getAbsolute(properties.getMediaDir() + File.separator + language.toLowerCase(), filePath);
  }

  public File getAbsoluteAudioFile(String filePath) {
    return getAbsolute(properties.getAudioBaseDir(), filePath);
  }

  private File getAbsolute(String realContextPath, String filePath) {
    return new File(realContextPath, filePath);
  }

  /**
   * Figure out from the servlet context {@link GenericServlet#getServletContext()} where this instance of the webapp was
   * installed.  This is really important since we use it to convert back and forth between
   * relative and absolute paths to audio and image files.
   *
   * @return path to webapp install location
   * @deprecated we don't store the audio underneath the install path anymore, if that's why you want the install path
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
        realContextPath = realContextPath.substring(0, realContextPath.length() - last.length() - 1);
      }
    }

    return realContextPath;
  }

  /**
   * Make a place to store the audio answer, of the form:<br></br>
   * <p>
   * "answers"/plan/exercise/question/"subject-"user/"answer_"timestamp".wav"  <br></br>
   * <p>
   * e.g. <br></br>
   * <p>
   * answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav <br></br>
   * <p>
   * or absolute  <br></br>
   * <p>
   * C:\Users\go22670\apache-tomcat-7.0.25\webapps\netPron2\answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav
   *
   * @param audioContext
   * @return a path relative to the install dir
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   */
  public String getAbsoluteToAnswer(AudioContext audioContext) {
    return getAbsoluteToAnswer(
        audioContext.getLanguage().toLowerCase(),
        audioContext.getExid(),
        audioContext.getQuestionID(),
        audioContext.getUserid());
  }

  @NotNull
  public String getRelToAnswer(String absoluteToAnswer) {
    return absoluteToAnswer.substring(properties.getAudioBaseDir().length());
  }

  /**
   * Somehow the question index default is 1.
   * @param language
   * @param exercise
   * @param user
   * @return
   */
  String getAbsoluteToAnswer(String language, int exercise, int user) {
    return getAbsoluteToAnswer(language, exercise, QUESTION, user);
  }

  /**
   * @param language
   * @param exercise
   * @param question - vestigial
   * @param user
   * @return
   */
  private String getAbsoluteToAnswer(String language, int exercise, int question, int user) {
    String planAndTestPath =
        language.toLowerCase() + File.separator +
            exercise + File.separator +
            question + File.separator +
            "subject-" + user;
    return getAbsoluteWavPathUnder(planAndTestPath);
  }

  private String getAbsoluteWavPathUnder(String planAndTestPath) {
    return getWavPath(getAnswerDir(), planAndTestPath);
  }

  private String getAnswerDir() {
    return properties.getAnswerDir();
  }

  /**
   * TODO : make wave file name more helpful - include exercise, user, etc.
   * <p>
   * Each file has a unique timestamp...
   *
   * @param tomcatWriteDirectory
   * @param planAndTestPath
   * @return
   */
  private String getWavPath(String tomcatWriteDirectory, String planAndTestPath) {
    String wavPath =
        tomcatWriteDirectory + File.separator +
            planAndTestPath + File.separator +
            ANSWER + System.currentTimeMillis() + ".wav";
//    logger.debug("getWavPath : file  : " + wavPath);
    //if (mkdirs) logger.debug("getAbsoluteToAnswer : making dir at : " + audioFilePath.getAbsolutePath());
    return wavPath;
  }

  /**
   * For routing from apache, path to audio images will be audioimages/russian, audioimages/spanish etc.
   * This supports putting russian, say, on hydra2.  This supports the rules:
   * <p>
   * <code>
   * /etc/httpd/conf/httpd.conf:        JkMount /netprof/audioimages/* pdominoWorker
   * </code>
   * <p>
   * <code>
   * /etc/httpd/conf/httpd.conf:        JkMount /netprof/audioimages/russian/* h2
   * </code>
   * <p>
   * This contrasts with where we put the reference audio (/opt/netprof/bestAudio) and student audio
   * (/opt/netprof/answers). I.e. this is under the webapp, not in a permanent location.
   *
   * @param language as suffix to audioimages directory
   * @return path to image output dir
   * @see mitll.langtest.server.services.AudioServiceImpl#getImageForAudioFile
   */
  public String getImageOutDir(String language) {
    String imageOutdir = context == null ? IMAGE_OUTDIR : context.getInitParameter(IMAGE_OUTDIR);
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir, language);
    String withSuffix = imageOutdir + File.separator + language;
    if (!test.exists()) {
      test = getAbsoluteFile(withSuffix);
      boolean mkdirs = test.mkdirs();
      if (mkdirs) {
        logger.debug("getImageOutDir : making dir at : " + test.getAbsolutePath());
      }
    }
    return withSuffix;
  }

  public void setProperties(ServerProperties properties) {
    this.properties = properties;
  }
}
