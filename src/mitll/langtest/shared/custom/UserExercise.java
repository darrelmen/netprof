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
  private static int globalCount  = 0;
  private int count = 0;
  private long uniqueID = -1; //set by database
  private String english;
  private String foreignLanguage;
  private long creator;
/*  String meaning;
  String context;
  String comment;*/

  boolean isPredef;

  public UserExercise() {}  // just for serialization

  public UserExercise(ExerciseShell shell,long creator) {
    super(shell.getID(),shell.getTooltip());
    isPredef = true;
    this.creator = creator;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#createNewItem(long, String, String)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage) {
    super(exerciseID,english);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    count = globalCount++;
    isPredef = !exerciseID.startsWith(CUSTOM_PREFIX);
  }

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getUserExercises(String)
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param refAudio
   * @param slowAudioRef
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String refAudio, String slowAudioRef) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage);
    setRefAudio(refAudio);
    setSlowRefAudio(slowAudioRef);
  }

    /**
     * @see mitll.langtest.client.custom.NPFExercise#populateListChoices(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController, com.github.gwtbootstrap.client.ui.SplitDropdownButton)
     * @param exercise
     */
  public UserExercise(Exercise exercise) {
    super(exercise.getID(), exercise.getEnglishSentence());

    this.isPredef = true;
    this.english = exercise.getEnglishSentence();
    this.foreignLanguage = exercise.getContent();
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#addNew
   * @return
   */
  public Exercise toExercise() {
    Exercise exercise = new Exercise("plan", CUSTOM_PREFIX + uniqueID, getEnglish(), getRefAudio(), getForeignLanguage(), getEnglish());
    exercise.setSlowRefAudio(getSlowAudioRef());
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
    System.out.println("toExercise : before " + imported + " " + getRefAudio() + " " + getSlowAudioRef());
    System.out.println("toExercise : before " + imported + " " + imported.getRefAudio() + " " + imported.getSlowAudioRef());

    if (getRefAudio() != null)
      imported.setRefAudio(getRefAudio());
    if (getSlowAudioRef() != null)
      imported.setSlowRefAudio(getSlowAudioRef());

    System.out.println("toExercise : after  " +imported + " " + getRefAudio() + " " + getSlowAudioRef());
    System.out.println("toExercise : after  " + imported + " " + imported.getRefAudio() + " " + imported.getSlowAudioRef());

    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);
    imported.setRefSentence(getForeignLanguage());
    imported.setEnglishSentence(english);
    return imported;
  }

  public String getEnglish() {
    return english;
  }

  public String getForeignLanguage() {
    return foreignLanguage;
  }

  public long getCreator() {
    return creator;
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

  public boolean isPredefined() {
    return isPredef;
  }

  public String toString() {
    return "UserExercise (" +count+
      ") #" + uniqueID + "/" + getID()+ (isPredef ? " <Predef>" :  " <User>")+  " creator " + getCreator()+
        " : " + getEnglish() + " = " + getForeignLanguage() + " audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes();
  }
}
