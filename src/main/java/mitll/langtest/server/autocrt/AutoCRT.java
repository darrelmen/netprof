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

package mitll.langtest.server.autocrt;

import ag.experiment.AutoGradeExperiment;
import mira.classifier.Classifier;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.DecoderOptions;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.database.export.Export;
import mitll.langtest.server.export.ExerciseExport;
import mitll.langtest.server.export.ResponseAndGrade;
import mitll.langtest.server.scoring.AlignDecode;
import mitll.langtest.server.scoring.InDictFilter;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AutoCRT support -- basically wrapping Jacob's work that lives in mira.jar <br></br>
 * Does some work to make a lm and lattice file suitable for doing small vocabulary decoding.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/10/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AutoCRT {
  private static final Logger logger = LogManager.getLogger(AutoCRT.class);
  private static final String SERIALIZED_CLASSIFIER = "serializedClassifier.ser";

  private static final double CORRECT_THRESHOLD = 0.499;
  private static final String UNEXPECTED_WORD = "Unexpected word.";
  private final String miraURL;
  private final boolean useMiraClassifier;

  private Classifier<AutoGradeExperiment.Event> classifier = null;
  private Map<String, ExerciseExport> exerciseIDToExport;
  private Map<String, ExerciseExport> exerciseIDToExportTest;
  private final String installPath;
  private final String mediaDir;
  private final Export exporter;
  private final AlignDecode autoCRTScoring;
  private final InDictFilter inDictFilter;
  private final double minPronScore;
  private final SmallVocabDecoder svd = new SmallVocabDecoder();
  private final String miraFlavor;
  private BufferedWriter writer;

  private final boolean doEighty = false;
  private final boolean usePreDefOnly = false;
  boolean comparisonTesting = false;
  protected ServerProperties serverProperties;

  /**
   * @param db
   * @param minPronScore
   * @param useMiraClassifier
   * @see mitll.langtest.server.audio.AudioFileHelper#makeClassifier
   */
  public AutoCRT(Export exporter, AlignDecode db, InDictFilter inDictFilter,
                 String installPath, String relativeConfigDir, double minPronScore,
                 String miraFlavor, String miraURL, boolean useMiraClassifier,
                 ServerProperties serverProperties) {
    this.installPath = installPath;
    this.mediaDir = relativeConfigDir;
    this.exporter = exporter;
    this.autoCRTScoring = db;
    this.inDictFilter = inDictFilter;
    this.minPronScore = minPronScore;
    this.miraFlavor = miraFlavor;
    this.miraURL = miraURL;
    this.useMiraClassifier = useMiraClassifier;
    this.serverProperties = serverProperties;
    if (comparisonTesting) {
      File file = getReportFile(miraFlavor);
      logger.debug("wrote to " + file.getAbsolutePath());
      try {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
        writer.write("id,questionID,response," +
            "score," +
            "expectedGrade," +
            "expectedCorrect," +

            "jacobClassScore," +
            "jacobClassCorrect," +
            "jacobError,jacobABSError,jacobSquaredError," +
            "jacobRight," +

            "miraClassRegressed," +
            "miraClassGrade," +
            "miraClassCorrect," +
            "miraError,miraABSError,miraSquaredError," +
            "miraRight," +

            "answerKeySize" +
            //"," +
            //"answerKey" +
            "\n");
      } catch (IOException e) {
        logger.error("got " + e.getMessage(), e);
      }
    }
  }

  public static File getReportFile(String prefix) {
    String suffix = "scores.csv";
    return getReportFile(prefix, suffix);
  }

  public static File getReportFile(String prefix, String suffix) {
    String today = getTodayMillis();
    return getReportFile(new PathHelper("war", null), today, prefix, suffix);
  }

  private static String getTodayMillis() {
    return getToday() + "_" + System.currentTimeMillis();
  }

  /**
   * ONLY for testing.
   *
   * @seex MiraTest#testClassifier
   */
  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static File getReportFile(PathHelper pathHelper, String today, String prefix, String suffix) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = prefix + "_report_" + today + "_" + suffix;
    return new File(reports, fileName);
  }

  private static String getToday() {
    return new SimpleDateFormat("MM_dd_yy").format(new Date());
  }

  /**
   * Get an auto crt reco output and score given an audio answer.
   *
   * @param exercise   for this exercise
   * @param questionID and this question
   * @param audioFile  score this file (
   * @param answer     mark decode output, correctness, and whether they said something in the set of expected sentences
   * @param useCache
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public void getAutoCRTDecodeOutput(AmasExerciseImpl exercise, int questionID, File audioFile, AudioAnswer answer,
                                     boolean useCache) {
    String exerciseID = exercise.getOldID();
    PretestScore asrScoreForAudio = getScoreForAudio(exercise, exerciseID, questionID, audioFile, useCache);
    if (asrScoreForAudio == null) {
      logger.error("can't find pretest for " + exerciseID + "/" + questionID);
    } else {
      markCorrectnessOnAnswer(exercise, questionID, asrScoreForAudio, answer);
    }
  }

  /**
   * So if the audio decoded to the unknown model, we mark it as incorrect and return "Unexpected Word".
   * If not, we ask the classifier whether text was correct, in the context of the exercise.
   *
   * @param exercise
   * @param questionID
   * @param asrScoreForAudio
   * @param answer
   */
  private void markCorrectnessOnAnswer(AmasExerciseImpl exercise, int questionID, PretestScore asrScoreForAudio,
                                       AudioAnswer answer) {
    String exerciseID = exercise.getOldID();
    String recoSentence = asrScoreForAudio.getRecoSentence();
    boolean lowPronScore = asrScoreForAudio.getHydecScore() < minPronScore;
    boolean matchedUnknown = recoSentence.equals(SLFFile.UNKNOWN_MODEL);

    if (asrScoreForAudio.isRanNormally()) {
      logger.info("markCorrectnessOnAnswer:" + //(now - then) + " millis to get score "+
          " for " + exerciseID + "/" + questionID +
          //" given " +exportedAnswers.size() + " possible matches," +
          " reco sentence was '" + recoSentence + "', score " + asrScoreForAudio.getHydecScore() +
          (lowPronScore ? " score too low " : "") +
          (matchedUnknown ? " matched unknown model " : ""));
    }
    answer.setPretestScore(asrScoreForAudio);
    if (matchedUnknown || lowPronScore) {
      //if (lowPronScore) logger.info("\t-----------> rejecting result since score too low");
      answer.setDecodeOutput(matchedUnknown ? UNEXPECTED_WORD : getAnnotatedResponse(exerciseID, questionID, recoSentence));
      answer.setScore(0);
      answer.setCorrect(false);
      answer.setSaidAnswer(false);
    } else {
      String annotatedResponse = getAnnotatedResponse(exerciseID, questionID, recoSentence);

      CRTScores scoreForAnswer = recoSentence.isEmpty() ? new CRTScores() :
          getScoreForExercise(exercise, questionID, recoSentence);

      answer.setDecodeOutput(annotatedResponse);
      answer.setScore(scoreForAnswer.oldScore);
      boolean correct = isCorrect(scoreForAnswer);
      answer.setCorrect(correct);
      answer.setSaidAnswer(true);

      logger.info("markCorrectnessOnAnswer: for " + exerciseID +
          " reco sentence was '" + recoSentence + "' classifier score " + scoreForAnswer +
          " correct " + correct);
    }
  }

  private boolean isCorrect(CRTScores scoreForAnswer) {
    double oldScore = scoreForAnswer.oldScore;
    return isCorrect(oldScore);
  }

  private boolean isCorrect(double oldScore) {
    return oldScore > CORRECT_THRESHOLD;
  }

  /**
   * Do decoding on an audio file over a set of possible reco sentences and the UNKNOWN model.
   * <p></p>
   * The possible sentences are student responses collected previously (exported answers from the student h2 autoCRTScoring).
   * Filter out student answers that we can't decode given our dictionary (e.g. numbers "222").
   *
   * @param exerciseID for this exercise
   * @param questionID for this question
   * @param audioFile  decode this file
   * @param useCache
   * @return the reco sentence and the score for this sentence
   * @see #getExportedAnswers(String, int)
   */
  private PretestScore getScoreForAudio(AmasExerciseImpl exercise, String exerciseID, int questionID, File audioFile, boolean useCache) {
    Collection<String> exportedAnswersOrig = getPredefAnswers(exercise, questionID - 1);
    if (exportedAnswersOrig == null) logger.warn("getScoreForAudio : can't find " + exerciseID + "/" + questionID);
    Collection<String> exportedAnswers = inDictFilter.getValidPhrases(exportedAnswersOrig);   // remove phrases that break hydec
    if (exportedAnswers == null)
      logger.warn("getScoreForAudio : can't find valid phrases for  " + exerciseID + "/" + questionID);

    int size = exportedAnswers == null ? 0 : exportedAnswers.size();
//    int size1 = exportedAnswersOrig == null ? 0 : exportedAnswersOrig.size();
    // logger.info("getScoreForAudio : got possible answers, num = " + size + " vs orig " + size1);
    long then = System.currentTimeMillis();

    PretestScore asrScoreForAudio = autoCRTScoring.getASRScoreForAudio(audioFile, exportedAnswers, exercise.getTransliteration(),
        new DecoderOptions().setCanUseCache(useCache), null);
    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("getScoreForAudio : took " + (now - then) + " millis to get score " + asrScoreForAudio +
          " given" + size +
          " answers for exercise " + exerciseID + "/" + questionID);
    }
    return asrScoreForAudio;
  }

  /**
   * @param exercise
   * @param questionID
   * @return
   * @see #getScoreForAudio(AmasExerciseImpl, String, int, File, boolean)
   */
  private Collection<String> getPredefAnswers(AmasExerciseImpl exercise, int questionID) {
    QAPair q = exercise.getQuestions().get(questionID);
    String question = q.getQuestion();
    List<String> possible = new ArrayList<>();
    //  String answer1 = q.getAnswer();
    possible.add(question);

    for (String answer : q.getAlternateAnswers()) {
      String removed = removePunct(answer.trim());
      if (answer.trim().isEmpty() || removed.isEmpty()) {
        logger.warn("huh? alternate answer is empty??? for " + q + " in " + exercise.getOldID());
        logger.warn("exercise " + exercise);
      } else {
//        if (answer.length() < MIN_LENGTH) {
//          logger.warn("for short answer " + answer + " len " + answer.length() +
//              " added " + new ResponseAndGrade(answer, correctGrade, correctGrade) + " for " + q);
//        }
        possible.add(answer);
//            count++;
      }
    }
    return possible;
  }
  /**
   * Decode the phrase from the exercise in {@link mitll.langtest.shared.CommonExercise#getForeignLanguage}
   *
   * @param canUseCache
   * @param commonExercise
   * @param audioFile
   * @param answer
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   * @see mitll.langtest.server.audio.AudioFileHelper#getFlashcardAnswer(CommonExercise, File, AudioAnswer)
   */
