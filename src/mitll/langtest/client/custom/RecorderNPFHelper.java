package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
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
import mitll.langtest.shared.monitoring.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 2/10/15.
 */
class RecorderNPFHelper extends SimpleChapterNPFHelper {
  private static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";

  //private Navigation navigation;
  final boolean doNormalRecording;
  private String answer = "Answer";
  boolean added = false;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param doNormalRecording
   * @see Navigation#Navigation
   */
  public RecorderNPFHelper(Navigation navigation, LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                           ExerciseController controller, boolean doNormalRecording, ListInterface exerciseList) {
    super(service, feedback, userManager, controller, exerciseList);
    //  this.navigation = navigation;
    this.doNormalRecording = doNormalRecording;
    answer = controller.getProps().getNameForAnswer();
  }

  @Override
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    return new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        //logger.info("getting exercise for " + e.getID() + " normal rec " +doNormalRecording);
        return new MyWaveformExercisePanel(e, controller, exerciseList);
      }
    };
  }

  @Override
  protected NPFHelper.FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                     UserManager userManager, ExerciseController controller,
                                                     SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
      @Override
      protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName,
                                                         boolean incorrectFirst) {
        return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst) {
          @Override
          protected void addTableWithPager(PagingContainer pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            final CheckBox w = new CheckBox(SHOW_ONLY_UNRECORDED);
            w.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                setUnrecorded(w.getValue());
                scheduleWaitTimer();
                loadExercises(getHistoryToken(""), getTypeAheadText(), false);
              }
            });
            w.addStyleName("leftFiveMargin");
            add(w);

            // row 3
            add(pagingContainer.getTableWithPager());
            setOnlyExamples(!doNormalRecording);
          }
        };

      }
    };
  }

//
//  public Widget getRecordingInfo() {
////    Widget label = new HTML();
//    final FlexTable flex=  new FlexTable();
//    // flex.setCellSpacing(8);
//    flex.addStyleName("leftFifteenPercentMargin");
//    service.get(new AsyncCallback<Map<String, Number>>() {
//      @Override
//      public void onFailure(Throwable caught) {
//      }
//
//      @Override
//      public void onSuccess(Map<String, Number> result) {
//        Number total = result.get("totalHrs");
//        final double totalHours = total.doubleValue();
//        final double avgSecs = result.get("avgSecs").doubleValue();
//        final int badRecordings = result.get("badRecordings").intValue();
//
//        service.getSessions(new AsyncCallback<List<Session>>() {
//          @Override
//          public void onFailure(Throwable caught) {
//          }
//
//          @Override
//          public void onSuccess(List<Session> sessions) {
//            long totalTime = 0;
//            long total = 0;
//            long valid = sessions.size();
//            Map<Long, Integer> rateToCount = new HashMap<Long, Integer>();
//            for (Session s : sessions) {
//              totalTime += s.duration;
//              total += s.getNumAnswers();
//
//              Integer count = rateToCount.get(s.getSecAverage());
//              if (count == null) rateToCount.put(s.getSecAverage(), s.getNumAnswers());
//              else rateToCount.put(s.getSecAverage(), count + s.getNumAnswers());
//            }
//            int row = 0;
//            int col = 0;
//
//            long hoursSpent = totalTime / HOUR;
//            double dhoursSpent = (double) hoursSpent;
//            flex.setText(row, col++, "Time spent");
//            flex.setHTML(row, col++, "&nbsp;<b>" + hoursSpent + " hours " + (totalTime - hoursSpent * HOUR) / MIN + " mins</b>&nbsp;");
//
//            flex.setText(row, col++, "Audio collected");
//            flex.setHTML(row, col++, "&nbsp;<b>" + roundToHundredth(totalHours) + " hours</b>&nbsp;");
//
//            if (dhoursSpent > 0) {
//              flex.setText(row, col++, "Yield");
//              flex.setHTML(row, col++, "&nbsp;<b>" + "" + Math.round((totalHours / dhoursSpent) * 100) + "%</b>&nbsp;");
//            }
//
//            flex.setText(row, col++, "Total " + "Recordings");
//            flex.setHTML(row, col++, "&nbsp;<b>" + "" + total + "</b>&nbsp;");
//          }
//        });
//      }
//    });
//
//    return flex;
//  }

  final FlexTable flex = new FlexTable();
  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
    // flex.setCellSpacing(8);
//    flex.addStyleName("leftFifteenPercentMargin");
    //   flex.getElement().getStyle().setMarginLeft(3, Style.Unit.PCT);
    getProgressInfo();
    return flex;
  }

  private void getProgressInfo() {
    service.getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Map<String, Float> result) {
        //    vp.add(new HTML("<h2>Male/Female Reference Audio Coverage</h2>"));
        //    FlexTable flex = new FlexTable();
        //    vp.add(flex);

        float total = result.get("total");
        //   Float male = result.get("male");

        //  logger.debug("ref audio coverage " + result);
         System.out.println("\n\n\nref audio coverage " + result);

        int r = 0;

        int row = 0;
        int col = 0;

//        flex.setHTML(r, col++, "Progress");
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
    private final CommonExercise e;

    public MyWaveformExercisePanel(CommonExercise e, ExerciseController controller1, ListInterface exerciseList1) {
      super(e, service, controller1, exerciseList1, RecorderNPFHelper.this.doNormalRecording);
      this.e = e;
      //    add(doMaleFemale());
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      if (!added) {
        //  getParent().addStyleName("userNPFContentLightPadding");
        ((Panel) getParent().getParent()).add(doMaleFemale());
        added = true;
      }
      else {
        System.out.println("get progress info...");
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
