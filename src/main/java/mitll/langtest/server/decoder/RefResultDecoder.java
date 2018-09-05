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
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.RecalcRefResponse;
import mitll.langtest.shared.scoring.RecalcResponses;
import mitll.langtest.shared.user.MiniUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//import mitll.langtest.shared.MiniUser;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/22/15.
 */
public class RefResultDecoder {
  private static final Logger logger = LogManager.getLogger(RefResultDecoder.class);

  //  private static final boolean DO_REF_DECODE = true;
  // private static final boolean DO_TRIM = false;
  // private static final int SLEEP_BETWEEN_DECODES = 2000;
//  private static final boolean DO_CALC_DNR = true;
  //private static final boolean ENSURE_OGG = false;
  private static final int MAX_SPEW = 50;
  //private static final int SLEEP_BETWEEN_DECODES = 2000;

  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  private final AudioFileHelper audioFileHelper;
  private boolean stopDecode = false;
  private final PathHelper pathHelper;
  private final AudioConversion audioConversion;
  //private final boolean hasModel;
  private final AudioCheck audioCheck;

  private final BlockingQueue<DecodeTask> queue = new LinkedBlockingQueue<>();
  private Thread consumer = null;
  private final int defaultUser;

  /**
   * @param db
   * @param serverProperties
   * @param pathHelper
   * @param audioFileHelper
   * @see Project#setAnalysis
   */
  public RefResultDecoder(DatabaseImpl db,
                          ServerProperties serverProperties,
                          PathHelper pathHelper,
                          AudioFileHelper audioFileHelper) {
    this.db = db;
    this.serverProps = serverProperties;
    this.pathHelper = pathHelper;
    this.audioFileHelper = audioFileHelper;
    this.audioConversion = new AudioConversion(serverProperties.shouldTrimAudio(), serverProperties.getMinDynamicRange());
    audioCheck = new AudioCheck(serverProperties.shouldTrimAudio(), serverProperties.getMinDynamicRange());
    defaultUser = db.getUserDAO().getDefaultUser();

  }

  /**
   * Used to have cheesy check for trying to convert old audio held outside of audio table into audio table
   * entries.
   * <p>
   * TODO : pass in project list!
   * TODO : don't pass in list of exercises.
   *
   * @param exercises
   * @see LangTestDatabaseImpl#init()
   */
/*  public void doRefDecode(final Collection<CommonExercise> exercises) {
    if (!serverProps.doAudioChecksInProduction()) return;
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (serverProps.shouldTrimAudio() || serverProps.shouldRecalcDNR()) {
          sleep(5000);
          Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(-1);
          if (serverProps.shouldTrimAudio()) {
            trimRef(exercises, exToAudio);
          }
          sleep(5000);

          if (ENSURE_OGG) {
            ensure(exercises, exToAudio);
          }

          // TODO : consider putting this back

 *//*         if (serverProps.shouldRecalcDNR()) {
            calcDNROnAudio(exercises, relativeConfigDir, exToAudio);
          }*//*
        }
        if (serverProps.shouldDoDecode()) {
          logger.warn("doRefDecode shouldDoDecode true");
          sleep(5000);
          // TODO : put this back -- !
//          if (!serverProps.isNoModel()) writeRefDecode(exercises, relativeConfigDir);
        } else {
          logger.debug(" not doing decode ref decode");
        }
*//*
        if (db.getAudioDAO().numRows() < 25) {
          populateAudioTable(exercises);
        }
*//*

// TODO : consider putting this back

        if (db.getServerProps().shouldRecalcStudentAudio()) {
          //  recalcStudentAudio();
          //logger.warn("not doing recalc student audio.");
        }
      }
    }).start();
  }*/

  /**
   * This was for a one time fix for weird sudanese thing where audio files were truncated on Aug 11 and 12
   * Very weird.
   *
   * @return
   * @paramz pathHelper
   * @paramz path
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
        } else {
          logger.debug(" not doing decode ref decode");
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

/*  @Deprecated
  private String getLanguage() {
    return serverProps.getLanguage();
  }*/

/*
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
*/

