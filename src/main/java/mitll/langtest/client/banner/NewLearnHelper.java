package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 4/5/17.
 */
public class NewLearnHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
//  private final Logger logger = Logger.getLogger("NewLearnHelper");
  NewLearnHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView) {
    super(controller, viewContaner, myView);
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
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
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName,
                                                                                 DivWidget listHeader,
                                                                                 DivWidget footer) {
        return new NPFlexSectionExerciseList(controller, topRow, currentExercisePanel,
            new ListOptions(instanceName), listHeader, false);
      }
    };
  }

  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(CommonExercise e) {
        return new TwoColumnExercisePanel<>(e, controller, exerciseList, alignments);
      }
    };
  }
}
