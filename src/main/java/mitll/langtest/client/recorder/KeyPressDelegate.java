package mitll.langtest.client.recorder;

public interface KeyPressDelegate {
  void gotRightArrow();

  void gotLeftArrow();

  void gotUpArrow();

  void gotDownArrow();

  void gotEnter();

  void stopRecordingSafe();

  void gotSpaceBar();
  void gotSpaceBarKeyUp();
}