/*  public PretestScore getFlashcardAnswer(CommonExercise commonExercise, File audioFile, AudioAnswer answer,
                                         String language) {
    Collection<String> foregroundSentences = getRefSentences(commonExercise, language);
    PretestScore flashcardAnswer = getFlashcardAnswer(audioFile, foregroundSentences, answer);

    // log what happened
    if (answer.isCorrect()) {
      logger.info("correct response for exercise #" + commonExercise.getOldID() +
          " reco sentence was '" + answer.getDecodeOutput() + "' vs " + "'" + foregroundSentences + "' " +
          "pron score was " + answer.getScore() + " answer " + answer);
    } else {
      int length = foregroundSentences.isEmpty() ? 0 : foregroundSentences.iterator().next().length();
      logger.info("getFlashcardAnswer : incorrect response for exercise #" + commonExercise.getOldID() +
          " reco sentence was '" + answer.getDecodeOutput() + "' (" + answer.getDecodeOutput().length() +
          ") vs " + "'" + foregroundSentences + "' (" + length +
          ") pron score was " + answer.getScore());
    }
    return flashcardAnswer;
  }*/

  /**
   * So we need to process the possible decode sentences so that hydec can handle them.
   * <p>
   * E.g. english is in UPPER CASE.
   * <p>
   * Decode result is correct if all the tokens match (ignore case) any of the possibleSentences AND the score is
   * above the {@link #minPronScore} min score, typically in the 30s.
   * <p>
   * If you want to see what the decoder output was, that's in {@link mitll.langtest.shared.AudioAnswer#getDecodeOutput()}.
   * For instance if you wanted to show that for debugging purposes.
   * If you want to know whether the said the right word or not (which might have scored too low to be correct)
   * see {@link mitll.langtest.shared.AudioAnswer#isSaidAnswer()}.
   *
   * @param audioFile         to score against
   * @param possibleSentences any of these can match and we'd call this a correct response
   * @param answer            holds the score, whether it was correct, the decode output, and whether one of the
   *                          possible sentences
   * @return PretestScore word/phone alignment with scores
   * @see #getFlashcardAnswer
   */
 /* private PretestScore getFlashcardAnswer(File audioFile, Collection<String> possibleSentences, AudioAnswer answer) {
    PretestScore asrScoreForAudio = autoCRTScoring.getASRScoreForAudio(audioFile, removePunct(possibleSentences));

    String recoSentence =
        asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
            asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    // logger.debug("recoSentence is " + recoSentence + " (" +recoSentence.length()+ ")");

    boolean isCorrect = isCorrect(possibleSentences, recoSentence);
    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ? -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > minPronScore);
    answer.setSaidAnswer(isCorrect);
    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
    return asrScoreForAudio;
  }*/

  /**
   * Is the reco sentence the same as any of the possible answer sentences?
   * <p>
   * Convert dashes into spaces and remove periods, and other punct
   *
   * @param answerSentences
   * @param recoSentence
   * @return
   * @see #getFlashcardAnswer
   */
