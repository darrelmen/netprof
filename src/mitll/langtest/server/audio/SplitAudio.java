package mitll.langtest.server.audio;

import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.AudioCheck;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.FileExerciseDAO;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
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
import java.util.Collections;
import java.util.Comparator;
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
  public static final double DURATION_CHECK = 1.2;
  private static Logger logger = Logger.getLogger(SplitAudio.class);

  private boolean debug;
  private static final int MAX = 22000;
  private static final double MIN_DUR = 0.2;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private AudioCheck audioCheck = new AudioCheck();

  public void normalize(String audioDir) {

    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    File[] bestAudioDirs = bestDir.listFiles();
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);

    List<File> temp = Arrays.asList(bestAudioDirs);
    temp = temp.subList(0,3); // for now

    String language = "english";
    final String configDir = getConfigDir(language);
    try {
      String fileName = configDir + "analist";
      File analistFile = new File(fileName);
      final FileWriter analist = new FileWriter(analistFile);
      int i = 0;
      Set<String> valid = new HashSet<String>();
      for (File file1 : temp) {
      //  File file1 = new File(file);
        if (!file1.exists()) {
             logger.error("huh? " +file1.getAbsolutePath() + " doesn't exist");
        }
        if (!file1.isDirectory()) {
          logger.error("huh? " +file1.getAbsolutePath() + " is not a directory...");

        }
        File[] files = file1.listFiles();
        String dirname = file1.getName();
     //   int i1 = Integer.parseInt(dirname);
        valid.add(dirname);
        if (files != null) {
          for (File wav : files) {
            System.out.println("path " + wav.getPath());
            boolean isFast = (wav.getName().startsWith("Fast"));
            String id = dirname + (isFast ? "F" : "S");
            analist.write(id + " " + wav.getPath().replace(".wav",".raw") + "\n");

            File answer = wav;
            String name = answer.getName().replaceAll(".wav", "");
            String parent = answer.getParent();

            double durationInSeconds = getDuration(answer, parent);
            if (durationInSeconds < MIN_DUR) {
              if (durationInSeconds > 0) logger.warn("skipping " + name + " since it's less than a 1/2 second long.");
              continue;
            }
            String testAudioFileNoSuffix = getConverted(name, parent);
            logger.info("converted " + testAudioFileNoSuffix);
          }
        }
      }
      analist.close();
      logger.debug("wrote to " +analistFile.getAbsolutePath());

      DatabaseImpl unitAndChapter = new DatabaseImpl(
        configDir,
        language,
        configDir+
          "ESL_ELC_5071-30books_chapters.xlsx");

      String fileName2 = configDir + "transcript";
      File transcriptFile = new File(fileName2);
      final FileWriter transcript = new FileWriter(transcriptFile);
      i = 0;
      for (Exercise e:unitAndChapter.getExercises()) {
        if (valid.contains(e.getID())) {
        //  String id = dirname + (isFast ? "F" : "S");

          transcript.write(e.getEnglishSentence() +" (" +e.getID()+ "F"+
            ")"+
            "\n");
          transcript.write(e.getEnglishSentence() +" (" +e.getID()+ "S"+
            ")"+
            "\n");
        }
      }
      transcript.close();
      logger.debug("wrote to " +transcriptFile.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    //  Set<String> files = new HashSet<String>(Arrays.asList(list));

    try {
      final Map<String, String> properties = getProperties(language, configDir);
      ASRScoring scoring = getAsrScoring(".", null, properties);

//    checkLTS(exercises, scoring.getLTS());

      final HTKDictionary dict = scoring.getDict();

      ASRScoring asrScoring = getAsrScoring(".", dict, properties);

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void dumpDir2 (String audioDir, String language, String dbName, String spreadsheet) {
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);
    Set<String> files = new HashSet<String>(Arrays.asList(list));

    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      dbName,
      configDir +
        spreadsheet);

    try {
      final FileWriter skip = new FileWriter(configDir + "skip3.txt");
      final FileWriter skipWords = new FileWriter(configDir + "skipWords3.txt");
      int skipped = 0;
      List<Exercise> exercises = unitAndChapter.getExercises();
      for (Exercise e : exercises) {
        String key = e.getEnglishSentence().toLowerCase().trim();

        if (!files.contains(e.getID())) {
          if (skipped++ < 30) logger.warn("skipping " + e.getID() + " : " + key);// + " no match in " +englishToEx.size() + " entries.");
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


  public void dumpDir (String audioDir, String language) {
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);
    Set<String> files = new HashSet<String>(Arrays.asList(list));

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

    DatabaseImpl flatList = new DatabaseImpl(
      configDir,
      language,
      configDir +
        "5100-english-no-gloss.txt");

    Map<String, List<Result>> idToResults = getIDToResultsMap(flatList);
    logger.debug("convertEnglish : id->results size " + idToResults.size() + " e.g. " + idToResults.keySet().iterator().next());
    Set<Long> nativeUsers = new UserDAO(flatList).getNativeUsers();
    if (nativeUsers.isEmpty()) {
      logger.error("huh? no native users???");
    }
    else {
      logger.debug("found " + nativeUsers.size() + " native users");
    }
    Set<String> flatEnglish = new HashSet<String>();
    Map<String, List<Result>> flastListEnglishToResults = new HashMap<String, List<Result>>();
    int count = 0;
   // int c = 0;
    int nonNativeRecordings = 0;
    for (Exercise e : flatList.getExercises()) {
      String englishSentence = e.getEnglishSentence();

      if (englishSentence == null) {
        //if (c++ < 10) logger.warn("convertEnglish huh? no english sentence for " + e.getID() + " instead " +e.getRefSentence());
        englishSentence = e.getRefSentence();
      }

      if (englishSentence.contains(";")) {
        //logger.warn("skipping semi colon : " + englishSentence);
      }
      else if (englishSentence == null) logger.warn("huh? no english sentence for " + e.getID());
      else {
        String key = englishSentence.toLowerCase().trim();

        // make sure we have result for exercise
        // make sure the result is by native speaker
        List<Result> resultList = idToResults.get(e.getID());//+"/0");

        boolean nativeResult = false;
        if (resultList != null) {
          for (Result r : resultList) {
            if (nativeUsers.contains(r.userid)) {
              nativeResult = true;
              List<Result> resultList1 = flastListEnglishToResults.get(key);
              if (resultList1 == null) flastListEnglishToResults.put(key, resultList1 = new ArrayList<Result>());
              resultList1.add(r);
            }
            else {
              nonNativeRecordings++;
            }
          }
          if (!nativeResult) {
            if (count++ < 10) logger.warn("no native recordings for " +e.getID());
          }
        }
        else {
          if (count++ < 10) logger.warn("no results for ex " + e.getID());
        }
        if (nativeResult) {
          if (flatEnglish.contains(key)) {
            logger.warn("skipping duplicate entry : " + e.getID() + " " + englishSentence);
          } else {
            flatEnglish.add(key);
          //  if (key.equalsIgnoreCase("complete")) {logger.warn("map " +key + " -> " + e); }
          }
        }
      }
    }
    logger.warn("no native recordings for " + count + " items. Skipped " + nonNativeRecordings + " non-native recordings");

    Map<String,Exercise> idToEx = new TreeMap<String, Exercise>();
    Map<String, List<Result>> idToResults2 = new TreeMap<String, List<Result>>();

    int skipped = 0;
    int count2 = 0;
    Map<Exercise, List<Result>> chapterExerciseToResult = new HashMap<Exercise, List<Result>>();
    try {
      final FileWriter skip = new FileWriter(configDir + "skip4.txt");
      final FileWriter skipWords = new FileWriter(configDir + "skipWords4.txt");

      DatabaseImpl unitAndChapter = new DatabaseImpl(
        configDir,
        language,
        configDir+
          "ESL_ELC_5071-30books_chapters.xlsx");

      for (Exercise e : unitAndChapter.getExercises()) {
        String key = e.getEnglishSentence().toLowerCase().trim();
        if (flastListEnglishToResults.containsKey(key)) {
          //logger.warn("found " +key + " : " + e.getID() + " "+ e.getEnglishSentence());
          List<Result> value = flastListEnglishToResults.get(key);
          chapterExerciseToResult.put(e, value);
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
      logger.warn("overlap is " + idToEx.size() + "/" + chapterExerciseToResult.size()+   "/"+idToResults2.size()+
        " out of " + unitAndChapter.getExercises().size() + " skipped " + skipped);
    } catch (IOException e) {
      logger.error("Got " + e,e);
    }

    try {
      if (true) convertExamples(numThreads, audioDir,language,idToEx,idToResults2,nativeUsers);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

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
   */
  // TODO later take data and use database so we keep metadata about audio
  private void convertExamples(int numThreads, String audioDir, String language, String spreadsheet, String dbName) throws Exception {
    final String configDir = getConfigDir(language);

    DatabaseImpl db = new DatabaseImpl(
      configDir,
      dbName,
      configDir +
        spreadsheet);

    final Map<String, Exercise> idToEx = getIdToExercise(db);
    Map<String, List<Result>> idToResults = getIDToResultsMap(db);
    Set<Long> nativeUsers = new UserDAO(db).getNativeUsers();
    logger.info("Found " + nativeUsers.size() + " native users");
    if (false) {
      for (String exid : idToEx.keySet()) {
        String resultID = exid + "/0";
        if (!idToResults.containsKey(resultID)) {
          idToResults.put(resultID, new ArrayList<Result>());
        }
      }
      //if (exercises.size() != idToResults.size()) logger.error("\n\n\nhuh? id->results map size " + idToResults.size());
    }

    convertExamples(numThreads, audioDir, language, idToEx, idToResults,nativeUsers);
  }

  private void convertExamples(int numThreads, String audioDir, String language,
                               Map<String, Exercise> idToEx,
                               Map<String, List<Result>> idToResults, Set<Long> nativeUsers) throws IOException, InterruptedException, ExecutionException {
    final String configDir = getConfigDir(language);


    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File newRefDir = new File(placeToPutAudio + "refAudio");
    newRefDir.mkdir();
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    bestDir.mkdir();

    final Map<String, String> properties = getProperties(language, configDir);
    ASRScoring scoring = getAsrScoring(".",null,properties);

    final HTKDictionary dict = scoring.getDict();

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    final FileWriter missingSlow = new FileWriter(configDir + "missingSlow.txt");
    final FileWriter missingFast = new FileWriter(configDir + "missingFast.txt");
    List<Future<?>> futures = getSplitAudioFutures(idToEx, missingSlow, missingFast, idToResults, placeToPutAudio,
      newRefDir, bestDir,
      properties, dict, executorService, language,nativeUsers);

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
    logger.debug("Got " + exercises.size() + " exercises first = " + exercises.iterator().next().getID());
    int count = 0;
    for (Exercise e: exercises) {
      idToEx.put(e.getID(),e);
    //  if (count++ < 100) logger.debug("got " + e.getID());
    }
    return idToEx;
  }

  private void blockUntilComplete(List<Future<?>> futures) throws InterruptedException, ExecutionException {
    logger.info("got " +futures.size() + " futures");
    for (Future<?> future : futures) {
      Object o = future.get();
    }
    logger.info("all " +futures.size() + " futures complete");
  }

  /**
   * @see #convertExamples
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
   * @return
   */
  private List<Future<?>> getSplitAudioFutures(final Map<String, Exercise> idToEx,
                                               final FileWriter missingSlow, final FileWriter missingFast,
                                               Map<String, List<Result>> idToResults,
                                               final String placeToPutAudio, final File newRefDir, final File bestDir,
                                               final Map<String, String> properties,
                                               final HTKDictionary dict, ExecutorService executorService, final String language, Set<Long> nativeUsers) {
    List<Future<?>> futures = new ArrayList<Future<?>>();
    List<String> ids = getIdsSorted(idToResults);
    for (final String id : ids) {
      final List<Result> resultList = idToResults.get(id);
      if (resultList == null) {
        logger.warn("skipping " + id + " no results...");
      } else {
        final List<Result> natives = getResultsByNatives(nativeUsers, resultList);
        if (natives.isEmpty() && !resultList.isEmpty()) {
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
                logger.error("Doing " + id + " and " + resultList + " Got " + e, e);
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
    for (Result r: resultList) {
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

  /**
   * @see #convertEnglish(int, String)
   * @see #convertExamples(int, String, String, String, String)
   * @param db
   * @return
   */
  private Map<String, List<Result>> getIDToResultsMap(DatabaseImpl db) {
    Map<String,List<Result>> idToResults = new HashMap<String, List<Result>>();
    List<Result> results = db.getResults();
    logger.debug("Got " + results.size() + " results");
    for (Result r : results) {
      String id = r.id;
      int i = Integer.parseInt(id);
      if (i < MAX //&&
        //(i == 675 || i == 3488 || i == 4374 || i == 3658 || i == 4026 || i == 3697  || i == 4116  || i == 627 || i == 4083|| i == 3375|| i == 4185)//39//3496
        //( i == 3375)//39//3496
        ) {
        List<Result> resultList = idToResults.get(id);//r.getID());
        if (resultList == null) {
          idToResults.put(id,//r.getID(),
            resultList = new ArrayList<Result>());
        }
        resultList.add(r);
      }
    }
    logger.debug("got " + idToResults + " first key " + idToResults.keySet().iterator().next());
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

  /**
   * @see #getSplitAudioFutures
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
   * @paramx pair
   * @throws IOException
   */
  private void getBestForEachExercise(String exid2, Map<String, Exercise> idToEx, FileWriter missingSlow, FileWriter missingFast,
                                      File newRefDir, File bestDir,
                                      List<Result> resultsForExercise,

                                      String collectedAudioDir, HTKDictionary dictionary, Map<String, String> properties, String language) throws IOException {
    if (resultsForExercise.isEmpty()) return;

    List<String> idsSorted = getIdsSorted(idToEx);

    if (idsSorted.size() != idToEx.size()) logger.error("huh? lost items???");

    Exercise exercise = idToEx.get(exid2);
    if (exercise == null) {
      if (exid2.length() > 3) {
        exercise = idToEx.get(exid2.substring(0, exid2.length() - 3));
      }
    }
    if (exercise == null) {
      logger.debug("getBestForEachExercise skipping ex id '" + exid2 + "' since not in exercises " + idsSorted);
      return;
    }
    getBest(missingSlow, missingFast, newRefDir, bestDir,
      collectedAudioDir, dictionary, properties, resultsForExercise, exid2, exercise, language);
  }

  private void getBest(FileWriter missingSlow, FileWriter missingFast, File newRefDir, File bestDir,
                       String collectedAudioDir,
                       HTKDictionary dictionary, Map<String, String> properties,
                       List<Result> resultsForExercise, String exid, Exercise exercise, String language) throws IOException {
    String refSentence = language.equalsIgnoreCase("english") ? exercise.getEnglishSentence() : exercise.getRefSentence();
    refSentence = refSentence.replaceAll("\\p{P}", "");
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefSentence(split).trim();
    int refLength = split.length;
    String firstToken = split[0].trim();
    String lastToken = split[refLength-1].trim();
    // logger.debug("refSentence " + refSentence + " length " + refLength + " first |" + firstToken + "| last |" +lastToken +"|");

    File refDirForExercise = new File(newRefDir, exid);
    //  if (!key.equals(exid)) logger.error("huh?> not the same " + key + "  and " + exid);
    //logger.debug("making dir " + key + " at " + refDirForExercise.getAbsolutePath());
    refDirForExercise.mkdir();

    File bestDirForExercise = new File(bestDir, exid);
  //  logger.debug("for '" +refSentence + "' making dir " + key + " at " + bestDirForExercise.getAbsolutePath());
    bestDirForExercise.mkdir();
    final ASRScoring scoring = getAsrScoring(".",dictionary,properties);
    String best = getBestFilesFromResults(scoring, resultsForExercise, exercise, refSentence,
      firstToken, lastToken,
      refLength, refDirForExercise,collectedAudioDir);

    if (best != null) {
      logger.debug("for " + exid + " : '" + exercise.getEnglishSentence() + "' best is " + best);// + " total " + bestTotal);
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
   * @see #getBest
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
    float/* bestSlow = 0, bestFast = 0,*/ bestTotal = 0;
    for (Result r : resultsForExercise) {
      File answer = new File(r.answer);
      String name = answer.getName().replaceAll(".wav", "");
      String parent = collectedAudioDir + File.separator + answer.getParent();

      double durationInSeconds = getDuration(answer, parent);
      if (durationInSeconds < MIN_DUR) {
        if (durationInSeconds > 0) logger.warn("skipping " + name + " since it's less than a 1/2 second long.");
        continue;
      }
      String testAudioFileNoSuffix = getConverted(name, parent);

      //logger.debug("parent " + parent + " running result " + r.uniqueID + " for exercise " + id + " and audio file " + name);

      Scores align = getAlignmentScores(scoring, refSentence, name, parent, testAudioFileNoSuffix);

      try {
        String wordLabFile = prependDeploy(parent,testAudioFileNoSuffix + ".words.lab");
        GetAlignments alignments = new GetAlignments(first,last, refLength, name, wordLabFile).invoke();
         boolean valid = alignments.isValid();
        float hydecScore = align.hydecScore;
        if (!valid) {
          logger.warn("\n---> ex " + id + " " + exercise.getEnglishSentence() +
            " score " + hydecScore +
            " invalid alignment : " + alignments.getWordSeq() + " : " + alignments.getScores());
        }
        if (bestTotal < hydecScore && valid && hydecScore > 0.1f) {
          boolean consistent = writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            alignments);
          if (consistent) {
            bestTotal = hydecScore;
            best = testAudioFileNoSuffix;

            logger.debug("new best : ex " + id + " " + exercise.getEnglishSentence() + " best so far is " + best + " score " + bestTotal + " hydecScore " + hydecScore);
          }
        } else if (valid) {
          logger.debug("ex " + id + " " + exercise.getEnglishSentence() + " best so far is " + best + " score " + bestTotal + " hydecScore " + hydecScore);
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
    }
    return best;
  }

  private Scores getAlignmentScores(ASRScoring scoring, String refSentence, String name, String parent, String testAudioFileNoSuffix) {
    String doubled = refSentence + " " + refSentence;
    doubled = doubled.toUpperCase();   // TODO : only for english!?
    Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
    logger.debug("\tgot " + align + " for " + name + " for " +doubled);
    return align;
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
      logger.error("getDuration can't find " + answer2.getAbsolutePath());
    }
    else {
      durationInSeconds = audioCheck.getDurationInSeconds(answer2);
   //   logger.debug("dur of " + answer2 + " is " + durationInSeconds + " seconds");
    }
    return durationInSeconds;
  }

  /**
   * @see #getBest(java.io.FileWriter, java.io.FileWriter, java.io.File, java.io.File, String, corpus.HTKDictionary, java.util.Map, java.util.List, String, mitll.langtest.shared.Exercise, String)
   * @param missingSlow
   * @param missingFast
   * @param exid
   * @param refDirForExercise
   * @param bestDirForExercise
   * @param best
   * @throws IOException
   */
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

  private boolean writeTheTrimmedFiles(File refDirForExercise, String parent, float floatDur, String testAudioFileNoSuffix,
                                    GetAlignments alignments) {
    float pad = 0.25f;

    float start1 = alignments.getStart1();
    float s1 = Math.max(0,start1-pad);
    float start2 = alignments.getStart2();
    float s2 = Math.max(0,start2-pad);

    float end1 = alignments.getEnd1();
    float e1 = Math.min(floatDur,end1+pad);
    float midPoint = end1 + ((start2 - end1) / 2);
    if (e1 > midPoint) {
      logger.warn("e1 " + e1 + " is after the midpoint " +midPoint);

      return false;
    /*  e1 = Math.max(end1,midPoint);
      s2 = Math.min(start2,midPoint);*/
    }

    float end2 = alignments.getEnd2();
   // logger.debug("first file " + start1 + "->" + end1 + " second " + start2 + "->" + end2);
    float e2 = Math.min(floatDur, end2 + pad);

    float d1 = e1 - s1;
    float d2 = e2 - s2;
    if (d2 < d1* DURATION_CHECK) {
      logger.debug("slow segment dur " + d2 + " < fast dur " + (d1 * DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      e2 = floatDur;
      d2 = e2-s2;
    }
    if (d2 < d1* DURATION_CHECK) {
      logger.warn("Still after repair slow segment dur "+d2 + " < fast dur " + (d1* DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      return false;
    }

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
      return false;
    } else {

      File fast = new File(refDirForExercise, testAudioFileNoSuffix + "_" + FAST + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        fast.getAbsolutePath(),
        s1,
        d1);

      if (fast.exists() && audioCheck.getDurationInSeconds(fast)< MIN_DUR) {
        logger.error("huh? after writing with sox, the audio file is too short?");
        fast.delete();
        return false;
      }
    }

    if (d2 < MIN_DUR && d2 > d1) {
      if (d2 < MIN_DUR)
        logger.warn("Skipping audio " + longFileFile.getName() + " since slow audio too short");
      else
        logger.warn("Skipping audio " + longFileFile.getName() + " since slow audio shorter than fast.");
      return false;
    } else {
      File slow = new File(refDirForExercise, testAudioFileNoSuffix + "_" + SLOW + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        slow.getAbsolutePath(),
        s2,
        d2);

      if (slow.exists() && audioCheck.getDurationInSeconds(slow)< MIN_DUR) {
        logger.error("huh? after writing with sox, the audio file is too short?");
        slow.delete();
        return false;
      }
    }
    return true;
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

    //private int starts,ends;
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
        //if (start) starts++;
        boolean end = word.equals("</s>");
        //if (end) ends++;

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
      new SplitAudio().convertEnglish(numThreads,audioDir);
      if (true) return;
      //  new SplitAudio().dumpDir(audioDir);
      if (arg.length == 2) {
        new SplitAudio().normalize(audioDir);
      } else {
        String language = arg[2];
        String spreadsheet = arg[3];
        String dbName = arg[4];
        new SplitAudio().convertExamples(numThreads, audioDir, language, spreadsheet, dbName);
       // new SplitAudio().dumpDir2(audioDir, language, dbName, spreadsheet);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
