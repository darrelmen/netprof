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

package mitll.langtest.server.decoder;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/22/15.
 */
public class RefResultDecoder {
  private static final Logger logger = LogManager.getLogger(RefResultDecoder.class);

  private static final boolean DO_REF_DECODE = true;
  private static final boolean DO_TRIM = false;
  // private static final int SLEEP_BETWEEN_DECODES = 2000;

  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  private final AudioFileHelper audioFileHelper;
  private boolean stopDecode = false;
  private final PathHelper pathHelper;
  private final AudioConversion audioConversion;
  private final boolean hasModel;

  /**
   * @param db
   * @param serverProperties
   * @param pathHelper
   * @param audioFileHelper
   * @see Project#setAnalysis
   */
  public RefResultDecoder(DatabaseImpl db, ServerProperties serverProperties, PathHelper pathHelper,
                          AudioFileHelper audioFileHelper, boolean hasModel) {
    this.db = db;
    this.serverProps = serverProperties;
    this.pathHelper = pathHelper;
    this.audioFileHelper = audioFileHelper;
    this.audioConversion = new AudioConversion(serverProperties);
    this.hasModel = hasModel;
  }

  /**
   * @param exercises
   * @see LangTestDatabaseImpl#init()
   */
  public void doRefDecode(final Collection<CommonExercise> exercises) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (serverProps.shouldTrimAudio()) {
          logger.warn("doRefDecode trimming audio!");
          sleep(5000);
          if (!exercises.isEmpty()) {
            CommonExercise next = exercises.iterator().next();
            trimRef(next.getProjectID(), exercises);
          }
        }
        if (serverProps.shouldDoDecode()) {
          logger.warn("doRefDecode shouldDoDecode true");
          sleep(5000);
          if (hasModel) {
            if (!exercises.isEmpty()) {
              CommonExercise next = exercises.iterator().next();
              writeRefDecode(exercises, next.getProjectID());
            }
          }
        } else {
          logger.debug(" not doing decode ref decode");
        }
      }
    }).start();
  }

  /**
   *
   * @param projid
   * @param exercises
   * @seex #runMissingInfo
   */
/*  private void doMissingInfo(final Collection<CommonExercise> exercises) {
    int count = 0;

    Map<String, CommonExercise> idToEx = getIdToExercise(exercises);
    if (idToEx == null) return;

    Collection<Result> resultsToDecode = db.getResultDAO().getResultsToDecode();
    int size = resultsToDecode.size();
    logger.info("doMissingInfo found " + size + " with missing info");

    for (Result res : resultsToDecode) {
      if (count++ < 20) {
        logger.debug("\t doMissingInfo found " + res);
      }

//      String[] bestAudios = res.getAnswer().split(File.separator);
      //    if (bestAudios.length > 1) {
      CommonExercise exercise = idToEx.get(res.getExerciseID());
      if (exercise != null) {
        logger.info("doMissingInfo #" + count + " of " + size + " align " + exercise.getOldID() + " and result " + res.getUniqueID());
        PretestScore alignmentScore = audioFileHelper.getAlignmentScore(exercise, res.getAnswer(), serverProps.usePhoneToDisplay(), false);
        db.rememberScore(res.getUniqueID(), alignmentScore);
      } else {
        logger.warn("no exercise for " + res.getExerciseID() + " from result?");
      }
      if (stopDecode) break;
      //	logger.debug("previously found " + res);
      //  }
    }
  }*/

