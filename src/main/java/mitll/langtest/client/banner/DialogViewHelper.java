package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 4/5/17.
 */
class DialogViewHelper<T extends CommonShell & ScoredExercise> extends SimpleChapterNPFHelper<T, IDialog> {
  //  private final Logger logger = Logger.getLogger("NewLearnHelper");

  /**
   * @param controller
   * @param viewContaner
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  DialogViewHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView) {
    super(controller, viewContaner, myView);
  }

  @Override
  protected FlexListLayout<T, IDialog> getMyListLayout(SimpleChapterNPFHelper<T, IDialog> outer) {
    return new MyFlexListLayout<T, IDialog>(controller, outer) {
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
      protected PagingExerciseList<T, IDialog> makeExerciseList(Panel topRow,
                                                                Panel currentExercisePanel,
                                                                String instanceName,
                                                                DivWidget listHeader,
                                                                DivWidget footer) {
        return new DialogExerciseList(topRow, currentExercisePanel, instanceName, listHeader);
      }
    };
  }

  protected ExercisePanelFactory<T, IDialog> getFactory(final PagingExerciseList<T, IDialog> exerciseList) {
    return new ExercisePanelFactory<T, IDialog>(controller, exerciseList) {

      @Override
      public Panel getExercisePanel(IDialog e) {
        return null;
        //return new TwoColumnExercisePanel<>(e, controller, exerciseList, alignments, false);
      }
    };
  }

  private class DialogExerciseList extends FacetExerciseList<T, IDialog> {
    public DialogExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader) {
      super(topRow, currentExercisePanel, DialogViewHelper.this.controller, new ListOptions(instanceName), listHeader, false);
    }

    @Override
    protected void getFullExercises(Collection<Integer> visibleIDs, int currentReq, Collection<Integer> requested, List<IDialog> alreadyFetched) {

    }

    @Override
    protected void goGetNextPage() {

    }
  }
}
