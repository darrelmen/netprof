package mitll.langtest.shared.amas;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MutableShell;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import net.sf.json.JSONObject;

import java.io.InputStream;
import java.util.*;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * <p>
 * TODO : consider subclass for pronunciation exercises?
 * <p>
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmasExerciseImpl implements CommonShell {
  public static final String EN = "en";
  public static final String FL = "fl";

  private String content;
  //private String contentTrans;

  private String orient;
  //private String orientTrans;
  //private String ilr;
  private boolean isListening;
  private String audioURL;
  private Map<String, String> unitToValue = new HashMap<String, String>();

  private Map<String, List<QAPair>> langToQuestion = null;

  private String id;
  private STATE state = STATE.UNSET;
  private String altID;

  public AmasExerciseImpl() {
  }  // required for serialization


  public AmasExerciseImpl(String id, String content, String altID) {
    this.id = id;
    this.content = content;
    this.altID = altID;
  }

  /**
   * @param id
   * @see mitll.langtest.server.database.exercise.AMASJSONURLExerciseDAO#toAMASExercise(JSONObject)
   */
  public AmasExerciseImpl(String id, String content, //String altID,
                          String contentTrans,
                          String orient,
                          String orientTrans,
                          boolean isListening,
                          String ilr,
                          String audioURL) {
    this.id = id;
    this.content = content;
    this.altID = id;  // TODO : how does this work? do we need to worry about it?
    //this.contentTrans = contentTrans;
    this.orient = orient;
   // this.orientTrans = orientTrans;
    this.isListening = isListening;
   // this.ilr = ilr;
    this.audioURL = audioURL;
  }

  public String getAltID() {
    return altID;
  }

  private void addQuestion(boolean isFL, String question, String answer) throws Exception {
    addQuestion(isFL ? FL : EN, question, answer);
  }

/*  public void addQuestion(boolean isFL, String question, String[] alternateAnswers) throws Exception {
    addQuestion(isFL ? FL : EN, question, alternateAnswers);
  }*/

  public void addQuestion(boolean isFL, String question, Collection<String> alternateAnswers) throws Exception {
    addQuestion(isFL ? FL : EN, question, alternateAnswers);
  }

  private void addQuestion(String lang, String question, String answer) {
    List<String> serializableCollection = new ArrayList<>();
    serializableCollection.add(answer);
    addQuestion(lang, new QAPair(question, serializableCollection));
  }

  private void addQuestion(String lang, String question, String[] alternateAnswers) {
    addQuestion(lang, new QAPair(question, Arrays.asList(alternateAnswers)));
  }

  /**
   * @param lang
   * @param question
   * @param alternateAnswers
   * @see mitll.langtest.server.amas.FileExerciseDAO#addQuestion
   */
  public void addQuestion(String lang, String question, Collection<String> alternateAnswers) {
    addQuestion(lang, new QAPair(question, alternateAnswers));
  }

  /**
   * @param lang
   * @param pairs
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises(String, String, String, InputStream)
   */
  public void addQuestions(String lang, List<QAPair> pairs) {
    for (QAPair pair : pairs) {
      addQuestion(lang, pair);
    }
  }

  private void addQuestion(String lang, QAPair pair) {
    if (langToQuestion == null) langToQuestion = new HashMap<>();
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }

    qaPairs.add(pair);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#populateIdToExportMap(AmasExerciseImpl)
   */
  public List<QAPair> getQuestions() {
    return getForeignLanguageQuestions();
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   */
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

  public String getContent() {
    return content;
  }

  public void setScores(List<CorrectAndScore> scores) {
  }

  public Map<String, String> getUnitToValue() {
    return unitToValue;
  }

  /**
   * @param unit
   * @param value
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   */

  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
    this.getUnitToValue().put(unit, value);
  }

  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public String getID() {
    return id;
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
   *
   * @return
   */
  @Override
  public STATE getSecondState() {
    return null;
  }

  @Override
  public void setSecondState(STATE state) {}

  public String toString() {
    return "Exercise " + getID() + (getAltID().isEmpty() ? "" : "/" + getAltID()) +
        " Questions " + getQuestions() + " unit->lesson " + getUnitToValue();
  }

  // TODO : workaround for the moment - hard to use the current exercise lists without extending from CommonShell...?
  @Override
  public String getEnglish() {
    return null;
  }

  @Override
  public String getMeaning() {
    return null;
  }

  @Override
  public String getForeignLanguage() {
    return "";
  }

  @Override
  public String getTransliteration() {
    return null;
  }

  @Override
  public String getDisplayID() {
    return id;
  }

  @Override
  public MutableShell getMutableShell() {
    return null;
  }

  @Override
  public Collection<String> getRefSentences() {
    return Collections.singleton(getForeignLanguage());
  }

/*
  public String getContentTrans() {
    return contentTrans;
  }
*/

  public String getOrient() {
    return orient;
  }

/*  public String getOrientTrans() {
    return orientTrans;
  }

  public String getIlr() {
    return ilr;
  }*/

  public boolean isListening() {
    return isListening;
  }

  /**
   * @see mitll.langtest.client.amas.AudioExerciseContent#addAudioRow(AmasExerciseImpl, String, boolean, Panel, int, int)
   * @return
   */
  public String getAudioURL() {
    return audioURL;
  }
}