  /**
   *
   * @param projid
   * @param exercises
   * @seex #runMissingInfo
   */
/*  private void doMissingInfo(final Collection<CommonExercise> exercises) {
    int childCount = 0;

    Map<String, CommonExercise> idToEx = getIdToExercise(exercises);
    if (idToEx == null) return;

    Collection<Result> resultsToDecode = db.getResultDAO().getResultsToDecode();
    int size = resultsToDecode.size();
    logger.info("doMissingInfo found " + size + " with missing info");

    for (Result res : resultsToDecode) {
      if (childCount++ < 20) {
        logger.debug("\t doMissingInfo found " + res);
      }

//      String[] bestAudios = res.getAnswer().split(File.separator);
      //    if (bestAudios.length > 1) {
      CommonExercise exercise = idToEx.get(res.getExerciseID());
      if (exercise != null) {
        logger.info("doMissingInfo #" + childCount + " of " + size + " align " + exercise.getOldID() + " and result " + res.getUniqueID());
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
   * @param exercises
   * @param relativeConfigDir
   * @param exToAudio
   */
/*  private void calcDNROnAudio(String language,
                              Collection<CommonExercise> exercises, String relativeConfigDir, Map<Integer, List<AudioAttribute>> exToAudio) {
    if (DO_CALC_DNR) {
      String installPath = pathHelper.getInstallPath();

      int numResults = db.getRefResultDAO().getNumResults();
      logger.debug(language + " writeRefDecode : found " +
          numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio, examining " +
          exercises.size() + " exercises");

      if (stopDecode) logger.debug("Stop decode true");

      int attrc = 0;
//      Set<String> failed = new TreeSet<>();
      int dnr = 0;
      int since = 0;
      //AudioDAO audioDAO = db.getAudioDAO();
      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
        if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
          boolean didAll = db.getAudioDAO().attachAudio(exercise, *//*installPath, relativeConfigDir,*//* audioAttributes, language);
          attrc += audioAttributes.size();
          //        if (!didAll) {
          //         failed.add(exercise.getID());
          //      }

          dnr += setDNROnAudio(installPath, db.getAudioDAO(), audioAttributes);
          if (dnr - since > 2000) {
            since = dnr;
            logger.info(language + ": trimRef : did DNR on " + dnr);
          }

          dnr += setDNROnAudio(installPath, db.getAudioDAO(), audioAttributes);
          if (dnr - since > 2000) {
            since = dnr;
            logger.info(language + ": trimRef : did DNR on " + dnr);
          }
        }
      }
    }
  }*/

  /**
   * @param exercises
   * @paramx exToAudio
   */
  /*private void trimRef(Collection<CommonExercise> exercises, Map<Integer, List<AudioAttribute>> exToAudio) {
    if (DO_TRIM) {
//      String installPath = pathHelper.getInstallPath();
      int numResults = db.getRefResultDAO().getNumResults();
      // String language = getLanguage();
      logger.debug(
          //language +
          " writeRefDecode : found " +
              numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio, examining " +
              exercises.size() + " exercises");

      if (stopDecode) logger.debug("Stop decode true");

      int count = 0;
      int trimmed = 0;
      int changed = 0;
      int attrc = 0;
      int maleAudio = 0;
      int femaleAudio = 0;
      int defaultAudio = 0;
//      Set<String> failed = new TreeSet<>();
      Set<Integer> preferredVoices = serverProps.getPreferredVoices();
      for (CommonExercise exercise : exercises) {
        if (stopDecode) return;

        // TODO : get this from the project via the exercise
        String language = "";

        Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices, false);
        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices, false);

        List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
        boolean maleEmpty = maleUsers.isEmpty();
        List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
        boolean femaleEmpty = femaleUsers.isEmpty();

        String title = exercise.getForeignLanguage();
        int id = exercise.getID();
        String comment = exercise.getEnglish();
        if (!maleEmpty) {
          List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
          maleAudio += audioAttributes1.size();
          Info info = doTrim(audioAttributes1, title, id, comment, language);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (!femaleEmpty) {
          List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
          femaleAudio += audioAttributes1.size();

          Info info = doTrim(audioAttributes1, title, id, comment, language);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        } else if (maleEmpty) {
          Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();
          defaultAudio += defaultUserAudio.size();
          Info info = doTrim(defaultUserAudio, title, id, comment, language);
          trimmed += info.trimmed;
          count += info.count;
          changed += info.changed;
        }
        if (count > 0 && count % 2000 == 0) logger.debug(" trimRef examined " + count + " files.");
      }

      logger.debug(" trimRef : Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
          defaultAudio + " default " + "examined " + count +
          " trimmed " + trimmed + " dropped ref result rows = " + changed);
//      if (!failed.isEmpty()) {
//        logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
//      }
    }
  }*/

