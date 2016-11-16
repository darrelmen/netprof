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
import mitll.langtest.server.audio.TrackInfo;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import java.io.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(DatabaseServlet.class);
  private static final int BUFFER_SIZE = 4096;
  //private static final String NO = "NO";
  // not clear this is a big win currently - this enables us to rewrite the mp3s and possibly mark
  // them with a title
//  private static final Boolean CHECK_FOR_MP3 = false;

  protected ServerProperties serverProps;
  String relativeConfigDir;
  protected String configDir;
  protected PathHelper pathHelper;

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
   * @see mitll.langtest.server.ScoreServlet#getAnswer
   * @param wavFile
   * @param trackInfo
   */
  protected void ensureMP3(String wavFile, TrackInfo trackInfo) { ensureMP3(wavFile, pathHelper, configDir, trackInfo);  }

  private boolean ensureMP3(String wavFile, PathHelper pathHelper, String configDir, TrackInfo trackInfo) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();
      //logger.debug("ensureMP3 : wav " + wavFile + " under " + parent);

      AudioConversion audioConversion = new AudioConversion(serverProps);
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      String filePath = audioConversion.ensureWriteMP3(wavFile, parent, false, trackInfo);
      return new File(filePath).exists();
    } else {
      return false;
    }
  }

  protected void setPaths() {
    pathHelper = getPathHelper();
    String config = getServletContext().getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
   // logger.debug("setPaths rel " + relativeConfigDir  + " pathHelper " + pathHelper);
    this.configDir = getConfigDir();
  }

  protected PathHelper getPathHelper() {
    return new PathHelper(getServletContext());
  }

  protected String getConfigDir() {
    return getConfigDir(pathHelper);
  }

  protected String getConfigDir(PathHelper pathHelper) {
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

  protected void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
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
    ex.put("id", exercise.getID());
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