/*
  private Map<Integer, CommonExercise> getIdToExercise(Collection<CommonExercise> exercises) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercises) {
      if (stopDecode) return null;
      idToEx.put(exercise.getID(), exercise);
    }
    return idToEx;
  }
*/

  /**
   * @param projid
   * @param exercises
   */
  private void trimRef(int projid, Collection<CommonExercise> exercises) {
    if (DO_TRIM) {
      Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(projid);
      String language = db.getLanguage(projid);
      String installPath = pathHelper.getInstallPath();

      int numResults = db.getRefResultDAO().getNumResults();
      //String language = getLanguage();
      logger.debug(" writeRefDecode : found " +
          numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio");

      if (stopDecode) logger.debug("Stop decode true");

      int count = 0;
      int trimmed = 0;
      int changed = 0;
      int attrc = 0;
      int maleAudio = 0;
      int femaleAudio = 0;
      int defaultAudio = 0;
      Set<Integer> failed = new TreeSet<>();
      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
        if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
          boolean didAll = db.getAudioDAO().attachAudio(exercise, /*installPath, relativeConfigDir,*/ audioAttributes, language);
          attrc += audioAttributes.size();
          if (!didAll) {
            failed.add(exercise.getID());
          }
        }

        Set<Long> preferredVoices = serverProps.getPreferredVoices();
        Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);

        List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
        boolean maleEmpty = maleUsers.isEmpty();

        List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
        boolean femaleEmpty = femaleUsers.isEmpty();
        String title = exercise.getForeignLanguage();
        int id = exercise.getID();
        if (!maleEmpty) {
          List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
          maleAudio += audioAttributes1.size();
          Info info = doTrim(audioAttributes1, title, id);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (!femaleEmpty) {
          List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
          femaleAudio += audioAttributes1.size();

          Info info = doTrim(audioAttributes1, title, id);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        } else if (maleEmpty) {
          Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();
          defaultAudio += defaultUserAudio.size();
          Info info = doTrim(defaultUserAudio, title, id);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (count > 0 && count % 2000 == 0) logger.debug("examined " + count + " files.");
      }

      logger.debug("trimRef : Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
          defaultAudio + " default " + "examined " + count +
          " trimmed " + trimmed + " dropped ref result rows = " + changed);
      if (!failed.isEmpty()) {
        logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
      }
    }
  }

  /**
   * TODO : put this back so we can run it per project
   * Do alignment and decoding on all the reference audio and store the results in the RefResult table.
   *
   * @see #doRefDecode
   */
  private void writeRefDecode(Collection<CommonExercise> exercises, int projid) {
    boolean b = db.getServerProps().shouldDoDecode();
    logger.warn("writeRefDecode got " + b + " for should do decode");
    if (false) {
      Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(projid);
     // String installPath = pathHelper.getInstallPath();

      int numResults = db.getRefResultDAO().getNumResults();
      String language = db.getLanguage(projid);
      logger.debug(" writeRefDecode : found " +
          numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio");

      Set<String> decodedFiles = getDecodedFiles();
      logger.debug(" found " + decodedFiles.size() + " previous ref results, checking " +
          exercises.size() + " exercises ");

      if (stopDecode) logger.debug("Stop decode true");

      int count = 0;
      int attrc = 0;
      int maleAudio = 0;
      int femaleAudio = 0;
      int defaultAudio = 0;

      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
        if (audioAttributes != null) {
          db.getAudioDAO().attachAudio(exercise, /*installPath, relativeConfigDir,*/ audioAttributes, language);
          attrc += audioAttributes.size();
        }

        Set<Long> preferredVoices = serverProps.getPreferredVoices();
        Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);

        List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
        boolean maleEmpty = maleUsers.isEmpty();

        List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
        boolean femaleEmpty = femaleUsers.isEmpty();

        if (!maleEmpty) {
          List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
          maleAudio += audioAttributes1.size();
          count += doDecode(decodedFiles, exercise, audioAttributes1);
        }
        if (!femaleEmpty) {
          List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
          femaleAudio += audioAttributes1.size();

          count += doDecode(decodedFiles, exercise, audioAttributes1);
        } else if (maleEmpty) {
          Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();
          defaultAudio += defaultUserAudio.size();
          count += doDecode(decodedFiles, exercise, defaultUserAudio);
        }
        if (count > 0 && count % 100 == 0) logger.debug("ref decode - did " + count + " decodes");
      }
      logger.debug("writeRefDecode : Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
          defaultAudio + " default " + "decoded " + count);

/*      if (serverProps.addMissingInfo()) {
        runMissingInfo(exercises);
      } else {
        logger.debug("not looking for missing info");
      }*/
    }
  }

