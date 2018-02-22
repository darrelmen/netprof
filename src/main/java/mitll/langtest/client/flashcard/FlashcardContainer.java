package mitll.langtest.client.flashcard;

interface FlashcardContainer {

  void reload();

  void startOver();

  void loadFirstExercise();

  int getNumExercises();

  void styleContent(boolean showCard);
}
