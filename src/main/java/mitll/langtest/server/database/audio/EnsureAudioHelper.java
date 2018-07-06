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

package mitll.langtest.server.database.audio;

import mitll.langtest.client.result.AudioTag;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;

public class EnsureAudioHelper implements IEnsureAudioHelper {
  private static final Logger logger = LogManager.getLogger(EnsureAudioHelper.class);

  protected DatabaseServices db;

  private static final String WAV1 = "wav";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final int MP3_LENGTH = MP3.length();

  private static final int WARN_THRESH = 10;

  private static final boolean DEBUG = false;
//  private static final boolean WARN_MISSING_FILE = false;

  private static final int CHECKED_INTERVAL = 1;
  public static final String UNKNOWN = "unknown";

  private AudioConversion audioConversion;

  private ServerProperties serverProps;
  private int spew = 0;
  private PathHelper pathHelper;

  /**
   * @param db
   * @param pathHelper
   */
  public EnsureAudioHelper(DatabaseServices db, PathHelper pathHelper) {
    this.db = db;
    serverProps = db.getServerProps();
    this.pathHelper = pathHelper;
    audioConversion = new AudioConversion(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange());
  }

  /**
   * This could take a long time - lots of files, shell out for each one...
   *
   * @param projectid
   * @see mitll.langtest.server.services.AudioServiceImpl#checkAudio
   */
  @Override
  public void ensureAudio(int projectid) {
    long now = System.currentTimeMillis();
    long then;
    then = now;
    List<CommonExercise> exercises = db.getExercises(projectid, false);
    now = System.currentTimeMillis();
    if (now - then > WARN_THRESH)
      logger.info("ensureAudio for " + projectid + " - took " + (now - then) + " millis to get exercises");

    ensureAudioForExercises(exercises, db.getLanguage(projectid));
  }

  /**
   * @param exercises
   * @param language
   * @see #ensureAudio(int)
   */
  private void ensureAudioForExercises(List<CommonExercise> exercises, String language) {
    long then = System.currentTimeMillis();
    db.getAudioDAO().attachAudioToExercises(exercises, language);
    long now = System.currentTimeMillis();

    if (now - then > WARN_THRESH)
      logger.info("ensureAudioForExercises (" + language + ") checkAudio - took " + (now - then) + " millis to attach audio");

    ensureCompressedAudio(exercises, language);
  }

