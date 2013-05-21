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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
  private static Logger logger = Logger.getLogger(FileExerciseDAO.class);

  private boolean debug;
  private static final int MAX = 12000;
  private static final double MIN_DUR = 0.2;
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private AudioCheck audioCheck = new AudioCheck();

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
  private void convertExamples(int numThreads, String audioDir, String language,String spreadsheet) throws Exception{
    String installPath = ".";
    String dariConfig = File.separator +
      "war" +
      File.separator +
      "config" +
      File.separator +
      language +
      File.separator;
    final String configDir = installPath + dariConfig;
    DatabaseImpl db = new DatabaseImpl(
      configDir,
      "template",
      configDir+
        spreadsheet);

    final Map<String,Exercise> idToEx = new HashMap<String, Exercise>();
    final FileWriter missingSlow = new FileWriter(configDir + "missingSlow.txt");
    final FileWriter missingFast = new FileWriter(configDir + "missingFast.txt");

    List<Exercise> exercises = db.getExercises();
    logger.debug("Got " + exercises.size() + " exercises");
    for (Exercise e: exercises) idToEx.put(e.getID(),e);

    Map<String, List<Result>> idToResults = getIDToResultsMap(db);

    if (true) {
      for (String exid : idToEx.keySet()) {
        String resultID = exid + "/0";
        if (!idToResults.containsKey(resultID)) {
          idToResults.put(resultID, new ArrayList<Result>());
        }
      }
      if (exercises.size() != idToResults.size()) logger.error("\n\n\nhuh? id->results map size " + idToResults.size());
    }

    final String placeToPutAudio = ".."+File.separator+audioDir + File.separator;
    final File newRefDir = new File(placeToPutAudio + "refAudio");
    newRefDir.mkdir();
    final File bestDir = new File(placeToPutAudio + "bestAudio");
    bestDir.mkdir();


    final Map<String, String> properties = getProperties(language, configDir);
    ASRScoring scoring = getAsrScoring(".",null,properties);

//    checkLTS(exercises, scoring.getLTS());

    final HTKDictionary dict = scoring.getDict();

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    List<Future<?>> futures = getSplitAudioFutures(idToEx, missingSlow, missingFast, idToResults, placeToPutAudio,
      newRefDir, bestDir,
      properties, dict, executorService);

    blockUntilComplete(futures);

    missingFast.close();
    missingSlow.close();
    logger.info("closing missing slow");
    executorService.shutdown();
  }

  private void blockUntilComplete(List<Future<?>> futures) throws InterruptedException, ExecutionException {
    logger.info("got " +futures.size() + " futures");
    for (Future<?> future : futures) {
      Object o = future.get();
    }
    logger.info("all " +futures.size() + " futures complete");
  }

  private List<Future<?>> getSplitAudioFutures(final Map<String, Exercise> idToEx, final FileWriter missingSlow, final FileWriter missingFast, Map<String, List<Result>> idToResults, final String placeToPutAudio, final File newRefDir, final File bestDir, final Map<String, String> properties, final HTKDictionary dict, ExecutorService executorService) {
    List<Future<?>> futures = new ArrayList<Future<?>>();
    for (final Map.Entry<String, List<Result>> pair : idToResults.entrySet()) {
      Future<?> submit = executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            getBestForEachExercise(idToEx, missingSlow, missingFast,
              newRefDir, bestDir, pair,placeToPutAudio,dict, properties);
          } catch (IOException e) {
            logger.error("Doing " + pair.getKey() + " and " + pair.getValue() +
              " Got " + e, e);
          }
        }
      });
      futures.add(submit);
    }
    return futures;
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
      if (i < MAX //&& i < 101
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

  private void getBestForEachExercise(Map<String, Exercise> idToEx, FileWriter missingSlow, FileWriter missingFast,
                                      File newRefDir, File bestDir, Map.Entry<String, List<Result>> pair,
                                      String collectedAudioDir, HTKDictionary dictionary,Map<String, String> properties) throws IOException {
    List<Result> resultsForExercise = pair.getValue();
    if (resultsForExercise.isEmpty()) return;
    String exid = resultsForExercise.iterator().next().id;
    Exercise exercise = idToEx.get(exid);

    String refSentence = exercise.getRefSentence();
    refSentence = refSentence.replaceAll("\\p{P}", "");
    String[] split = refSentence.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    refSentence = getRefSentence(split);
    int refLength = split.length;
    String firstToken = split[0].trim();
    String lastToken = split[refLength-1].trim();
    logger.debug("refSentence " + refSentence + " length " + refLength + " first |" + firstToken + "| last |" +lastToken +"|");

    File refDirForExercise = new File(newRefDir, exid);
    String key = pair.getKey();
    if (key.equals(exid)) logger.error("huh?> not the same " + key + "  and " + exid);
    //logger.debug("making dir " + key + " at " + refDirForExercise.getAbsolutePath());
    refDirForExercise.mkdir();

    File bestDirForExercise = new File(bestDir, exid);
    logger.debug("making dir " + key + " at " + bestDirForExercise.getAbsolutePath());
    bestDirForExercise.mkdir();
    final ASRScoring scoring = getAsrScoring(".",dictionary,properties);
    String best = getBestFilesFromResults(scoring, resultsForExercise, exercise, refSentence,
      firstToken, lastToken,
      refLength, refDirForExercise,collectedAudioDir);

    if (best != null) {
      logger.debug("for " +key +
        " best is " + best);// + " total " + bestTotal);
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

      logger.debug("parent " + parent + " running result " + r.uniqueID + " for exercise " + id + " and audio file " + name);

      Scores align = scoring.align(parent, testAudioFileNoSuffix, refSentence + " " + refSentence);
      //  logger.debug("\tgot " + align + " for " + name);
      float hydecScore = align.hydecScore;

      String wordLabFile = prependDeploy(parent,testAudioFileNoSuffix + ".words.lab");
      try {
        GetAlignments getAlignments = new GetAlignments(first,last, refLength, name, wordLabFile).invoke();
   /*       float fastScore = getAlignments.getFastScore();
          float slowScore = getAlignments.getSlowScore();

          if (fastScore > bestFast) {
            bestFast = fastScore;
          }
          if (slowScore > bestSlow) {
            bestSlow = slowScore;
          }*/
        boolean valid = getAlignments.isValid();
        if (!valid) {
          logger.warn("\n-----------> ex " + id + " score " + hydecScore + "  couldn't find start and end ");
        }
        if (bestTotal < hydecScore && valid && hydecScore > 0.1f) {//fastScore + slowScore) {
          bestTotal = hydecScore;
          best = testAudioFileNoSuffix;

          logger.debug("ex " + id+ " best so far is  " + best + " score " + bestTotal + " hydecScore " + hydecScore);
          //  " fast " + fastScore + "/" + slowScore);
          writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            getAlignments);

        }
       /* if (valid) {//getAlignments.isValid()) {
          writeTheTrimmedFiles(refDirForExercise, parent, (float) durationInSeconds, testAudioFileNoSuffix,
            getAlignments);
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
      logger.debug("dur of " + answer2 + " is " + durationInSeconds + " seconds");
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
    private String name;
    private String wordLabFile;
    private float start1;
    private float end1;
    private float start2;
    private float end2;
    private float fastScore;
    private float slowScore;

    private int starts,ends;

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
      return starts >= 2 && ends >= 2;
    }
/*
    public float getFastScore() {
      return fastScore;
    }

    public float getSlowScore() {
      return slowScore;
    }
*/

    public GetAlignments invoke() throws IOException {
      SortedMap<Float,TranscriptEvent> timeToEvent = new TranscriptReader().readEventsFromFile(wordLabFile);

      start1 = -1;
      end1 = -1;
      start2 = -1;
      end2 = -1;
      boolean didFirst = false;

      int tokenCount = 0;
      float scoreTotal1 = 0, scoreTotal2 = 0;

      for (Map.Entry<Float, TranscriptEvent> timeEventPair : timeToEvent.entrySet()) {
        TranscriptEvent transcriptEvent = timeEventPair.getValue();
        String word = transcriptEvent.event.trim();
        boolean start = word.equals("<s>");
        if (start) starts++;
        boolean end = word.equals("</s>");
        if (end) ends++;

        if (start || end) continue;

        if (debug) logger.debug("\ttoken " + tokenCount + " got " + word + " and " + transcriptEvent);

        if (!didFirst) scoreTotal1 += transcriptEvent.score; else scoreTotal2 += transcriptEvent.score;
        tokenCount++;
        if (tokenCount == 1 && first.equals(word)) {
          if (!didFirst) {
            start1 = transcriptEvent.start;
          } else {
            start2 = transcriptEvent.start;
          }
          if (debug) logger.debug("\t1 token " + tokenCount + " vs " + (refLength-1));

        }
        if (tokenCount == refLength && last.equals(word)) {
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

  public static void main(String [] arg) {
    try {
      int numThreads = Integer.parseInt(arg[0]);
      String audioDir = arg[1];
      new SplitAudio().convertExamples(numThreads, audioDir, arg[2], arg[3]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
