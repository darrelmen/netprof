package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFExercise extends GoodwaveExercisePanel {
  private SplitDropdownButton addToList;
  private int activeCount = 0;

  /**
   * @see mitll.langtest.client.custom.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList)
   *
   * @param e
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param addKeyHandler
   * @param instance
   */
  public NPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer, float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  private Panel makeAddToList(Exercise e, ExerciseController controller) {
    addToList = new SplitDropdownButton("Add Item to List");
    addToList.setIcon(IconType.PLUS_SIGN);

    System.out.println("makeAddToList : populate list choices for " + controller.getUser());

    populateListChoices(e, controller, addToList);
    addToList.setType(ButtonType.PRIMARY);
    return addToList;
  }

  /**
   * Ask server for the set of current lists for this user.
   * @param e
   * @param controller
   * @param w1
   * @see #makeAddToList(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController)
   * @see #wasRevealed()
   */
  private void populateListChoices(final Exercise e, final ExerciseController controller, final SplitDropdownButton w1) {
    System.out.println("populateListChoices : populate list choices for " + controller.getUser());
    service.getListsForUser(controller.getUser(), true, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<UserList> result) {
        w1.clear();
        activeCount = 0;
        boolean anyAdded = false;
        System.out.println("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          if (!ul.contains(new UserExercise(e))) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                service.addItemToUserList(ul.getUniqueID(), new UserExercise(e,controller.getUser()), new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                  }

                  @Override
                  public void onSuccess(Void result) {
                    showPopup("Item Added!");
                    widget.setVisible(false);
                    activeCount--;
                    if (activeCount == 0) {
                      NavLink widget = new NavLink("Exercise already added to your list(s)");
                      w1.add(widget);
                    }
                  }
                });
              }
            });
          }
        }
        if (!anyAdded) {
          NavLink widget = new NavLink("Exercise already added to your list(s)");
          w1.add(widget);
        }
      }
    });
  }

  public void wasRevealed() {
    System.out.println("\nwasRevealed : populate list choices for " + controller.getUser() + "\n\n");

    populateListChoices(exercise, controller, addToList);
  }

  @Override
  protected void addQuestionContentRow(Exercise e, ExerciseController controller, HorizontalPanel hp) {
      hp.getElement().setId("GoodwaveHorizontalPanel");
      Panel addToList = makeAddToList(e, controller);
      Widget questionContent = getQuestionContent(e, addToList);
      questionContent.addStyleName("floatLeft");
      hp.add(questionContent);
  }

  private void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(2000);
  }
}
