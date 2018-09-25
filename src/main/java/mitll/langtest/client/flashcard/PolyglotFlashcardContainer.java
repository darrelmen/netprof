package mitll.langtest.client.flashcard;

interface PolyglotFlashcardContainer extends FlashcardContainer {
  void startTimedRun();

  void cancelRoundTimer();

  long getSessionStartMillis();

  long getRoundTimeLeftMillis();

//  int getRoundTimeMinutes(boolean isDry);

  boolean getIsDry();

  PolyglotDialog.MODE_CHOICE getMode();

  boolean isInLightningRound();

  void showDrill();
  void showQuiz();

  boolean isComplete();

  void postAudio();
  void addRoundTrip(long roundTripMillis);
}