  /**
   * @param exercises
   * @param language
   * @see #ensureAudioForExercises
   */
  @Override
  public void ensureCompressedAudio(Collection<CommonExercise> exercises, String language) {
    long then = System.currentTimeMillis();

    int c = 0;
    int success = 0;
    logger.info("ensureCompressedAudio (" + language + ") examining " + exercises.size() + " exercises");

    Map<Integer, User> idToUser = new HashMap<>();
    for (CommonExercise exercise : exercises) {
      if (exercise != null) {
        for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
          c++;
          if (c < 10) {
//            logger.info("checkAudio exercise.g. ensure audio for " + audioAttribute + " on " + exercise);
          }

          try {
            if (!ensureCompressed(
                exercise,
                audioAttribute,
                language, idToUser).equalsIgnoreCase(FILE_MISSING)) success++;

            if (c % 1000 == 0) {
              logger.debug("ensureCompressedAudio checked " + c + ", success = " + success + " e.g. " + audioAttribute);
            }
          } catch (Exception e1) {
            logger.warn("ensureCompressedAudio Got " + e1 + " for exercise " + exercise.getID() + " : " + audioAttribute.getAudioRef());
          }
        }

        exercise.getDirectlyRelated().forEach(exercise1 -> exercise1.getAudioAttributes().forEach(audioAttribute -> {
          ensureCompressed(
              exercise,
              audioAttribute,
              language, idToUser);
//          if (c % 1000 == 0) {
//            logger.debug("ensureCompressedAudio checked " + c + ", success = " + success + " e.g. " + audioAttribute);
//          }
        }));
      }
    }
    long now = System.currentTimeMillis();
    if (now - then > WARN_THRESH) {
      logger.info("ensureCompressedAudio - took " + (now - then) + " millis to ensure ogg and mp3 for " + c + " attributes for " +
          exercises.size() + " exercises, " +
          success + " files successful");
    }
  }

  private String ensureCompressed(CommonExercise exercise, AudioAttribute audioAttribute, String language, Map<Integer, User> idToUser) {
    return ensureCompressedAudio(
        audioAttribute.getUserid(),
        exercise,
        audioAttribute.getAudioRef(),
        audioAttribute.getAudioType(),
        language, idToUser);
  }

  /**
   * Tries to remember if we've checked a file before...
   *
   * @param user
   * @param commonShell
   * @param path
   * @param audioType
   * @param language
   * @param idToUser
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   */
  @Override
  public String ensureCompressedAudio(int user,
                                      ClientExercise commonShell,
                                      String path,
                                      AudioType audioType,
                                      String language, Map<Integer, User> idToUser) {
    if (checkedExists.contains(path)) {
      return path;
    }

    User userBy = idToUser.get(user);

    if (userBy == null) {
      userBy = getUserBy(user);
      idToUser.put(user, userBy);
    }

    String userID = getUserIDForgiving(user, userBy);

    if (userID == null) {
      logger.warn("ensureCompressedEquivalent huh? no user for " + user);
    }

    boolean noExerciseYet = commonShell == null;
    String title = noExerciseYet ? UNKNOWN : commonShell.getForeignLanguage();
    String comment = noExerciseYet ? UNKNOWN : commonShell.getEnglish();

    if (audioType.isContext() && !noExerciseYet) {
      if (commonShell.hasContext()) {
        CommonShell contextSentence = commonShell.getDirectlyRelated().iterator().next();
        title = contextSentence.getForeignLanguage();
        comment = contextSentence.getEnglish();
      }
    }

    String filePath = ensureMP3(path, new TrackInfo(title, userID, comment, language), language);
    boolean isMissing = filePath.equals(FILE_MISSING);

    if (!isMissing) {
      checkedExists.add(path);
      if (checkedExists.size() % CHECKED_INTERVAL == 10)
        logger.debug("ensureCompressedAudio checked " + checkedExists.size() + " files...");
    }
    return filePath;
  }

  private String getUserIDForgiving(int user, User userBy) {
    return userBy == null ? "" + user : userBy.getUserID();
  }

  private User getUserBy(int id) {
    return db.getUserDAO().getUserWhere(id);
  }

  private final Set<String> checkedExists = new HashSet<>();

  /**
   * for both audio in answers and best audio -- could be more efficient...
   *
   * @param wavFile
   * @param trackInfo
   * @param language
   * @return file path of mp3 file
   * @see IEnsureAudioHelper#ensureCompressedAudio(int, ClientExercise, String, AudioType, String, Map)
   */
  private String ensureMP3(String wavFile, TrackInfo trackInfo, String language) {
    String parent = serverProps.getAnswerDir();
    if (wavFile != null) {
      if (DEBUG || true) {
        logger.debug("ensureMP3 : trying to ensure compressed" +
            "\n\tfor " + wavFile +
            "\n\tunder " + parent);
      }
      // File test = new File(parent + File.separator + language, wavFile);
      parent = audioConversion.getParentForFilePathUnderBaseAudio(wavFile, language, parent, serverProps.getAudioBaseDir());
/*      if (!audioConversion.exists(wavFile, parent)) {// && wavFile.contains("1310")) {
        if (WARN_MISSING_FILE && spew++ < 10) {
          logger.error("ensureMP3 : can't find " + wavFile + " under " + parent + " for " + title + " " + artist);
        }
      }*/

      String s = audioConversion.ensureWriteMP3(parent, wavFile, false, trackInfo);
      boolean isMissing = s.equals(FILE_MISSING);
      if (isMissing) {
        if (spew++ < 10 || spew % 1000 == 0) {
          logger.error("ensureMP3 : (count = " + spew + ")" +
              " can't find " + wavFile + " under " + parent + " for " + trackInfo);
        }
      }
      return s;
    } else {
      return FILE_MISSING;
    }
  }

  /**
   * Here we assume the audioFile path is like
   * <p>
   * bestAudio/spanish/bestAudio/123/regular_xxx.mp3
   * OR answers/spanish/answers/123/regular_xxx.mp3
   *
   * @param audioFile
   * @return
   */
  @Override
  public String getWavAudioFile(String audioFile, String language) {
    String reqFile = audioFile;
    if (audioFile.endsWith("." + AudioTag.COMPRESSED_TYPE) || audioFile.endsWith(MP3)) {
      String wavFile = removeSuffix(audioFile) + WAV;
//      logger.info("getWavAudioFile " + audioFile);
      if (new File(wavFile).exists()) {
        return wavFile;
      } else {
        File test = pathHelper.getAbsoluteAudioFile(wavFile);
        if (!test.exists()) {
          //     logger.warn("not at " + test.getAbsolutePath());
          test = pathHelper.getAbsoluteBestAudioFile(wavFile, language.toLowerCase());
          if (!test.exists()) {
            logger.warn("getWavAudioFile NOPE at " + test.getAbsolutePath());
          }
        }
        //      logger.info("getWavAudioFile test " + test.getAbsolutePath());
        audioFile = test.exists() ? test.getAbsolutePath() : "FILE_MISSING.wav";
      }
    }

    String s = ensureWAV(audioFile);

    logger.info("getWavAudioFile" +
        "\n\treqFile      " + reqFile +
        "\n\taudio before " + audioFile +
        "\n\tafter        " + s
    );

    return s;
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - MP3_LENGTH);
  }

  private String ensureWAV(String audioFile) {
    if (!audioFile.endsWith(WAV1)) {
      return audioFile.substring(0, audioFile.length() - MP3_LENGTH) + WAV;
    } else {
      return audioFile;
    }
  }
}

