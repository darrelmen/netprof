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

import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * TODO : consider how to partition audio services...
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = LogManager.getLogger(DatabaseServlet.class);
  private static final int BUFFER_SIZE = 4096;
  // private static final String NO = "NO";
  // not clear this is a big win currently - this enables us to rewrite the mp3s and possibly mark
  // them with a title
//  private static final Boolean CHECK_FOR_MP3 = false;
  protected ServerProperties serverProps;
  private String configDir;
  protected PathHelper pathHelper;
  private UserSecurityManager securityManager;

  /**
   * @see #ensureMP3s
   * @param byID
   * @param pathHelper
   * @param configDir
   * @return
   */
/*  private boolean ensureMP3(CommonExercise byID, PathHelper pathHelper, String configDir) {
    return ensureMP3(byID.getRefAudio(), pathHelper, configDir, byID.getForeignLanguage(), "");
  }*/

  /**
   * @param wavFile
   * @param title
   * @param author
   * @see mitll.langtest.server.ScoreServlet#ensureMP3Later(int, String, String)
   */
  void ensureMP3(String wavFile, String title, String author) {
    ensureMP3(wavFile, configDir, title, author);
  }

  private void ensureMP3(String wavFile, String configDir, String title, String author) {
    if (wavFile != null) {
      //  String parent = pathHelper.getInstallPath();
      String parent = serverProps.getMediaDir();

      //logger.debug("ensureMP3 : wav " + wavFile + " under " + parent);

      AudioConversion audioConversion = new AudioConversion(serverProps);
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      String filePath = audioConversion.ensureWriteMP3(wavFile, parent, false, title, author);
      logger.info("wrote " + wavFile + " to " + filePath);
      // return new File(filePath).exists();
    } else {
      //return;
    }
  }

  void setPaths() {
    pathHelper = getPathHelper();
    // String config = getServletContext().getInitParameter("config");
    //this.relativeConfigDir = "config" + File.separator + config;
    // logger.debug("setPaths rel " + relativeConfigDir  + " pathHelper " + pathHelper);
    this.configDir = getConfigDir();
  }

  private PathHelper getPathHelper() {
    serverProps = getDatabase().getServerProps();
    if (serverProps == null) throw new IllegalArgumentException("huh? props is null?");
    return new PathHelper(getServletContext(), serverProps);
  }

  private String getConfigDir() {
    return getConfigDir(pathHelper);
  }

  private String getConfigDir(PathHelper pathHelper) {
    String config = getServletContext().getInitParameter("config");
    return pathHelper.getInstallPath() + File.separator + "config" + File.separator + config;
  }

  /**
   * @seex mitll.langtest.server.load.LoadTestServlet#getJsonArray(List)
   * @paramx byID
   */
/*
  protected void ensureMP3s(CommonExercise byID) {
    ensureMP3(byID, pathHelper, configDir);
  }
*/
  protected void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }

  private void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
  }

  /**
   * @param request
   * @return
   * @see #doGet
   */
  int getProject(HttpServletRequest request) {
    try {
      User loggedInUser = securityManager.getLoggedInUser(request);
      if (loggedInUser == null) return -1;
      int id = loggedInUser.getID();
      int i = getMostRecentProjectByUser(id);
      return i;
    } catch (DominoSessionException e) {
      logger.error("Got " + e, e);
      return -1;
    }
  }

  /**
   * @param id
   * @return
   * @see #getProject(HttpServletRequest)
   */
  int getMostRecentProjectByUser(int id) {
    return getDatabase().getUserProjectDAO().mostRecentByUser(id);
  }

  private DatabaseImpl db = null;

  protected DatabaseImpl getDatabase() {
    if (db == null) {
      Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
      if (databaseReference != null) {
        db = (DatabaseImpl) databaseReference;
        securityManager = new UserSecurityManager(db.getUserDAO(), db.getUserSessionDAO());
        // logger.debug("found existing database reference " + db + " under " +getServletContext());
      } else {
        logger.error("huh? no existing db reference?");
      }
    }
    return db;
  }

  /**
   * Make json for an exercise
   *
   * Prefer recordings by voices on the preferred list.
   *
   * @param exercise
   * @return
   */
