package mitll.langtest.client.flashcard;

public interface FlashcardContainer {
  void startTimedRun();

  boolean getIsDry();

  void cancelRoundTimer();

  PolyglotDialog.MODE_CHOICE getMode();

  long getSessionStartMillis();

  boolean isInLightningRound();

  void reload();

  void startOver();

  void loadFirstExercise();

  long getRoundTimeLeftMillis();

  int getNumExercises();

  void showDrill();

  void styleContent(boolean showCard);
}
