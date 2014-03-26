package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ChapterNPFHelper extends NPFHelper {
  public ChapterNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback,
                          UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller);

    System.out.println(getClass() + " : ChapterNPFHelper " );

  }

  /**
   * Left and right components
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

   // Panel vp = new VerticalPanel();
    Panel twoRows = new FlowPanel();
    twoRows.getElement().setId("twoRows");
/*    FluidRow topRow;
    vp.add(topRow = new FluidRow());*/
    // FluidRow bottomListAndContent;
    //vp.add(bottomListAndContent = new FluidRow());

  /*  Panel bottomRow = new HorizontalPanel();
    vp.add(bottomRow);
*/
/*    Panel leftSide = new SimplePanel();
    bottomRow.add(leftSide);
    Panel rightSide = new SimplePanel();
    bottomRow.add(rightSide);*/


    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeft");
    exerciseListContainer.getElement().setId("exerciseListContainer");

   // Panel twoRows = new FlowPanel();

    // second row ---------------
    FluidRow topRow = new FluidRow();
    topRow.getElement().setId("topRow");

    twoRows.add(topRow);

    Panel bottomRow = new HorizontalPanel();
    bottomRow.add(exerciseListContainer);
    bottomRow.getElement().setId("bottomRow");
    bottomRow.setWidth("100%");
    bottomRow.addStyleName("trueInlineStyle");

    twoRows.add(bottomRow);

    FlowPanel   currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("defect_currentExercisePanel");

    currentExerciseVPanel.addStyleName("floatLeftList");
    bottomRow.add(currentExerciseVPanel);


/*
    Panel hp = new HorizontalPanel();
    Panel left = new SimplePanel();
    hp.add(left);
    left.addStyleName("floatLeft");*/

    // Panel rightSideContent = getRightSideContent(ul, instanceName);
    FlexSectionExerciseList widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, ul.getUniqueID());
    npfExerciseList = widgets;
   // npfContentPanel = rightSideContent;

    //hp.add(npfContentPanel);

    Widget exerciseListOnLeftSide = npfExerciseList.getExerciseListOnLeftSide(controller.getProps());
    exerciseListContainer.add(exerciseListOnLeftSide);

    widgets.addWidgets();
    return twoRows;
  }

/*  protected Panel getRightSideContent(UserList ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.addStyleName("floatRight");
    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_"+ul.getUniqueID(),ul.getUniqueID());
    return npfContentPanel;
  }*/

  FlexSectionExerciseList makeNPFExerciseList(final Panel topRow,Panel currentExercisePanel, String instanceName, long userListID) {
/*    Panel vp = new VerticalPanel();
    FluidRow topRow;
    vp.add(topRow = new FluidRow());
   // FluidRow bottomListAndContent;
    //vp.add(bottomListAndContent = new FluidRow());

    Panel thirdRow = new HorizontalPanel();
    vp.add(thirdRow);

    Panel leftColumn = new SimplePanel();
    thirdRow.add(leftColumn);*/

    final FlexSectionExerciseList exerciseList = makeExerciseList(topRow,currentExercisePanel, instanceName);



    setFactory(exerciseList, instanceName, userListID);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  FlexSectionExerciseList makeExerciseList(final Panel topRow,Panel currentExercisePanel, final String instanceName) {
    //Panel currentExercisePanel = null;
    return new FlexSectionExerciseList(topRow, currentExercisePanel, service, feedback,false,false, controller, true, instanceName) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }
    };
  }
}
