/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.Collection;
import java.util.Map;

/**
 * Sets up recording both ref recordings and context ref recordings.
 * <p>
 * Created by go22670 on 2/10/15.
 * <p>
 * <T extends CommonShell & AudioRefExercise>
 */
class RecorderNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  private static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";

  private final boolean doNormalRecording;
  private boolean added = false;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param doNormalRecording
   * @see Navigation#Navigation
   */
  public RecorderNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                           ExerciseController controller, boolean doNormalRecording,
                           ReloadableContainer exerciseList) {
    super(service, feedback, userManager, controller, exerciseList);
    this.doNormalRecording = doNormalRecording;
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    final String oinstance = exerciseList.getInstance();
    return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        return new MyWaveformExercisePanel(e, controller, exerciseList, oinstance);
      }
    };
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                                        final UserManager userManager, ExerciseController controller,
                                                                        SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(service, feedback, controller, outer) {

      final FlexListLayout outerLayout = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName,
                                                                                 boolean incorrectFirst) {
        return new NPFlexSectionExerciseList(outerLayout, topRow, currentExercisePanel, instanceName, incorrectFirst) {
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
                setUnrecorded(filterOnly.getValue());
                scheduleWaitTimer();
                loadExercises(getHistoryToken(""), getTypeAheadText(), false);
              }
            });
            filterOnly.addStyleName("leftFiveMargin");
            add(filterOnly);

            // row 3
            add(pagingContainer.getTableWithPager());
            setOnlyExamples(!doNormalRecording);
          }

          @Override
          protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection, String prefix, boolean onlyWithAudioAnno) {
            super.loadExercisesUsingPrefix(typeToSection, prefix, onlyWithAudioAnno);
            filterOnly.setText(setCheckboxTitle(userManager));
          }

          private String setCheckboxTitle(UserManager userManager) {
            return SHOW_ONLY_UNRECORDED + (userManager.isMale() ? " by Males" : " by Females");
          }
        };

      }
    };
  }


  private final RecordingProgressTable flex = new RecordingProgressTable();

  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
    getProgressInfo();
    return flex;
  }

  /**
   * @see #doMaleFemale()
   * @see MyWaveformExercisePanel#onLoad()
   */
  private void getProgressInfo() {
    service.getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Map<String, Float> result) {
        flex.populate(result);
      }
    });
  }

  private class MyWaveformExercisePanel extends WaveformExercisePanel<CommonShell, CommonExercise> {
   // private final CommonExercise e; // TODO REMOVE!

    MyWaveformExercisePanel(CommonExercise e, ExerciseController controller1, ListInterface<CommonShell> exerciseList1, String instance) {
      super(e, service, controller1, exerciseList1, RecorderNPFHelper.this.doNormalRecording, instance);
     // this.e = e;
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      if (!added) {
        DivWidget c = new DivWidget();
        c.add(doMaleFemale());
        c.setWidth("100%");
        Widget parent = getParent().getParent();
        parent.removeStyleName("floatLeft");
        getParent().removeStyleName("floatLeft");
        ((Panel) parent).add(c);
        added = true;
      } else {
        getProgressInfo();
      }
    }

    /**
     * @param controller
     * @param completedExercise
     * @see RecorderNPFHelper.MyWaveformExercisePanel#postAnswers
     */
    @Override
    public void postAnswers(ExerciseController controller, HasID completedExercise) {
      super.postAnswers(controller, completedExercise);
      tellOtherListExerciseDirty(exercise);
    }
  }
}
