package mitll.langtest.client.flashcard;

import mitll.langtest.shared.custom.QuizSpec;

interface PolyglotFlashcardContainer extends FlashcardContainer {
  void startTimedRun();

  void cancelRoundTimer();

  long getSessionStartMillis();

  long getRoundTimeLeftMillis();

  boolean getIsDry();

  PolyglotDialog.MODE_CHOICE getMode();

  boolean isInLightningRound();

  void showDrill();
  void showQuiz();

  boolean isComplete();

  void postAudio();
  void addRoundTrip(long roundTripMillis);

  QuizSpec getQuizSpec();
}
