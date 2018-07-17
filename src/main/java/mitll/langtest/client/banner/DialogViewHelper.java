package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.dialog.IDialog;

/**
 * Created by go22670 on 4/5/17.
 */
public class DialogViewHelper  extends SimpleChapterNPFHelper<IDialog, IDialog> {
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
  protected FlexListLayout<IDialog, IDialog> getMyListLayout(SimpleChapterNPFHelper<IDialog, IDialog> outer) {
    return new MyFlexListLayout<IDialog, IDialog>(controller, outer) {
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
      protected PagingExerciseList<IDialog, IDialog> makeExerciseList(Panel topRow,
                                                                Panel currentExercisePanel,
                                                                String instanceName,
                                                                DivWidget listHeader,
                                                                DivWidget footer) {
        return new DialogExerciseList(topRow, currentExercisePanel, instanceName, listHeader, controller);
      }
    };
  }

  protected ExercisePanelFactory<IDialog, IDialog> getFactory(final PagingExerciseList<IDialog, IDialog> exerciseList) {
    return new ExercisePanelFactory<IDialog, IDialog>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(IDialog e) {     return null;      }
    };
  }
}
