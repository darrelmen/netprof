package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.Map;

/**
 * Created by GO22670 on 3/28/2014.
 */
public abstract class FlexListLayout implements RequiresResize {
  protected PagingExerciseList npfExerciseList;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserFeedback feedback;
  private final UserManager userManager;

  /**
   * @see ChapterNPFHelper#ChapterNPFHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, boolean)
   * @see mitll.langtest.client.custom.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public FlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                        UserManager userManager, ExerciseController controller) {
    this.controller = controller;
    this.service = service;
    this.feedback = feedback;
    this.userManager = userManager;
  }

  /**
   * @see mitll.langtest.client.custom.ChapterNPFHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   * @see mitll.langtest.client.custom.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

    Panel twoRows = new FlowPanel();
    twoRows.getElement().setId("twoRows");

    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeft");
    exerciseListContainer.getElement().setId("exerciseListContainer");

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

    FlowPanel currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("defect_currentExercisePanel");

    currentExerciseVPanel.addStyleName("floatLeftList");
    bottomRow.add(currentExerciseVPanel);

    long uniqueID = ul == null ? -1 : ul.getUniqueID();
    FlexSectionExerciseList widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID);
    npfExerciseList = widgets;

    Widget exerciseListOnLeftSide = npfExerciseList.getExerciseListOnLeftSide(controller.getProps());
    exerciseListContainer.add(exerciseListOnLeftSide);

    widgets.addWidgets();
    return twoRows;
  }

  private FlexSectionExerciseList makeNPFExerciseList(final Panel topRow, Panel currentExercisePanel, String instanceName, long userListID) {
    final FlexSectionExerciseList exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName);
    exerciseList.setUserListID(userListID);

    exerciseList.setFactory(getFactory(exerciseList, instanceName), userManager, 1);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  protected abstract ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName);

  protected FlexSectionExerciseList makeExerciseList(final Panel topRow, Panel currentExercisePanel, final String instanceName) {
    return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName);
  }

  @Override
  public void onResize() {
    if (npfExerciseList != null) {
      //System.out.println(getClass() + " : onResize " + npfExerciseList.getInstance());
      npfExerciseList.onResize();
    }
  }

  protected class MyFlexSectionExerciseList extends FlexSectionExerciseList {
//    private final String instanceName;

    public MyFlexSectionExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName) {
      super(topRow, currentExercisePanel, FlexListLayout.this.service, FlexListLayout.this.feedback, false, false, FlexListLayout.this.controller, true, instanceName);
  //    this.instanceName = instanceName;
    }

    @Override
    protected void onLastItem() {
      new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
        @Override
        public void onHidden(HiddenEvent hiddenEvent) {
          reloadExercises();
        }
      });
    }

    @Override
    protected void noSectionsGetExercises(long userID) {
     // System.out.println(getClass() + " : noSectionsGetExercises instanceName = " + instanceName + " for list " + userListID);
      loadExercises(getHistoryToken(""), getPrefix());
    }

    @Override
    protected void loadExercises(final Map<String, Collection<String>> typeToSection, final String item) {
      System.out.println(getClass() + ".loadExercises : instance " + getInstance() + " " + typeToSection + " and item '" + item + "'" + " for list " + userListID);
      loadExercisesUsingPrefix(typeToSection, getPrefix());
    }
  }
}
