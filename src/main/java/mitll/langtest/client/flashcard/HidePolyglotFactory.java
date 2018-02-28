package mitll.langtest.client.flashcard;

import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

public class HidePolyglotFactory<L extends CommonShell, T extends CommonExercise> extends PolyglotFlashcardFactory<L,T>{
  public HidePolyglotFactory(ExerciseController controller, ListInterface<L,T> exerciseList, String instance) {
    super(controller, exerciseList, instance);
  }

  @NotNull
  @Override
  protected PolyglotPracticePanel<L,T> getCurrentFlashcard(T e) {
    return new HidePolyglotPanel<L,T>(this,
        controlState,
        controller,
        soundFeedback,
        prompt,
        e.getCommonAnnotatable(),
        sticky,
        exerciseList);
  }
}
