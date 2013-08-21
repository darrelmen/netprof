package mitll.langtest.server.audio;

import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ExcelImport;
import mitll.langtest.server.database.FileExerciseDAO;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
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
  private static final double DURATION_CHECK = 1.2;

  private static final int LOW_WORD_SCORE_THRESHOLD = 20;
  private static final boolean THROW_OUT_FAST_LONGER_THAN_SLOW = false;
  private static final float MSA_MIN_SCORE = 0.2f;
  public static final float LOW_SCORE_THRESHOLD = 0.2f;
  public static final float LOW_SINGLE_SCORE_THRESHOLD = 0.4f;

  private static Logger logger = Logger.getLogger(SplitAudio.class);

  private boolean debug;
  private static final int MAX = 50000;//22000;
  public static final double BAD_PHONE = 0.1;
  public static final double BAD_PHONE_PERCENTAGE = 0.2;
  private static final double MIN_DUR = 0.2;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private AudioCheck audioCheck = new AudioCheck();

  public void checkTamas() {
    File file = new File("89");
    file = new File(file,"0");
    file = new File(file,"subject-6");
    File[] files = file.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        String name = pathname.getName();
        return name.endsWith(".wav") && !name.contains("16K") ;
      }
    });
    logger.info("got " + files.length+ " files ");

    String language = "english";
    final String configDir = getConfigDir(language);


    try {
      final Map<String, String> properties = getProperties(language, configDir);
      ASRScoring scoring = getAsrScoring(".",null,properties);

      for (File fastFile : files) {
        String fastName = fastFile.getName().replaceAll(".wav", "");
        Scores fast = getAlignmentScoresNoDouble(scoring, "fifty", fastName, fastFile.getParent(), getConverted(fastFile));
        logger.info("score for " + fastFile.getName() + " score " + fast);
      }
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void splitSimpleMSA(int numThreads) {
    File file = new File("war");
    file = new File(file,"config");
    File relativeConfigDir = new File(file,"msa");
    file = new File(relativeConfigDir,"headstartMediaOriginal");


    final Set<String> rerunSet = new HashSet<String>();
    new ExcelImport().getMissing(relativeConfigDir.getPath(),"rerun.txt",rerunSet);
    logger.info("rerun " + rerunSet);

  //  if (true) return;

    File[] files = file.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        String name = pathname.getName();
        boolean inRerun = rerunSet.contains(name.substring(0,name.length()-4)) || rerunSet.isEmpty();
        if (inRerun) logger.debug("redoing " + name);
        return inRerun && name.endsWith(".wav") && !name.contains("16K") ;
      }
    });
    logger.info("got " + files.length+ " files ");

    String language = "msa";
    final String configDir = getConfigDir(language);

   DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
     language,
      configDir+
        "MSA-headstart.xlsx");
    final Map<String, Exercise> idToEx = getIdToExercise(unitAndChapter);
    final String placeToPutAudio = ".."+ File.separator+"msaHeadstartAudio" + File.separator;
    final File newRefDir = new File(placeToPutAudio + "refAudio");
    newRefDir.mkdir();

    final File newRefDir2 = new File(placeToPutAudio + "refAudio2");
    newRefDir2.mkdir();

    try {
      final Map<String, String> properties = getProperties(language, configDir);
      ASRScoring scoringFirst = getAsrScoring(".", null, properties);
      final HTKDictionary dict = scoringFirst.getDict();

      List<File> sublist = Arrays.asList(files);
      //   sublist = sublist.subList(0, 3);

      ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

      final FileWriter missingSlow = new FileWriter(configDir + "missingSlow.txt");
      final FileWriter missingFast = new FileWriter(configDir + "missingFast.txt");

      List<Future<?>> futures = new ArrayList<Future<?>>();
      for (final File unsplit : sublist) {
        Future<?> submit = executorService.submit(new Runnable() {
          @Override
          public void run() {
            try {
              writeOneMSAFile("msa", idToEx, newRefDir, properties, dict, unsplit, missingFast, missingSlow);
            } catch (Exception e) {
              logger.error("Doing " + unsplit.getName() + " Got " + e, e);
            }
          }
        });
        futures.add(submit);
      }

      blockUntilComplete(futures);

        missingFast.close();
         missingSlow.close();
      logger.info("closing missing slow");
      executorService.shutdown();

    /*  for (File unsplit : sublist) {
        writeOneMSAFile(language, idToEx, newRefDir, properties, dict, unsplit);
      }*/
    } catch (Exception e) {
      logger.error("got " + e);
    }
  }

  /**
   * @see #splitSimpleMSA(int)
   * @param language
   * @param idToEx
   * @param newRefDir
   * @param properties
   * @param dict
   * @param unsplit
   * @param missingFast
   * @param missingSlow
   */
  private void writeOneMSAFile(String language, Map<String, Exercise> idToEx, File newRefDir, Map<String, String> properties, HTKDictionary dict, File unsplit, FileWriter missingFast,
                               FileWriter missingSlow) {
    String name = unsplit.getName().replaceAll(".wav", "");
    String parent = /*"." + File.separator + */unsplit.getParent();

    double durationInSeconds = getDuration(unsplit, parent);
    if (durationInSeconds < MIN_DUR) {
      if (durationInSeconds > 0) logger.warn("skipping " + name + " since it's less than a 1/2 second long.");
      return;
    }
    String testAudioFileNoSuffix = getConverted(parent, name);

    Exercise exercise = idToEx.get(name);
    if (exercise == null) {

      recordMissing(missingFast, missingSlow, name);
      return;
    }
    String refSentence = language.equalsIgnoreCase("english") ? exercise.getEnglishSentence() : exercise.getRefSentence();
    refSentence = refSentence.replaceAll("\\p{P}", "");

    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefFromSplit(split);
    int refLength = split.length;
    String firstToken = split[0].trim();
    String lastToken = split[refLength - 1].trim();

    ASRScoring scoring = getAsrScoring(".",dict,properties);
    Scores align = getAlignmentScores(scoring, refSentence, name, parent, testAudioFileNoSuffix);

    if (align.hydecScore == -1 || align.hydecScore < MSA_MIN_SCORE) {
      logger.warn("-----> adding " + name + " to missing list b/c of score = " +align.hydecScore);
      recordMissing(missingFast, missingSlow, name);
      return;
    }
    try {
      String wordLabFile = prependDeploy(parent, testAudioFileNoSuffix + ".words.lab");

      GetAlignments alignments = new GetAlignments(firstToken, lastToken, refLength, name, wordLabFile).invoke();
      boolean valid = alignments.isValid();
      float hydecScore = align.hydecScore;
      if (!valid) {
        logger.warn("\n---> ex " + name + " " + exercise.getEnglishSentence() +
          " score " + hydecScore +
          " invalid alignment : " + alignments.getWordSeq() + " : " + alignments.getScores());
      }
      File refDirForExercise = new File(newRefDir, name);
      refDirForExercise.mkdir();
      if (valid && hydecScore > LOW_SCORE_THRESHOLD) {
        FastAndSlow consistent = writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
          alignments, true);

        if (consistent.valid) {
          File fastFile = consistent.fast;
          String fastName = fastFile.getName().replaceAll(".wav", "");

          Scores fast = getAlignmentScoresNoDouble(scoring, refSentence, fastName, fastFile.getParent(), getConverted(fastFile));
          if (fast.hydecScore > MSA_MIN_SCORE) {
            float percentBadFast = getPercentBadPhones(fastName, fast.eventScores.get("phones"));
            if (percentBadFast > BAD_PHONE_PERCENTAGE) {
              logger.warn("---> rejecting new best b/c % bad phones = " + percentBadFast + " so not new best fast : ex " + name + //" " + exercise.getEnglishSentence() +
                " hydecScore " + hydecScore + "/" + fast.hydecScore);
              recordMissingFast(missingFast,name);
            }
          }

          File slowFile = consistent.slow;
          String slowName = slowFile.getName().replaceAll(".wav", "");
          Scores slow = getAlignmentScoresNoDouble(scoring, refSentence, slowName, slowFile.getParent(), getConverted(slowFile));
          if (slow.hydecScore > MSA_MIN_SCORE) {
            float percentBadSlow = getPercentBadPhones(slowName, slow.eventScores.get("phones"));
            if (percentBadSlow > BAD_PHONE_PERCENTAGE) {
              logger.warn("---> rejecting new best b/c % bad phones = " + percentBadSlow + " so not new best slow : ex " + name + //" " + exercise.getEnglishSentence() +
                " hydecScore " + hydecScore + "/" + slow.hydecScore);
              recordMissingFast(missingSlow,name);
            }
          }
        }

        if (!consistent.valid) logger.error(name + " not valid");
      }
    } catch (Exception e) {
      logger.error("got " + e);
    }
  }

  private void recordMissing(FileWriter missingFast, FileWriter missingSlow, String name) {
    try {
      recordMissingFast(missingFast, name);
      recordMissingFast(missingSlow, name);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private void recordMissingFast(FileWriter missingFast, String name) throws IOException {
    synchronized (missingFast) {
      missingFast.write(name + "\n");
      missingFast.flush();
    }
  }

  /**
   * Write out an analist and a transcript file suitable for use with the englishRecalcPhoneNormalizer.cfg config
   */
  public void normalize() {
    String audioDir = "englishAudio";
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "refAudio");
    File[] bestAudioDirs = bestDir.listFiles();
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);

    List<File> temp = Arrays.asList(bestAudioDirs);
   // temp = temp.subList(0,3); // for now

    String language = "english";
    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      language,
      configDir+
        "ESL_ELC_5071-30books_chapters.xlsx");
    final Map<String, Exercise> idToEx = getIdToExercise(unitAndChapter);

    try {
      String fileName = configDir + "analist";
      File analistFile = new File(fileName);
      final FileWriter analist = new FileWriter(analistFile);


      String fileName2 = configDir + "transcript";
      File transcriptFile = new File(fileName2);
      final FileWriter transcript = new FileWriter(transcriptFile);

      for (File file1 : temp) {
        logger.warn("reading from " + file1.getName());
        if (!file1.exists()) {
             logger.error("huh? " +file1.getAbsolutePath() + " doesn't exist");
        }
        if (!file1.isDirectory()) {
          logger.error("huh? " +file1.getAbsolutePath() + " is not a directory...");

        }
        File[] files = file1.listFiles(new FileFilter() {
          @Override
          public boolean accept(File pathname) {
            return pathname.getName().endsWith(".raw");
          }
        });
        String dirname = file1.getName();
        Exercise exercise = idToEx.get(dirname);

        if (files != null && exercise != null) {
          for (File rawFile : files) {
            String id = rawFile.getName().replace(".raw","");
            analist.write(id + " " + ensureForwardSlashes(rawFile.getPath())+ "\n");
            transcript.write("<s> "+getEnglishRefSentence(exercise.getEnglishSentence()) +" </s> (" +id+
              ")"+
              "\n");
          }
        }
      }
      analist.close();
      logger.debug("wrote to " +analistFile.getAbsolutePath());
      transcript.close();
      logger.debug("wrote to " +transcriptFile.getAbsolutePath());
    } catch (IOException e) {
      logger.error("got "+e,e);
    }
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
  public void dumpDirEnglish () {
   // logger.warn("audio dir " + audioDir + " lang " + language + " db " +dbName + " spreadsheet " + spreadsheet);

    Set<String> files = getFilesInBestDir("englishAudio");

    final String configDir = getConfigDir("english");

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      "english",
      configDir+
        "ESL_ELC_5071-30books_chapters.xlsx");

    writeMissingFiles(files, configDir, unitAndChapter);
  }

  public void dumpDir2 (String audioDir, String language, String dbName, String spreadsheet) {
    logger.warn("audio dir " + audioDir + " lang " + language + " db " +dbName + " spreadsheet " + spreadsheet);

    Set<String> files = getFilesInBestDir(audioDir);

    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      dbName,
      configDir +
        spreadsheet);

    writeMissingFiles(files, configDir, unitAndChapter);
  }

  private void writeMissingFiles(Set<String> files, String configDir, DatabaseImpl unitAndChapter) {
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
      logger.error("Got " +e,e);
    }
  }

  private Set<String> getFilesInBestDir(String audioDir) {
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);
    return new HashSet<String>(Arrays.asList(list));
  }


  public void dumpDir (String audioDir, String language) {
    Set<String> files = getFilesInBestDir(audioDir);

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
        //if (c++ < 10) logger.warn("convertEnglish huh? no english sentence for " + e.getID() + " instead " +e.getRefFromSplit());
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
        //if (c++ < 10) logger.warn("convertEnglish huh? no english sentence for " + e.getID() + " instead " +e.getRefFromSplit());
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

    Map<String,String> englishToCorrection = getCorrection();

    int c = 0;
    int diff =0;
    for (Exercise e : idToEx.values()) {
      String eng = e.getEnglishSentence();
      String chinese = englishToCorrection.get(eng);
      if (chinese != null) {
        c++;
        if (!e.getRefSentence().equals(chinese)) diff++;
        e.setRefSentence(chinese);
      }
    }

    logger.debug("diff " + diff + " count " + c + " vs " + idToEx.size());

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

  private Map<String,String> getCorrection() {
    Map<String,String> englishToCorrection = new HashMap<String,String>();
    File file = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin.tsv");
    try {
      if (!file.exists()) {
        logger.error("can't find '" + file + "'");
        return null;
      } /*else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }*/
      BufferedReader reader = getReader(file);

      String line;

      int error = 0;
      int error2 = 0;
      int c = 0;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) continue;
      //  if (c++ < 10) logger.debug("line " +line);
        String[] split = line.split("\\t");
        try {
          englishToCorrection.put(split[0], split[1]);
        } catch (Exception e) {
          if (error++ < 10) logger.error("reading " + file.getAbsolutePath() + " line '" + line + "' split len " +split.length, e);
          error2++;
          //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
      reader.close();
      if (error2 > 0) logger.error("got " + error2 + " errors");
    } catch (Exception e) {
      logger.error("reading " + file.getAbsolutePath() + " got " + e, e);
    }

    return englishToCorrection;
  }


  public Map<String,String> getCorrection2() {
    Map<String,String> englishToCorrection = new HashMap<String,String>();
    File file = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin.tsv");
    File file2 = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin_utf8.tsv");

    try {
      if (!file.exists()) {
        logger.error("can't find '" + file + "'");
        return null;
      } /*else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }*/
      FileOutputStream resourceAsStream = new FileOutputStream(file2);
      BufferedWriter utf8 = new BufferedWriter(new OutputStreamWriter(resourceAsStream, "UTF8"));
      BufferedReader reader = getReader(file);

      String line;

      int error = 0;
      int error2 = 0;
      int c = 0;
      while ((line = reader.readLine()) != null) {
        utf8.write(line);utf8.write("\n");
      }
      reader.close();
      utf8.close();
      //if (error2 > 0) logger.error("got " + error2 + " errors");
    } catch (Exception e) {
      logger.error("reading " + file.getAbsolutePath() + " got " + e, e);
    }

    return englishToCorrection;
  }

  private BufferedReader getReader(File lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream,"UTF16"));
  }

  /**
   * @see #convertExamples(int, String, String, String, String)
   * @param numThreads
   * @param audioDir
   * @param language
   * @param idToEx
   * @param idToResults
   * @param nativeUsers
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
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

  /**
   * @see #convertExamples(int, String, String, String, String)
   * @see #normalize()
   * @see #splitSimpleMSA(int)
   * @param db
   * @return
   */
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
    //List<Integer> integers = Arrays.asList(675, 3488, 4374,3658,4026,3697,4116,627,4083,3375,4185,1826);
    //List<Integer> integers = Arrays.asList( 3488 // clock
    //);
    //List<Integer> integers = Arrays.asList( 4374 // it
    //);
    /*List<Integer> integers = Arrays.asList( 1826 // hello
    );*/
   // Set<Integer> test = new HashSet<Integer>(integers);
    for (Result r : results) {
      String id = r.id;
      int i = Integer.parseInt(id);
      if (i < MAX
     // && test.contains(i)
       // (i == 675 || i == 3488 || i == 4374 || i == 3658 || i == 4026 || i == 3697  || i == 4116  || i == 627 || i == 4083|| i == 3375|| i == 4185)//39//3496
        //( i == 3375)//39//3496
        // ( i == 4026)//39//3496
       //( i == 675)//39//3496   // notebook
        //( i == 1826)//39//3496      // hello
      // && ( i == 4185)//39//3496      // eighty
        ) {
        List<Result> resultList = idToResults.get(id);//r.getID());
        if (resultList == null) {
          idToResults.put(id,//r.getID(),
            resultList = new ArrayList<Result>());
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
        String trimmed = exid2.substring(0, exid2.length() - 3);
        logger.debug("getBestForEachExercise trying exid " + trimmed);

        exercise = idToEx.get(trimmed);
      }
    }
    if (exercise == null) {
      logger.debug("getBestForEachExercise skipping ex id '" + exid2 + "' since not in " + idsSorted.size() + " exercises");
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
   * @see #getBestForEachExercise(String, java.util.Map, java.io.FileWriter, java.io.FileWriter, java.io.File, java.io.File, java.util.List, String, corpus.HTKDictionary, java.util.Map, String)
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
   */
  private void getBest(FileWriter missingSlow, FileWriter missingFast, File newRefDir, File bestDir,
                       String collectedAudioDir,
                       HTKDictionary dictionary, Map<String, String> properties,
                       List<Result> resultsForExercise, String exid, Exercise exercise, String language) throws IOException {
    String refSentence = language.equalsIgnoreCase("english") ? exercise.getEnglishSentence() : exercise.getRefSentence();
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefFromSplit(split);
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
    FastAndSlow fastAndSlow = getBestFilesFromResults(scoring, resultsForExercise, exercise, refSentence,
      firstToken, lastToken,
      refLength, refDirForExercise,collectedAudioDir);

    if (fastAndSlow.valid) {
      logger.debug("for " + exid + " : '" + exercise.getEnglishSentence() + "'/'" +
        exercise.getRefSentence() +
        "' best is " + fastAndSlow);// + " total " + bestTotal);
      boolean gotBoth = writeBestFiles(missingSlow, missingFast, exid,
        bestDirForExercise, fastAndSlow);
      if (gotBoth) both++; // only if both fast and slow were written do we record it as having both parts done properly
      good++;
    }
    else {
      bad++;

      int pct = (int)(100f*(float)both/((float)(good+bad)));
      logger.warn("Tally : bad " + bad + "/" +good + " good / "+ both+" both (" +pct+
        "%): no valid audio for " + exid);

      recordMissingFast(missingFast, exid);
      recordMissingFast(missingSlow, exid);
    }
  }

  private String getRefSentence(String refSentence) {
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    return getRefFromSplit(split);
  }

  private String getEnglishRefSentence(String refSentence) {
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    return getRefFromSplit(split).toUpperCase();
  }

  /**
   * TODO : do we want to remove punctuation???
   * @param split
   * @return
   */
  private String getRefFromSplit(String[] split) {
    String newRefSentence = getRefSentence(split).trim();
   // newRefSentence = newRefSentence.replaceAll("\\p{P}", "");   // remove punct
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
  private FastAndSlow getBestFilesFromResults(ASRScoring scoring, List<Result> resultsForExercise,
                                         Exercise exercise, String refSentence,
                                         String first, String last, int refLength,
                                         File refDirForExercise,String collectedAudioDir ) {
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
          logger.warn("skipping " + name + " since it's less than a " + MIN_DUR+ " second long.");
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
        if (//bestTotal < hydecScore &&
          valid && hydecScore > 0.1f) {
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
                  //" best so far is " + bestFastFile.getName() +
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
                  //" best so far is " + bestSlowFile.getName() +
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

  private float getPercentBadPhones(String fastName, Map<String, Float> phones) {
    int countBad = 0;
    for (Map.Entry<String,Float> phoneToScore : phones.entrySet()) {
       if (phoneToScore.getValue() < BAD_PHONE) {
      //   logger.warn("\tfor " + fastName + " got bad phone score " + phoneToScore.getKey() + " : " + phoneToScore.getValue());
         countBad++;
       }
    }
    if (countBad > 0) {
      logger.warn("getPercentBadPhones : " + phones);
      for (Map.Entry<String,Float> phoneToScore : phones.entrySet()) {
        if (phoneToScore.getValue() < BAD_PHONE) {
          logger.warn("\tfor " + fastName + " got bad phone score " + phoneToScore.getKey() + " : " + phoneToScore.getValue());
        }
      }
    }
    float percentBad = (float) countBad/(float)phones.size();
    return percentBad;
  }

  private Scores getAlignmentScores(ASRScoring scoring, String refSentence, String name, String parent, String testAudioFileNoSuffix) {
    String doubled = refSentence + " " + refSentence;
    doubled = doubled.toUpperCase();   // TODO : only for english!?
    Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
    logger.debug("\tdoubled : got " + align + " for " + name + " for '" +doubled +"'");
    return align;
  }

  private Scores getAlignmentScoresNoDouble(ASRScoring scoring, String refSentence, String name, String parent, String testAudioFileNoSuffix) {
    String doubled = refSentence;
    doubled = doubled.toUpperCase();   // TODO : only for english!?
    Scores align = scoring.align(parent, testAudioFileNoSuffix, doubled);
    logger.debug("\tsingle : got " + align + " for " + name + " for '" +doubled +"'");
    return align;
  }

  private String getConverted(File answer) {
    String name = answer.getName().replaceAll(".wav", "");
    return getConverted(answer.getParent(), name);
  }

  private String getConverted(String parent, String name) {
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
   * @param bestDirForExercise
   * @throws IOException
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
      gotBoth = false;
    } else {
      File file = new File(bestDirForExercise, SLOW + ".wav");
      logger.debug("slow wrote best to " + file.getAbsolutePath());

      new FileCopier().copy(slow.getAbsolutePath(), file.getAbsolutePath());
    }
    return gotBoth;
  }

  private FastAndSlow writeTheTrimmedFiles(File refDirForExercise, String parent, float floatDur, String testAudioFileNoSuffix,
                                           GetAlignments alignments, boolean remove16K) {
    float pad = 0.25f;

    float start1 = alignments.getStart1();
    float s1 = Math.max(0,start1-pad);
    float start2 = alignments.getStart2();
    float s2 = Math.max(0,start2-pad);

    float end1 = alignments.getEnd1();
    float e1 = Math.min(floatDur,end1+pad);
    float midPoint = end1 + ((start2 - end1) / 2);

/*    if (e1 > s2) {
      e1 = s2;
    }*/

    if (e1 > midPoint) {
      logger.debug("e1 " + e1 + " is after the midpoint " + midPoint);
  //    return new FastAndSlow();
      e1 = Math.max(end1,midPoint);
      //s2 = Math.min(start2,midPoint);
    }

    if (e1 > start2)  {
      logger.warn("for " + testAudioFileNoSuffix +
        " end of fast " + e1 + " is after start of slow " + start2);
      return new FastAndSlow();
    }

    if (s2 < midPoint) {
      logger.debug("s2 " + s2 + " is before the midpoint " + midPoint);
      s2 = Math.min(start2,midPoint);
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
    if (d2 < d1* DURATION_CHECK) {
      logger.debug("slow segment dur " + d2 + " < fast dur " + (d1 * DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      e2 = floatDur;
      d2 = e2-s2;
    }
    if (d2 < d1* DURATION_CHECK) {
      logger.warn("Still after repair slow segment dur "+d2 + " < fast dur " + (d1* DURATION_CHECK) + " so fixing end to end of audio");
      // can either repair or throw this one out
      if (THROW_OUT_FAST_LONGER_THAN_SLOW) return new FastAndSlow();
    }

    File longFileFile = new File(parent,testAudioFileNoSuffix+".wav");
    if (!longFileFile.exists())logger.error("huh? can't find  " + longFileFile.getAbsolutePath());

    logger.debug("writing ref files to " + refDirForExercise.getAbsolutePath() + " input " + longFileFile.getName() +
      " pad s1 " + s1 + "-" + e1+
      " dur " + d1 +
      " s2 " + s2 + "-" + e2+
      " dur " + d2);

    AudioConverter audioConverter = new AudioConverter();
    String binPath = AudioConversion.WINDOWS_SOX_BIN_DIR;
    if (! new File(binPath).exists()) binPath = AudioConversion.LINUX_SOX_BIN_DIR;
    String sox = audioConverter.getSox(binPath);

    File fast,slow;
    if (d1 < MIN_DUR) {
      logger.warn("Skipping audio " + longFileFile.getName() + " since fast audio too short ");
      return new FastAndSlow();
    } else {

      String nameToUse = remove16K ? testAudioFileNoSuffix.replace("_16K","") : testAudioFileNoSuffix;
      fast = new File(refDirForExercise, nameToUse + "_" + FAST + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        fast.getAbsolutePath(),
        s1,
        d1);

      if (fast.exists() && audioCheck.getDurationInSeconds(fast)< MIN_DUR) {
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
      String nameToUse = remove16K ? testAudioFileNoSuffix.replace("_16K","") : testAudioFileNoSuffix;

      slow = new File(refDirForExercise, nameToUse + "_" + SLOW + ".wav");
      audioConverter.trim(sox,
        longFileFile.getAbsolutePath(),
        slow.getAbsolutePath(),
        s2,
        d2);

      if (slow.exists() && audioCheck.getDurationInSeconds(slow)< MIN_DUR) {
        logger.error("huh? after writing with sox, the audio file is too short?");
        slow.delete();
        return new FastAndSlow();
      }
    }
    return new FastAndSlow(fast,slow);
  }

  private class FastAndSlow {
    private final boolean valid;
    public File fast;
    public File slow;
    public FastAndSlow() { this.valid = false; }
    public FastAndSlow(File fast, File slow) { this.valid = true; this.fast = fast; this.slow = slow; }
    public String toString() {
      return (valid ? " valid " : " invalid ") +
        (fast != null ? (" fast " + fast.getName()) : "") +
        (slow != null ? (" slow " + slow.getName()) : "")
        ;
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
      return lowWordScores < LOW_WORD_SCORE_THRESHOLD;
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
      //  new SplitAudio().checkTamas();
  //     new SplitAudio().splitSimpleMSA(numThreads);
  if (false)     new SplitAudio().getCorrection2();
    //if (true) return;

      String audioDir = arg[1];
      // new SplitAudio().convertExamples(numThreads, audioDir, arg[2], arg[3]);
//      new SplitAudio().convertEnglish(numThreads,audioDir);
       // new SplitAudio().dumpDir2(audioDir);
      if (arg.length == 2) {
        new SplitAudio().normalize();
      } else {
        String language = arg[2];
        String spreadsheet = arg[3];
        String dbName = arg[4];
        new SplitAudio().convertExamples(numThreads, audioDir, language, spreadsheet, dbName);
     //   new SplitAudio().dumpDir2(audioDir, language, dbName, spreadsheet);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
