package mitll.langtest.shared.custom;

import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.ExerciseShell;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise {
  public static final String CUSTOM_PREFIX = "Custom_";
  private long uniqueID = -1; //set by database

  private String english;
  private String foreignLanguage;
  private String transliteration;
  private long creator;
  private boolean isPredef;
  private boolean isOverride;

  public UserExercise() {}  // just for serialization

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @param shell
   * @param creator
   */
  public UserExercise(ExerciseShell shell, long creator) {
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
   * @param refAudio
   * @param slowAudioRef
   * @param isOverride
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration, String refAudio, String slowAudioRef, boolean isOverride,
                      Map<String,String> unitToValue) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, transliteration);
    setRefAudio(refAudio);
    setSlowRefAudio(slowAudioRef);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
  }

    /**
     * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
     * @param exercise
     */
  public UserExercise(Exercise exercise) {
    super(exercise.getID(), exercise.getEnglishSentence().trim().isEmpty() ? exercise.getRefSentence() : exercise.getEnglishSentence());

    this.isPredef = true;
    this.english = exercise.getEnglishSentence();
    this.foreignLanguage = exercise.getRefSentence();
    this.transliteration = exercise.getTranslitSentence();
    setRefAudio(exercise.getRefAudio());
    setSlowRefAudio(exercise.getSlowAudioRef());
    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#addNew
   * @return
   */
  public Exercise toExercise() {
    String tooltip = getEnglish().trim().isEmpty() ? getForeignLanguage() : getEnglish();
    Exercise exercise = new Exercise("plan", getID(), getEnglish(), getRefAudio(), getForeignLanguage(), tooltip);
    copyFields(exercise);

    return exercise;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseWhere(String)
   * @param language
   * @return
   */
  public Exercise toExercise(String language) {
    String content = ExerciseFormatter.getContent(getForeignLanguage(), "", english, "", language);
    String tooltip = english.trim().isEmpty() ? getForeignLanguage() : english;
    Exercise imported = new Exercise("import", id, content, false, true, tooltip);
    if (getRefAudio() != null)
      imported.setRefAudio(getRefAudio());

    imported.setRefSentence(getForeignLanguage());

    copyFields(imported);
    return imported;
  }

  protected void copyFields(Exercise imported) {
    if (getSlowAudioRef() != null)
      imported.setSlowRefAudio(getSlowAudioRef());
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);
    imported.setEnglishSentence(getEnglish());
    imported.setTranslitSentence(getTransliteration());
    imported.setUnitToValue(getUnitToValue());
    imported.setFieldToAnnotation(getFieldToAnnotation());
  }

  public String getEnglish() { return english; }
  public String getForeignLanguage() { return foreignLanguage;  }
  public String getTransliteration() { return transliteration;  }
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

  public boolean isPredefined() {  return isPredef;  }

  public String toString() {
    return "UserExercise" +
      " #" + uniqueID + "/" + getID() +
      (isPredef ? " <Predef>" : " <User>") +
      (isOverride ? " <Override>" : "") +
      " creator " + getCreator() +
      " : English '" + getEnglish() + "', " +
      "foreign language '" + getForeignLanguage() + "'" + " (" + getTransliteration() + ") " +
      "tooltip " + getTooltip() +
      "audio attr (" + getAudioAttributes().size() +
      ") :" + getAudioAttributes() + " unit/lesson " + getUnitToValue();
  }
}
