/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.decoder;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by go22670 on 7/22/15.
 */
public class RefResultDecoder {
  private static final Logger logger = Logger.getLogger(RefResultDecoder.class);
  private static final boolean DO_REF_DECODE = true;
  private static final boolean DO_TRIM = true;
  public static final int SLEEP_BETWEEN_DECODES = 2000;
//  private static final boolean RUN_MISSING_INFO = false;

  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  private final AudioFileHelper audioFileHelper;
  private boolean stopDecode = false;
  private final PathHelper pathHelper;
  //private final LangTestDatabaseImpl langTestDatabase;

  /**
   * @param db
   * @param serverProperties
   * @param pathHelper
   * @param audioFileHelper
   * @see LangTestDatabaseImpl#init()
   */
  public RefResultDecoder(DatabaseImpl db, ServerProperties serverProperties, PathHelper pathHelper,
                          AudioFileHelper audioFileHelper) {
    this.db = db;
    this.serverProps = serverProperties;
    this.pathHelper = pathHelper;
    this.audioFileHelper = audioFileHelper;
    //  this.langTestDatabase = langTestDatabase;
  }

  /**
   * @param exercises
   * @param relativeConfigDir
   * @see LangTestDatabaseImpl#init()
   */
  public void doRefDecode(final List<CommonExercise> exercises, final String relativeConfigDir) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (serverProps.shouldTrimAudio()) {
          sleep(5000);
          trimRef(exercises, relativeConfigDir);
        }
        if (serverProps.shouldDoDecode()) {
          sleep(5000);
          if (!serverProps.isNoModel()) writeRefDecode(exercises, relativeConfigDir);
        } else {
          logger.debug(getLanguage() + " not doing decode ref decode");
        }
      }
    }).start();
  }

  private String getLanguage() {
    return serverProps.getLanguage();
  }

  /**
   * @param exercises
   * @see #runMissingInfo(List)
   */
  private void doMissingInfo(final List<CommonExercise> exercises) {
    List<Result> resultsToDecode = db.getResultDAO().getResultsToDecode();
    int count = 0;

    Map<String, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercises) {
      if (stopDecode) return;
      idToEx.put(exercise.getID(), exercise);
    }

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
        logger.info("doMissingInfo #" + count + " of " + size + " align " + exercise.getID() + " and result " + res.getUniqueID());
        PretestScore alignmentScore = audioFileHelper.getAlignmentScore(exercise, res.getAnswer(), serverProps.usePhoneToDisplay(), false);
        db.rememberScore(res.getUniqueID(), alignmentScore);
      } else {
        logger.warn("no exercise for " + res.getExerciseID() + " from result?");
      }
      if (stopDecode) break;
      //	logger.debug("previously found " + res);
      //  }
    }
  }

  private void trimRef(List<CommonExercise> exercises, String relativeConfigDir) {
    if (DO_TRIM) {
      Map<String, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio();
      String installPath = pathHelper.getInstallPath();

      int numResults = db.getRefResultDAO().getNumResults();
      String language = getLanguage();
      logger.debug(language + " writeRefDecode : found " +
          numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio");

      if (stopDecode) logger.debug("Stop decode true");

      int count = 0;
      int trimmed = 0;
      int changed = 0;
      int attrc = 0;
      int maleAudio = 0;
      int femaleAudio = 0;
      int defaultAudio = 0;
      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
        if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
          db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
          attrc += audioAttributes.size();
        }

        Set<Long> preferredVoices = serverProps.getPreferredVoices();
        Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);

        List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
        boolean maleEmpty = maleUsers.isEmpty();

        List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
        boolean femaleEmpty = femaleUsers.isEmpty();
        String title = exercise.getForeignLanguage();
        if (!maleEmpty) {
          List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
          maleAudio += audioAttributes1.size();
          Info info = doTrim(audioAttributes1, title, exercise.getID());
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (!femaleEmpty) {
          List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
          femaleAudio += audioAttributes1.size();

          Info info = doTrim(audioAttributes1, title, exercise.getID());
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        } else if (maleEmpty) {
          Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();
          defaultAudio += defaultUserAudio.size();
          Info info = doTrim(defaultUserAudio, title, exercise.getID());
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (count > 0 && count % 2000 == 0) logger.debug("examined " + count + " files.");
      }

      logger.debug("trimRef : Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
          defaultAudio + " default " + "examined " + count +
          " trimmed " + trimmed + " dropped ref result rows = " + changed);
    }
  }

  /**
   * Do alignment and decoding on all the reference audio and store the results in the RefResult table.
   *
   * @see #doRefDecode(List, String)
   */
  private void writeRefDecode(List<CommonExercise> exercises, String relativeConfigDir) {
    if (DO_REF_DECODE) {
      Map<String, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio();
      String installPath = pathHelper.getInstallPath();

      int numResults = db.getRefResultDAO().getNumResults();
      String language = getLanguage();
      logger.debug(language + " writeRefDecode : found " +
          numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio");

      Set<String> decodedFiles = getDecodedFiles();
      logger.debug(language + " found " + decodedFiles.size() + " previous ref results, checking " +
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
          db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
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


      if (serverProps.addMissingInfo()
          ) {
        runMissingInfo(exercises);
      } else {
        logger.debug("not looking for missing info");
      }
    }
  }

  private void runMissingInfo(final List<CommonExercise> exercises) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        sleep(2000);
        doMissingInfo(exercises);
      }
    }).start();
  }

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
    List<Result> results = db.getRefResultDAO().getResults();
