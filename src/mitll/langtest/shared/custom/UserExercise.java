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
  //User creator;
  String meaning;
  String context;
  String audioRef;
  String slowAudioRef;
  boolean isPredef;
  String exerciseID;

  public UserExercise() {}

  public UserExercise(int uniqueID, String english, String foreignLanguage) {
    super("Custom_"+uniqueID,english);
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
  }

  public UserExercise(Exercise exercise) {
    super(exercise.getID(),exercise.getEnglishSentence());

    this.isPredef = true;
    this.exerciseID = exercise.getID();
    this.english = exercise.getEnglishSentence();
    this.foreignLanguage = exercise.getContent();
  }

  public UserExercise(int uniqueID, String english, String foreignLanguage, String audioRef) {
    this(uniqueID, english, foreignLanguage);
    this.audioRef = audioRef;
  }

/*  public Exercise toExercise() {
    return new Exercise("plan", "Custom_" + uniqueID, english, audioRef, foreignLanguage, english);
  }*/

  public String toString() {
    return "UserExercise #" + uniqueID + " : " + english + " = " + foreignLanguage + " audio " + audioRef;
  }
}
