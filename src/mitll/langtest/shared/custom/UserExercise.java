package mitll.langtest.shared.custom;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends ExerciseShell {
  int uniqueID;
  String english;
  String foreignLanguage;
  // String whichLanguage;
  long creator;
  String meaning;
  String context;
  String comment;
  private String audioRef;
  String slowAudioRef;
  boolean isPredef;
  String exerciseID;

  public UserExercise() {}

  public UserExercise(int uniqueID, long creator, String english, String foreignLanguage) {
    super("Custom_"+uniqueID,english);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
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

 /* private UserExercise(int uniqueID, String english, String foreignLanguage, String audioRef) {
    this(uniqueID, english, foreignLanguage);
    this.setAudioRef(audioRef);
  }*/

  public Exercise toExercise() {
    return new Exercise("plan", "Custom_" + uniqueID, english, audioRef, foreignLanguage, english);
  }

  public String getAudioRef() {
    return audioRef;
  }

  public void setAudioRef(String audioRef) {
    this.audioRef = audioRef;
  }

  public String toString() {
    return "UserExercise #" + uniqueID + " : " + english + " = " + foreignLanguage + " audio " + getAudioRef();
  }
}
