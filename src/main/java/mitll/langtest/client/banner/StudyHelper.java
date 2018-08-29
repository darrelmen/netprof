package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.list.StudyExerciseList;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/5/17.
 */
class StudyHelper<T extends CommonShell & ScoredExercise> extends LearnHelper<T> {
  //  private final Logger logger = Logger.getLogger("LearnHelper");

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  StudyHelper(ExerciseController controller) {
    super(controller);
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName, boolean fromClick) {
    int dialogFromURL = getDialogFromURL();
    controller.getDialogService().getDialog(dialogFromURL, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        // TODO fill in
      }

      @Override
      public void onSuccess(IDialog dialog) {
        showDialogGetRef(dialogFromURL, dialog, listContent);
      }
    });
  }

  private void showDialogGetRef(int dialogFromURL, IDialog dialog, Panel child) {
    if (dialog == null) {
      child.add(new HTML("hmm can't find dialog #" + dialogFromURL + " in database?"));
    } else {
      DivWidget header = new DialogHeader(controller, getPrevView(), getNextView()).getHeader(dialog);
      header.addStyleName("bottomFiveMargin");
      child.add(header);
    }
    super.showContent(child, INavigation.VIEWS.STUDY, false);
    hideList();
  }

  @NotNull
  private INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.DIALOG;
  }

  @NotNull
  private INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.LISTEN;
  }

  private int getDialogFromURL() {
    return new SelectionState().getDialog();
  }

  @Override
  protected FlexListLayout<T, ClientExercise> getMyListLayout(SimpleChapterNPFHelper<T, ClientExercise> outer) {
    return new MyFlexListLayout<T, ClientExercise>(controller, outer) {
      /**
       * @see FlexListLayout#makeNPFExerciseList
       * @param topRow
       * @param currentExercisePanel
       * @param instanceName
       * @param listHeader
       * @param footer
       * @return
       */
      @Override
      protected PagingExerciseList<T, ClientExercise> makeExerciseList(Panel topRow,
                                                                       Panel currentExercisePanel,
                                                                       INavigation.VIEWS instanceName,
                                                                       DivWidget listHeader,
                                                                       DivWidget footer) {
        StudyExerciseList<T> widgets = new StudyExerciseList<>(
            topRow,
            currentExercisePanel,
            controller,
            new ListOptions(instanceName),
            listHeader,
            false);
        widgets.hideSectionPanel();
        return widgets;
      }
    };
  }
}
