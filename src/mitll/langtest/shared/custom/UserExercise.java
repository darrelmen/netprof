package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise implements IsSerializable {
  int uniqueID;
  String english;
  String foreignLanguage;
  // String whichLanguage;
  //User creator;
  String meaning;
  String context;
  String audioRef;
  String slowAudioRef;

  public UserExercise() {}

  public UserExercise(int uniqueID, String english, String foreignLanguage) {
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
  }

  public UserExercise(int uniqueID, String english, String foreignLanguage, String audioRef) {
    this(uniqueID, english, foreignLanguage);
    this.audioRef = audioRef;
  }

  public Exercise toExercise() {
    return new Exercise("plan", "Custom_" + uniqueID, english, audioRef, foreignLanguage, english);
  }

  public String toString() {
    return "UserExercise #" + uniqueID + " : " + english + " = " + foreignLanguage + " audio " + audioRef;
  }
}
