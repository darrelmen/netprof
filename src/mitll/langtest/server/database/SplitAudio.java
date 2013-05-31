package mitll.langtest.server.database;

import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.AudioCheck;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
  private static Logger logger = Logger.getLogger(SplitAudio.class);

  private boolean debug;
  private static final int MAX = 12000;
  private static final double MIN_DUR = 0.2;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private AudioCheck audioCheck = new AudioCheck();

  public void dumpDir (String audioDir) {
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);
    Set<String> files = new HashSet<String>(Arrays.asList(list));

    String language = "english";
    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      language,
      configDir+
        "ESL_ELC_5071-30books_chapters.xlsx");

    DatabaseImpl collected = new DatabaseImpl(
      configDir,
      language,
      configDir +
        "5100-english-no-gloss.txt");

    Set<String> valid = new HashSet<String>();
    for (Exercise e: collected.getExercises()) {
      String englishSentence = e.getEnglishSentence();

      if (englishSentence == null) {
        //if (c++ < 10) logger.warn("convertEnglish huh? no english sentence for " + e.getID() + " instead " +e.getRefSentence());
        englishSentence = e.getRefSentence();
      }

      if (englishSentence == null) logger.warn("huh? no english sentence for " + e.getID());
      else {
        String key = englishSentence.toLowerCase().trim();
        valid.add(key);
      }
    }
    logger.warn("valid has " + valid.size());

    try {
      final FileWriter skip = new FileWriter(configDir + "skip3.txt");
      final FileWriter skipWords = new FileWriter(configDir + "skipWords3.txt");
      int skipped = 0;
      List<Exercise> exercises = unitAndChapter.getExercises();
      for (Exercise e : exercises) {
        String key = e.getEnglishSentence().toLowerCase().trim();

        if (!files.contains(e.getID())) {
          if (skipped++ < 10) logger.warn("skipping " + e.getID() + " : " + key);// + " no match in " +englishToEx.size() + " entries.");
          skip.write(e.getID()+"\n");
          skipWords.write(key +"\n");
        }
      }
      skip.close();
      skipWords.close();

      logger.warn("skipped " + skipped + " of " + exercises.size() + " files " + files.size() +  " e.g. " + files.iterator().next());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
  /**
   * read the english spreadsheet
   * find items in the word list that match
   * for the exercises with matches, find audio recorded by native speakers (exp = 240 months)
   * MAYBE try to find partial matches...?
   */
  private void convertEnglish(int numThreads, String audioDir) {
    String language = "english";
    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      language,
      configDir+
        "ESL_ELC_5071-30books_chapters.xlsx");

    DatabaseImpl db2 = new DatabaseImpl(
      configDir,
      language,
      configDir +
        "5100-english-no-gloss.txt");

    Map<String, List<Result>> idToResults = getIDToResultsMap(db2);
    logger.warn("id->results " + idToResults.size() + " e.g. " + idToResults.keySet().iterator().next());
    Map<Long, User> userMap = new UserDAO(db2).getUserMap();

    Map<String, Exercise> englishToEx = new HashMap<String, Exercise>();
    Map<String, List<Result>> englishToResults2 = new HashMap<String, List<Result>>();
    int count = 0;
    int c = 0;
    for (Exercise e : db2.getExercises()) {
      String englishSentence = e.getEnglishSentence();

      if (englishSentence == null) {
        //if (c++ < 10) logger.warn("convertEnglish huh? no english sentence for " + e.getID() + " instead " +e.getRefSentence());
        englishSentence = e.getRefSentence();
      }

      if (englishSentence == null) logger.warn("huh? no english sentence for " + e.getID());
      else {
        String key = englishSentence.toLowerCase().trim();

        // make sure we have result for exercise
        // make sure the result is by native speaker
        List<Result> resultList = idToResults.get(e.getID()+"/0");

        boolean nativeResult = false;
        if (resultList != null) {
          for (Result r : resultList) {
            User user = userMap.get(r.userid);
            if (user == null) logger.warn("huh? no user " + r.userid + " for " + r);
            else {
              if (user.experience > 239) {
                nativeResult = true;
                List<Result> resultList1 = englishToResults2.get(key);
                if (resultList1 == null) englishToResults2.put(key, resultList1 = new ArrayList<Result>());
                resultList1.add(r);
              }
            }
          }
          if (!nativeResult) {
            if (count++ < 10) logger.warn("no native recordings for " +e.getID());
          }
        }
        else {
          /*if (count++ < 100)*/ logger.warn("no results for ex " + e.getID());
        }
        if (nativeResult) {
          if (englishToEx.containsKey(key)) {
            //logger.warn("skipping duplicate entry : " + e.getID() + " " + englishSentence);
          } else {
            englishToEx.put(key, e);
            if (key.equalsIgnoreCase("complete")) {logger.warn("map " +key + " -> " + e); }
          }
        }
      }
    }

    logger.warn("no native recordings for " + count + " items.");

    Map<String,Exercise> idToEx = new TreeMap<String, Exercise>();
    Map<String, List<Result>> idToResults2 = new TreeMap<String, List<Result>>();

    int skipped = 0;
    int count2 = 0;
    Map<Exercise, List<Result>> chapterToResult = null;
    try {
      final FileWriter skip = new FileWriter(configDir + "skip.txt");
      final FileWriter skipWords = new FileWriter(configDir + "skipWords.txt");

      chapterToResult = new HashMap<Exercise, List<Result>>();
      for (Exercise e : unitAndChapter.getExercises()) {
        String key = e.getEnglishSentence().toLowerCase().trim();
        if (englishToEx.containsKey(key)) {
          //logger.warn("skipping duplicate entry : " + e.getID() + " "+ e.getEnglishSentence());
          List<Result> value = englishToResults2.get(key);
          chapterToResult.put(e,value);
          idToEx.put(e.getID(),e);
          if (value.isEmpty()) logger.warn("huh? no results for ex " +key); //never happen
          idToResults2.put(e.getID(), value);

          if (key.equals("complete")) {
            logger.warn("key " + key + " value " + value + " ex " + e.getID() + "");
          }
          //if (e.getID().equals("0")) {
          if (count2++ < 10)
            logger.warn("ex " +e.getID()+
              " key " + key + " value " + value + " ex " + e.getID() + " " + idToResults2.get(key));

          //}
        } else {
          if (skipped++ < 10) logger.warn("skipping " + e.getID() + " : " + key);// + " no match in " +englishToEx.size() + " entries.");
          skip.write(e.getID()+"\n");
          skipWords.write(key +"\n");
        }
        //  else englishToEx2.put(key,e);
      }
      skip.close();
      skipWords.close();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    logger.warn("overlap is " + idToEx.size() + "/" + chapterToResult.size()+   "/"+idToResults2.size()+
      " out of " + unitAndChapter.getExercises().size() + " skipped " + skipped);

    try {
      if (false) convertExamples(numThreads, audioDir,language,idToEx,idToResults2);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (ExecutionException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

/*
  private void convertExamples(int numThreads, String audioDir, String language,String spreadsheet) throws Exception{
    convertExamples(numThreads, audioDir, language, spreadsheet,"template");
  }
*/

    /**
      * Go through all exercise, find all results for each, take best scoring audio file from results
     *
     * e.g. for dari:
     *
     * 10 dariAudio dari 9000-dari-course-examples.xlsx
     *
     * for msa:
     *
     * 20 msaAudio msa 3700-msa-course-examples.xlsx
    * @param numThreads
    * @param audioDir
    */
  // TODO later take data and use database so we keep metadata about audio
  private void convertExamples(int numThreads, String audioDir, String language,String spreadsheet,String dbName) throws Exception{
    final String configDir = getConfigDir(language);

    DatabaseImpl db = new DatabaseImpl(
      configDir,
      dbName,
      configDir+
        spreadsheet);

    final Map<String, Exercise> idToEx = getIdToExercise(db);
    Map<String, List<Result>> idToResults = getIDToResultsMap(db);

    if (true) {
      for (String exid : idToEx.keySet()) {
        String resultID = exid + "/0";
        if (!idToResults.containsKey(resultID)) {
          idToResults.put(resultID, new ArrayList<Result>());
        }
      }
      //if (exercises.size() != idToResults.size()) logger.error("\n\n\nhuh? id->results map size " + idToResults.size());
    }

    convertExamples(numThreads, audioDir, language,/* configDir,*/ idToEx, idToResults);
  }

  private void convertExamples(int numThreads, String audioDir, String language,// String configDir,
                               Map<String, Exercise> idToEx,
                               Map<String, List<Result>> idToResults) throws IOException, InterruptedException, ExecutionException {
    final String configDir = getConfigDir(language);


    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File newRefDir = new File(placeToPutAudio + "refAudio");
    newRefDir.mkdir();
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    bestDir.mkdir();

    final Map<String, String> properties = getProperties(language, configDir);
    ASRScoring scoring = getAsrScoring(".",null,properties);

//    checkLTS(exercises, scoring.getLTS());

    final HTKDictionary dict = scoring.getDict();

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    final FileWriter missingSlow = new FileWriter(configDir + "missingSlow.txt");
    final FileWriter missingFast = new FileWriter(configDir + "missingFast.txt");
    List<Future<?>> futures = getSplitAudioFutures(idToEx, missingSlow, missingFast, idToResults, placeToPutAudio,
      newRefDir, bestDir,
      properties, dict, executorService);

    blockUntilComplete(futures);

    missingFast.close();
    missingSlow.close();
    logger.info("closing missing slow");
    executorService.shutdown();
  }

  private String getConfigDir(String language) {
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

  private Map<String, Exercise> getIdToExercise(DatabaseImpl db) {
    final Map<String,Exercise> idToEx = new HashMap<String, Exercise>();

    List<Exercise> exercises = db.getExercises();
    logger.debug("Got " + exercises.size() + " exercises");
    for (Exercise e: exercises) idToEx.put(e.getID(),e);
    return idToEx;
  }

  private void blockUntilComplete(List<Future<?>> futures) throws InterruptedException, ExecutionException {
    logger.info("got " +futures.size() + " futures");
    for (Future<?> future : futures) {
      Object o = future.get();
    }
    logger.info("all " +futures.size() + " futures complete");
  }

  private List<Future<?>> getSplitAudioFutures(final Map<String, Exercise> idToEx,
                                               final FileWriter missingSlow, final FileWriter missingFast,
                                               Map<String, List<Result>> idToResults,
                                               final String placeToPutAudio, final File newRefDir, final File bestDir,
                                               final Map<String, String> properties,
                                               final HTKDictionary dict, ExecutorService executorService) {
    List<Future<?>> futures = new ArrayList<Future<?>>();
    for (final Map.Entry<String, List<Result>> pair : idToResults.entrySet()) {
      logger.debug("pair " + pair);
      Future<?> submit = executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            getBestForEachExercise(pair.getKey(), idToEx, missingSlow, missingFast,
              newRefDir, bestDir, pair,placeToPutAudio,dict, properties);
          } catch (IOException e) {
            logger.error("Doing " + pair.getKey() + " and " + pair.getValue() +
              " Got " + e, e);
          }
        }
      });
      futures.add(submit);
    //  break;
    }
    return futures;
  }

  private Map<String, String> getProperties(String language, String configDir) throws IOException {
    Properties props = new Properties();
    String propFile = configDir + File.separator + language + ".properties";
    logger.debug("reading from props " +propFile);
    FileInputStream inStream = new FileInputStream(propFile);
    props.load(inStream);
    Map<String, String> properties = getProperties(props);
    inStream.close();
    return properties;
  }

  private Map<String, List<Result>> getIDToResultsMap(DatabaseImpl db) {
    Map<String,List<Result>> idToResults = new HashMap<String, List<Result>>();
    List<Result> results = db.getResults();
    logger.debug("Got " + results.size() + " results");
    for (Result r : results) {
      String id = r.id;
      int i = Integer.parseInt(id);
      if (i < MAX //&& i == 3496
        ) {
        List<Result> resultList = idToResults.get(r.getID());
        if (resultList == null) {
          idToResults.put(r.getID(), resultList = new ArrayList<Result>());
        }
        resultList.add(r);
      }
    }
    return idToResults;
  }

  private ASRScoring getAsrScoring(String installPath, HTKDictionary dictionary,Map<String, String> properties) {
    String deployPath = installPath + File.separator + "war";
    return dictionary == null ? new ASRScoring(deployPath, properties) : new ASRScoring(deployPath, properties, dictionary);
  }

  private Map<String, String> getProperties(Properties props) {
    Map<String,String> kv = new HashMap<String, String>();
    for (Object prop : props.keySet()) {
      String sp = (String)prop;
      kv.put(sp,props.getProperty(sp).trim());
    }
    return kv;
  }

  private void getBestForEachExercise(String exid2, Map<String, Exercise> idToEx, FileWriter missingSlow, FileWriter missingFast,
                                      File newRefDir, File bestDir, Map.Entry<String, List<Result>> pair,
                                      String collectedAudioDir, HTKDictionary dictionary, Map<String, String> properties) throws IOException {
    List<Result> resultsForExercise = pair.getValue();
    if (resultsForExercise.isEmpty()) return;
    Exercise exercise = idToEx.get(exid2);
    if (exercise == null) {
      logger.info("skipping ex id " + exid2 + " since not in  " + idToEx.keySet());
      return;
    }
    getBest(missingSlow, missingFast, newRefDir, bestDir, pair, collectedAudioDir, dictionary, properties, resultsForExercise, exid2, exercise);
  }

  private void getBest(FileWriter missingSlow, FileWriter missingFast, File newRefDir, File bestDir,
                       Map.Entry<String, List<Result>> pair, String collectedAudioDir,
                       HTKDictionary dictionary, Map<String, String> properties,
                       List<Result> resultsForExercise, String exid, Exercise exercise) throws IOException {
    //   String refSentence = exercise.getRefSentence();   // TODO switch on language
    String refSentence = exercise.getEnglishSentence();
    refSentence = refSentence.replaceAll("\\p{P}", "");
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefSentence(split).trim();
    int refLength = split.length;
    String firstToken = split[0].trim();
    String lastToken = split[refLength-1].trim();
    // logger.debug("refSentence " + refSentence + " length " + refLength + " first |" + firstToken + "| last |" +lastToken +"|");

    File refDirForExercise = new File(newRefDir, exid);
    String key = pair.getKey();
    if (!key.equals(exid)) logger.error("huh?> not the same " + key + "  and " + exid);
    //logger.debug("making dir " + key + " at " + refDirForExercise.getAbsolutePath());
    refDirForExercise.mkdir();

    File bestDirForExercise = new File(bestDir, exid);
    logger.debug("for '" +refSentence +
      "' making dir " + key + " at " + bestDirForExercise.getAbsolutePath());
    bestDirForExercise.mkdir();
    final ASRScoring scoring = getAsrScoring(".",dictionary,properties);
    String best = getBestFilesFromResults(scoring, resultsForExercise, exercise, refSentence,
      firstToken, lastToken,
      refLength, refDirForExercise,collectedAudioDir);

    if (best != null) {
      logger.debug("for " +key + " : '" + exercise.getEnglishSentence() + "' best is " + best);// + " total " + bestTotal);
      writeBestFiles(missingSlow, missingFast, exid, refDirForExercise, bestDirForExercise, best);
    }
    else {
      logger.warn("\n\n------------- no valid audio for " + exid);

      synchronized (missingFast) {
        missingFast.write(exid + "\n");
        missingFast.flush();
      }
      synchronized (missingSlow) {
        missingSlow.write(exid + "\n");
        missingSlow.flush();
      }
    }
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
   */
  private String getBestFilesFromResults(ASRScoring scoring, List<Result> resultsForExercise,
                                         Exercise exercise, String refSentence,
                                         String first, String last, int refLength,
                                         File refDirForExercise,String collectedAudioDir ) {
    String best = null;
    String id = exercise.getID();
    float bestSlow = 0, bestFast = 0, bestTotal = 0;
    for (Result r : resultsForExercise) {
      //  if (exercise != null) {
      File answer = new File(r.answer);
      String name = answer.getName().replaceAll(".wav", "");
      String parent = collectedAudioDir + File.separator + answer.getParent();

      double durationInSeconds = getDuration(answer, parent);
      if (durationInSeconds < MIN_DUR) {
        logger.warn("skipping " + name + " since it's less than a 1/2 second long.");
        continue;
      }
      String testAudioFileNoSuffix = getConverted(name, parent);

      //logger.debug("parent " + parent + " running result " + r.uniqueID + " for exercise " + id + " and audio file " + name);

      String doubled = refSentence + " " + refSentence;
      doubled = doubled.toUpperCase();
      Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
      logger.debug("\tgot " + align + " for " + name + " for " +doubled);
      float hydecScore = align.hydecScore;

      String wordLabFile = prependDeploy(parent,testAudioFileNoSuffix + ".words.lab");
      try {
        GetAlignments alignments = new GetAlignments(first,last, refLength, name, wordLabFile).invoke();
         boolean valid = alignments.isValid();
        if (!valid) {
          logger.warn("\n---> ex " + id + " " + exercise.getEnglishSentence() +
            " score " + hydecScore +
            " invalid alignment : " + alignments.getWordSeq() + " : " + alignments.getScores());
        }
        if (bestTotal < hydecScore && valid && hydecScore > 0.1f) {
          bestTotal = hydecScore;
          best = testAudioFileNoSuffix;

          logger.debug("ex " + id+ " " + exercise.getEnglishSentence() +" best so far is " + best + " score " + bestTotal + " hydecScore " + hydecScore);
          writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            alignments);

        }
       /* if (valid) {//alignments.isValid()) {
          writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            alignments);
        }*/

/*            logger.debug("writing ref files to " + refDirForExercise.getName() + " input " + longFileFile.getName() +
            " Start " + start1 + " " + dur1 + " start2 " + start2 + " dur " + dur2);*/
      } catch (IOException e) {
        e.printStackTrace();
      }
      // }
      // else logger.warn("couldn't find " + r.getID() + " exercise");
    }
    return best;
  }

  private String getConverted(String name, String parent) {
    String testAudioFileNoSuffix = null;
    try {
      testAudioFileNoSuffix = new AudioConversion().convertTo16Khz(parent, name);
    } catch (UnsupportedAudioFileException e) {
      logger.error("got " +e,e);
    }
    return testAudioFileNoSuffix;
  }

  private double getDuration(File answer, String parent) {
    File answer2 = new File(parent,answer.getName());

    double durationInSeconds = 0;
    if (!answer2.exists()) {
      logger.error("can't find " + answer2.getAbsolutePath());
    }
    else {
      durationInSeconds = audioCheck.getDurationInSeconds(answer2);
   //   logger.debug("dur of " + answer2 + " is " + durationInSeconds + " seconds");
    }
    return durationInSeconds;
  }

  private void writeBestFiles(FileWriter missingSlow, FileWriter missingFast,
                              String exid, File refDirForExercise, File bestDirForExercise, String best) throws IOException {
    File fast = new File(refDirForExercise, best + "_" + FAST + ".wav");
    if (!fast.exists()) {
      missingFast.write(exid + "\n");
    } else {
      File file = new File(bestDirForExercise, FAST + ".wav");
      logger.debug("fast wrote best to " + file.getAbsolutePath());
      new FileCopier().copy(fast.getAbsolutePath(), file.getAbsolutePath());
    }
    File slow = new File(refDirForExercise, best + "_" + SLOW + ".wav");
    if (!slow.exists()) {
      missingSlow.write(exid + "\n");
    } else {
      File file = new File(bestDirForExercise, SLOW + ".wav");
      logger.debug("slow wrote best to " + file.getAbsolutePath());

      new FileCopier().copy(slow.getAbsolutePath(), file.getAbsolutePath());
    }
  }

  private void writeTheTrimmedFiles(File refDirForExercise, String parent, float floatDur, String testAudioFileNoSuffix,
                                    GetAlignments alignments) {
    float start1 = alignments.getStart1();
    float start2 = alignments.getStart2();
    float end1 = alignments.getEnd1();
    float end2 = alignments.getEnd2();
    float pad = 0.25f;
    float s1 = Math.max(0,start1-pad);
    float s2 = Math.max(0,start2-pad);

    float e1 = Math.min(floatDur,end1+pad);
    float midPoint = end1 + ((start2 - end1) / 2);
    if (e1 > midPoint) {
      //  logger.warn("adjust e1 from " + e1 + " to " +midPoint);
      e1 = Math.max(end1,midPoint);
      s2 = Math.min(start2,midPoint);
    }

    float e2 = Math.min(floatDur, end2 + pad);

    float d1 = e1 - s1;
    float d2 = e2 - s2;

    File longFileFile = new File(parent,testAudioFileNoSuffix+".wav");
    if (!longFileFile.exists())logger.error("huh? can't find  " + longFileFile.getAbsolutePath());

    logger.debug("writing ref files to " + refDirForExercise.getName() + " input " + longFileFile.getName() +
      " pad s1 " + s1 + "-" + e1+
      " dur " + d1 +
      " s2 " + s2 + "-" + e2+
      " dur " + d2);

    AudioConverter audioConverter = new AudioConverter();
    String binPath = AudioConversion.WINDOWS_SOX_BIN_DIR;
    if (! new File(binPath).exists()) binPath = AudioConversion.LINUX_SOX_BIN_DIR;
    String sox = audioConverter.getSox(binPath);
    if (d1 < MIN_DUR) {
      logger.warn("Skipping audio " + longFileFile.getName() + " since fast audio too short ");
    } else {

      File fast = new File(refDirForExercise, testAudioFileNoSuffix + "_" + FAST + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        fast.getAbsolutePath(),
        s1,
        d1);

      if (fast.exists() && audioCheck.getDurationInSeconds(fast)< MIN_DUR) {
        fast.delete();
      }
    }

    if (d2 < MIN_DUR) {
      logger.warn("Skipping audio " + longFileFile.getName() + " since slow audio too short ");

    } else {
      File slow = new File(refDirForExercise, testAudioFileNoSuffix + "_" + SLOW + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        slow.getAbsolutePath(),
        s2,
        d2);

      if (slow.exists() && audioCheck.getDurationInSeconds(slow)< MIN_DUR) {
        slow.delete();
      }
    }
  }

  private class GetAlignments {
    private String first,last;
    private int refLength;
    int lowWordScores =0;
    private String name;
    private String wordLabFile;
    private float start1;
    private float end1;
    private float start2;
    private float end2;
    private float fastScore;
    private float slowScore;

    private int starts,ends;
    private String wordSeq = "", scores = "";

    public GetAlignments(String first, String last,int refLength, String name, String wordLabFile) {
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
      return lowWordScores < 2;
    }

    public String getWordSeq() { return wordSeq; }
    public String getScores() { return scores; }

    public GetAlignments invoke() throws IOException {
      SortedMap<Float,TranscriptEvent> timeToEvent = new TranscriptReader().readEventsFromFile(wordLabFile);

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
        if (start) starts++;
        boolean end = word.equals("</s>");
        if (end) ends++;

        if (start || end) continue;

        if (debug) logger.debug("\ttoken " + tokenCount + " got " + word + " and " + transcriptEvent);

        float score = transcriptEvent.score;
        if (!didFirst) scoreTotal1 += score; else scoreTotal2 += score;
        if (score < 0.2f) lowWordScores++;

        tokenCount++;
        if (tokenCount == 1 && first.equalsIgnoreCase(word)) {
          if (!didFirst) {
            start1 = transcriptEvent.start;
          } else {
            start2 = transcriptEvent.start;
          }
          if (debug) logger.debug("\t1 token " + tokenCount + " vs " + (refLength-1));

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

        if (tokenCount > refLength) logger.error("\n\n\n------ huh? didn't catch ending token???");
        if (debug) logger.debug("\t3 token " + tokenCount + " vs " + (refLength));
      }

      float dur1 = end1 - start1;
      float dur2 = end2 - start2;
      fastScore = scoreTotal1 / (float) refLength;
      slowScore = scoreTotal2 / (float) refLength;
      if (debug) logger.debug("\tfirst  " + start1 + "-" +end1 + " dur " + dur1 +
        " score " + fastScore +
        " second " + start2 + "-" +end2 + " dur " + dur2 +
        " score " + slowScore +
        " for " + name);
      return this;
    }
  }


  private String prependDeploy(String deployPath, String pathname) {
    if (!new File(pathname).exists()) {
      pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }


  private void checkLTS(List<Exercise> exercises, LTS lts) {
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ltsIssues.txt"), FileExerciseDAO.ENCODING));

      SmallVocabDecoder svd = new SmallVocabDecoder();
      int errors = 0;
      for (Exercise e : exercises) {
        String id = e.getID();

        if (checkLTS(Integer.parseInt(id), writer, svd, lts, e.getEnglishSentence(), e.getRefSentence())) errors++;
      }

      if (errors > 0) logger.error("found " + errors + " lts errors");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean checkLTS(int id, BufferedWriter writer, SmallVocabDecoder svd, LTS lts, String english, String foreignLanguagePhrase) {
    List<String> tokens = svd.getTokens(foreignLanguagePhrase);
    boolean error = false;
    try {

      for (String token : tokens) {
        String[][] process = lts.process(token);
        if (process == null) {
          String message = "couldn't do lts on exercise #" + (id - 1) + " token '" + token +
            "' length " + token.length() + " trim '" + token.trim() +
            "' " +
            " '" + foreignLanguagePhrase + "' english = '" + english + "'";
          logger.error(message);
          //logger.error("\t tokens " + tokens + " num =  " + tokens.size());

          writer.write(message);
          writer.write("\n");
          error = true;
        }
      }
    } catch (Exception e) {
      logger.error("couldn't do lts on " + (id - 1) + " " + foreignLanguagePhrase + " " + english);
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return error;
  }

  public static void main(String [] arg) {
    try {
      int numThreads = Integer.parseInt(arg[0]);
      String audioDir = arg[1];
      // new SplitAudio().convertExamples(numThreads, audioDir, arg[2], arg[3]);
   //   new SplitAudio().convertEnglish(numThreads,audioDir);
      new SplitAudio().dumpDir(audioDir);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