  /**
   * @param language
   * @param exercises
   * @see Project#ensureAudio
   */
  public void ensure(String language,
                     Collection<CommonExercise> exercises) {
//      String installPath = pathHelper.getInstallPath();
    int numResults = db.getRefResultDAO().getNumResults();
    logger.debug(language + " writeRefDecode : found " +
        numResults +
        " in ref results table vs " +
//        exToAudio.size() + " exercises with audio, examining " +
        exercises.size() + " exercises");

    if (stopDecode) logger.debug("Stop decode true");

    int count = 0;
    int trimmed = 0;
    int changed = 0;
    int attrc = 0;
    int maleAudio = 0;
    int femaleAudio = 0;
    int defaultAudio = 0;

    Set<Integer> preferredVoices = serverProps.getPreferredVoices();
    for (CommonExercise exercise : exercises) {
      if (stopDecode) return;

      Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices, false);
      Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices, false);

      List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
      boolean maleEmpty = maleUsers.isEmpty();
      List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
      boolean femaleEmpty = femaleUsers.isEmpty();

      String title = exercise.getForeignLanguage();
      String comment = exercise.getEnglish();

      if (!maleEmpty) {
        List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
        maleAudio += audioAttributes1.size();
        doEnsure(audioAttributes1, title, comment, language);

      }
      if (!femaleEmpty) {
        List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
        femaleAudio += audioAttributes1.size();

        doEnsure(audioAttributes1, title, comment, language);

      } else if (maleEmpty) {
        Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();
        defaultAudio += defaultUserAudio.size();
        doEnsure(defaultUserAudio, title, comment, language);

      }
      // if (childCount > 0 && childCount % 2000 == 0) logger.debug(getLanguage() + " trimRef examined " + childCount + " files.");
    }

    logger.debug(language + " ensure : Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
        defaultAudio + " default " + "examined " + count +
        " trimmed " + trimmed + " dropped ref result rows = " + changed);
//      if (!failed.isEmpty()) {
//        logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
//      }

  }

  /*private void recalcStudentAudio() {
    ResultDAO resultDAO = db.getResultDAO();

    Map<String, CommonExercise> idToEx = new HashMap<>();
    List<CommonExercise> rawExercises = db.getExerciseDAO().getRawExercises();
    for (CommonExercise ex : rawExercises) idToEx.put(ex.getID(), ex);

    int childCount = 0;
    Set<String> skip = new HashSet<>(Arrays.asList("slow", "regular", "slow_by_WebRTC", "regular_by_WebRTC"));
    List<Result> results = resultDAO.getResults();
    int skipped = 0;
    int notThere = 0;
    int staleExercise = 0;
    int currentAlready = 0;

    String currentModel = db.getServerProps().getCurrentModel();

    for (Result result : results) {
      if (result.isValid()) {
        if (skip.contains(result.getAudioType())) {
          skipped++;
        } else {
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
                childCount++;
                if (childCount % 100 == 0) logger.info("recalc " + childCount + "/" + results.size());
              } else {
                currentAlready++;
              }
            } else {
              if (staleExercise < 1000) {
                logger.info("can't find ex '" + exid + "' in " + idToEx.size());
              }
              staleExercise++;
            }
          } else {
            if (notThere < 100 *//*|| exid.startsWith("1")*//*)
              logger.info("Can't find " + pathHelper.getAbsoluteFile(audioRef).getAbsolutePath());
            notThere++;
          }
        }
      }
    }

    logger.info("recalcStudentAudio did recalc on " + childCount + "/" + results.size() +
        " skipped " + skipped +
        " not there " + notThere + " stale exercises " + staleExercise + " currentAlready " + currentAlready);

  }*/

