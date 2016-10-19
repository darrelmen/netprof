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
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/22/15.
 */
public class RefResultDecoder {
  private static final Logger logger = Logger.getLogger(RefResultDecoder.class);

  private static final boolean DO_REF_DECODE = true;
  private static final boolean DO_TRIM = true;
  //private static final int SLEEP_BETWEEN_DECODES = 2000;

  private final DatabaseImpl<CommonExercise> db;
  private final ServerProperties serverProps;

  private final AudioFileHelper audioFileHelper;
  private boolean stopDecode = false;
  private final PathHelper pathHelper;
  private final AudioConversion audioConversion;
  String mediaDir;
  String installPath;
  AudioCheck audioCheck;

  /**
   * @param db
   * @param serverProperties
   * @param pathHelper
   * @param audioFileHelper
   * @see LangTestDatabaseImpl#init()
   */
  public RefResultDecoder(DatabaseImpl<CommonExercise> db,
                          ServerProperties serverProperties,
                          PathHelper pathHelper,
                          AudioFileHelper audioFileHelper,
                          String mediaDir,
                          String installPath) {
    this.db = db;
    this.serverProps = serverProperties;
    this.pathHelper = pathHelper;
    this.audioFileHelper = audioFileHelper;
    this.audioConversion = new AudioConversion(serverProperties);
    this.mediaDir = mediaDir;
    this.installPath = installPath;
    audioCheck = new AudioCheck(serverProperties);
  }

  /**
   * Used to have cheesy check for trying to convert old audio held outside of audio table into audio table
   * entries.
   *
   * @param exercises
   * @param relativeConfigDir
   * @see LangTestDatabaseImpl#init()
   */
  public void doRefDecode(final Collection<CommonExercise> exercises, final String relativeConfigDir) {
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
/*
        if (db.getAudioDAO().numRows() < 25) {
          populateAudioTable(exercises);
        }
*/
        if (db.getServerProps().shouldRecalcStudentAudio()) {
          recalcStudentAudio();
        }
      }
    }).start();
  }

  /**
   * This was for a one time fix for weird sudanese thing where audio files were truncated on Aug 11 and 12
   * Very weird.
   *
   * @paramz pathHelper
   * @paramz path
   * @return
   */
 /* public void fixTruncated(PathHelper war) {
    Collection<AudioAttribute> audioAttributes = db.getAudioDAO().getAudioAttributes();
    int fixed = 0;
    for (AudioAttribute attribute : audioAttributes) {
      String audioRef = attribute.getAudioRef();
      File audioFile = getAbsoluteFile(war, audioRef);
      String exid = attribute.getExid();
      if (audioFile.exists()) {
        long length = audioFile.length();
        if (length == 16428) {
          List<CorrectAndScore> resultsForExIDInForUser = db.getResultDAO().getResultsForExIDInForUser(attribute.getUserid(), false, exid);
          logger.info("fixTruncated found suspect file " + audioRef + " and " + resultsForExIDInForUser.size());

          for (CorrectAndScore correctAndScore : resultsForExIDInForUser) {
            long diff = attribute.getTimestamp() - correctAndScore.getTimestamp();
            if (diff > 0 && diff < 200) {
              String orig = correctAndScore.getPath();
              logger.info("\tin db, found original (" + diff + ") " + orig);
              File origFile = getAbsoluteFile(war, orig);
              if (origFile.exists()) {
                double durationInSeconds = new AudioCheck(null).getDurationInSeconds(origFile)*1000;
                long durationInMillis = attribute.getDurationInMillis();

                if (durationInMillis == (long)durationInSeconds) {
                  logger.info("\t\tDur " + durationInSeconds + " vs " + durationInMillis + " got match - fixing...");
                  new PathWriter().copyAndNormalize(origFile, db.getServerProps(), audioFile);
                  logger.info("\t\tgot match - after length = " +audioFile.length());
                  fixed++;
                }
                else {
                  logger.warn("\t\tNO MATCH Dur " + durationInSeconds + " vs " + durationInMillis);
                }
              } else {
                logger.warn("\t\tcan't find " + origFile.getAbsolutePath());
              }
            }
          }
        }
      } else {
        if (exid.startsWith("3007")) {
          logger.info("no file at " + audioFile.getAbsolutePath());
        }
      }
    }
    logger.info("Fixed " +fixed + " files");
  }*/
