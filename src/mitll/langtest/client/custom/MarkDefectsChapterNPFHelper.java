package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

/**
 * Created by go22670 on 3/30/16.
 */
class MarkDefectsChapterNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  private static final String SHOW_ONLY_UNRECORDED = "Show Only Audio by Unknown Gender";

  private static final String MARK_DEFECTS1 = "markDefects";

  MarkDefectsChapterNPFHelper( LangTestDatabaseAsync service,
                                     UserFeedback feedback, UserManager userManager, ExerciseController controller,
                                     SimpleChapterNPFHelper learnHelper) {
    super(service, feedback, userManager, controller, learnHelper);
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(LangTestDatabaseAsync service,
                                                                        UserFeedback feedback,
                                                                        UserManager userManager,
                                                                        ExerciseController controller,
                                                                        SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(service, feedback, controller, outer) {
      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName, boolean incorrectFirst) {
        return new NPFlexSectionExerciseList(this, topRow, currentExercisePanel, instanceName, incorrectFirst) {
          private CheckBox filterOnly;

          @Override
          protected void addTableWithPager(ClickablePagingContainer<CommonShell> pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            filterOnly = new CheckBox(SHOW_ONLY_UNRECORDED);
            filterOnly.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                setDefaultAudioFilter(filterOnly.getValue());
                scheduleWaitTimer();
                loadExercises(getHistoryToken(""), getTypeAheadText(), false);
              }
            });
            filterOnly.addStyleName("leftFiveMargin");
            add(filterOnly);

            // row 3
            add(pagingContainer.getTableWithPager());
            //setOnlyExamples(!doNormalRecording);
          }

          private String setCheckboxTitle(UserManager userManager) {
            return SHOW_ONLY_UNRECORDED;// + (userManager.isMale() ? " by Males" : " by Females");
          }
        };
      }
    };
  }

  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        return new QCNPFExercise<CommonExercise>(e, controller, exerciseList, MARK_DEFECTS1);
      }
    };
  }
}
