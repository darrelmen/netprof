package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created by go22670 on 2/10/14.
 */
public class Combined extends TabContainer {

  private Tab yourItems;
  private Panel yourItemsContent;
  private Navigation.TabAndContent browse;
  private Navigation.TabAndContent review, commented;

  public Combined(final LangTestDatabaseAsync service, final UserManager userManager,
                  final ExerciseController controller, final ListInterface<? extends ExerciseShell> listInterface,
                  UserFeedback feedback) {}

  public Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

    // chapter tab
    final Navigation.TabAndContent chapters = makeTab(tabPanel, IconType.LIGHTBULB, LEARN_PRONUNCIATION);
    chapters.content.add(secondAndThird);

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
    tabPanel.selectTab(0);

  }

  @Override
  public void onResize() {

  }
}