/*
  private File getAbsoluteFile(PathHelper pathHelper, String path) {
    return pathHelper.getAbsoluteFile(path);
  }
*/

  private String getLanguage() {
    return serverProps.getLanguage();
  }

  private void populateAudioTable(final Collection<CommonExercise> exercises) {
    int total = 0;
    for (CommonExercise ex : exercises) {
      if (stopDecode) return;

      String refAudioIndex = ex.getRefAudioIndex();
      if (refAudioIndex != null && !refAudioIndex.isEmpty()) {
        try {
          total += db.getAudioDAO().addOldSchoolAudio(refAudioIndex,
              (AudioExercise) ex,
              serverProps.getAudioOffset(),
              mediaDir, installPath);
        } catch (SQLException e) {
          return;
        }
      }
    }
    logger.info(getLanguage() + " : populateAudioTable added " + total);
  }

  /**
   * @param exercises
   * @see #runMissingInfo
   */
  private void doMissingInfo(final Collection<CommonExercise> exercises) {
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
        db.rememberScore(res.getUniqueID(), alignmentScore, false);
      } else {
        logger.warn("no exercise for " + res.getExerciseID() + " from result?");
      }
      if (stopDecode) break;
      //	logger.debug("previously found " + res);
      //  }
    }
  }

  private void trimRef(Collection<CommonExercise> exercises, String relativeConfigDir) {
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
      Set<String> failed = new TreeSet<>();
      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
        if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
          boolean didAll = db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
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
      if (!failed.isEmpty()) {
        logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
      }
    }
  }

  private void recalcStudentAudio() {
    ResultDAO resultDAO = db.getResultDAO();

    Map<String, CommonExercise> idToEx = new HashMap<>();
    List<CommonExercise> rawExercises = db.getExerciseDAO().getRawExercises();
    for (CommonExercise ex : rawExercises) idToEx.put(ex.getID(), ex);

    int count = 0;
    Set<String> skip = new HashSet<>(Arrays.asList("slow","regular","slow_by_WebRTC","regular_by_WebRTC"));
    List<Result> results = resultDAO.getResults();
    int skipped = 0;
    int notThere = 0;
    int staleExercise = 0;
    int currentAlready = 0;

    String currentModel =  db.getServerProps().getCurrentModel();

    for (Result result : results) {
      if (result.isValid()) {
        if (skip.contains(result.getAudioType())) {skipped++;}
        else {
          String audioRef = result.getAnswer();

          boolean fileExists = false;
          if (!audioRef.contains("context=")) {
            //logger.debug("doing alignment -- ");
            // Do alignment...
            File absoluteFile = pathHelper.getAbsoluteFile(audioRef);
            fileExists = absoluteFile.exists();
          }

          String exid = result.getExid();

          if (fileExists) {
            CommonExercise exercise = idToEx.get(exid);
            if (exercise != null) {
              if (result.getModel() == null || !result.getModel().equals(currentModel)) {
                audioFileHelper.recalcOne(result, exercise);
                sleep(serverProps.getSleepBetweenDecodes());
                count++;
                if (count % 100 == 0) logger.info("recalc " + count + "/" + results.size());
              }
              else {
                currentAlready++;
              }
            }
            else {
              if (staleExercise < 1000) {
                logger.info("can't find ex '" + exid + "' in " + idToEx.size());
              }
              staleExercise++;
            }
          }
          else {
            if (notThere < 10 /*|| exid.startsWith("1")*/) logger.info("Can't find " + pathHelper.getAbsoluteFile(audioRef).getAbsolutePath());
            notThere++;
          }
        }
      }
    }

    logger.info("recalcStudentAudio did recalc on " + count + "/" + results.size() +
        " skipped " + skipped +
        " not there " + notThere + " stale exercises " + staleExercise + " currentAlready " + currentAlready);

  }

  /**
   * Do alignment and decoding on all the reference audio and store the results in the RefResult table.
   *
   * @see #doRefDecode
   */
  private void writeRefDecode(Collection<CommonExercise> exercises, String relativeConfigDir) {
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

      if (serverProps.addMissingInfo()) {
        runMissingInfo(exercises);
      } else {
        logger.debug("not looking for missing info");
      }
    }
  }

  private void runMissingInfo(final Collection<CommonExercise> exercises) {
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
        String audioRef = attribute.getAudioRef();

        boolean fileExists = false;
        File absoluteFile = pathHelper.getAbsoluteFile(audioRef);
        if (!audioRef.contains("context=")) {
          //logger.debug("doing alignment -- ");
          // Do alignment...
          fileExists = absoluteFile.exists();
        }

        if (fileExists) {
          double durationInSeconds = audioCheck.getDurationInSeconds(absoluteFile);
          long durationInMillis = attribute.getDurationInMillis();

          if (durationInMillis > 100) {
            if (durationInSeconds < 0.01) {
              logger.warn("doDecode : 1 huh? attr dur " + durationInMillis + " but check dur " + durationInSeconds);
            }
            if (audioCheck.hasValidDynamicRange(absoluteFile)) {
              audioFileHelper.decodeOneAttribute(exercise, attribute, doHydec);
              sleep(serverProps.getSleepBetweenDecodes());
              count++;
            }
            else {
              logger.info("doDecode : for " +exercise.getID()+ " (" + durationInMillis+
                  ") skip low dynamic range file "+ audioRef);
            }
          }
          else {
            if (durationInSeconds > 0.01) {
              logger.warn("doDecode : 2 huh? attr dur " + durationInMillis + " but check dur " + durationInSeconds);
            }
            logger.info("doDecode : for " +exercise.getID()+ " (" + durationInMillis+
                ") skip short file "+ audioRef);
          }
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
          AudioConversion.TrimInfo trimInfo = audioConversion.trimSilence(absoluteFile);
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
