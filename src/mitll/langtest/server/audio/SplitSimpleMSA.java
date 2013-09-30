package mitll.langtest.server.audio;

import corpus.HTKDictionary;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ExcelImport;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SplitSimpleMSA extends SplitAudio {
 // public static final float LOW_SCORE_THRESHOLD = 0.2f;
  private static Logger logger = Logger.getLogger(SplitSimpleMSA.class);
  protected static final float MSA_MIN_SCORE = 0.2f;

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
      if (valid && hydecScore > Mandarin.LOW_SCORE_THRESHOLD) {
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

  protected void recordMissing(FileWriter missingFast, FileWriter missingSlow, String name) {
    try {
      recordMissingFast(missingFast, name);
      recordMissingFast(missingSlow, name);
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}