/*  private boolean isCorrect(Collection<String> answerSentences, String recoSentence) {
    // logger.debug("iscorrect - answer " + answerSentences + " vs " + recoSentence);
    List<String> recoTokens = svd.getTokens(recoSentence);
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.\\.\\.", " ").replaceAll("\\.", "").replaceAll(":", "").toLowerCase();
      List<String> answerTokens = svd.getTokens(converted);
      if (answerTokens.size() == recoTokens.size()) {
        boolean same = true;
        for (int i = 0; i < answerTokens.size() && same; i++) {
          String s = answerTokens.get(i);
          String anotherString = recoTokens.get(i);
          //    logger.debug("comparing '" + s + "' " +s.length()+ " to '" + anotherString  +"' "  +anotherString.length());
          same = s.equalsIgnoreCase(anotherString);
          // if (!same) {
          //logger.debug("comparing '" + s + "' " + s.length() + " to '" + anotherString + "' " + anotherString.length());
          //  }
        }
        if (same) return true;
      }
      //else {
      //logger.debug("not same number of tokens " + answerTokens + " " + answerTokens.size() + " vs " + recoTokens + " " + recoTokens.size());
      // }
    }
    return false;
  }*/

  /**
   * @param other
   * @return
   * @see #getFlashcardAnswer
   */
