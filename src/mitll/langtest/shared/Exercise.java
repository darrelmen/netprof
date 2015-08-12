package mitll.langtest.shared;

import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 *
 * TODO : consider subclass for pronunciation exercises?
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends AudioExercise implements CommonExercise {
  private static final int MAX_TOOLTIP_LENGTH = 15;
  private String content;

  private String englishSentence;
  private String meaning, context, contextTranslation;
  private transient Collection<String> refSentences = new ArrayList<String>();
  private List<String> translitSentences = new ArrayList<String>();
  private String foreignLanguage;
  private STATE state;
  private List<CorrectAndScore> scores;
  private float avgScore;

  private transient List<String> firstPron = new ArrayList<String>();

  public Exercise() {}     // required for serialization

  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   *
   * @param id
   * @param content
   * @param tooltip
   * @param context   */
  public Exercise(String id, String content, String tooltip, String context, String contextTranslation) {
    super(id, tooltip);
    this.setContent(content);
    this.context = context;
    this.contextTranslation = contextTranslation;
  }

  /**
   * @see UserExercise#toExercise()
   * @param id
   * @param content
   * @param sentenceRef
   * @param tooltip
   */
  public Exercise(String id, String content, String sentenceRef, String tooltip) {
    super(id, tooltip);

    this.setContent(content);
    this.refSentences.add(sentenceRef);
  }

  public void setTooltip() {
    setTooltip(getCombinedTooltip());
  }

  public CommonShell getShellCombinedTooltip() {
    String combined = getCombinedTooltip();
    return new ExerciseShell(getID(), combined);
  }

  /**
   * Hack - if we have "N/A" for english, don't show it in the list.
   * @return
   */
  public String getCombinedTooltip() {
    String refSentence = getRefSentence();
    if (refSentence.length() > MAX_TOOLTIP_LENGTH) {
      refSentence = refSentence.substring(0, MAX_TOOLTIP_LENGTH);
    }
    boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getRefSentence().trim());
    String combined = refSentenceEqualsTooltip ? getTooltip() : getTooltip() + (refSentence.isEmpty() ? "": " / " + refSentence);
    if (getTooltip().isEmpty() || getTooltip().equals("N/A")) combined = refSentence;
    return combined;
  }

  public String getContent() { return content; }

  public String getRefSentence() {  return getForeignLanguage();  }

  @Override
  public Collection<String> getRefSentences() { return refSentences; }

  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise(String, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, boolean, String, boolean)
   * @param sentenceRefs
   */
  @Override
  public void setRefSentences(Collection<String> sentenceRefs) {
    this.refSentences = sentenceRefs;
  }

  public void setForeignLanguage(String foreignLanguage) { this.foreignLanguage = foreignLanguage; }

  public String getTransliteration() { return translitSentences.isEmpty() ? "" : translitSentences.get(0); }

  public void setTranslitSentence(String translitSentence) {
    translitSentences.clear();
    translitSentences.add(translitSentence);
  }

  public String getEnglish() {  return englishSentence;  }

  private void setContent(String content) { this.content = content;  }
  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   * @param englishSentence
   */
  public void setEnglishSentence(String englishSentence) {
    this.englishSentence = englishSentence;
  }

  @Override
  public String getMeaning() {
    return meaning;
  }

  public void setMeaning(String meaning) {
    this.meaning = meaning;
  }

  @Override
  public String getForeignLanguage() {  return foreignLanguage;  }

  @Override
  public String getContext() {
    return context;
  }
  
  @Override
  public String getContextTranslation() {
    return contextTranslation;
  }

  /**
   * @see mitll.langtest.shared.custom.UserExercise#copyFields(Exercise)  - only
   * @param context
   */
  public void setContext(String context) {
    this.context = context;
  }

  /**
   * @see mitll.langtest.shared.custom.UserExercise#copyFields
   * @param contextTranslation
   */
  public void setContextTranslation(String contextTranslation){
	  this.contextTranslation = contextTranslation;
  }

  public Exercise toExercise() {
    return this;
  }

  @Override
  public long getModifiedDateTimestamp() {
    return 0;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  public List<CorrectAndScore> getScores() {
    return scores;
  }

  @Override
  public void setScores(List<CorrectAndScore> scores) { this.scores = scores; }

  public float getAvgScore() {
    return avgScore;
  }
  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   * @param bagOfPhones
   */
  @Override
  public void setBagOfPhones(Set<String> bagOfPhones) {}

  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
  }

  public String toString() {
    //  String moreAboutQuestions = DEBUG ? " : " +  getQuestionToString() : "";
    //  String questionInfo = langToQuestion == null ? " no questions" : " num questions " + langToQuestion.size() + moreAboutQuestions;
    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();

    // warn about attr that have no user
    StringBuilder builder = new StringBuilder();
    for (AudioAttribute attr:audioAttributes1) {
      if (attr.getUser() == null) {
        builder.append("\t").append(attr.toString()).append("\n");
      }
    }

    return "Exercise " + id +  " content bytes = " + content.length() + " english '" + getEnglish() +
        "'/'" + getRefSentence() +"' context " + getContext() + "/"+getContextTranslation()+
        " audio count = " + audioAttributes1.size()+
        (builder.toString().isEmpty() ? "":" \n\tmissing user audio " + builder.toString()) +
        //    " : " + questionInfo +
        " unit->lesson " + getUnitToValue();
  }
}
