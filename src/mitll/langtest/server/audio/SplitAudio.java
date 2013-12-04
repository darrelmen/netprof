package mitll.langtest.server.audio;

import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import corpus.HTKDictionary;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/16/13
 * Time: 6:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class SplitAudio {
  private static final double DURATION_CHECK = 1.2;

  private static final int LOW_WORD_SCORE_THRESHOLD = 20;
  private static final boolean THROW_OUT_FAST_LONGER_THAN_SLOW = false;
  public static final float LOW_SINGLE_SCORE_THRESHOLD = 0.4f;

  private static Logger logger = Logger.getLogger(SplitAudio.class);

  private boolean debug;
  private static final int MAX = Integer.MAX_VALUE;
  public static final double BAD_PHONE = 0.1;
  public static final double BAD_PHONE_PERCENTAGE = 0.2;
  protected static final double MIN_DUR = 0.2;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private AudioCheck audioCheck = new AudioCheck();

  /**
   * Go through all exercise, find all results for each, take best scoring audio file from results
   * <p/>
   * e.g. for dari:
   * <p/>
   * 10 dariAudio dari 9000-dari-course-examples.xlsx
   * <p/>
   * for msa:
   * <p/>
   * 20 msaAudio msa 3700-msa-course-examples.xlsx
   *
   * @param numThreads
   * @param audioDir
   * @param throwAwayNonNativeAudio
   * @param onlyTheseIDs
   */
  // TODO later take data and use database so we keep metadata about audio
  private void convertExamples(int numThreads, String audioDir, String language, String spreadsheet, String dbName,
                               boolean throwAwayNonNativeAudio, Set<String> onlyTheseIDs) throws Exception {
    final String configDir = getConfigDir(language);

    DatabaseImpl db = new DatabaseImpl(
      configDir,
      dbName,
      configDir +
        spreadsheet);

    final Map<String, Exercise> idToEx = getIdToExercise(db);

    if (language.equals("mandarin")) {
      new Mandarin().correctMandarin(idToEx);
    }

    Map<String, List<Result>> idToResults = getIDToResultsMap(db,onlyTheseIDs);
    Set<Long> nativeUsers = new UserDAO(db).getNativeUsers();
    logger.info("convertExamples : Found " + nativeUsers.size() + " native users");
    convertExamples(numThreads, audioDir, language, idToEx, idToResults, nativeUsers, throwAwayNonNativeAudio);
  }

  /**
   * @param numThreads
   * @param audioDir
   * @param language
   * @param idToEx
   * @param idToResults
   * @param nativeUsers
   * @param throwAwayNonNativeAudio
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   * @see #convertExamples(int, String, String, String, String, boolean, java.util.Set
   */
  protected void convertExamples(int numThreads, String audioDir, String language,
                                 Map<String, Exercise> idToEx,
                                 Map<String, List<Result>> idToResults, Set<Long> nativeUsers,
                                 boolean throwAwayNonNativeAudio) throws IOException, InterruptedException, ExecutionException {
    final String configDir = getConfigDir(language);

    final String placeToPutAudio = ".." + File.separator + audioDir + File.separator;
    final File newRefDir = new File(placeToPutAudio + "refAudio");
    newRefDir.mkdir();
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    bestDir.mkdir();

    final Map<String, String> properties = getProperties(language, configDir);
    ASRScoring scoring = getAsrScoring(".", null, properties);

    final HTKDictionary dict = scoring.getDict();

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    final FileWriter missingSlow = new FileWriter(configDir + "missingSlow.txt");
    final FileWriter missingFast = new FileWriter(configDir + "missingFast.txt");
    List<Future<?>> futures = getSplitAudioFutures(idToEx, missingSlow, missingFast, idToResults, placeToPutAudio,
      newRefDir, bestDir,
      properties, dict, executorService, language, nativeUsers, throwAwayNonNativeAudio);

    blockUntilComplete(futures);

    missingFast.close();
    missingSlow.close();
    logger.info("closing missing slow");
    executorService.shutdown();
  }

  protected String getConfigDir(String language) {
    String installPath = ".";
    String dariConfig = File.separator +
      "war" +
      File.separator +
      "config" +
      File.separator +
      language +
      File.separator;
    return installPath + dariConfig;
  }

  /**
   * @param db
   * @return
   * @see #convertExamples(int, String, String, String, String, boolean, java.util.Set
   * @see English#normalize()
   * @see SplitSimpleMSA#splitSimpleMSA(int)
   */
  protected Map<String, Exercise> getIdToExercise(DatabaseImpl db) {
    final Map<String, Exercise> idToEx = new HashMap<String, Exercise>();

    List<Exercise> exercises = db.getExercises();
    logger.debug("Got " + exercises.size() + " exercises first = " + exercises.iterator().next().getID());
    // int count = 0;
    for (Exercise e : exercises) {
      idToEx.put(e.getID(), e);
      //  if (count++ < 100) logger.debug("got " + e.getID());
    }
    return idToEx;
  }

  protected void blockUntilComplete(List<Future<?>> futures) throws InterruptedException, ExecutionException {
    logger.info("got " + futures.size() + " futures");
    for (Future<?> future : futures) {
      /*Object o =*/ future.get();
    }
    logger.info("all " + futures.size() + " futures complete");
  }

  /**
   * @param idToEx
   * @param missingSlow
   * @param missingFast
   * @param idToResults
   * @param placeToPutAudio
   * @param newRefDir
   * @param bestDir
   * @param properties
   * @param dict
   * @param executorService
   * @param language
   * @param throwAwayNonNativeAudio
   * @return
   * @see #convertExamples
   */
  private List<Future<?>> getSplitAudioFutures(final Map<String, Exercise> idToEx,
                                               final FileWriter missingSlow, final FileWriter missingFast,
                                               Map<String, List<Result>> idToResults,
                                               final String placeToPutAudio, final File newRefDir, final File bestDir,
                                               final Map<String, String> properties,
                                               final HTKDictionary dict, ExecutorService executorService, final String language,
                                               Set<Long> nativeUsers,
                                               boolean throwAwayNonNativeAudio) {
    List<Future<?>> futures = new ArrayList<Future<?>>();
    List<String> resultIDs = getIdsSorted(idToResults);
    List<String> exerciseIDs = getIdsSorted(idToEx);
    if (exerciseIDs.size() != idToEx.size()) logger.error("\n\n\ngetSplitAudioFutures huh? lost items???");
    if (!resultIDs.containsAll(exerciseIDs)) {
      List<String> missingIds = new ArrayList<String>(resultIDs);
      /*boolean b =*/ missingIds.removeAll(exerciseIDs);
      logger.warn("result resultIDs without matching exercise resultIDs are " + missingIds.subList(0, Math.min(100, missingIds.size())));
    }
    for (final String id : resultIDs) {
      final List<Result> resultList = idToResults.get(id);
      if (resultList == null) {
        logger.warn("getSplitAudioFutures skipping " + id + " no results...");
      } else {
        final List<Result> natives = getResultsByNatives(nativeUsers, resultList);
        if (natives.isEmpty() && !resultList.isEmpty() && throwAwayNonNativeAudio) {
          logger.warn("getSplitAudioFutures id " + id + " *all* " + resultList.size() + " responses were by non-native speakers");
        } else {
          //logger.debug("getSplitAudioFutures id " + id + " examining " + natives.size() + " native responses.");

          Future<?> submit = executorService.submit(new Runnable() {
            @Override
            public void run() {
              try {
                getBestForEachExercise(id, idToEx, missingSlow, missingFast,
                  newRefDir, bestDir,
                  natives,
                  placeToPutAudio, dict, properties, language);
              } catch (IOException e) {
                logger.error("getSplitAudioFutures Doing " + id + " and " + resultList + " Got " + e, e);
              }
            }
          });
          futures.add(submit);
        }
      }
    }
    return futures;
  }

  private List<Result> getResultsByNatives(Set<Long> nativeUsers, List<Result> resultList) {
    final List<Result> natives = new ArrayList<Result>();
    for (Result r : resultList) {
      if (nativeUsers.contains(r.userid)) {
        natives.add(r);
      }
    }
    return natives;
  }

  private List<String> getIdsSorted(Map<String, ?> idToResults) {
    List<String> ids = new ArrayList<String>(idToResults.keySet());
    return getIdsSorted(ids);
  }

  private List<String> getIdsSorted(List<String> ids) {
    Collections.sort(ids, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        int i = Integer.parseInt(o1);
        int j = Integer.parseInt(o2);
        return i < j ? -1 : i > j ? +1 : 0;
      }
    });
    return ids;
  }

  protected Map<String, String> getProperties(String language, String configDir) throws IOException {
    Properties props = new Properties();
    String propFile = configDir + File.separator + language + ".properties";
    logger.debug("reading from props " + propFile);
    FileInputStream inStream = new FileInputStream(propFile);
    props.load(inStream);
    Map<String, String> properties = getProperties(props);
    inStream.close();
    return properties;
  }

  /**
   *
   * @param db
   * @param onlyTheseIDs
   * @return
   * @see English#convertEnglish(int, String)
   * @see #convertExamples(int, String, String, String, String, boolean, java.util.Set
   */
  protected Map<String, List<Result>> getIDToResultsMap(DatabaseImpl db, Set<String> onlyTheseIDs) {
    Map<String, List<Result>> idToResults = new HashMap<String, List<Result>>();
    List<Result> results = db.getResults();
    logger.debug("Got " + results.size() + " results");
    for (Result r : results) {
      String id = r.id;
      int i = Integer.parseInt(id);
      if (i < MAX && (onlyTheseIDs.isEmpty()  || onlyTheseIDs.contains(id))) {
        List<Result> resultList = idToResults.get(id);
        if (resultList == null) {
          idToResults.put(id, resultList = new ArrayList<Result>());
        }
        resultList.add(r);
      }
    }
    if (idToResults.isEmpty())
      logger.warn("id->results is empty?");
    else
      logger.debug("got " + idToResults.size() + " id->results, first key " + idToResults.keySet().iterator().next());
    return idToResults;
  }

  protected ASRScoring getAsrScoring(String installPath, HTKDictionary dictionary, Map<String, String> properties) {
    String deployPath = installPath + File.separator + "war";
    return dictionary == null ? new ASRScoring(deployPath, properties, (LangTestDatabaseImpl)null) : new ASRScoring(deployPath, properties, dictionary);
  }

  private Map<String, String> getProperties(Properties props) {
    Map<String, String> kv = new HashMap<String, String>();
    for (Object prop : props.keySet()) {
      String sp = (String) prop;
      kv.put(sp, props.getProperty(sp).trim());
    }
    return kv;
  }

  /**
   * @param exid2
   * @param idToEx
   * @param missingSlow
   * @param missingFast
   * @param newRefDir
   * @param bestDir
   * @param collectedAudioDir
   * @param dictionary
   * @param properties
   * @param language
   * @throws IOException
   * @see #getSplitAudioFutures
   */
  private void getBestForEachExercise(String exid2, Map<String, Exercise> idToEx, FileWriter missingSlow, FileWriter missingFast,
                                      File newRefDir, File bestDir,
                                      List<Result> resultsForExercise,

                                      String collectedAudioDir, HTKDictionary dictionary, Map<String, String> properties, String language) throws IOException {
    if (resultsForExercise.isEmpty()) return;

    Exercise exercise = idToEx.get(exid2);
    if (exercise == null) {
      if (exid2.length() > 3) {
        String trimmed = exid2.substring(0, exid2.length() - 3);
        logger.warn("getBestForEachExercise trying exid " + trimmed);

        exercise = idToEx.get(trimmed);
      }
    }
    if (exercise == null) {
      List<String> idsSorted = getIdsSorted(idToEx);
      logger.warn("getBestForEachExercise skipping ex id '" + exid2 + "' since not in " + idToEx.size() + " exercises (likely has semicolon), e.g. '" + idsSorted.get(0) + "' and '" + idsSorted.get(1) + "'");
      return;
    }
    getBest(missingSlow, missingFast, newRefDir, bestDir,
      collectedAudioDir, dictionary, properties, resultsForExercise, exid2, exercise, language);
  }

  int good = 0;
  int bad = 0;
  int both = 0;

  /**
   * find best scoring fast and slow audio split files from the set of originals
   *
   * @param missingSlow
   * @param missingFast
   * @param newRefDir
   * @param bestDir
   * @param collectedAudioDir
   * @param dictionary
   * @param properties
   * @param resultsForExercise
   * @param exid
   * @param exercise
   * @param language
   * @throws IOException
   * @see #getBestForEachExercise(String, java.util.Map, java.io.FileWriter, java.io.FileWriter, java.io.File, java.io.File, java.util.List, String, corpus.HTKDictionary, java.util.Map, String)
   */
  private void getBest(FileWriter missingSlow, FileWriter missingFast, File newRefDir, File bestDir,
                       String collectedAudioDir,
                       HTKDictionary dictionary, Map<String, String> properties,
                       List<Result> resultsForExercise, String exid, Exercise exercise, String language) throws IOException {
    String refSentence = language.equalsIgnoreCase("english") ? exercise.getEnglishSentence() : exercise.getRefSentence();
    refSentence = cleanRefSentence(refSentence);

    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefFromSplit(split);
    int refLength = split.length;
    String firstToken = split[0].trim();
    String lastToken = split[refLength - 1].trim();
    logger.debug("refSentence '" + refSentence + "' length " + refLength + " first |" + firstToken + "| last |" + lastToken + "|");

    File refDirForExercise = new File(newRefDir, exid);
    refDirForExercise.mkdir();

    final ASRScoring scoring = getAsrScoring(".", dictionary, properties);
    FastAndSlow fastAndSlow = getBestFilesFromResults(scoring, resultsForExercise, exercise, refSentence,
      firstToken, lastToken,
      refLength, refDirForExercise, collectedAudioDir);

    if (fastAndSlow.valid) {
      logger.debug("for " + exid + " : '" + exercise.getEnglishSentence() + "'/'" + exercise.getRefSentence() +
        "' best is " + fastAndSlow);// + " total " + bestTotal);

      File bestDirForExercise = new File(bestDir, exid);
      //  logger.debug("for '" +refSentence + "' making dir " + key + " at " + bestDirForExercise.getAbsolutePath());
      bestDirForExercise.mkdir();
      boolean gotBoth = writeBestFiles(missingSlow, missingFast, exid,
        bestDirForExercise, fastAndSlow);
      if (gotBoth) both++; // only if both fast and slow were written do we record it as having both parts done properly
      good++;
    } else {
      bad++;

      int pct = (int) (100f * (float) both / ((float) (good + bad)));
      logger.warn("Tally : bad " + bad + "/" + good + " good / " + both + " both (" + pct +
        "%): no valid audio for " + exid);

      recordMissingFast(missingFast, exid);
      recordMissingFast(missingSlow, exid);
    }
  }

  private String cleanRefSentence(String refSentence) {
    refSentence = removeTrailingPunct(refSentence.trim());
    refSentence = removeDotDotDot(refSentence);
    return refSentence;
  }

  private String removeTrailingPunct(String refSentence) {
    int before = refSentence.length();
    String newSentence = refSentence.replaceAll("\\p{P}+$", "");
    if (newSentence.length() != before)
      logger.info("removeTrailingPunct : removed ending punct : sentence now '" + refSentence +
        "'");
    return newSentence;
  }

  private String removeDotDotDot(String refSentence) {
    int before = refSentence.length();
    String newSentence = refSentence.replaceAll("\\.\\.\\. ", "");
    if (newSentence.length() != before) logger.info("removeDotDotDot : removed ... : sentence now '" + refSentence +
      "'");
    return newSentence;
  }

  protected void recordMissingFast(FileWriter missingFast, String name) throws IOException {
    synchronized (missingFast) {
      missingFast.write(name + "\n");
      missingFast.flush();
    }
  }

  /**
   * TODO : do we want to remove punctuation???
   *
   * @param split
   * @return
   */
  protected String getRefFromSplit(String[] split) {
    String newRefSentence = getRefSentence(split).trim();
    return newRefSentence;
  }

  private String getRefSentence(String[] refSentences) {
    StringBuilder builder = new StringBuilder();
    for (String s : refSentences) {
      builder.append(s).append(" ");
    }
    return builder.toString();
  }

  /**
   * Run alignment and split the audio.
   *
   * @param scoring
   * @param resultsForExercise
   * @param exercise
   * @param refSentence
   * @param first
   * @param last
   * @param refLength
   * @param refDirForExercise
   * @param collectedAudioDir
   * @return
   * @see #getBest
   */
  private FastAndSlow getBestFilesFromResults(ASRScoring scoring, List<Result> resultsForExercise,
                                              Exercise exercise, String refSentence,
                                              String first, String last, int refLength,
                                              File refDirForExercise, String collectedAudioDir) {
    String id = exercise.getID();
    float bestSlow = 0, bestFast = 0;
    File bestSlowFile = null, bestFastFile = null;
    for (Result r : resultsForExercise) {
      File answer = new File(r.answer);
      String name = answer.getName().replaceAll(".wav", "");
      String parent = collectedAudioDir + File.separator + answer.getParent();

      double durationInSeconds = getDuration(answer, parent);
      if (durationInSeconds < MIN_DUR) {
        if (durationInSeconds > 0) {
          logger.warn("skipping " + name + " since it's less than a " + MIN_DUR + " second long.");
        }
        continue;
      }
      String testAudioFileNoSuffix = getConverted(parent, name);

      //logger.debug("parent " + parent + " running result " + r.uniqueID + " for exercise " + id + " and audio file " + name);

      Scores align = getAlignmentScores(scoring, refSentence, name, parent, testAudioFileNoSuffix);

      try {
        String wordLabFile = prependDeploy(parent, testAudioFileNoSuffix + ".words.lab");
        GetAlignments alignments = new GetAlignments(first, last, refLength, name, wordLabFile).invoke();
        boolean valid = alignments.isValid();
        float hydecScore = align.hydecScore;
        if (!valid) {
          logger.warn("\n---> ex " + id + " " + exercise.getEnglishSentence() +
            " score " + hydecScore +
            " invalid alignment : " + alignments.getWordSeq() + " : " + alignments.getScores());
        }
        if (valid && hydecScore > 0.1f) {
          FastAndSlow consistent = writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            alignments, false);
          if (consistent.valid) {
            File fastFile = consistent.fast;
            String fastName = fastFile.getName().replaceAll(".wav", "");

            Scores fast = getAlignmentScoresNoDouble(scoring, refSentence, fastName, fastFile.getParent(), getConverted(fastFile));
            if (fast.hydecScore > bestFast && fast.hydecScore > LOW_SINGLE_SCORE_THRESHOLD) {
              float percentBadFast = getPercentBadPhones(fastName, fast.eventScores.get("phones"));
              if (percentBadFast > BAD_PHONE_PERCENTAGE) {
                logger.warn("---> rejecting new best b/c % bad phones = " + percentBadFast + " so not new best fast : ex " + id + " " + exercise.getEnglishSentence() +
                  " score " + bestFast + " hydecScore " + hydecScore + "/" + fast.hydecScore);
              } else {
                bestFast = fast.hydecScore;
                bestFastFile = fastFile;
                logger.debug("new best fast : ex " + id + " " + exercise.getEnglishSentence() +
                  " best so far is " + bestFastFile.getName() + " score " + bestFast + " hydecScore " + hydecScore + "/" + fast.hydecScore);
              }
            }

            File slowFile = consistent.slow;
            String slowName = slowFile.getName().replaceAll(".wav", "");
            Scores slow = getAlignmentScoresNoDouble(scoring, refSentence, slowName, slowFile.getParent(), getConverted(slowFile));
            if (slow.hydecScore > bestSlow && slow.hydecScore > LOW_SINGLE_SCORE_THRESHOLD) {
              float percentBadSlow = getPercentBadPhones(slowName, slow.eventScores.get("phones"));
              if (percentBadSlow > BAD_PHONE_PERCENTAGE) {
                logger.warn("---> rejecting new best b/c % bad phones = " + percentBadSlow + " so not new best slow : ex " + id + " " + exercise.getEnglishSentence() +
                  " score " + bestSlow + " hydecScore " + hydecScore + "/" + slow.hydecScore);
              } else {
                bestSlow = slow.hydecScore;
                bestSlowFile = slowFile;

                logger.debug("new best slow : ex " + id + " " + exercise.getEnglishSentence() +
                  " best so far is " + bestSlowFile.getName() + " score " + bestSlow + " hydecScore " + hydecScore + "/" + slow.hydecScore);
              }
            }
          }
        }
      } catch (IOException e) {
        logger.error("Got " + e, e);
      }
    }
    return new FastAndSlow(bestFastFile, bestSlowFile);
  }

  protected float getPercentBadPhones(String fastName, Map<String, Float> phones) {
    int countBad = 0;
    for (Map.Entry<String, Float> phoneToScore : phones.entrySet()) {
      if (phoneToScore.getValue() < BAD_PHONE) {
        //   logger.warn("\tfor " + fastName + " got bad phone score " + phoneToScore.getKey() + " : " + phoneToScore.getValue());
        countBad++;
      }
    }
    if (countBad > 0) {
      logger.warn("getPercentBadPhones : " + phones);
      for (Map.Entry<String, Float> phoneToScore : phones.entrySet()) {
        if (phoneToScore.getValue() < BAD_PHONE) {
          logger.warn("\tfor " + fastName + " got bad phone score " + phoneToScore.getKey() + " : " + phoneToScore.getValue());
        }
      }
    }
    return (float) countBad / (float) phones.size();
  }

  protected Scores getAlignmentScores(ASRScoring scoring, String refSentence, String name, String parent, String testAudioFileNoSuffix) {
    String doubled = refSentence + " " + refSentence;
    doubled = doubled.toUpperCase();   // TODO : only for english!?
    Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
    logger.debug("\tdoubled : got " + align + " for " + name + " for '" + doubled + "'");
    return align;
  }

  protected Scores getAlignmentScoresNoDouble(ASRScoring scoring, String refSentence, String name, String parent, String testAudioFileNoSuffix) {
    String doubled = refSentence;
    doubled = doubled.toUpperCase();   // TODO : only for english!?
    Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
    logger.debug("\tsingle : got " + align + " for " + name + " for '" + doubled + "'");
    return align;
  }

  protected String getConverted(File answer) {
    String name = answer.getName().replaceAll(".wav", "");
    return getConverted(answer.getParent(), name);
  }

  protected String getConverted(String parent, String name) {
    String testAudioFileNoSuffix = null;
    try {
      testAudioFileNoSuffix = new AudioConversion().convertTo16Khz(parent, name);
    } catch (UnsupportedAudioFileException e) {
      logger.error("got " + e, e);
    }
    return testAudioFileNoSuffix;
  }

  protected double getDuration(File answer, String parent) {
    File answer2 = new File(parent, answer.getName());

    double durationInSeconds = 0;
    if (!answer2.exists()) {
      logger.error("getDuration can't find " + answer2.getAbsolutePath());
    } else {
      durationInSeconds = audioCheck.getDurationInSeconds(answer2);
      //   logger.debug("dur of " + answer2 + " is " + durationInSeconds + " seconds");
    }
    return durationInSeconds;
  }

  /**
   * @param missingSlow
   * @param missingFast
   * @param exid
   * @param bestDirForExercise
   * @throws IOException
   * @see #getBest(java.io.FileWriter, java.io.FileWriter, java.io.File, java.io.File, String, corpus.HTKDictionary, java.util.Map, java.util.List, String, mitll.langtest.shared.Exercise, String)
   */
  private boolean writeBestFiles(FileWriter missingSlow, FileWriter missingFast,
                                 String exid,
                                 File bestDirForExercise,
                                 FastAndSlow fastAndSlow) throws IOException {
    File fast = fastAndSlow.fast;
    boolean gotBoth = true;

    if (fast == null || !fast.exists()) {
      if (fast != null) logger.warn("can't find fast " + fast.getAbsolutePath());
      missingFast.write(exid + "\n");
      missingFast.flush();
      gotBoth = false;
    } else {
      File file = new File(bestDirForExercise, FAST + ".wav");
      logger.debug("fast wrote best to " + file.getAbsolutePath());
      new FileCopier().copy(fast.getAbsolutePath(), file.getAbsolutePath());
    }
    File slow = fastAndSlow.slow;
    if (slow == null || !slow.exists()) {
      if (slow != null) logger.warn("can't find slow " + slow.getAbsolutePath());
      missingSlow.write(exid + "\n");
      missingSlow.flush();
      gotBoth = false;
    } else {
      File file = new File(bestDirForExercise, SLOW + ".wav");
      logger.debug("slow wrote best to " + file.getAbsolutePath());

      new FileCopier().copy(slow.getAbsolutePath(), file.getAbsolutePath());
    }
    return gotBoth;
  }

  protected FastAndSlow writeTheTrimmedFiles(File refDirForExercise, String parent, float floatDur, String testAudioFileNoSuffix,
                                             GetAlignments alignments, boolean remove16K) {
    float pad = 0.25f;

    float start1 = alignments.getStart1();
    float s1 = Math.max(0, start1 - pad);
    float start2 = alignments.getStart2();
    float s2 = Math.max(0, start2 - pad);

    float end1 = alignments.getEnd1();
    float e1 = Math.min(floatDur, end1 + pad);
    float midPoint = end1 + ((start2 - end1) / 2);

/*    if (e1 > s2) {
      e1 = s2;
    }*/

    if (e1 > midPoint) {
      logger.debug("e1 " + e1 + " is after the midpoint " + midPoint);
      //    return new FastAndSlow();
      e1 = Math.max(end1, midPoint);
      //s2 = Math.min(start2,midPoint);
    }

    if (e1 > start2) {
      logger.warn("for " + testAudioFileNoSuffix +
        " end of fast " + e1 + " is after start of slow " + start2);
      return new FastAndSlow();
    }

    if (s2 < midPoint) {
      logger.debug("s2 " + s2 + " is before the midpoint " + midPoint);
      s2 = Math.min(start2, midPoint);
    }

    if (s2 < e1) {
      logger.error("huh? start of fast " + s2 + " is before end of slow " + e1);
      return new FastAndSlow();
    }

    if (s2 < end1) {
      logger.error("huh? start of fast " + s2 + " is before unpadded end of slow " + e1);
      return new FastAndSlow();
    }

    float end2 = alignments.getEnd2();
    // logger.debug("first file " + start1 + "->" + end1 + " second " + start2 + "->" + end2);
    float e2 = Math.min(floatDur, end2 + pad);

    float d1 = e1 - s1;
    float d2 = e2 - s2;
    if (d2 < d1 * DURATION_CHECK) {
      logger.debug("slow segment dur " + d2 + " < fast dur " + (d1 * DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      e2 = floatDur;
      d2 = e2 - s2;
    }
    if (d2 < d1 * DURATION_CHECK) {
      logger.warn("Still after repair slow segment dur " + d2 + " < fast dur " + (d1 * DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      if (THROW_OUT_FAST_LONGER_THAN_SLOW) return new FastAndSlow();
    }

    File longFileFile = new File(parent, testAudioFileNoSuffix + ".wav");
    if (!longFileFile.exists()) logger.error("huh? can't find  " + longFileFile.getAbsolutePath());

    logger.debug("writing ref files to " + refDirForExercise.getAbsolutePath() + " input " + longFileFile.getName() +
      " pad s1 " + s1 + "-" + e1 +
      " dur " + d1 +
      " s2 " + s2 + "-" + e2 +
      " dur " + d2);

    AudioConverter audioConverter = new AudioConverter();
    String binPath = AudioConversion.WINDOWS_SOX_BIN_DIR;
    if (!new File(binPath).exists()) binPath = AudioConversion.LINUX_SOX_BIN_DIR;
    String sox = audioConverter.getSox(binPath);

    File fast, slow;
    if (d1 < MIN_DUR) {
      logger.warn("Skipping audio " + longFileFile.getName() + " since fast audio too short ");
      return new FastAndSlow();
    } else {

      String nameToUse = remove16K ? testAudioFileNoSuffix.replace("_16K", "") : testAudioFileNoSuffix;
      fast = new File(refDirForExercise, nameToUse + "_" + FAST + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        fast.getAbsolutePath(),
        s1,
        d1);

      if (fast.exists() && audioCheck.getDurationInSeconds(fast) < MIN_DUR) {
        logger.error("huh? after writing with sox, the audio file is too short?");
        fast.delete();
        return new FastAndSlow();
      }
    }

    if (d2 < MIN_DUR /*&& d2 > d1*/) {
      if (d2 < MIN_DUR)
        logger.warn("Skipping audio " + longFileFile.getName() + " since slow audio too short");
    /*  else
        logger.warn("Skipping audio " + longFileFile.getName() + " since slow audio shorter than fast.");*/
      return new FastAndSlow();
    } else {
      String nameToUse = remove16K ? testAudioFileNoSuffix.replace("_16K", "") : testAudioFileNoSuffix;

      slow = new File(refDirForExercise, nameToUse + "_" + SLOW + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        slow.getAbsolutePath(),
        s2,
        d2);

      if (slow.exists() && audioCheck.getDurationInSeconds(slow) < MIN_DUR) {
        logger.error("huh? after writing with sox, the audio file is too short?");
        slow.delete();
        return new FastAndSlow();
      }
    }
    return new FastAndSlow(fast, slow);
  }

  protected static class FastAndSlow {
    protected final boolean valid;
    public File fast;
    public File slow;

    public FastAndSlow() {
      this.valid = false;
    }

    public FastAndSlow(File fast, File slow) {
      this.valid = true;
      this.fast = fast;
      this.slow = slow;
    }

    public String toString() {
      return (valid ? " valid " : " invalid ") +
        (fast != null ? (" fast " + fast.getName()) : "") +
        (slow != null ? (" slow " + slow.getName()) : "")
        ;
    }
  }

  protected class GetAlignments {
    private String first, last;
    private int refLength;
    private int lowWordScores = 0;
    private String name;
    private String wordLabFile;
    private float start1;
    private float end1;
    private float start2;
    private float end2;
    private float fastScore;
    private float slowScore;

    private String wordSeq = "", scores = "";

    /**
     * @see SplitAudio#getBestFilesFromResults(mitll.langtest.server.scoring.ASRScoring, java.util.List, mitll.langtest.shared.Exercise, String, String, String, int, java.io.File, String)
     * @param first
     * @param last
     * @param refLength
     * @param name
     * @param wordLabFile
     */
    public GetAlignments(String first, String last, int refLength, String name, String wordLabFile) {
      this.first = first;
      this.last = last;
      this.refLength = refLength;
      this.name = name;
      this.wordLabFile = wordLabFile;
    }

    public float getStart1() {
      return start1;
    }

    public float getEnd1() {
      return end1;
    }

    public float getStart2() {
      return start2;
    }

    public float getEnd2() {
      return end2;
    }

    public boolean isValid() {
      return lowWordScores < LOW_WORD_SCORE_THRESHOLD;
    }

    public boolean isValid2() {
      return start1 != -1 && start2 != -1 && end1 != -1 && end2 != -1;
    }

    public String getWordSeq() {
      return wordSeq;
    }

    public String getScores() {
      return scores;
    }

    public GetAlignments invoke() throws IOException {
      SortedMap<Float, TranscriptEvent> timeToEvent = new TranscriptReader().readEventsFromFile(wordLabFile);

      if (timeToEvent.size() != 2 * refLength) {     // warn if we don't get the expected number of tokens/events
        String sentence = getSentenceFromLabelFile(timeToEvent);

        logger.warn("Alignment returned " + timeToEvent.size() + " tokens (sentence='" + sentence +
          "') but expected " + 2 * refLength);
      }
      start1 = -1;
      end1 = -1;
      start2 = -1;
      end2 = -1;
      boolean didFirst = false;

      int tokenCount = 0;
      float scoreTotal1 = 0, scoreTotal2 = 0;
      wordSeq = "";

      for (Map.Entry<Float, TranscriptEvent> timeEventPair : timeToEvent.entrySet()) {
        TranscriptEvent transcriptEvent = timeEventPair.getValue();
        scores += transcriptEvent.toString() + ", ";
        String word = transcriptEvent.event.trim();
        wordSeq += word + " ";
        boolean start = word.equals("<s>");
        //if (start) starts++;
        boolean end = word.equals("</s>");
        //if (end) ends++;

        if (start || end) continue;

        if (debug) logger.debug("\ttoken " + tokenCount + " got " + word + " and " + transcriptEvent);

        float score = transcriptEvent.score;
        if (!didFirst) scoreTotal1 += score;
        else scoreTotal2 += score;
        if (score < 0.2f) lowWordScores++;

        tokenCount++;
        if (tokenCount == 1 && first.equalsIgnoreCase(word)) {
          if (!didFirst) {
            start1 = transcriptEvent.start;
          } else {
            start2 = transcriptEvent.start;
          }
          if (debug) logger.debug("\t1 token " + tokenCount + " vs " + (refLength - 1));

        }
        if (tokenCount == refLength && last.equalsIgnoreCase(word)) {
          if (!didFirst) {
            end1 = transcriptEvent.end;
            if (debug) logger.debug("\tgot end of fast, token " + tokenCount);

            didFirst = true;
            tokenCount = 0;
          } else {
            end2 = transcriptEvent.end;
          }
        }

        if (tokenCount > refLength)
          logger.error("\n ------ huh? didn't catch ending token? expecting last '" + last + "' but saw '" + word + "'");
        if (debug) logger.debug("\t3 token " + tokenCount + " vs " + (refLength));
      }

      float dur1 = end1 - start1;
      float dur2 = end2 - start2;
      fastScore = scoreTotal1 / (float) refLength;
      slowScore = scoreTotal2 / (float) refLength;

      if (debug || !isValid2()) {
        logger.debug("\tfirst  " + start1 + "-" + end1 + " dur " + dur1 +
          " score " + fastScore +
          " second " + start2 + "-" + end2 + " dur " + dur2 +
          " score " + slowScore +
          " for " + name + " and alignment sentence was " + getSentenceFromLabelFile(timeToEvent));
      }
      return this;
    }

    private String getSentenceFromLabelFile(SortedMap<Float, TranscriptEvent> timeToEvent) {
      StringBuilder builder = new StringBuilder();
      for (TranscriptEvent event : timeToEvent.values()) {
        builder.append(event.event.trim()).append(" ");
      }
      return builder.toString();
    }
  }

  protected String prependDeploy(String deployPath, String pathname) {
    if (!new File(pathname).exists()) {
      pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }

  public static void main(String[] arg) {
    try {
      int numThreads = Integer.parseInt(arg[0]);
      String audioDir = arg[1];
      String language = arg[2];
      String spreadsheet = arg[3];
      String dbName = arg[4];
      boolean throwAwayNonNativeAudio = true;
      if (arg.length == 6) {
        throwAwayNonNativeAudio = arg[5].equalsIgnoreCase("true");
      }
      Set<String> onlyTheseIDs = new HashSet<String>();
      if (arg.length > 6) {
        onlyTheseIDs.add(arg[7]);
      }
      new SplitAudio().convertExamples(numThreads, audioDir, language, spreadsheet, dbName, throwAwayNonNativeAudio,
        onlyTheseIDs);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
