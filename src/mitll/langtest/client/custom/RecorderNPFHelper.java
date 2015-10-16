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
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;

import java.util.Collection;
import java.util.Map;

/**
 * Created by go22670 on 2/10/15.
 */
class RecorderNPFHelper extends SimpleChapterNPFHelper {
  private static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";

  final boolean doNormalRecording;
  boolean added = false;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param doNormalRecording
   * @see Navigation#Navigation
   */
  public RecorderNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                           ExerciseController controller, boolean doNormalRecording, ListInterface exerciseList) {
    super(service, feedback, userManager, controller, exerciseList);
    this.doNormalRecording = doNormalRecording;
  }

  @Override
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    final String oinstance = exerciseList.getInstance();
    return new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        return new MyWaveformExercisePanel(e, controller, exerciseList, oinstance);
      }
    };
  }

  @Override
  protected NPFHelper.FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                     final UserManager userManager, ExerciseController controller,
                                                     SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
      @Override
      protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName,
                                                         boolean incorrectFirst) {
        return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst) {
          private CheckBox filterOnly;

          @Override
          protected void addTableWithPager(PagingContainer pagingContainer) {
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


  final FlexTable flex = new FlexTable();

  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
    getProgressInfo();
    return flex;
  }

  private void getProgressInfo() {
    service.getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Float> result) {
        float total = result.get("total");

        //  logger.debug("ref audio coverage " + result);
//         System.out.println("\n\n\nref audio coverage " + result);

        int r = 0;
        int col = 0;

        String sp = "&nbsp;";
        String p = "<b>";
        String s = "</b>" + sp;
        flex.setHTML(r, col++, "Male ");
        flex.setHTML(r, col++, sp + result.get("male").intValue() + "");
        flex.setHTML(r, col++, p + getPercent(result.get("male"), total) + "%" + s);

        flex.setHTML(r, col++, "regular");
        flex.setHTML(r, col++, sp + result.get("maleFast").intValue());
        flex.setHTML(r, col++, p + getPercent(result.get("maleFast"), total) + "%" + s);

        flex.setHTML(r, col++, "slow");
        flex.setHTML(r, col++, sp + result.get("maleSlow").intValue());
        flex.setHTML(r++, col++, p + getPercent(result.get("maleSlow"), total) + "%" + s);

        col = 0;
        flex.setHTML(r, col++, "Female");
        flex.setHTML(r, col++, sp + result.get("female").intValue());
        flex.setHTML(r, col++, p + getPercent(result.get("female"), total) + "%" + s);

        flex.setHTML(r, col++, "regular");
        flex.setHTML(r, col++, sp + result.get("femaleFast").intValue());
        flex.setHTML(r, col++, p + getPercent(result.get("femaleFast"), total) + "%" + s);

        flex.setHTML(r, col++, "slow");
        flex.setHTML(r, col++, sp + result.get("femaleSlow").intValue());
        flex.setHTML(r++, col++, p + getPercent(result.get("femaleSlow"), total) + "%" + s);
        col = 0;

        flex.setHTML(r, col++, "Context Male ");
        flex.setHTML(r, col++, sp + result.get("maleContext").intValue() + "");
        flex.setHTML(r, col++, p + getPercent(result.get("maleContext"), total) + "%" + s);

        flex.setHTML(r, col++, "Female");
        flex.setHTML(r, col++, sp + result.get("femaleContext").intValue() + "");
        flex.setHTML(r, col++, p + getPercent(result.get("femaleContext"), total) + "%" + s);
        // do the next one...
      }
    });
  }

  private int getPercent(Float male, float total) {
    float ratio = total > 0 ? male / (total) : 0;
    return (int) (ratio * 100f);
  }

  private class MyWaveformExercisePanel extends WaveformExercisePanel {
    private final CommonExercise e; // TODO REMOVE!

    public MyWaveformExercisePanel(CommonExercise e, ExerciseController controller1, ListInterface exerciseList1, String instance) {
      super(e, service, controller1, exerciseList1, RecorderNPFHelper.this.doNormalRecording, instance);
      this.e = e;
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

    @Override
    public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
      super.postAnswers(controller, completedExercise);
      tellOtherListExerciseDirty(e);
    }
  }
}
