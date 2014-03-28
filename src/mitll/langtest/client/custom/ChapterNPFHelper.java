package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ChapterNPFHelper extends NPFHelper {
  FlexListLayout flexListLayout;
  public ChapterNPFHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                          final UserManager userManager, final ExerciseController controller) {
    super(service, feedback, userManager, controller);
    final NPFHelper outer = this;
    this.flexListLayout = new FlexListLayout(service,feedback,userManager,controller) {
      @Override
      protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, String instanceName) {
        return outer.getFactory(exerciseList, instanceName);
      }
    };

    System.out.println(getClass() + " : ChapterNPFHelper " );
  }

  /**
   * Left and right components
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }
/*
  private FlexSectionExerciseList makeNPFExerciseList(final Panel topRow,Panel currentExercisePanel, String instanceName, long userListID) {
    final FlexSectionExerciseList exerciseList = makeExerciseList(topRow,currentExercisePanel, instanceName);
    exerciseList.setUserListID(userListID);

    setFactory(exerciseList, instanceName, userListID);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }*/

/*  private FlexSectionExerciseList makeExerciseList(final Panel topRow,Panel currentExercisePanel, final String instanceName) {
    return new FlexSectionExerciseList(topRow, currentExercisePanel, service, feedback, false, false, controller, true, instanceName) {
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
        System.out.println(getClass() + " : noSectionsGetExercises instanceName = " + instanceName + " for list " + userListID);
        loadExercises(getHistoryToken(""), getPrefix());
      }

      @Override
      protected void loadExercises(final Map<String, Collection<String>> typeToSection, final String item) {
        System.out.println(getClass() +".loadExercises : instance " + instance + " " + typeToSection + " and item '" + item + "'"+ " for list " + userListID);
        loadExercisesUsingPrefix(typeToSection, getPrefix());
      }
    };
  }*/
}
