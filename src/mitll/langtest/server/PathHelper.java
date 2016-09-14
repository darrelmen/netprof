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
import org.apache.log4j.Logger;

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
  private static final Logger logger = Logger.getLogger(PathHelper.class);

  public static final String ANSWERS = "answers";
  private static final String IMAGE_WRITER_IMAGES = "audioimages";
  private static final String IMAGE_OUTDIR = "imageOutdir";
//  private static final String TOMCAT_WRITE_DIRECTORY_FULL_PATH = "tomcatWriteDirectoryFullPath";

  private String realContextPathTest;
  private final ServletContext context;
  private String configDir;
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
   * NOT FOR AUDIO.
   *
   * @param filePath
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile(int, String, String, int, int, String)
   *
   */
  public File getAbsoluteFile(String filePath) {
    return getAbsolute(getInstallPath(), filePath);
  }

//  public File getAbsoluteAnswerAudioFile(String filePath, String language) {
//    return getAbsolute(getAnswerDir() + File.separator + language.toLowerCase(), filePath);
//  }

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

  /**
   * CHEESY - assumes audio base dir is related to answers.
   * @param audioContext
   * @return
   */
  public String getRelToAnswer(AudioContext audioContext) {
    return getAbsoluteToAnswer(audioContext).substring(properties.getAudioBaseDir().length());
  }

    /**
     * TODO : fix this -
     *
     * @param language
     * @param exercise
     * @param question
     * @param user
     * @return
     */
  String getAbsoluteToAnswer(String language, int exercise, int question, int user) {
    String planAndTestPath =
        language.toLowerCase() + File.separator +
            exercise + File.separator +
            question + File.separator +
            "subject-" + user;
    return getAbsoluteWavPathUnder(planAndTestPath);
  }

  public String getAbsoluteWavPathUnder(String planAndTestPath) {
    return getWavPath(getAnswerDir(), planAndTestPath);
  }

/*  public String getRelPathUnder(String planAndTestPath) {
    return getWavPath(getAnswerDir().substring(properties.getAudioBaseDir().length()), planAndTestPath);
  }*/

  private String getAnswerDir() {
    return properties.getAnswerDir();
  }

  /**
   * TODO : make wave file name more helpful - include exercise, user, etc.
   *
   * @param tomcatWriteDirectory
   * @param planAndTestPath
   * @return
   */
  private String getWavPath(String tomcatWriteDirectory, String planAndTestPath) {
    String wavPath =
        tomcatWriteDirectory + File.separator +
            planAndTestPath + File.separator +
            "answer_" + System.currentTimeMillis() + ".wav";
    //if (mkdirs) logger.debug("getAbsoluteToAnswer : making dir at : " + audioFilePath.getAbsolutePath());
    return wavPath;
  }

  /**
   * @return path to image output dir
   * @see LangTestDatabaseImpl#getImageForAudioFile
   */
  public String getImageOutDir() {
    String imageOutdir = context == null ? IMAGE_OUTDIR : context.getInitParameter(IMAGE_OUTDIR);
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir);
    if (!test.exists()) {
      test = getAbsoluteFile(imageOutdir);
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

  public void setProperties(ServerProperties properties) {
    this.properties = properties;
  }
}
