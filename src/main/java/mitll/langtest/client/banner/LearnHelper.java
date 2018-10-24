package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ClientExerciseFacetExerciseList;
import mitll.langtest.client.list.ListFacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 4/5/17.
 */
class LearnHelper<T extends CommonShell & ScoredExercise> extends SimpleChapterNPFHelper<T, ClientExercise>
    implements IListenView {
  //  private final Logger logger = Logger.getLogger("LearnHelper");

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  LearnHelper(ExerciseController controller) {
    super(controller);
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
        return new ListFacetExerciseList<T>(
            topRow,
            currentExercisePanel,
            controller,
            new ListOptions(instanceName),
            listHeader,
            instanceName);
      }
    };
  }

  protected ExercisePanelFactory<T, ClientExercise> getFactory(final PagingExerciseList<T, ClientExercise> exerciseList) {
    return new ExercisePanelFactory<T, ClientExercise>(controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(ClientExercise e) {
        return new TwoColumnExercisePanel<>(e,
            controller,
            exerciseList,
            alignments,
            false,
            LearnHelper.this,
            e.isContext());
      }
    };
  }

  @Override
  public int getVolume() {
    return 100;
  }

  @Override
  public int getDialogSessionID() {
    return -1;
  }
}
