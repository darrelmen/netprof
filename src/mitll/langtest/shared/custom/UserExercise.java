package mitll.langtest.shared.custom;

import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise {
  public static final String CUSTOM_PREFIX = "Custom_";
//  private static int globalCount = 0;
  //private int count = 0;
  private long uniqueID = -1; //set by database

  private String english;
  private String foreignLanguage;
  private String transliteration;
  private long creator;
  private boolean isPredef;

  public UserExercise() {}  // just for serialization

  public UserExercise(ExerciseShell shell, long creator) {
    super(shell.getID(), shell.getTooltip());
    isPredef = !shell.getID().startsWith(CUSTOM_PREFIX);
    this.creator = creator;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#createNewItem(long, String, String, String)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration) {
    super(exerciseID,english);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
 //   count = globalCount++;
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
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration, String refAudio, String slowAudioRef) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, transliteration);
    setRefAudio(refAudio);
    setSlowRefAudio(slowAudioRef);
  }

    /**
     * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
     * @param exercise
     */
  public UserExercise(Exercise exercise) {
    super(exercise.getID(), exercise.getEnglishSentence());

    this.isPredef = true;
    this.english = exercise.getEnglishSentence();
    this.foreignLanguage = exercise.getRefSentence();
    this.transliteration = exercise.getTranslitSentence();
    setRefAudio(exercise.getRefAudio());
    setSlowRefAudio(exercise.getSlowAudioRef());
    setFieldToAnnotation(exercise.getFieldToAnnotation());
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#addNew
   * @return
   */
  public Exercise toExercise() {
    Exercise exercise = new Exercise("plan", ""+uniqueID, getEnglish(), getRefAudio(), getForeignLanguage(), getEnglish());
    exercise.setTranslitSentence(getTransliteration());
    exercise.setSlowRefAudio(getSlowAudioRef());
    exercise.setEnglishSentence(getEnglish());
    return exercise;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseWhere(String)
   * @param language
   * @return
   */
  public Exercise toExercise(String language) {
    String content = ExerciseFormatter.getContent(getForeignLanguage(), "", english, "", language);
    Exercise imported = new Exercise("import", id, content, false, true, english);
    if (getRefAudio() != null)
      imported.setRefAudio(getRefAudio());
    if (getSlowAudioRef() != null)
      imported.setSlowRefAudio(getSlowAudioRef());
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);
    imported.setRefSentence(getForeignLanguage());
    imported.setEnglishSentence(english);
    imported.setTranslitSentence(getTransliteration());
    return imported;
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
    setTooltip(english);
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
      //" (" +count+ ")" +
      " #" + uniqueID + "/" + getID()+ (isPredef ? " <Predef>" :  " <User>")+  " creator " + getCreator()+
        " : " + getEnglish() + " = " + getForeignLanguage() + " (" +getTransliteration()+
      ") audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes();
  }
}
