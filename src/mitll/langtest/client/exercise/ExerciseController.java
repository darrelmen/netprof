package mitll.langtest.client.exercise;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.GameInfo;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/9/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseController {
  LangTestDatabaseAsync getService();
  UserFeedback getFeedback();
  boolean loadNextExercise(ExerciseShell current);
  boolean loadNextExercise(String id);
  boolean isLastExercise(String exerciseID);
  void setGame(GameInfo game);
  void startOver();

  boolean loadPreviousExercise(Exercise current);

  boolean onFirst(Exercise current);

  int getUser();
  void pingAliveUser();

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
  boolean isGoodwaveMode();
  int getLeftColumnWidth();
  int getHeightOfTopRows();
  boolean shouldAddRecordKeyBinding();

  int getFlashcardPreviewFrameHeight();

  void addAdHocExercise(String label);
  void setSelectionState(Map<String,Collection<String>> selectionState);
  PropertyHandler getProps();

}
