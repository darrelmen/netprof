package mitll.langtest.server.audio;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class English extends SplitAudio {
  public static final float LOW_SCORE_THRESHOLD = 0.2f;
  private static Logger logger = Logger.getLogger(English.class);

  /**
   * Write out an analist and a transcript file suitable for use with the englishRecalcPhoneNormalizer.cfg config
   */
  public void normalize() {
    String audioDir = "englishAudio";
    final String placeToPutAudio = ".." + File.separator + audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "refAudio");
    File[] bestAudioDirs = bestDir.listFiles();
    String[] list = bestDir.list();
    logger.warn("in " + bestDir.getAbsolutePath() + " there are " + list.length);

    List<File> temp = Arrays.asList(bestAudioDirs);
    // temp = temp.subList(0,3); // for now

    String language = "english";
    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      language,
      configDir +
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
          logger.error("huh? " + file1.getAbsolutePath() + " doesn't exist");
        }
        if (!file1.isDirectory()) {
          logger.error("huh? " + file1.getAbsolutePath() + " is not a directory...");

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
            String id = rawFile.getName().replace(".raw", "");
            analist.write(id + " " + ensureForwardSlashes(rawFile.getPath()) + "\n");
            transcript.write("<s> " + getEnglishRefSentence(exercise.getEnglishSentence()) + " </s> (" + id +
              ")" +
              "\n");
          }
        }
      }
      analist.close();
      logger.debug("wrote to " + analistFile.getAbsolutePath());
      transcript.close();
      logger.debug("wrote to " + transcriptFile.getAbsolutePath());
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private String getEnglishRefSentence(String refSentence) {
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    return getRefFromSplit(split).toUpperCase();
  }

  public void dumpDir(String audioDir, String language) {
    Set<String> files = getFilesInBestDir(audioDir);

    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      language,
      configDir +
        "ESL_ELC_5071-30books_chapters.xlsx");

    DatabaseImpl collected = new DatabaseImpl(
      configDir,
      language,
      configDir +
        "5100-english-no-gloss.txt");

    Set<String> valid = new HashSet<String>();
    for (Exercise e : collected.getExercises()) {
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
          if (skipped++ < 10)
            logger.warn("skipping " + e.getID() + " : " + key);// + " no match in " +englishToEx.size() + " entries.");
          skip.write(e.getID() + "\n");
          skipWords.write(key + "\n");
        }
      }
      skip.close();
      skipWords.close();

      logger.warn("skipped " + skipped + " of " + exercises.size() + " files " + files.size() + " e.g. " + files.iterator().next());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void dumpDirEnglish() {
    // logger.warn("audio dir " + audioDir + " lang " + language + " db " +dbName + " spreadsheet " + spreadsheet);

    Set<String> files = getFilesInBestDir("englishAudio");

    final String configDir = getConfigDir("english");

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      "english",
      configDir +
        "ESL_ELC_5071-30books_chapters.xlsx");

    writeMissingFiles(files, configDir, unitAndChapter);
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

    Map<String, List<Result>> idToResults = getIDToResultsMap(flatList, java.util.Collections.EMPTY_SET);
    logger.debug("convertEnglish : id->results size " + idToResults.size() + " e.g. " + idToResults.keySet().iterator().next());
    Set<Long> nativeUsers = new UserDAO(flatList).getNativeUsers();
    if (nativeUsers.isEmpty()) {
      logger.error("huh? no native users???");
    } else {
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
            } else {
              nonNativeRecordings++;
            }
          }
          if (!nativeResult) {
            if (count++ < 10) logger.warn("no native recordings for " + e.getID());
          }
        } else {
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

    Map<String, Exercise> idToEx = new TreeMap<String, Exercise>();
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
        configDir +
          "ESL_ELC_5071-30books_chapters.xlsx");

      for (Exercise e : unitAndChapter.getExercises()) {
        String key = e.getEnglishSentence().toLowerCase().trim();
        if (flastListEnglishToResults.containsKey(key)) {
          //logger.warn("found " +key + " : " + e.getID() + " "+ e.getEnglishSentence());
          List<Result> value = flastListEnglishToResults.get(key);
          chapterExerciseToResult.put(e, value);
          idToEx.put(e.getID(), e);
          if (value.isEmpty()) logger.warn("huh? no results for ex " + key); //never happen
          idToResults2.put(e.getID(), value);

          if (key.equals("complete")) {
            logger.warn("key " + key + " value " + value + " ex " + e.getID() + "");
          }
          //if (e.getID().equals("0")) {
          if (count2++ < 10)
            logger.warn("ex " + e.getID() +
              " key " + key + " value " + value + " ex " + e.getID() + " " + idToResults2.get(key));

          //}
        } else {
          if (skipped++ < 10)
            logger.warn("skipping " + e.getID() + " : " + key);// + " no match in " +englishToEx.size() + " entries.");
          skip.write(e.getID() + "\n");
          skipWords.write(key + "\n");
        }
        //  else englishToEx2.put(key,e);
      }
      skip.close();
      skipWords.close();
      logger.warn("overlap is " + idToEx.size() + "/" + chapterExerciseToResult.size() + "/" + idToResults2.size() +
        " out of " + unitAndChapter.getExercises().size() + " skipped " + skipped);
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }

    try {
      if (true) convertExamples(numThreads, audioDir, language, idToEx, idToResults2, nativeUsers, false);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

  protected Set<String> getFilesInBestDir(String audioDir) {
    final String placeToPutAudio = ".."+ File.separator+audioDir + File.separator;
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    String[] list = bestDir.list();
    logger.warn("in " +bestDir.getAbsolutePath() + " there are " + list.length);
    return new HashSet<String>(Arrays.asList(list));
  }

  protected void writeMissingFiles(Set<String> files, String configDir, DatabaseImpl unitAndChapter) {
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

  protected void recordMissing(FileWriter missingFast, FileWriter missingSlow, String name) {
    try {
      recordMissingFast(missingFast, name);
      recordMissingFast(missingSlow, name);
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}
