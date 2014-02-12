package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.ExerciseListLayout;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created by go22670 on 2/10/14.
 */
public class Combined<T extends ExerciseShell> extends TabContainer {
//  private Tab yourItems;
 // private Panel yourItemsContent;
//  private Navigation.TabAndContent browse;
 // private Navigation.TabAndContent review, commented;
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  ListInterface<T> listInterface1;
  //private ScrollPanel listScrollPanel;
//  private ListInterface<? extends ExerciseShell> listInterface;
  private UserFeedback feedback;

  public Combined(final LangTestDatabaseAsync service, final UserManager userManager,
                  final ExerciseController controller, final ListInterface<? extends ExerciseShell> listInterface,
                  UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
  //  this.listInterface = listInterface;
    this.feedback = feedback;
  }

  public /*<T extends ExerciseShell> */Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

    // chapter tab
    final Navigation.TabAndContent chapters = makeTab(tabPanel, IconType.LIGHTBULB, LEARN_PRONUNCIATION);
    chapters.content.add(secondAndThird);

    final Navigation.TabAndContent practice = makeTab(tabPanel, IconType.CHECK,  PRACTICE);

    ExerciseListLayout<T> layout = new ExerciseListLayout<T>(controller.getProps());

    Panel currentExerciseVPanel = new FluidContainer();

    Panel thirdRow = new HorizontalPanel();
    Panel leftColumn = new SimplePanel();
    thirdRow.add(leftColumn);
    leftColumn.addStyleName("floatLeft");
    thirdRow.getElement().setId("outerThirdRow");
    thirdRow.setWidth("100%");
    thirdRow.addStyleName("trueInlineStyle");

    Panel bothSecondAndThird = new FlowPanel();
    FluidRow secondRow = new FluidRow();

    bothSecondAndThird.add(secondRow);
    bothSecondAndThird.add(thirdRow);

    currentExerciseVPanel.addStyleName("floatLeftList");
    thirdRow.add(currentExerciseVPanel);

    practice.content.add(bothSecondAndThird);


   // ListInterface<T> listInterface1 =
     listInterface1 =
      layout.makeExerciseList(secondRow, leftColumn, feedback, currentExerciseVPanel, service, controller);

    listInterface1.setFactory(
      new MyFlashcardExercisePanelFactory<T>(service, feedback, controller, listInterface1), userManager, 1);

    if (controller.gotMicPermission()) {
      listInterface1.getExercises(controller.getUser(), true);
    }

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        //System.out.println("getButtonRow2 : got shown event : '" +showEvent + "' target '" + targetName + "'");

        //boolean wasChapters = targetName.contains(CHAPTERS);
       /* Panel createdPanel = listInterface.getCreatedPanel();
        boolean hasCreated = createdPanel != null;
        if (hasCreated && wasChapters) {
          System.out.println("\tgot chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((GoodwaveExercisePanel) createdPanel).wasRevealed();
        }*/
      }
    });

    return tabPanel;    // TODO - consider how to tell panels when they are hidden by tab changes
  }

  @Override
  public void showInitialState() {
    tabPanel.selectTab(1); // for now
  }

  @Override
  public void onResize() {
    listInterface1.onResize();
  }
}
