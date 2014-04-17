package mitll.langtest.shared.custom;

import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.ExerciseShell;

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
  private String content;
  private String context;

  private long creator;
  private boolean isPredef;
  private boolean isOverride;
  private Date modifiedDate;

  public UserExercise() {}  // just for serialization

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @param shell
   * @param creator
   */
  public UserExercise(CommonShell shell, long creator) {
    super(shell.getID(), shell.getTooltip());
    isPredef = !shell.getID().startsWith(CUSTOM_PREFIX);
    this.creator = creator;
  }

  /**
   * Tooltip is the english phrase, but if it's empty, use the foreign language.
   *
   * @see mitll.langtest.client.custom.EditItem#createNewItem(long)
   * @see mitll.langtest.client.custom.EditItem#getNewItem() (long)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration) {
    super(exerciseID,english.trim().isEmpty() ? foreignLanguage : english);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    isPredef = !exerciseID.startsWith(CUSTOM_PREFIX);
  }

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getUserExercises(String)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   * @param context
   * @param refAudio
   * @param slowAudioRef
   * @param isOverride
   * @param modifiedDate
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration, String context,
                      String refAudio, String slowAudioRef, boolean isOverride,
                      Map<String, String> unitToValue, Date modifiedDate
  ) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, transliteration);
    setRefAudio(refAudio);
    setSlowRefAudio(slowAudioRef);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.modifiedDate = modifiedDate;
    this.context = context;
  }

  /**
   * @param exercise
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   */
  public UserExercise(CommonExercise exercise) {
    super(exercise.getID(), exercise.getEnglish().trim().isEmpty() ? exercise.getRefSentence() : exercise.getEnglish());

    this.isPredef = true;
    this.english = exercise.getEnglish();
    this.foreignLanguage = exercise.getRefSentence();
    this.transliteration = exercise.getTransliteration();
   // setRefAudio(exercise.getRefAudio());
    //setSlowRefAudio(exercise.getSlowAudioRef());

/*
    AudioAttribute slowSpeed = exercise.getSlowSpeed();
    if (slowSpeed != null) {
      addAudio(slowSpeed);
    }*/

    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    setState(exercise.getState());
    setSecondState(exercise.getSecondState());
    setContext(exercise.getContext());
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) addAudio(audioAttribute);
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#addNew
   * @deprecated ideally we shouldn't have to do this
   * @return
   */
  public Exercise toExercise() {
    String tooltip = getEnglish().trim().isEmpty() ? getForeignLanguage() : getEnglish();
    Exercise exercise = new Exercise("plan", getID(), getEnglish(), getRefAudio(), getForeignLanguage(), tooltip);
    copyFields(exercise);

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
  public String getContent() {
    return content;
  }

  public CommonShell getShellCombinedTooltip() {
    String refSentence = getForeignLanguage();
    if (refSentence.length() > 15) {
      refSentence = refSentence.substring(0, 15);
    }
    boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getForeignLanguage().trim());
    String combined = refSentenceEqualsTooltip ? getTooltip() : getTooltip() + (refSentence.isEmpty() ? "": " / " + refSentence);
    if (getTooltip().isEmpty()) combined = refSentence;

    return new ExerciseShell(getID(), combined);
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

  public void setEnglish(String english) {
    this.english = english;
    setTooltip(english.isEmpty() ? getForeignLanguage() : getEnglish());
  }

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

  public boolean checkPredef() {  return !getID().startsWith(CUSTOM_PREFIX);  }

  @Override
  public Date getModifiedDate() {
    return modifiedDate;
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
