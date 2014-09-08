package mitll.langtest.shared.custom;

import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.ScoreAndPath;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise implements CommonUserExercise {
  public static final String CUSTOM_PREFIX = "Custom_";
  private long uniqueID = -1; //set by database

  private String english;
  private String foreignLanguage;
  private String transliteration;
  private String context;

  private long creator;
  private boolean isPredef;
  private boolean isOverride;
  private Date modifiedDate;
  private Collection<ScoreAndPath> scores;
  private float avgScore;
  private static final int MAX_TOOLTIP_LENGTH = 15;

  public UserExercise() {}  // just for serialization

  /**
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   * @param shell
   * @param creator
   */
  public UserExercise(CommonShell shell, long creator) {
    super(shell.getID());
    isPredef = !shell.getID().startsWith(CUSTOM_PREFIX);
    this.creator = creator;
  }

  /**
   * Tooltip is the english phrase, but if it's empty, use the foreign language.
   *
   * @see mitll.langtest.client.custom.dialog.EditItem#createNewItem(long)
   * @see mitll.langtest.client.custom.dialog.EditItem#getNewItem() (long)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration) {
    super(exerciseID);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    isPredef = !exerciseID.startsWith(CUSTOM_PREFIX);
    setTooltip();
  }

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getUserExercises
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   * @param context
   * @param isOverride
   * @param modifiedDate
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration, String context,
                      boolean isOverride,
                      Map<String, String> unitToValue, Date modifiedDate
  ) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, transliteration);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.modifiedDate = modifiedDate;
    this.context = context;
  }

  /**
   * @param exercise
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public UserExercise(CommonExercise exercise) {
    super(exercise.getID());
    this.isPredef = true;
    this.english = exercise.getEnglish();
    this.foreignLanguage = exercise.getRefSentence();
    this.transliteration = exercise.getTransliteration();
    setTooltip();
    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    setState(exercise.getState());
    setSecondState(exercise.getSecondState());
    setContext(exercise.getContext());
    copyAudio(exercise);
  }

  private void copyAudio(CommonExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      addAudio(audioAttribute);
    }
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#addNew
   * @deprecated ideally we shouldn't have to do this
   * @return
   */
  public Exercise toExercise() {
    Exercise exercise = new Exercise("plan", getID(), getEnglish(), null, getForeignLanguage(), getTooltip());
    copyFields(exercise);
    copyAudio(exercise);

    return exercise;
  }

  public CommonUserExercise toCommonUserExercise() { return this; }

  private void copyFields(Exercise imported) {
    AudioAttribute slowSpeed = getSlowSpeed();
    if (slowSpeed != null) {
      imported.addAudio(slowSpeed);
    }
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);
    imported.setEnglishSentence(getEnglish());
    imported.setTranslitSentence(getTransliteration());
    imported.setUnitToValue(getUnitToValue());
    imported.setFieldToAnnotation(getFieldToAnnotation());
    imported.setContext(getContext());
  }

  @Override
  public String getPlan() {
    return "plan";
  }

  @Override
  public String getEnglish() { return english; }
  @Override
  public String getForeignLanguage() { return foreignLanguage;  }

  @Override
  public String getRefSentence() {
    return foreignLanguage;
  }

  @Override
  public String getTransliteration() { return transliteration;  }

  @Override
  public String getMeaning() {
    return "";
  }

  /**
   * @deprecated  - nobody set this
   * @return
   */
  @Override
  public String getContent() { return ""; }

  public CommonShell getShellCombinedTooltip() {
    String combined = getCombinedTooltip();

    return new ExerciseShell(getID(), combined);
  }

  public String getCombinedTooltip() {
    String refSentence = getForeignLanguage();
    if (refSentence.length() > MAX_TOOLTIP_LENGTH) {
      refSentence = refSentence.substring(0,  MAX_TOOLTIP_LENGTH);
    }
   // boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getForeignLanguage().trim());
    boolean englishSameAsForeign = getEnglish().trim().equals(getForeignLanguage().trim());
    String combined = englishSameAsForeign ? getEnglish() : getEnglish() + (refSentence.isEmpty() ? "": " / " + refSentence);
    if (combined.isEmpty()) combined = refSentence;
    return combined;
  }

  public void setTooltip() {
    setTooltip(getCombinedTooltip());
  }

  @Override
  public long getCreator() {
    return creator;
  }
  public void setCreator(long id) {
    creator = id;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#add
   * @param uniqueID
   */
  public void setUniqueID(long uniqueID) { this.uniqueID = uniqueID;  }
  @Override
  public long getUniqueID() { return uniqueID; }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#grabInfoFromFormAndStuffInfoExercise()
   * @param english
   */
  public void setEnglish(String english) { this.english = english;  }
  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }
  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  @Override
  public String getContext() { return context;  }
  private void setContext(String context) { this.context = context; }

  @Override
  public boolean isPredefined() {  return isPredef;  }

  @Override
  public UserExercise toUserExercise() {  return this;  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#getCreateButton(UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   * @return
   */
  public boolean checkPredef() {  return !getID().startsWith(CUSTOM_PREFIX);  }

  @Override
  public Date getModifiedDate() {
    return modifiedDate;
  }

  public Collection<ScoreAndPath> getScores() {
    return scores;
  }

  @Override
  public void setScores(Collection<ScoreAndPath> scores) {
    this.scores = scores;
  }

  public float getAvgScore() {
    return avgScore;
  }

  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }

  public String toString() {
    return "UserExercise" +
      " #" + uniqueID + "/" + getID() +
      (isPredef ? " <Predef>" : " <User>") +
      (isOverride ? " <Override>" : "") +
      " creator " + getCreator() +
      " : English '" + getEnglish() + "', " +
      "foreign language '" + getForeignLanguage() + "'" + " (" + getTransliteration() + ") " +
      "context '" + getContext()+ "' " +
      "tooltip '" + getTooltip() +
      "' audio attr (" + getAudioAttributes().size() +
      ") :" + getAudioAttributes() + " unit/lesson " + getUnitToValue() +
      " state " + getState()+"/" +getSecondState()+
      " modified " + modifiedDate;
  }
}
