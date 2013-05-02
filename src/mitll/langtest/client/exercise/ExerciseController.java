package mitll.langtest.client.exercise;

import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.Exercise;

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

  void startRecording();
  void stopRecording();
  String getBase64EncodedWavFile();

  boolean getEnglishOnly();
  int getNumGradesToCollect();

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
  boolean isDataCollectMode();
  boolean isCRTDataCollectMode();
  boolean isCollectAudio();
  boolean isMinimalUI();
  boolean isGrading();
  boolean isLogClientMessages();
  String getAudioType();

  void showFlashHelp();
  String getLanguage();
  boolean isPromptBeforeNextItem();
  boolean isRightAlignContent();
  boolean isFlashCard();
  int getLeftColumnWidth();
  int getHeightOfTopRows();
}
