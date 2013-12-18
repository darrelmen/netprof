package mitll.langtest.server.autocrt;

import ag.experiment.AutoGradeExperiment;
import mira.classifier.Classifier;
import mitll.langtest.server.database.Export;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * AutoCRT support -- basically wrapping Jacob's work that lives in mira.jar <br></br>
 * Does some work to make a lm and lattice file suitable for doing small vocabulary decoding.
 *
 * User: GO22670
 * Date: 1/10/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AutoCRT {
  private static Logger logger = Logger.getLogger(AutoCRT.class);
  private static final String SERIALIZED_CLASSIFIER = "serializedClassifier.ser";

  public static final double CORRECT_THRESHOLD = 0.499;
  private static final boolean GET_MSA = false;
  private static final boolean TESTING = false; // this doesn't really work
  private Classifier<AutoGradeExperiment.Event> classifier = null;
  private Map<String, Export.ExerciseExport> exerciseIDToExport;
  private String installPath;
  private String mediaDir;
  private final Export exporter;
  private AutoCRTScoring db;
  private double minPronScore;

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#makeAutoCRT
   * @param exporter
   * @param db
   * @param installPath
   * @param relativeConfigDir
   * @param minPronScore
   */
  public AutoCRT(Export exporter, AutoCRTScoring db, String installPath, String relativeConfigDir, double minPronScore) {
    this.installPath = installPath;
    this.mediaDir = relativeConfigDir;
    this.exporter = exporter;
    this.db = db;
    this.minPronScore = minPronScore;
  }

  /**
   * Get an auto crt reco output and score given an audio answer.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, int, int, int, boolean, String, boolean)
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer(String, int, int, int, java.io.File, mitll.langtest.server.audio.AudioCheck.ValidityAndDur, String, boolean, mitll.langtest.client.LangTestDatabase)
   * @param exerciseID for this exercise
   * @param questionID and this question
   * @param audioFile score this file (
   * @param answer mark decode output, correctness, and whether they said something in the set of expected sentences
   */
  public void getAutoCRTDecodeOutput(String exerciseID, int questionID, File audioFile, AudioAnswer answer) {
    PretestScore asrScoreForAudio = getScoreForAudio(exerciseID, questionID, audioFile);
    markCorrectnessOnAnswer(exerciseID, questionID, asrScoreForAudio, answer);
  }

  private void markCorrectnessOnAnswer(String exerciseID, int questionID, PretestScore asrScoreForAudio, AudioAnswer answer) {
    String recoSentence = asrScoreForAudio.getRecoSentence();
    boolean lowPronScore = asrScoreForAudio.getHydecScore() < minPronScore;
    boolean matchedUnknown = recoSentence.equals(SmallVocabDecoder.UNKNOWN_MODEL);

    logger.info("markCorrectnessOnAnswer: took " + //(now - then) + " millis to get score "+
      " for " + exerciseID + "/" +questionID+
      //" given " +exportedAnswers.size() + " possible matches," +
      " reco sentence was '" + recoSentence + "', score " + asrScoreForAudio.getHydecScore() +
      (lowPronScore ? " score too low " : "") +
      (matchedUnknown ? " matched unknown model " : ""));

    if (matchedUnknown || lowPronScore) {
      //if (lowPronScore) logger.info("\t-----------> rejecting result since score too low");
      answer.setDecodeOutput("Unexpected word.");
      answer.setScore(asrScoreForAudio.getHydecScore());
      answer.setCorrect(false);
      answer.setSaidAnswer(false);
    } else {
      String annotatedResponse = getAnnotatedResponse(exerciseID, questionID, recoSentence);

      double scoreForAnswer = (recoSentence.length() > 0) ? getScoreForExercise(exerciseID, questionID, recoSentence) : 0.0d;

      answer.setDecodeOutput(annotatedResponse);
      answer.setScore(scoreForAnswer);
      boolean correct = scoreForAnswer > CORRECT_THRESHOLD;

      logger.info("markCorrectnessOnAnswer: for " + exerciseID +
        " reco sentence was '" + recoSentence + "' classifier score "+scoreForAnswer +
        " correct " + correct + " matched answer is "+ !matchedUnknown);
      answer.setCorrect(correct);
      answer.setSaidAnswer(!matchedUnknown);
    }
  }

  /**
   * Do decoding on an audio file over a set of possible reco sentences and the UNKNOWN model.
   * <p></p>
   * The possible sentences are student responses collected previously (exported answers from the student h2 db).
   * Filter out student answers that we can't decode given our dictionary (e.g. numbers "222").
   *
   * @see #getExportedAnswers(String, int)
   * @param exerciseID for this exercise
   * @param questionID for this question
   * @param audioFile decode this file
   * @return the reco sentence and the score for this sentence
   */
  private PretestScore getScoreForAudio(String exerciseID, int questionID, File audioFile) {
    Collection<String> exportedAnswers = getExportedAnswers(exerciseID, questionID);
    exportedAnswers = db.getValidPhrases(exportedAnswers);   // remove phrases that break hydec
    //logger.info("getAutoCRTDecodeOutput : got answers, num = " + exportedAnswers.size());
    long then = System.currentTimeMillis();

    PretestScore asrScoreForAudio = db.getASRScoreForAudio(audioFile, exportedAnswers);
    long now = System.currentTimeMillis();
    if (now-then > 100) {
      logger.info("getScoreForAudio : took " + (now - then) + " millis to get score " + asrScoreForAudio +
        " given" + exportedAnswers.size() +
        " answers for exercise " + exerciseID + "/" + questionID);
    }
    return asrScoreForAudio;
  }

  /**
   * Allow multiple correct answers from synonym sentence list.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer(String, int, int, int, java.io.File, mitll.langtest.server.audio.AudioCheck.ValidityAndDur, String, boolean, mitll.langtest.client.LangTestDatabase)
   * @see mitll.langtest.server.audio.AudioFileHelper#getFlashcardAnswer(mitll.langtest.shared.Exercise, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @param e
   * @param audioFile
   * @param answer
   */
  public void getFlashcardAnswer(Exercise e,
                                 File audioFile,
                                 AudioAnswer answer) {
    List<String> foregroundSentences = getRefSentences(e);
    List<String> foreground = new ArrayList<String>();
    for (String ref : foregroundSentences) {
      foreground.add(removePunct(ref));
    }
    int n = foreground.size();
    for (String synonym : getRefs(e.getSynonymSentences())) {
      if (!foregroundSentences.contains(synonym)) {
        foreground.add(removePunct(synonym));
      }
    }
    if (foreground.size() > n) logger.debug("Added " + e.getSynonymSentences() + " synonyms");

    logger.debug("getFlashcardAnswer : foreground " + foreground);
    PretestScore asrScoreForAudio = db.getASRScoreForAudio(audioFile, foreground);

    String recoSentence =
      asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
        asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    boolean isCorrect = recoSentence != null && isCorrect(foregroundSentences, recoSentence);
    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ? -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > minPronScore);
    answer.setSaidAnswer(isCorrect);
    if (!isCorrect) {
      logger.info("incorrect response for exercise #" +e.getID() +
        " reco sentence was '" + recoSentence + "' vs " + "'"+foregroundSentences +"' pron score was " + scoreForAnswer);
    }
    else {
      logger.info("correct response for exercise #" +e.getID() +
        " reco sentence was '" + recoSentence + "' vs " + "'"+foregroundSentences +"' pron score was " + scoreForAnswer + " answer " + answer);
    }

    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
  }

  private boolean isCorrect(List<String> answerSentences, String recoSentence) {
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.", "").toLowerCase();
      converted = removePunct(converted);
      if (converted.equalsIgnoreCase(recoSentence)) return true;
    }
    return false;
  }

  /**
   * @param other
   * @return
   */
  private List<String> getRefSentences(Exercise other) {
    List<String> refSentences = other.getRefSentences();
    return getRefs(refSentences);
  }

  private List<String> getRefs(List<String> refSentences) {
    List<String> refs = new ArrayList<String>();
    for (String ref : refSentences) {
      refs.add(ref.trim().toUpperCase());
    }
    return refs;
  }

  /**
   * Mark words in the response as right or wrong depending on their overlap with answers marked
   * good or bad.
   * Mainly for demo purposes.
   * @param exercise
   * @param questionID
   * @param recoSentence
   * @return to show to user
   */
  private String getAnnotatedResponse(String exercise, int questionID, String recoSentence) {
    List<String> good = new ArrayList<String>();
    List<String> bad = new ArrayList<String>();
    for (Export.ResponseAndGrade resp : getExportForExercise(exercise, questionID).rgs) {
      if (resp.grade >= 0.6) good.add(resp.response);
      else bad.add(resp.response);
    }

    Set<String> goodTokens = getTokenSet(good);
    Set<String> badTokens = getTokenSet(bad);

    StringBuilder sb = new StringBuilder();
    for (String recoToken : recoSentence.split("\\s")) {
      if (goodTokens.contains(recoToken)) {
        sb.append("<u>"+recoToken +"</u> ");
      } else if (badTokens.contains(recoToken)) {
        sb.append("<s>"+recoToken +"</s> ");
      } else sb.append(recoToken + " ");
    }
    return sb.toString().trim();
  }

  /**
   * For this exercise and question, score the answer.<br></br>
   * Do this by getting all other answers to this question and the answer key and given this information
   * and the answer, ask the classifier to score the answer.
   * @see mitll.langtest.server.audio.AudioFileHelper#getScoreForAnswer(mitll.langtest.shared.Exercise, int, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAnswer
   * @param e for this exercise
   * @param questionID for this question (when multiple questions in an exercise)
   * @param answer to score (correct->incorrect)
   * @return 0-1
   */
  public double getScoreForExercise(Exercise e, int questionID, String answer) {
    String exerciseID = e.getID();
    if (answer.isEmpty()) {
      logger.warn("huh? for exercise " + exerciseID + " question " + questionID + " answer is empty?");
      return 0d;
    }
    return getScoreForExercise(exerciseID, questionID, answer);
  }

  private double getScoreForExercise(String id, int questionID, String answer) {
    if (TESTING) return 0.1;
    getClassifier();
    String key = id + "_" + questionID;
    Export.ExerciseExport exerciseExport = getExportForExercise(key);
    if (exerciseExport == null) {
      logger.error("couldn't find exercise id " + key + " in " + exerciseIDToExport.keySet());
      return 0d;
    }
    else {
      long then = System.currentTimeMillis();

      double score = AutoGradeExperiment.getScore(getClassifier(), answer, exerciseExport);
      long now = System.currentTimeMillis();

      DecimalFormat format = new DecimalFormat("#.###");
      logger.info("AutoGradeExperiment : score was " + format.format(score) + " for answer '" +  answer+
        "' in context of " + exerciseExport +" and took " +(now-then) + " millis");
      return score;
    }
  }

  private Set<String> getTokenSet(List<String> exportedAnswers) {
    Set<String> tokens = new HashSet<String>();
    for (String l : exportedAnswers) {
      for (String t : l.split("\\s")) {
        String tt = removePunct(t);
        if (tt.trim().length() > 0) {
          tokens.add(tt.trim());
        }}}
    return tokens;
  }

  private String removePunct(String t) {
    return t.replaceAll("\\p{P}","");
  }

  /**
   * @see #getAutoCRTDecodeOutput
   * @param id
   * @param questionID
   * @return
   */
  private Collection<String> getExportedAnswers(String id, int questionID) {
    getClassifier();
    long then = System.currentTimeMillis();

    Set<String> answers = new TreeSet<String>();
    Export.ExerciseExport exportForExercise = getExportForExercise(id, questionID);
    if (exportForExercise != null) {
      for (Export.ResponseAndGrade resp : exportForExercise.rgs) {
        answers.add(resp.response);
      }
    }

    long now = System.currentTimeMillis();
    if (now-then > 100) {
      logger.info("getExportedAnswers : took " + (now - then) + " millis to get " + answers.size() +
        " possible answers for exercise " + id);
    }
    return answers;
  }

  private Export.ExerciseExport getExportForExercise(String id, int questionID) {
    return getExportForExercise(id + "_" + questionID);
  }
  private Export.ExerciseExport getExportForExercise(String key) {
    return exerciseIDToExport.get(key);
  }

  public void makeClassifier() { getClassifier(); }

  /**
   * Make a classifier given the export date, which has answers and their grades.<br></br>
   * The export data also includes the answer key for each question.<br></br>
   * This uses Jacob's classifier.
   * @see #getExportedAnswers(String, int)
   * @see #getScoreForExercise(String, int, String)
   *
   * @see Export.ExerciseExport
   * @see AutoGradeExperiment
   * @return a mira classifier
   */
  private Classifier<AutoGradeExperiment.Event> getClassifier() {
    if (classifier != null) return classifier;

    String configDir = (installPath != null ? installPath + File.separator : "") + mediaDir + File.separator;
    File serializedClassifier = new File(configDir, SERIALIZED_CLASSIFIER);
    if (serializedClassifier.exists()) {
      classifier = rehydrateClassifier(configDir, serializedClassifier);
      return classifier;
    } else {
      if (TESTING) {
        exerciseIDToExport = new HashMap<String, Export.ExerciseExport>();
        return null;
      } else if (GET_MSA) {
        List<Export.ExerciseExport> export = getExportedGradedItems();
        AutoGradeExperiment.runOverallModelOnExport(export);
        return null;
      } else {
        List<Export.ExerciseExport> export = getExportedGradedItems();
        readConfigFile(configDir);

        long then = System.currentTimeMillis();
        AutoGradeExperiment.saveClassifierAfterExport(export, serializedClassifier.getAbsolutePath());
        long now = System.currentTimeMillis();

        logger.info("took " + ((now - then) / 1000) + " seconds to saved classifier to " + serializedClassifier.getAbsolutePath());
        classifier = AutoGradeExperiment.getClassifierFromSavedModel(serializedClassifier.getAbsolutePath(), export);
        return classifier;
      }
    }
  }

  /**
   * TODO : this is overkill - we should serialize/rehydrate the model without calling the svm load/save code directly
   * @param configDir
   * @param serializedClassifier
   * @return
   */
  private Classifier<AutoGradeExperiment.Event> rehydrateClassifier(String configDir, File serializedClassifier) {
    logger.info("rehydrateClassifier : using previously calculated classifier.");
    long then = System.currentTimeMillis();

    List<Export.ExerciseExport> export = getExportedGradedItems();
    long now = System.currentTimeMillis();
    if (now-then > 100) {
      logger.info("rehydrateClassifier : took " + (now - then) + " millis to export " + export.size() + " items");
    }
    readConfigFile(configDir);
    then = now;
    Classifier<AutoGradeExperiment.Event> classifier =
      AutoGradeExperiment.getClassifierFromSavedModel(serializedClassifier.getAbsolutePath(), export);
     now = System.currentTimeMillis();
    long l = (now - then) / 1000;
    if (l > 60) {
      long min = l/60;
      l -= min*60;
      logger.warn("rehydrateClassifier : took " + min + " minutes, " + l + " seconds to rehydrate classifier");
    }
    else if (l > 1) {
      logger.debug("rehydrateClassifier : took " + l + " seconds to rehydrate classifier");
    }
    return classifier;
  }

  private void readConfigFile(String configDir) {
    String[] args = new String[6];

    String config = configDir + "runAutoGradeWinNoBad.cfg";     // TODO use template for deploy/platform specific config
    if (!new File(config).exists()) logger.error("readConfigFile : couldn't find " + config);
    args[0] = "-C";
    args[1] = config;
    args[2] = "-log";
    args[3] = configDir + "out.log";
    args[4] = "-blacklist-file";
    args[5] = configDir + "blacklist.txt";

    AutoGradeExperiment.main(args);
  }

  private List<Export.ExerciseExport> getExportedGradedItems() {
    long then = System.currentTimeMillis();

    List<Export.ExerciseExport> export = exporter.getExport(true, false);
    populateExportIdToExport(export);
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("getExportedGradedItems took " + (now - then) + " millis to get " + export.size() +
        " exported GradedItems items.");
    }
    return export;
  }

  private void populateExportIdToExport(List<Export.ExerciseExport> export) {
    exerciseIDToExport = new HashMap<String, Export.ExerciseExport>();
    for (Export.ExerciseExport exp : export) {
      exerciseIDToExport.put(exp.id, exp);
    }
  }
}
