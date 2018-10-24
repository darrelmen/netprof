package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

/**
 * @see mitll.langtest.client.banner.PracticeHelper#getFactory
 * @param <L>
 * @param <T>
 */
public class HidePolyglotFactory<L extends CommonShell, T extends ClientExercise> extends PolyglotFlashcardFactory<L,T>{
  public HidePolyglotFactory(ExerciseController controller, ListInterface<L,T> exerciseList, INavigation.VIEWS instance) {
    super(controller, exerciseList,instance);
  }

  @NotNull
  @Override
  protected PolyglotPracticePanel<L,T> getFlashcard(T e) {
    return new HidePolyglotPanel<L,T>(this,
        controlState,
        controller,
        soundFeedback,
        e,
        sticky,
        exerciseList,
        instance
    );
  }
}
