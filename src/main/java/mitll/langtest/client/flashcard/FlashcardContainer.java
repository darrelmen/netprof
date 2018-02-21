package mitll.langtest.client.flashcard;

public interface FlashcardContainer {

  void reload();

  void startOver();

  void loadFirstExercise();

  int getNumExercises();

  void styleContent(boolean showCard);
}
