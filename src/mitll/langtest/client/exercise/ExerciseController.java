package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.ButtonFactory;
import mitll.langtest.client.instrumentation.EventLogger;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ImageResponse;
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
  boolean isReviewMode();


  void logEvent(Tab button, String widgetType, String exid, String context);

  boolean showCompleted();

  void getImage(int reqid, String path, String type, int toUse, int height, String exerciseID, AsyncCallback<ImageResponse> client);

  String getLanguage();
  boolean isPromptBeforeNextItem();
  boolean isRightAlignContent();
  boolean isGoodwaveMode();
  int getLeftColumnWidth();
  int getHeightOfTopRows();

  PropertyHandler getProps();

  String logException(Throwable throwable);
  void logMessageOnServer(String message, String prefix);

  StartupInfo getStartupInfo();
  boolean gotMicPermission();
  EventLogger getButtonFactory();
  void register(Button button, String exid);

  void logEvent(UIObject button, String widgetType, String exid, String context);
}
