package mitll.langtest.client.exercise;

import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/9/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseController {
  boolean loadNextExercise(Exercise current);

  boolean loadPreviousExercise(Exercise current);

  boolean onFirst(Exercise current);

  int getUser();
  String getGrader();

  void startRecording();
  void stopRecording();
  String getBase64EncodedWavFile();

  boolean getEnglishOnly();

  SoundManagerAPI getSoundManager();

  // parameters
  float getScreenPortion();
  boolean showOnlyOneExercise();
  int getSegmentRepeats();
  boolean isArabicTextDataCollect();
  boolean useBkgColorForRef();
  boolean isDemoMode();
  boolean isAutoCRTMode();
  int getRecordTimeout();
  String getAudioType();
}
