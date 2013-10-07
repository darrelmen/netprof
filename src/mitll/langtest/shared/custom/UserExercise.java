package mitll.langtest.shared.custom;

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
public class UserExercise extends ExerciseShell {
  private long uniqueID; //set by database
  private String english;
  private String foreignLanguage;
  private long creator;
  String meaning;
  String context;
  String comment;
  private String refAudio;
  private String slowAudioRef = "";

  boolean isPredef;
  String exerciseID;

  public UserExercise() {}

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#createNewItem(long, String, String)
   * @param uniqueID
   * @param creator
   * @param english
   * @param foreignLanguage
   */
  public UserExercise(long uniqueID, long creator, String english, String foreignLanguage) {
    super("Custom_"+uniqueID,english);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
  }

  public UserExercise(long uniqueID, long creator, String english, String foreignLanguage, String refAudio, String slowAudioRef) {
    this(uniqueID,creator,english,foreignLanguage);
    this.refAudio = refAudio;
    this.slowAudioRef = slowAudioRef;
  }

    /**
     * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#populateListChoices(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController, com.github.gwtbootstrap.client.ui.SplitDropdownButton)
     * @param exercise
     */
  public UserExercise(Exercise exercise) {
    super(exercise.getID(),exercise.getEnglishSentence());

    this.isPredef = true;
    this.exerciseID = exercise.getID();
    this.english = exercise.getEnglishSentence();
    this.foreignLanguage = exercise.getContent();
  }

 /* private UserExercise(int uniqueID, String english, String foreignLanguage, String refAudio) {
    this(uniqueID, english, foreignLanguage);
    this.setRefAudio(refAudio);
  }*/

  public Exercise toExercise() {
    return new Exercise("plan", "Custom_" + uniqueID, getEnglish(), getRefAudio(), getForeignLanguage(), getEnglish());
  }

  public Exercise toExercise(String language) {
    String content = ExerciseFormatter.getContent(getForeignLanguage(), "", english, "", language);
    Exercise imported = new Exercise("import", id, content, false, true, english);
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT);
    return imported;
  }

  public String getRefAudio() {
    return refAudio;
  }

  public void setRefAudio(String refAudio) {
    this.refAudio = refAudio;
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

  public String getSlowAudioRef() {
    return slowAudioRef;
  }

  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public long getUniqueID() { return uniqueID; }

  @Override
  public String getID() {
    return "Custom_"+uniqueID;    //To change body of overridden methods use File | Settings | File Templates.
  }

  public void setSlowAudioRef(String slowAudioRef) {
    this.slowAudioRef = slowAudioRef;
  }

  public String toString() {
    return "UserExercise #" + uniqueID + " : " + getEnglish() + " = " + getForeignLanguage() + " audio " + getRefAudio();
  }
}
