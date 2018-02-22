package mitll.langtest.client.flashcard;

interface PolyglotFlashcardContainer extends FlashcardContainer {

  void startTimedRun();

  boolean getIsDry();

  void cancelRoundTimer();

  PolyglotDialog.MODE_CHOICE getMode();

  long getSessionStartMillis();

  boolean isInLightningRound();

  long getRoundTimeLeftMillis();

  void showDrill();

}