/*  private int setDNROnAudio(String installPath, IAudioDAO audioDAO, List<AudioAttribute> audioAttributes) {
    int c = 0;

    for (AudioAttribute audio : audioAttributes) {
      float dnr1 = audio.getDnr();
      if (dnr1 < 0) {
        String audioRef = audio.getAudioRef();
        File test = new File(installPath, audioRef);

        boolean exists = test.exists();
        if (!exists) {
          test = new File(installPath, audioRef);
          exists = test.exists();
          // child = audioRef;
        }
        if (exists) {
          dnr1 = audioCheck.getDNR(test);
          audioDAO.updateDNR(audio.getUniqueID(), dnr1);
          c++;
        }
      }
    }
    return c;
  }*/

  /*private void recalcStudentAudio() {
    IResultDAO resultDAO = db.getResultDAO();

    Map<String, CommonExercise> idToEx = new HashMap<>();
    List<CommonExercise> rawExercises = db.getExerciseDAO().getRawExercises();
    for (CommonExercise ex : rawExercises) idToEx.put(ex.getID(), ex);

    int childCount = 0;
    Set<String> skip = new HashSet<>(Arrays.asList("slow", "regular", "slow_by_WebRTC", "regular_by_WebRTC"));
    List<Result> results = resultDAO.getResults();
    int skipped = 0;
    int notThere = 0;
    int staleExercise = 0;
    int currentAlready = 0;

    String currentModel = db.getServerProps().getCurrentModel();

    for (Result result : results) {
      if (result.isValid()) {
        if (skip.contains(result.getAudioType())) {
          skipped++;
        } else {
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
                childCount++;
                if (childCount % 100 == 0) logger.info("recalc " + childCount + "/" + results.size());
              } else {
                currentAlready++;
              }
            } else {
              if (staleExercise < 1000) {
                logger.info("can't find ex '" + exid + "' in " + idToEx.size());
              }
              staleExercise++;
            }
          } else {
            if (notThere < 10 *//*|| exid.startsWith("1")*//*)
              logger.info("Can't find " + pathHelper.getAbsoluteFile(audioRef).getAbsolutePath());
            notThere++;
          }
        }
      }
    }

    logger.info("recalcStudentAudio did recalc on " + childCount + "/" + results.size() +
        " skipped " + skipped +
        " not there " + notThere + " stale exercises " + staleExercise + " currentAlready " + currentAlready);

  }
*/

  /**
   * TODOx : put this back so we can run it per project
   * Do alignment and decoding on all the reference audio and store the results in the RefResult table.
   *
   * @see Project#recalcRefAudio
   */
  public RecalcRefResponse writeRefDecode(String language, Collection<CommonExercise> exercises, int projid) {
    // boolean b = db.getServerProps().shouldDoDecode();
    //logger.warn("writeRefDecode got " + b + " for should do decode");
    //if (false) {
//    Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(projid);
    // String installPath = pathHelper.getInstallPath();

    if (exercises.isEmpty()) {
      logger.error("writeRefDecode huh? no exercises?");
      return new RecalcRefResponse(RecalcResponses.ERROR);
    }

    db.getAudioDAO().attachAudioToExercises(exercises, language);

//    int numResults = db.getRefResultDAO().getNumResults();
//    logger.debug("writeRefDecode : " +
//        // numResults +
//        " in ref results table" +
//        " vs " + exToAudio.size() + " exercises with audio");

    db.getRefResultDAO().deleteForProject(projid);

    Set<Integer> decodedFiles = getDecodedFiles(projid);
    logger.info("writeRefDecode for " +
        "\n\tproject  " + projid +
        "\n\tfound    " + decodedFiles.size() + " previous ref results," +
        "\n\tchecking " + exercises.size() + " exercises ");

    if (stopDecode) logger.debug("Stop decode true");
//      int childCount = 0;
//      int attrc = 0;
//      int maleAudio = 0;
//      int femaleAudio = 0;
//      int defaultAudio = 0;

    Stats allstats = new Stats();

    int total = 0;
    int context = 0;
    for (CommonExercise exercise : exercises) {
      if (stopDecode) return new RecalcRefResponse(RecalcResponses.STOPPED);

      Stats total1 = queueDecodeExercise(language, decodedFiles, exercise);
      total += total1.vocab;
      context += total1.context;
    }

    if (consumer == null) {
      consumer = new Thread(new Consumer());
      consumer.setDaemon(true);
      consumer.start();
    }

    logger.info("writeRefDecode " +
        "\n\texamined " + exercises.size() + " exercises" +
        "\n\tqueued   " + total + " vocab audio files" +
        "\n\tqueued   " + context + " context audio files"
    );
//    logger.debug("writeRefDecode : Out of " + allstats.attrc + " best audio files, " +
//        allstats.maleAudio + " male, " + allstats.femaleAudio + " female, " +
//        allstats.defaultAudio + " default " + "decoded " + allstats.count);

    return new RecalcRefResponse(total + context == 0 ? RecalcResponses.COMPLETED : RecalcResponses.WORKING, total + context);
  }

  /**
   * @see #RefResultDecoder
   */
  class Consumer implements Runnable {
    public void run() {
      try {
        int c = 0;
        while (!stopDecode) {
          {
            DecodeTask remove = queue.take();
            decodeOneExercise(remove.language, remove.exercise, remove.toDecode, defaultUser);
          }

          if (++c % 500 == 0) logger.info("decode did " + c);

          if (queue.isEmpty()) {
            logger.info("decode queue is empty.");
          }
          if (stopDecode) {
            logger.info("stop decoding remaning " + queue.size() + " jobs");
            queue.clear();
          }
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }
  }

  /**
   * @param language
   * @param decodedFiles
   * @param exercise
   * @see #writeRefDecode(String, Collection, int)
   */
  @NotNull
  private Stats queueDecodeExercise(String language, Set<Integer> decodedFiles, CommonExercise exercise) {
    int added = 0;
    int possible = 0;
    int vocab = 0;
    int context = 0;
    Stats stats = new Stats();

    {
      Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
      int added1 = queueDecode(language, decodedFiles, exercise, audioAttributes);
      added += added1;
      vocab += added1;
      stats.add(new Stats(vocab, 0));
      possible += audioAttributes.size();
    }

    for (CommonExercise direct : exercise.getDirectlyRelated()) {
      Collection<AudioAttribute> audioAttributes2 = direct.getAudioAttributes();
      int added1 = queueDecode(language, decodedFiles, direct, audioAttributes2);

      added += added1;
      context += added1;
      stats.add(new Stats(0, context));
      possible += audioAttributes2.size();
    }

    if (added == 0 && (spew++ < MAX_SPEW || spew % 100 == 0)) {
      logger.info("queueDecodeExercise (" + spew +
          ") no audio for ex " + exercise.getID() + " out of " + possible);
    }
    return stats;
  }

  /**
   * @param language
   * @param decodedFiles
   * @param exercise
   * @param audioAttributes1
   * @return number of audio cuts to decode again
   * @see #queueDecodeExercise
   */
  private int queueDecode(String language,
                          Set<Integer> decodedFiles,
                          CommonExercise exercise,
                          Collection<AudioAttribute> audioAttributes1) {

    int num = 0;
    if (!audioAttributes1.isEmpty()) {
      try {
        DecodeTask e = doDecode(language, decodedFiles, exercise, audioAttributes1);

        if (e.hasAudioToDecode()) {
          queue.put(e);
          num += e.getNumToDecode();
        }
      } catch (InterruptedException e) {
        logger.error("got " + e, e);
      }
    }
    return num;
  }

  private int spew = 0;

  private void sleep(int millis) {
    try {
      Thread.sleep(millis); // ???
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

 /* private static class Stats {
    final int count = 0;
    final int attrc = 0;
    final int maleAudio = 0;
    final int femaleAudio = 0;
    final int defaultAudio = 0;

//    public void add(Stats other) {
//      count += other.count;
//      attrc += other.attrc;
//      maleAudio += other.maleAudio;
//      femaleAudio += other.femaleAudio;
//      defaultAudio += other.defaultAudio;
//    }
  }
  */

  private static class Stats {
    private int vocab = 0;
    private int context = 0;

    public Stats() {
    }

    public Stats(int vocab, int context) {
      this.vocab = vocab;
      this.context = context;
    }

    public void add(Stats other) {
      this.vocab += other.vocab;
      this.context += other.context;
    }

    public String toString() {
      return "Stats " + vocab + " context " + context;
    }
  }

  /**
   * Get the set of files that have already been decoded and aligned so we don't do them a second time.
   *
   * @return
   * @see #writeRefDecode
   */
  private Set<Integer> getDecodedFiles(int projid) {
    //logger.info("getDecodedFiles ----");
    //List<Result> results = db.getRefResultDAO().getResults();

    //List<String> files = db.getRefResultDAO().getAllFilesForProject(projid);
    // String modelsDir = db.getProject(projid).getModelsDir();

    List<Integer> files = db.getRefResultDAO().getAllAudioIDsForProject(projid);

 /*   Set<String> decodedFiles = new HashSet<>();
    //   for (Result res : results) {
    for (Integer res : files) {
      // String[] bestAudios = res.getAnswer().split(File.separator);
      String[] bestAudios = res.split(File.separator);
      if (bestAudios.length > 1) {
        String bestAudio = bestAudios[bestAudios.length - 1];
        decodedFiles.add(bestAudio);
        if (stopDecode) break;
      }
    }*/

    return new HashSet<>(files);
  }

  /**
   * @param decodedFiles
   * @param exercise
   * @param audioAttributes
   * @return
   * @see #queueDecodeExercise
   */
  private DecodeTask doDecode(String language,
                              Set<Integer> decodedFiles,
                              CommonExercise exercise,
                              Collection<AudioAttribute> audioAttributes) {
    List<AudioAttribute> notYetDecoded = getNotYetDecoded(decodedFiles, audioAttributes);
    return new DecodeTask(language, exercise, notYetDecoded);
  }

  private static class DecodeTask {
    final String language;
    final CommonExercise exercise;
    final List<AudioAttribute> toDecode;

    DecodeTask(String language, CommonExercise exercise, List<AudioAttribute> toDecode) {
      this.language = language;
      this.exercise = exercise;
      this.toDecode = toDecode;
    }

    boolean hasAudioToDecode() {
      return !toDecode.isEmpty();
    }

    int getNumToDecode() {
      return toDecode.size();
    }
  }

  /**
   * @param language
   * @param exercise
   * @param toDecode
   * @param defaultUser
   * @return
   * @see Consumer#run
   */
  private int decodeOneExercise(String language, CommonExercise exercise, List<AudioAttribute> toDecode, int defaultUser) {
    int count = 0;
    for (AudioAttribute attribute : toDecode) {
      if (stopDecode) return 0;

      try {
        String audioRef = attribute.getAudioRef();
        File absoluteFile = new File(getAbsFilePath(attribute, language));
        boolean fileExists = absoluteFile.exists();

        if (fileExists) {
          double durationInSeconds = audioCheck.getDurationInSeconds(absoluteFile);
          long durationInMillis = attribute.getDurationInMillis();

          if (durationInMillis > 100) {
            if (durationInSeconds < 0.01) {
              logger.warn("doDecode : 1 huh? attr dur " + durationInMillis + " but check dur " + durationInSeconds);
            }
            float dnr = attribute.getDnr();
            if (dnr < 0) dnr = audioCheck.getDNR(absoluteFile);
            if (dnr > audioCheck.getMinDNR()) {
              audioFileHelper.decodeOneAttribute(exercise, attribute, defaultUser, absoluteFile);
              sleep(serverProps.getSleepBetweenDecodes());
              count++;
            } else {
              logger.info("doDecode : for " + exercise.getID() + " (" + durationInMillis +
                  ") skip low dynamic range file " + audioRef);
            }
          } else {
            if (durationInSeconds > 0.01) {
              logger.warn("doDecode : 2 huh? attr dur " + durationInMillis + " but check dur " + durationInSeconds);
            }
            logger.info("doDecode : for " + exercise.getID() + " (" + durationInMillis +
                ") skip short file " + audioRef);
          }
        } else {
          logger.info("decodeOneExercise : can't find audio file in attribute at " + absoluteFile.getAbsolutePath());
        }

      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }

    return count;
  }

  private String getAbsFilePath(AudioAttribute attribute, String language) {
    String audioRef = attribute.getAudioRef();
    return getAbsFilePath(language, audioRef);
  }

  private String getAbsFilePath(String language, String audioRef) {
    String audioBaseDir = serverProps.getAudioBaseDir();
    String absPathForAudio = audioConversion.getAbsPathForAudio(audioRef, language, "", audioBaseDir);
//    logger.info("getAbsFilePath audioBaseDir " + audioBaseDir + " " + absPathForAudio);
    return absPathForAudio;
  }

  @NotNull
  private List<AudioAttribute> getNotYetDecoded(Set<Integer> decodedFiles, Collection<AudioAttribute> audioAttributes) {
    List<AudioAttribute> toDecode = new ArrayList<>();

    for (AudioAttribute attribute : audioAttributes) {
      if (!decodedFiles.contains(attribute.getUniqueID())) {
        toDecode.add(attribute);
      }
    }
    return toDecode;
  }

  private void doEnsure(Collection<AudioAttribute> audioAttributes, String title, String comment, String language) {
    int c = 0;
    for (AudioAttribute attribute : audioAttributes) {
      String audioRef = attribute.getAudioRef();
      File absoluteFile = pathHelper.getAbsoluteFile(audioRef);

      if (absoluteFile.exists()) {
        String author = attribute.getUser().getUserID();
        audioConversion.ensureWriteMP3(pathHelper.getInstallPath(), audioRef, false,
            new TrackInfo(title, author, comment, language));
      }
    }
  }

  /**
   * @return
   * @paramx title
   * @paramx exid
   * @paramx audioAttributes
   * @seex #trimRef
   */
/*  private Info doTrim(Collection<AudioAttribute> audioAttributes, String title, int exid, String comment,
                      String language) {
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
            logger.error("doTrim got " + e, e);
          }
          TrimInfo trimInfo = audioConversion.trimSilence(absoluteFile);
          if (trimInfo.didTrim()) {
            // drop ref result info
            logger.debug("doTrim trimmed " + exid + " " + attribute + " audio " + bestAudio);
            if (hasModel) {
              //  boolean b = db.getRefResultDAO().removeForAudioFile(absoluteFile.getAbsolutePath());
              boolean b = db.getRefResultDAO().removeByAudioID(attribute.getUniqueID());
              if (!b) {
                logger.warn("doTrim for " + exid + " couldn't remove " + absoluteFile.getAbsolutePath() + " for " + attribute);
              } else {
                changed++;
              }
            }
            trimmed++;
          }
          count++;

          audioConversion.ensureWriteMP3(pathHelper.getInstallPath(), audioRef, trimInfo.didTrim(),
              new TrackInfo(title, attribute.getUser().getUserID(), comment, language));
        }
      } else {
//        logger.warn("no file for " + exid + " " + attribute + " at audio file " + bestAudio);
      }
    }

    return new Info(trimmed, count, changed);
  }*/

/*  private static class Info {
    final int trimmed;
    final int count;
    final int changed;

    Info(int trimmed, int count, int changed) {
      this.trimmed = trimmed;
      this.count = count;
      this.changed = changed;
    }
  }*/

/*  private String getFile(AudioAttribute attribute) {
    String[] bestAudios = attribute.getAudioRef().split(File.separator);
    return bestAudios[bestAudios.length - 1];
  }*/
  public void setStopDecode(boolean stopDecode) {
    this.stopDecode = stopDecode;
  }
}