/*  protected JSONObject getJsonForExercise(CommonExercise exercise) {
    JSONObject ex = new JSONObject();
    ex.put("id", exercise.getOldID());
    ex.put("fl", exercise.getForeignLanguage());
    ex.put("tl", exercise.getTransliteration());
    ex.put("en", exercise.getEnglish());
    ex.put("ct", exercise.getContext());
    ex.put("ctr", exercise.getContextTranslation());
    AudioAttribute latestContext = exercise.getLatestContext(true);
    //if (latestContext != null) {
    //  String author = latestContext.getUser().getUserID();
    //  if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
   // }
    ex.put("ctmref", latestContext == null ? NO : latestContext.getAudioRef());
    latestContext = exercise.getLatestContext(false);
   // if (latestContext != null) {
     // String author = latestContext.getUser().getUserID();
     // if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
   // }
    ex.put("ctfref", latestContext == null ? NO : latestContext.getAudioRef());
    ex.put("ref", exercise.hasRefAudio() ? exercise.getRefAudioWithPrefs(serverProps.getPreferredVoices()) : NO);

    addLatestRefs(exercise, ex);

    return ex;
  }*/

  /**
   * Male/female reg/slow speed
   * Prefer voices on the preferred list.
   *
   * @param exercise
   * @param ex
   * @see #getJsonForExercise
   */
/*  private void addLatestRefs(AudioRefExercise exercise, JSONObject ex) {
    Set<Long> preferredVoices = serverProps.getPreferredVoices();

    String mr = null, ms = null, fr = null, fs = null;
    long mrt = 0, mst = 0, frt = 0, fst = 0;
    AudioAttribute mra = null, msa = null, fra = null, fsa = null;

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      long timestamp = audioAttribute.getTimestamp();
      //boolean isPrefVoice = preferredVoices.contains(audioAttribute.getUserid());
      if (audioAttribute.isMale()) {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= mrt) {
            if (mra == null || !preferredVoices.contains(mra.getUserid())) {
              mrt = timestamp;
              mr = audioAttribute.getAudioRef();
              mra = audioAttribute;
            }
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= mst) {
            if (msa == null || !preferredVoices.contains(msa.getUserid())) {
              mst = timestamp;
              ms = audioAttribute.getAudioRef();
              msa = audioAttribute;
            }
          }
        }
      } else {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= frt) {
            if (fra == null || !preferredVoices.contains(fra.getUserid())) {
              frt = timestamp;
              fr = audioAttribute.getAudioRef();
              fra = audioAttribute;
            }
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= fst) {
            if (fsa == null || !preferredVoices.contains(fsa.getUserid())) {
              fst = timestamp;
              fs = audioAttribute.getAudioRef();
              fsa = audioAttribute;
            }
          }
        }
      }
    }

    // male regular speed reference audio (m.r.r.)
    // we want the item text so we can label the mp3 with a title
    //String foreignLanguage = exercise.getForeignLanguage();

//    if (mr != null) {
//      if (CHECK_FOR_MP3) ensureMP3(mr, foreignLanguage, author);
//    }
    ex.put("mrr", mr == null ? NO : mr);

//    if (ms != null) {
//      if (CHECK_FOR_MP3) ensureMP3(ms, foreignLanguage, author);
//    }
    ex.put("msr", ms == null ? NO : ms);

//    if (fr != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fr, foreignLanguage, author);
//    }
    ex.put("frr", fr == null ? NO : fr);

//    if (fs != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fs, foreignLanguage, author);
//    }
    ex.put("fsr", fs == null ? NO : fs);
  }*/
}
