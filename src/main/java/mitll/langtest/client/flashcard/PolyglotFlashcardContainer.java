package mitll.langtest.client.flashcard;

public interface PolyglotFlashcardContainer extends FlashcardContainer {

  void startTimedRun();

  boolean getIsDry();

  void cancelRoundTimer();

  PolyglotDialog.MODE_CHOICE getMode();

  long getSessionStartMillis();

  boolean isInLightningRound();

  long getRoundTimeLeftMillis();

  void showDrill();

}