/*  private Collection<String> getRefSentences(CommonExercise other, String language) {
    String foreignLanguage = other.getForeignLanguage();
    String phraseToDecode = getPhraseToDecode(foreignLanguage, language);
    return Collections.singleton(phraseToDecode);
  }*/

  /**
   * Special rule for mandarin - break it up into characters
   *
   * @param rawRefSentence
   * @param language
   * @return
  /*   *//*
  private String getPhraseToDecode(String rawRefSentence, String language) {
    return language.equalsIgnoreCase("mandarin") && !rawRefSentence.trim().equalsIgnoreCase(SLFFile.UNKNOWN_MODEL) ?
        Scoring.getSegmented(rawRefSentence.trim().toUpperCase()) :
        rawRefSentence.trim().toUpperCase();
  }*/

/*
  private List<String> removePunct(Collection<String> possibleSentences) {
    List<String> foreground = new ArrayList<String>();
    for (String ref : possibleSentences) {
      foreground.add(removePunct(ref));
    }
    return foreground;
  }
*/

  /**
   * Do some attempt at marking the right and wrong tokens in the response -
   * TODO : not sure how good an idea this is...  UH, for instance, what if the tokens appear in both right and wrong answers???
   *
   * @param exercise
   * @param questionID
   * @param recoSentence
   * @return
   */

  private String getAnnotatedResponse(String exercise, int questionID, String recoSentence) {
    List<String> good = new ArrayList<String>();
    List<String> bad = new ArrayList<String>();
    for (ResponseAndGrade resp : getExportForExercise(exercise, questionID).rgs) {
      if (resp.grade >= 0.6) good.add(resp.response);
      else bad.add(resp.response);
    }

    Set<String> goodTokens = getTokenSet(good);
    Set<String> badTokens = getTokenSet(bad);
    Set<String> defGood = new HashSet<String>(good);
    defGood.removeAll(badTokens); // only good tokens!
    badTokens.removeAll(goodTokens); // only bad tokens

    StringBuilder sb = new StringBuilder();
    for (String recoToken : recoSentence.split("\\s")) {
      if (defGood.contains(recoToken)) {
        sb.append("<u>" + recoToken + "</u> ");
      } else if (badTokens.contains(recoToken)) {
        sb.append("<s>" + recoToken + "</s> ");
      } else {
        sb.append(recoToken + " ");
      }
    }
    return sb.toString().trim();
  }

  /**
   * Score a text response.
   *
   * @param exercise
   * @param questionID
   * @param answer
   * @return
   * @seex #markCorrectnessOnAnswer(CommonExercise, int, PretestScore, AudioAnswer)
   * @seex mitll.langtest.server.audio.AudioFileHelper#getScoreForAnswer(CommonExercise, int, String)
   */
  public CRTScores getScoreForExercise(AmasExerciseImpl exercise, int questionID, String answer) {
    return getScoreForExercise(exercise, questionID, answer, -1);
  }

  /**
   * Only public for testing...
   * <p>
   * For this exercise and question, score the answer.<br></br>
   * Do this by getting all other answers to this question and the answer key and given this information
   * and the answer, ask the classifier to score the answer.
   *
   * @param exercise      for this exercise
   * @param questionID    for this question (when multiple questions in an exercise)
   * @param answer        to score (correct->incorrect)
   * @param expectedGrade only valid for predef answer key items (5) or questions (1)
   * @return 0-1
   * @seex #getScoreForExercise(CommonExercise, int, String)
   */
  public CRTScores getScoreForExercise(AmasExerciseImpl exercise, int questionID, String answer, float expectedGrade) {
    getClassifier();
    String id = exercise.getOldID();
    String key = id + "_" + questionID;
    ExerciseExport exerciseExport = getExportForExercise(key);

//    logger.info("getScoreForExercise export for " + key + " " + exerciseExport);

    DecimalFormat format = new DecimalFormat("#.###");

    if (exerciseExport == null) {
      logger.error("couldn't find exercise id " + key + " in " + exerciseIDToExport.keySet());
      return new CRTScores();
    } else {
      List<String> completelyCorrect = new ArrayList<>();

      for (ResponseAndGrade rg : exerciseExport.rgs) {
        if (rg.grade > 0.9) completelyCorrect.add(rg.response);
      }
      //    List<String> completelyCorrect = exerciseExport.rgs.stream().filter(rg -> rg.grade > 0.9).map(rg -> rg.response).collect(Collectors.toList());
      //    long then = System.currentTimeMillis();
      String answer1 = answer.trim();
      if (removePunct(answer1).isEmpty()) {
        logger.warn("huh = got empty answer?");
        return new CRTScores();
      }
      //     List<Float> grades = new ArrayList<>();
      //    for (ResponseAndGrade g : exerciseExport.rgs) grades.add(g.grade);

 /*     logger.debug("checking for " + key + " grading " +
          //answer1 +
          " (" + answer1.length() + ") expected " + expectedGrade + " given " + exerciseExport.id +
          " keys  " + exerciseExport.key +  " rg " + exerciseExport.rgs.size() + " : " + grades);*/

      // TODO : don't use Jacob classifier for now - 2/1/16 GWFV
      double classScore = 0;//AutoGradeExperiment.getScore(getClassifier(), answer1, exerciseExport);

      long now = System.currentTimeMillis();
      // List<ResponseAndGrade> rgs = exerciseExport.rgs;

      // Optional<ResponseAndGrade> first = exerciseExport.rgs.stream().filter(rg -> rg.response.trim().equals(answer1)).findFirst();
      ResponseAndGrade first = null;
      for (ResponseAndGrade rg : exerciseExport.rgs) {
        if (rg.response.trim().equals(answer1)) {
          first = rg;
          break;
        }
      }

//      if (usePreDefOnly && !first.isPresent()) {
//        ExerciseExport exportForExerciseTest = getExportForExerciseTest(key);
//        classScore = AutoGradeExperiment.getScore(getClassifier(), answer1, exportForExerciseTest);
//        first = exportForExerciseTest.rgs.stream().filter(rg -> rg.response.trim().equals(answer1)).findFirst();
//      }

      MiraClassifier.Info info = useMiraClassifier ?
          new MiraClassifier().getMiraScore(exercise, questionID, answer, miraFlavor, miraURL, completelyCorrect) :
          new MiraClassifier.Info(0, 0, 0);
      Double miraClassScore = info.getGrade();
      CRTScores crtScores;
      boolean present = first != null;
      if (present) {
        //ResponseAndGrade rg = first;
        double grade = first.grade;
        crtScores = new CRTScores(grade, miraClassScore, classScore);
        // logger.debug("----> clamping score since found graded result - " + rg + " for " + id + " class score " + classScore);
        try {
          double diff = grade - classScore;
          double mdiff = grade - info.getRegressed();
          String response = first.response.trim();
          boolean expectedCorrect = isCorrect(expectedGrade);
          boolean jacobCorrect = isCorrect(classScore);
          boolean miraCorrect = isCorrect(miraClassScore);
          writer.write(id + "," + questionID + ",\"" + response + "\"," +
              grade + "," +
              expectedGrade + "," +
              (expectedCorrect ? "1" : "0") + "," +

              format.format(classScore) + "," +
              (jacobCorrect ? "1" : "0") + "," +
              format.format(diff) + "," +
              format.format(Math.abs(diff)) + "," +
              format.format(diff * diff) + "," +
              (expectedCorrect == jacobCorrect ? "1" : "0") + "," +

              format.format(info.getRegressed()) + "," +
              format.format(miraClassScore) + "," +

              (miraCorrect ? "1" : "0") + "," +
              format.format(mdiff) + "," +
              format.format(Math.abs(mdiff)) + "," +
              format.format(mdiff * mdiff) + "," +
              (expectedCorrect == miraCorrect ? "1" : "0") + "," +


              info.getAnswerKeySize() +
              //","+
              //exercise.getForeignLanguageQuestions().get(questionID-1).getAllAnswers()+
              "\n");
          writer.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
        //   break;
        // } else {
        //logger.debug("no match '" + response + "' vs answer '" + answer1 + "'");
        // }
      } else {
        crtScores = new CRTScores(classScore, miraClassScore, classScore);
        try {
          logger.info("----> not clamping score since didn't find graded result  for " + id + " answer = '" + answer1 + "'");
          writer.write(id + "," + questionID + "," + "" + "," + format.format(classScore) + "," +
              format.format(miraClassScore) + "," +
              format.format(info.getRegressed()) + "," +
              "-1" + "," + expectedGrade + ",0,0,0" +
              "\n");
          writer.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return crtScores;
    }
  }

  public static class CRTScores {
    private final double oldScore;
    private final double newScore;
    private double jacobScore;
    private boolean valid = false;

    public CRTScores() {
      this.oldScore = 0;
      this.newScore = 0;
      valid = false;
    }

    public CRTScores(double oldScore, double newScore, double jacobScore) {
      this.oldScore = oldScore;
      this.newScore = newScore;
      this.jacobScore = jacobScore;
      this.valid = true;
    }

    public double getOldScore() {
      return oldScore;
    }

    public double getNewScore() {
      return newScore;
    }

    public boolean isValid() {
      return valid;
    }

    @Override
    public String toString() {
      return "CRT Scores jacob class " + jacobScore +
          " old " + oldScore + " new " + newScore + " diff " + (newScore - oldScore) + " diffJacob " + (newScore - jacobScore) +
          " valid " + valid;
    }
  }

  /**
   * @param exportedAnswers
   * @return
   * @see #getAnnotatedResponse
   */
  private Set<String> getTokenSet(List<String> exportedAnswers) {
    Set<String> tokens = new HashSet<String>();
    for (String l : exportedAnswers) {
      for (String t : l.split("\\s")) {
        String tt = removePunct(t);
        if (tt.trim().length() > 0) {
          tokens.add(tt.trim());
        }
      }
    }
    return tokens;
  }

  /**
   * Replace elipsis with space. Then remove all punct.
   *
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t.replaceAll("\\.\\.\\.", " ").replaceAll("\\p{P}", "");
  }

  /**
   * @param id
   * @param questionID
   * @return
   * @see #getScoreForAudio
   */
  private Collection<String> getExportedAnswers(String id, int questionID) {
    getClassifier();
    long then = System.currentTimeMillis();

    Set<String> answers = new TreeSet<String>();
    ExerciseExport exportForExercise = getExportForExercise(id, questionID);
    if (exportForExercise != null) {
      // logger.debug("found " + exportForExercise.rgs.size() + " graded responses");
      for (ResponseAndGrade resp : exportForExercise.rgs) {
        answers.add(resp.response);
      }
    } else {
      logger.warn("no graded responses for " + id + "/" + questionID);
    }

    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("getExportedAnswers : took " + (now - then) + " millis to get " + answers.size() +
          " possible answers for exercise " + id);
    }
    //logger.info("for " + id+"/" + questionID + " found " + answers);
    return answers;
  }

  private ExerciseExport getExportForExercise(String id, int questionID) {
    return getExportForExercise(id + "_" + questionID);
  }

  /**
   * @param key
   * @return
   * @see #populateExportIdToExport
   */
  public ExerciseExport getExportForExercise(String key) {
    return exerciseIDToExport.get(key);
  }

  public ExerciseExport getExportForExerciseTest(String key) {
    return exerciseIDToExportTest == null ? exerciseIDToExport.get(key) : exerciseIDToExportTest.get(key);
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#makeAutoCRT
   */
  public void makeClassifier() {
    getClassifier();
  }

  /**
   * Make a classifier given the export date, which has answers and their grades.<br></br>
   * The export data also includes the answer key for each question.<br></br>
   * This uses Jacob's classifier.
   *
   * @return a mira classifier
   * @see #getExportedAnswers(String, int)
   * @see #getScoreForExercise
   * @see mitll.langtest.server.export.ExerciseExport
   * @see AutoGradeExperiment
   */
  private Classifier<AutoGradeExperiment.Event> getClassifier() {
    if (true) return null;
    if (classifier != null) return classifier;

//    logger.debug("install " + installPath + " media " + mediaDir);

    String configDir = (installPath != null ? installPath + File.separator : "") + mediaDir + File.separator;
    if (mediaDir.startsWith(installPath)) {
      configDir = mediaDir + File.separator;
    }
    File serializedClassifier = new File(configDir, SERIALIZED_CLASSIFIER);
    if (serializedClassifier.exists()) {
      classifier = rehydrateClassifier(configDir, serializedClassifier);
      return classifier;
    } else {
      // List<ExerciseExport> export = getExportedGradedItems();
      List<ExerciseExport> export = usePreDefOnly ? getPreDefSlice() : doEighty ? getExportedGradedItemsSlice() : getExportedGradedItems();

      readConfigFile(configDir);

      long then = System.currentTimeMillis();
      logger.info("training classifier...");

      // TODO : don't support Jacob Classifier for now 2/1/16 GWFV
      // AutoGradeExperiment.saveClassifierAfterExport(export, serializedClassifier.getAbsolutePath());

      long now = System.currentTimeMillis();

      logger.info("took " + ((now - then) / 1000) + " seconds to save classifier to " + serializedClassifier.getAbsolutePath());

      // TODO : don't support Jacob Classifier for now 2/1/16 GWFV
      //classifier = AutoGradeExperiment.getClassifierFromSavedModel(serializedClassifier.getAbsolutePath(), export);

      return classifier;
    }
  }

  /**
   * TODO : this is overkill - we should serialize/rehydrate the model without calling the svm load/save code directly
   *
   * @param configDir
   * @param serializedClassifier
   * @return
   * @see #getClassifier
   */
  private Classifier<AutoGradeExperiment.Event> rehydrateClassifier(String configDir, File serializedClassifier) {
    logger.info("rehydrateClassifier : using previously calculated classifier.");
    long then = System.currentTimeMillis();

    List<ExerciseExport> export = getExportedGradedItems();
    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("rehydrateClassifier : took " + (now - then) + " millis to export " + export.size() + " items");
    }
    readConfigFile(configDir);
    then = now;
    // TODO : don't support Jacob Classifier for now 2/1/16 GWFV

    Classifier<AutoGradeExperiment.Event> classifier = null;
//        AutoGradeExperiment.getClassifierFromSavedModel(serializedClassifier.getAbsolutePath(), export);
    reportTime(then);
    return classifier;
  }

  private void reportTime(long then) {
    long now = System.currentTimeMillis();
    long l = (now - then) / 1000;
    if (l > 60) {
      long min = l / 60;
      l -= min * 60;
      logger.warn("rehydrateClassifier : took " + min + " minutes, " + l + " seconds to rehydrate classifier");
    } else if (l > 1) {
      logger.debug("rehydrateClassifier : took " + l + " seconds to rehydrate classifier");
    }
  }

  /**
   * @param configDir
   * @see #getClassifier
   * @see #rehydrateClassifier
   */
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

  /**
   * @return
   * @see #getClassifier
   * @see #rehydrateClassifier
   */
  private List<ExerciseExport> getExportedGradedItems() {
    long then = System.currentTimeMillis();

    Export.ExportPair export1 = exporter.getExport();
    List<ExerciseExport> export = export1.getAllExports();
    populateExportIdToExport(export);
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("getExportedGradedItems took " + (now - then) + " millis to get " + export.size() +
          " exported GradedItems items.");
    }
    return export;
  }

  private List<ExerciseExport> getPreDefSlice() {
    Export.ExportPair export1 = exporter.getExport();
    List<ExerciseExport> export = export1.getPredefOnly();

    for (ExerciseExport exp : export) {
      logger.debug("exp " + exp);
    }
    populateExportIdToExport(export);

    exerciseIDToExportTest = new HashMap<String, ExerciseExport>();
    toMap(export1.getAllExports(), exerciseIDToExportTest);

    return export;
  }

  /**
   * @param export
   * @see #getExportedGradedItems
   */
  private void populateExportIdToExport(List<ExerciseExport> export) {
    exerciseIDToExport = new HashMap<String, ExerciseExport>();
    toMap(export, exerciseIDToExport);
  }

  private void toMap(List<ExerciseExport> export, Map<String, ExerciseExport> idToExport) {
    for (ExerciseExport exp : export) {
      idToExport.put(exp.id, exp);
      //logger.debug(exp.id  + " -> " + exp.rgs.size() + " response-grade pairs");
    }
    logger.debug("populateExportIdToExport found " + idToExport.size() + " exported exercises from " + export.size() + " exports.");
  }

  /**
   * @return
   * @see #getClassifier()
   */
  private List<ExerciseExport> getExportedGradedItemsSlice() {
    long then = System.currentTimeMillis();

    Export.ExportPair export1 = exporter.getExport();
    List<ExerciseExport> export = export1.getAllExports();
    populateExportIdToExportSlice(export);

    List<ExerciseExport> copy = new ArrayList<>();
    for (ExerciseExport v : exerciseIDToExport.values()) {
      copy.add(v);
    }

    populateExportIdToExport(export);

    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("getExportedGradedItems took " + (now - then) + " millis to get " + copy.size() +
          " exported GradedItems items.");
    }
    return copy;
  }

  private void populateExportIdToExportSlice(List<ExerciseExport> export) {
    exerciseIDToExport = new HashMap<String, ExerciseExport>();
    exerciseIDToExportTest = new HashMap<String, ExerciseExport>();

    for (ExerciseExport exp : export) {
      ExerciseExport copy = new ExerciseExport(exp.id, "");
      copy.key = exp.key;

      int eightyPercent = (4 * exp.rgs.size()) / 5;
      List<ResponseAndGrade> responseAndGrades = exp.rgs.subList(0, eightyPercent);
      //  List<ResponseAndGrade> responseAndGradesTest = exp.rgs.subList(eightyPercent, exp.rgs.size());

//      logger.info("orig " + exp.rgs.size() + " vs train " + responseAndGrades.size());
      int maxGrade = 5;

      for (ResponseAndGrade rgc : responseAndGrades) {
        float v = rgc.grade * (float) (5 - 1);
        copy.addRG(rgc.response, ((int) v) + 1, maxGrade);
      }

      exerciseIDToExport.put(copy.id, copy);

      copy = new ExerciseExport(exp.id, "");
      copy.key = exp.key;

//      logger.info("orig " + exp.rgs.size() + " vs test " + responseAndGradesTest.size());
//      for (ResponseAndGrade rgc : responseAndGradesTest) {
//        float v = rgc.grade * (float) (5 - 1);
//        ResponseAndGrade responseAndGrade = copy.addRG(rgc.response, ((int) v) + 1, maxGrade);
//  //      logger.debug("v " + v + " vs " + responseAndGrade.grade);
//      }

      exerciseIDToExportTest.put(copy.id, copy);
    }
    logger.debug("populateExportIdToExport found " + exerciseIDToExport.size() + " exported exercises from " + export.size() + " exports.");
  }
}
