package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.DropdownButton;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.CommonExercise;
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
  private static final String ADD_ITEM = "Add Item to List";

  private DropdownButton addToList;
  private int activeCount = 0;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param addKeyHandler
   * @param instance
   * @see NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, long)
   */
  NPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer, float screenPortion,
              boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  @Override
  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                           ListInterface listContainer, boolean addKeyHandler) {
    NavigationHelper navigationHelper = super.getNavigationHelper(controller, listContainer, addKeyHandler);
    navigationHelper.add(makeAddToList(exercise, controller));
    return navigationHelper;
  }

  /**
   * @see #addQuestionContentRow
   * @param e
   * @param controller
   * @return
   */
  private Panel makeAddToList(CommonExercise e, ExerciseController controller) {
    addToList = new DropdownButton("");
    addToList.getElement().setId("NPFExercise_AddToList");
    addToList.setDropup(true);
    addToList.setIcon(IconType.PLUS_SIGN);
    addToList.setType(ButtonType.PRIMARY);
    addTooltip(addToList, ADD_ITEM);
    addToList.addStyleName("leftFiveMargin");
    populateListChoices(e, controller, addToList);
    return addToList;
  }

  /**
   * @see Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
   */
  @Override
  public void wasRevealed() { populateListChoices(exercise, controller, addToList);  }

  /**
   * Ask server for the set of current lists for this user.
   *
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   *
   * @param e
   * @param controller
   * @param w1
   * @see #makeAddToList(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController)
   * @see #wasRevealed()
   */
  private void populateListChoices(final CommonExercise e, final ExerciseController controller, final DropdownBase w1) {
    //System.out.println("populateListChoices : populate list choices for " + controller.getUser());
    service.getListsForUser(controller.getUser(), true, false, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<UserList> result) {
        w1.clear();
        activeCount = 0;
        boolean anyAdded = false;
        //System.out.println("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          if (!ul.contains(new UserExercise(e))) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                controller.logEvent(w1,"DropUp",e.getID(),"Adding to list " + ul.getID() +"/"+ul.getName());
                service.addItemToUserList(ul.getUniqueID(), new UserExercise(e, controller.getUser()), new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                  }

                  @Override
                  public void onSuccess(Void result) {
                    showPopup("Item Added!", w1);
                    widget.setVisible(false);
                    activeCount--;
                    if (activeCount == 0) {
                      NavLink widget = new NavLink("Item already added to your list(s)");
                      w1.add(widget);
                    }
                  }
                });
              }
            });
          }
        }
        if (!anyAdded) {
          NavLink widget = new NavLink("CommonExercise already added to your list(s)");
          w1.add(widget);
        }
      }
    });
  }

  private void showPopup(String html, Widget target) {
    Widget content = new HTML(html);
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(content);
    pleaseWait.showRelativeTo(target);
    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(2000);
  }
}
