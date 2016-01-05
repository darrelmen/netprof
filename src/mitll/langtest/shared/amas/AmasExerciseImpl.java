package mitll.langtest.shared.amas;

import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.*;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * <p/>
 * TODO : consider subclass for pronunciation exercises?
 * <p/>
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmasExerciseImpl implements Shell /*implements CommonExercise*/ {
  public static final String EN = "en";
  public static final String FL = "fl";
 // private static final int MAX_TOOLTIP_LENGTH = 15;
  private String content;
  private Map<String, String> unitToValue = new HashMap<String, String>();

  private Map<String, List<QAPair>> langToQuestion = null;
  private List<String> refSentences = new ArrayList<String>();

  private transient List<String> firstPron = new ArrayList<String>();
  protected String id;
  private STATE state = STATE.UNSET;
  private String altID;

  //  private String imageURL;
  //  private String audioURL;

  public AmasExerciseImpl() {}  // required for serialization

  /**
   * @param id
   * Exercise exercise = new Exercise(id, "", id, "", "", "");
   * @see mitll.langtest.server.amas.FileExerciseDAO#readTSVLine(String, String, String, int)
   */
  public AmasExerciseImpl(String id, String content, String altID) {
    this.id = id;
    this.content = content;
    this.altID = altID;
  }

  /**
   * @seex UserExercise#toExercise()
   * @param id
   * @param content
   * @param sentenceRef
   * @param tooltip
   * @see mitll.langtest.server.amas.FileExerciseDAO#getSimpleExerciseForLine(String, int)
   */
  public AmasExerciseImpl(String id, String content, String sentenceRef, String tooltip) {
    this.id = id;
    this.setContent(content);
    this.refSentences.add(sentenceRef);
  }

  /**
   * @param id
   * @param content
   * @param sentenceRefs
   * @param tooltip
   * @see mitll.langtest.server.amas.FileExerciseDAO#getFlashcardExercise(int, String, String, String, String)
   */

  public AmasExerciseImpl(String id, String content, List<String> sentenceRefs, String tooltip) {
    this.id = id;
    this.setContent(content);
    this.refSentences = sentenceRefs;
  }

  //@Override
  public String getAltID() { return altID;  }

/*  public CommonShell getShellCombinedTooltip() {
    ExerciseShell exerciseShell = new ExerciseShell(getID(), getCombinedTooltip());
    exerciseShell.setState(getState());
    return exerciseShell;
  }*/

  /**
   * Hack - if we have "N/A" for english, don't show it in the list.
   *
   * @return
   */
/*  public String getCombinedTooltip() {
    String refSentence = getRefSentence();
    if (refSentence.length() > MAX_TOOLTIP_LENGTH) {
      refSentence = refSentence.substring(0, MAX_TOOLTIP_LENGTH);
    }
    boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getRefSentence().trim());
    String combined = refSentenceEqualsTooltip ? getTooltip() : getTooltip() + (refSentence.isEmpty() ? "" : " / " + refSentence);
    if (getTooltip().isEmpty() || getTooltip().equals("N/A")) combined = refSentence;
  //  if (combined.isEmpty()) combined = getTitle();
    return combined;
  }*/

  /**
   * @param lang
   * @param question
   * @param alternateAnswers
   * @see mitll.langtest.server.database.exercise.FileExerciseDAO#addQuestion(String, String, AmasExerciseImpl, boolean)
   */
  public void addQuestion(String lang, String question, List<String> alternateAnswers) {
    QAPair pair = new QAPair(question, alternateAnswers);
    addQuestion(lang, pair);
  }

  public void addQuestions(String lang, List<QAPair> pairs) {
    for (QAPair pair : pairs) {
      addQuestion(lang, pair);
    }
  }

  private void addQuestion(String lang, QAPair pair) {
    if (langToQuestion == null) langToQuestion = new HashMap<String, List<QAPair>>();
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }

    qaPairs.add(pair);
  }
/*
  public String getRefSentence() {
    StringBuilder builder = new StringBuilder();
    for (String s : refSentences) {
      builder.append(s).append(" ");
    }
    return builder.toString();
  }*/

/*
  public String getTransliteration() {
    return "";//    translitSentences.isEmpty() ? "" : translitSentences.get(0);
  }
*/

  /**
   * @return
   * @see mitll.langtest.server.database.Export#populateIdToExportMap(AmasExerciseImpl)
   */
  //@Override
  public List<QAPair> getQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   */
  //@Override
  public List<QAPair> getEnglishQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(EN);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   */
  //@Override
  public List<QAPair> getForeignLanguageQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

/*
  public String getEnglish() {
    return "";
  }
*/

  public String getContent() {  return content; }

  private void setContent(String content) {  this.content = content; }

/*
  @Override
  public String getForeignLanguage() {
    return getRefSentence();
  }
*/

//  public Exercise toExercise() {
//    return this;
//  }

//  @Override
  public void setScores(List<CorrectAndScore> scores) {}

  public void setAvgScore(float avgScore) {
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   * @param bagOfPhones
   */
//  @Override
  public void setBagOfPhones(Set<String> bagOfPhones) {
  }

/*
  @Override
  public String getImageURL() {
    return null;//imageURL;
  }
*/

  /**
   *
   * @paramx imageURL
   */
/*
  public void setImageURL(String imageURL) {
    this.imageURL = imageURL;
  }
*/

/*  @Override
  public String getAudioQuestionURL() {

    return null;//audioURL;
  }*/

/*
  public void setAudioURL(String audioURL) {
    this.audioURL = audioURL;
  }
*/

//  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

//  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
  }

  public Map<String, String> getUnitToValue() { return unitToValue; }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */

  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
    //if (value.isEmpty()) {
    // System.out.println("addUnitToValue " + unit + " value " + value);
    // }
    this.getUnitToValue().put(unit, value);
  }

  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public String getID() {
    return id;
  }

  public void setID(String id) {
    this.id = id;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  /**
   * TODO : refactor so this isn't needed.
   * @return
   */
  @Override
  public STATE getSecondState() {
    return null;
  }

  @Override
  public void setSecondState(STATE state) {

  }

  public String toString() {
  //  Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();

    // warn about attr that have no user
/*    StringBuilder builder = new StringBuilder();
    for (AudioAttribute attr : audioAttributes1) {
      if (attr.getUser() == null) {
        builder.append("\t").append(attr.toString()).append("\n");
      }
    }*/

    return "Exercise " + getID() +  (getAltID().isEmpty() ? "" : "/"+getAltID())+
     //   " english '" + getEnglish() +
     //   "'" +
      //  "/'" + getRefSentence() +
  //      " audio count = " + audioAttributes1.size()+
    //    (builder.toString().isEmpty() ? "":" \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue();
  }
}