/*  private void runMissingInfo(final Collection<CommonExercise> exercises) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        sleep(2000);
        doMissingInfo(exercises);
      }
    }).start();
  }*/

  private void sleep(int millis) {
    try {
      Thread.sleep(millis); // ???
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the set of files that have already been decoded and aligned so we don't do them a second time.
   *
   * @return
   * @see #writeRefDecode
   */
  private Set<String> getDecodedFiles() {
    logger.info("getDecodedFiles ----");
    List<Result> results = db.getRefResultDAO().getResults();
    Set<String> decodedFiles = new HashSet<>();
    for (Result res : results) {
      String[] bestAudios = res.getAnswer().split(File.separator);
      if (bestAudios.length > 1) {
        String bestAudio = bestAudios[bestAudios.length - 1];
        decodedFiles.add(bestAudio);
        if (stopDecode) break;
      }
    }
    return decodedFiles;
  }

  /**
   * @param decodedFiles
   * @param exercise
   * @param audioAttributes
   * @return
   * @see #writeRefDecode
   */
  private int doDecode(Set<String> decodedFiles, CommonExercise exercise, Collection<AudioAttribute> audioAttributes) {
    int count = 0;
    boolean doHydec = serverProps.shouldDoDecodeWithHydec();
    List<AudioAttribute> toDecode = new ArrayList<>();
    for (AudioAttribute attribute : audioAttributes) {
      if (!attribute.isContextAudio()) {
        String bestAudio = getFile(attribute);
        if (!decodedFiles.contains(bestAudio)) {
          toDecode.add(attribute);
        }
      }
    }
    for (AudioAttribute attribute : toDecode) {
      if (stopDecode) return 0;

      try {
        String audioRef = attribute.getAudioRef();

        boolean fileExists = false;
        if (!audioRef.contains("context=")) {
          //logger.debug("doing alignment -- ");
          // Do alignment...
          File absoluteFile = pathHelper.getAbsoluteAudioFile(audioRef);
          fileExists = absoluteFile.exists();
        }

        if (fileExists) {
          audioFileHelper.decodeOneAttribute(exercise, attribute, doHydec);
          sleep(serverProps.getSleepBetweenDecodes());
          count++;
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }

    return count;
  }


  /**
   * @param audioAttributes
   * @param title
   * @param exid
   * @return
   * @see #trimRef
   */
  private Info doTrim(Collection<AudioAttribute> audioAttributes, String title, int exid) {
    int count = 0;
    int trimmed = 0;
    int changed = 0;
    for (AudioAttribute attribute : audioAttributes) {
      String bestAudio = getFile(attribute);
      String audioRef = attribute.getAudioRef();
      File absoluteFile = pathHelper.getAbsoluteAudioFile(audioRef);

      if (absoluteFile.exists()) {
        File replacement = new File(absoluteFile.getParent(), "orig_" + absoluteFile.getName());
        if (!replacement.exists()) {
          try {
            FileUtils.copyFile(absoluteFile, replacement);
          } catch (IOException e) {
            logger.error("got " + e, e);
          }
          AudioConversion.TrimInfo trimInfo = audioConversion.trimSilence(absoluteFile);
          if (trimInfo.didTrim()) {
            // drop ref result info
            logger.debug("trimmed " + exid + " " + attribute + " audio " + bestAudio);
            if (hasModel) {
              boolean b = db.getRefResultDAO().removeForAudioFile(absoluteFile.getAbsolutePath());
              if (!b) {
                logger.warn("for " + exid + " couldn't remove " + absoluteFile.getAbsolutePath() + " for " + attribute);
              } else {
                changed++;
              }
            }

            trimmed++;
          }

          count++;

          String author = attribute.getUser().getUserID();
          audioConversion.ensureWriteMP3(audioRef, pathHelper.getInstallPath(), trimInfo.didTrim(), title, author);
        }
      } else {
//        logger.warn("no file for " + exid + " " + attribute + " at audio file " + bestAudio);
      }
    }

    return new Info(trimmed, count, changed);
  }

  private static class Info {
    final int trimmed;
    final int count;
    final int changed;

    Info(int trimmed, int count, int changed) {
      this.trimmed = trimmed;
      this.count = count;
      this.changed = changed;
    }
  }

  private String getFile(AudioAttribute attribute) {
    String[] bestAudios = attribute.getAudioRef().split(File.separator);
    return bestAudios[bestAudios.length - 1];
  }

  public void setStopDecode(boolean stopDecode) {
    this.stopDecode = stopDecode;
  }
}