//    logger.debug(getLanguage() + " found " + results.size() + " previous ref results");

    Set<String> decodedFiles = new HashSet<String>();
//    int count = 0;
    for (Result res : results) {
//      if (count++ < 20) {
//        logger.debug("\t found " + res);
//      }

      String[] bestAudios = res.getAnswer().split(File.separator);
      if (bestAudios.length > 1) {
        String bestAudio = bestAudios[bestAudios.length - 1];
        //		logger.debug("added " + bestAudio);
        decodedFiles.add(bestAudio);
        if (stopDecode) break;
        //	logger.debug("previously found " + res);
      }
    }
    return decodedFiles;
  }

  /**
   * @param decodedFiles
   * @param exercise
   * @param audioAttributes
   * @return
   * @see #writeRefDecode(List, String)
   */
  private int doDecode(Set<String> decodedFiles, CommonExercise exercise, Collection<AudioAttribute> audioAttributes) {
    int count = 0;
    boolean doHydec = serverProps.shouldDoDecodeWithHydec();
    List<AudioAttribute> toDecode = new ArrayList<AudioAttribute>();
    for (AudioAttribute attribute : audioAttributes) {
      if (!attribute.isExampleSentence()) {
        String bestAudio = getFile(attribute);
        if (!decodedFiles.contains(bestAudio)) {
          toDecode.add(attribute);
        }
      }
    }
    for (AudioAttribute attribute : toDecode) {
      if (stopDecode) return 0;

      try {
        audioFileHelper.decodeOneAttribute(exercise, attribute, doHydec);
        sleep(SLEEP_BETWEEN_DECODES);
        count++;
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }

    return count;
  }

  private final AudioConversion audioConversion = new AudioConversion(null);

  /**
   * @param audioAttributes
   * @param title
   * @param exid
   * @return
   * @see #trimRef(List, String)
   */
  private Info doTrim(Collection<AudioAttribute> audioAttributes, String title, String exid) {
    int count = 0;
    int trimmed = 0;
    int changed = 0;
    for (AudioAttribute attribute : audioAttributes) {
      String bestAudio = getFile(attribute);
      String audioRef = attribute.getAudioRef();
      File absoluteFile = pathHelper.getAbsoluteFile(audioRef);

      if (absoluteFile.exists()) {
        File replacement = new File(absoluteFile.getParent(), "orig_" + absoluteFile.getName());
        if (!replacement.exists()) {
          try {
            FileUtils.copyFile(absoluteFile, replacement);
          } catch (IOException e) {
            logger.error("got " + e, e);
          }
          AudioConversion.TrimInfo trimInfo = audioConversion.trimSilence(absoluteFile, true);
          if (trimInfo.didTrim()) {
            // drop ref result info
            logger.debug("trimmed " + exid + " " + attribute + " audio " + bestAudio);
            if (!serverProps.isNoModel()) {
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
  //      if (count < 20) {
    //      logger.warn("no file for " + exid + " " + attribute + " at audio file " + bestAudio);
      //  }
      }
    }

    return new Info(trimmed, count, changed);
  }

  private static class Info {
    final int trimmed;
    final int count;
    final int changed;

    public Info(int trimmed, int count, int changed) {
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
