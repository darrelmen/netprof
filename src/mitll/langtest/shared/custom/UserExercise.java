package mitll.langtest.shared.custom;

import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise {
  static int globalCount  = 0;
  int count = 0;
  private long uniqueID; //set by database
  private String english;
  private String foreignLanguage;
  private long creator;
/*  String meaning;
  String context;
  String comment;*/

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
    count = globalCount++;
  }

  /**
   * @see mitll.langtest.server.database.UserExerciseDAO#getUserExercises(String)
   * @param uniqueID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param refAudio
   * @param slowAudioRef
   */
  public UserExercise(long uniqueID, long creator, String english, String foreignLanguage, String refAudio, String slowAudioRef) {
    this(uniqueID,creator,english,foreignLanguage);
    setRefAudio(refAudio);
    setSlowRefAudio(slowAudioRef);
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

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#addNew
   * @return
   */
  public Exercise toExercise() {
    Exercise exercise = new Exercise("plan", "Custom_" + uniqueID, getEnglish(), getRefAudio(), getForeignLanguage(), getEnglish());
    exercise.setSlowRefAudio(getSlowAudioRef());
    return exercise;
  }

  public Exercise toExercise(String language) {
    String content = ExerciseFormatter.getContent(getForeignLanguage(), "", english, "", language);
    Exercise imported = new Exercise("import", id, content, false, true, english);
    imported.setRefAudio(getRefAudio());
    imported.setSlowRefAudio(getSlowAudioRef());
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

  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public long getUniqueID() { return uniqueID; }

  @Override
  public String getID() {
    return isPredef ? super.getID() : "Custom_"+uniqueID;    //To change body of overridden methods use File | Settings | File Templates.
  }

  public void setEnglish(String english) {
    this.english = english;
    setTooltip(english);
  }

  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  public String toString() {
    return "UserExercise (" +count+
      ") #" + uniqueID + " : " + getEnglish() + " = " + getForeignLanguage() + " audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes();
  }
}
