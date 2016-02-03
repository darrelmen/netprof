package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;

/**
 * Created by go22670 on 1/5/16.
 */
public class NPExerciseList extends HistoryExerciseList<CommonShell,CommonExercise> {
  protected NPExerciseList(Panel currentExerciseVPanel,
                           LangTestDatabaseAsync service,
                           UserFeedback feedback,
                           ExerciseController controller,
                           boolean showTypeAhead,
                           String instance,
                           boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, controller, showTypeAhead, instance, incorrectFirst);
  }

  /**
   * @return
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   */
  protected ClickablePagingContainer<CommonShell> makePagingContainer() {
    final PagingExerciseList<CommonShell,CommonExercise> outer = this;
    pagingContainer =
        new PagingContainer<CommonShell>(controller,
            getVerticalUnaccountedFor(),
            getRole().equals(Result.AUDIO_TYPE_RECORDER)) {
          @Override
          protected void gotClickOnItem(CommonShell e) {
            outer.gotClickOnItem(e);
          }
        };
    return pagingContainer;
  }
}
