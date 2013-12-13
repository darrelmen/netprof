package mitll.langtest.client.exercise;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.StartupInfo;


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
  ListInterface getExerciseList();
  void showProgress();

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
 // boolean isFlashCard();
  boolean isGoodwaveMode();
  int getLeftColumnWidth();
  int getHeightOfTopRows();
  boolean shouldAddRecordKeyBinding();

  int getFlashcardPreviewFrameHeight();

  PropertyHandler getProps();

  String logException(Throwable throwable);

  String getGreeting();

  void resetState();
  StartupInfo getStartupInfo();
}
