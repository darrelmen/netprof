package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates editing an item -
 * makes a panel that has the list and content on the right
 *
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  private static final String NEW_ITEM = "*New Item*";
  public static final String NEW_EXERCISE_ID = "NewExerciseID";
  private static final String EDIT_ITEM = "editItem";

  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  private final UserManager userManager;

  protected final ListInterface predefinedContentList;

  private UserFeedback feedback = null;
  private HasText itemMarker;

  protected PagingExerciseList exerciseList;
  protected NPFHelper npfHelper;

  /**
   * @param service
   * @param userManager
   * @param controller
   * @param npfHelper
   * @see mitll.langtest.client.custom.Navigation#Navigation
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                  ListInterface listInterface, UserFeedback feedback, NPFHelper npfHelper) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
    this.predefinedContentList = listInterface;
    this.feedback = feedback;
    this.npfHelper = npfHelper;
    //System.out.println(getClass() + " : npfHelper " + npfHelper);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#showEditItem
   * @param originalList
   * @param itemMarker
   * @param includeAddItem
   * @return
   */
  public Panel editItem(UserList originalList, final HasText itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_"+originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    UserList copy = makeListOfOnlyYourItems(originalList);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, copy, originalList, includeAddItem);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  public void onResize() { if (exerciseList != null) exerciseList.onResize(); }

  private UserList makeListOfOnlyYourItems(UserList toCopy) {
    UserList copy2 = new UserList(toCopy);
    for (CommonUserExercise ue : toCopy.getExercises()) {
      copy2.addExercise(ue);
    }
    return copy2;
  }

  /**
   * @see #editItem
   * @param right
   * @param instanceName
   * @param ul
   * @param originalList
   * @param includeAddItem
   * @return
   */
  private PagingExerciseList makeExerciseList(Panel right, String instanceName, UserList ul, UserList originalList,
                                                 final boolean includeAddItem) {
    System.out.println("EditItem.makeExerciseList - ul = " + ul.getName() + " " + includeAddItem);

    if (includeAddItem) {
      UserExercise newItem = getNewItem();
      System.out.println("makeExerciseList : Adding " + newItem + " with " + newItem.getTooltip());
      ul.addExercise(newItem);
    }

    final PagingExerciseList exerciseList =
      new PagingExerciseList(right, service, feedback, null, controller,
        true, instanceName) {
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
        protected void askServerForExercise(String itemID) {
          if (itemID.equals(NEW_EXERCISE_ID)) {
            useExercise(getNewItem());
          }
          else {
            System.out.println("EditItem.makeExerciseList - askServerForExercise = "  + itemID);

            super.askServerForExercise(itemID);
          }
        }

        @Override
        protected List<CommonShell> rememberExercises(List<CommonShell> result) {
          clear();
          boolean addNewItem = includeAddItem;

          for (final CommonShell es : result) {
            addExercise(es);
            if (includeAddItem && es.getID().equals(NEW_EXERCISE_ID)) {
              addNewItem = false;
            }
          }

          if (addNewItem) {
            addExercise(getNewItem());  // TODO : fix this
          }
          flush();
          return result;
        }
      };
    setFactory(exerciseList, ul, originalList);
    exerciseList.setUnaccountedForVertical(280);   // TODO do something better here
   // System.out.println("setting vertical on " +exerciseList.getElement().getId());
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  private UserExercise getNewItem() {
    return new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");
  }

  private void setFactory(final PagingExerciseList exerciseList, final UserList ul, final UserList originalList) {
    final PagingExerciseList outer = exerciseList;

    exerciseList.setFactory(new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        Panel panel = new SimplePanel();
        panel.getElement().setId("EditItemPanel");
        // TODO : do something better here than toCommonUserExercise
        populatePanel(e.toCommonUserExercise(), panel, ul, originalList, itemMarker, outer);
        return panel;
      }
    }, userManager, 1);
  }

  /**
   * @see #editItem
   * @param ul
   * @param npfExerciseList
   */
  private void rememberAndLoadFirst(final UserList ul, PagingExerciseList npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<CommonShell> userExercises = new ArrayList<CommonShell>();
    for (CommonShell e : ul.getExercises()) {
      userExercises.add(e);  // TODO something better here
    }
    npfExerciseList.rememberAndLoadFirst(userExercises);
  }

  private UserExercise newExercise;

  /**
   * @see #setFactory(mitll.langtest.client.list.PagingExerciseList, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList)
   * @param exercise
   * @param right
   * @param ul
   * @param originalList
   * @param itemMarker
   * @param pagingContainer
   */
  private void populatePanel(CommonUserExercise exercise, final Panel right, final UserList ul, final UserList originalList, final HasText itemMarker,
                             final ListInterface pagingContainer) {
    System.out.println("exercise " + exercise.getAudioAttributes());
    if (exercise.getID().equals(NEW_EXERCISE_ID)) {
      if (newExercise == null) {
        newExercise = createNewItem(userManager.getUser());
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, false);

      } else {
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, true);
      }
    } else {
      addEditOrAddPanel(exercise, itemMarker, originalList, right, ul, pagingContainer, false, true);
    }
  }

  private UserExercise createNewItem(long userid) {
    return new UserExercise(-1, UserExercise.CUSTOM_PREFIX + Long.MAX_VALUE, userid, "", "", "");
  }

  /**
   * @see #populatePanel(mitll.langtest.shared.CommonUserExercise, com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HasText, mitll.langtest.client.list.ListInterface)
   * @param newExercise
   * @param itemMarker
   * @param originalList
   * @param right
   * @param ul
   * @param pagingContainer
   * @param doNewExercise
   * @param setFields
   */
  private void addEditOrAddPanel(CommonUserExercise newExercise, HasText itemMarker, UserList originalList,
                                   Panel right, UserList ul, ListInterface pagingContainer, boolean doNewExercise, boolean setFields) {
    NewUserExercise editableExercise = getAddOrEditPanel(newExercise, itemMarker, originalList, doNewExercise);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    if (setFields) editableExercise.setFields(newExercise);
  }

  public void clearNewExercise() {  this.newExercise = null;  }

  /**
   * We can go three ways:
   *
   * 1) Make a new item
   * 2) Edit an item that I created
   * 3) Edit an item on the list made by someone else - all I can do then is remove it from my list.
   *
   * @see #populatePanel
   * @param exercise
   * @param itemMarker
   * @param originalList
   * @param doNewExercise
   * @return
   */
  private NewUserExercise getAddOrEditPanel(CommonUserExercise exercise, HasText itemMarker, UserList originalList, boolean doNewExercise) {
    NewUserExercise editableExercise;
    if (doNewExercise) {
      editableExercise = new NewUserExercise(service, controller, itemMarker, this, exercise);
    } else {
      boolean iCreatedThisItem = didICreateThisItem(exercise, originalList);
      if (iCreatedThisItem) {
        editableExercise = new EditableExercise(service, controller, this, itemMarker, exercise.toUserExercise(),
          originalList,
          exerciseList,
          predefinedContentList,
          npfHelper);
      }
      else {
        editableExercise = new NewUserExercise(service, controller, itemMarker, this, exercise) {
          @Override
          public Panel addNew(final UserList ul, final UserList originalList, ListInterface listInterface, Panel toAddTo) {
            final FluidContainer container = new FluidContainer();

            this.ul = ul;
            this.originalList = originalList;
            this.listInterface = listInterface;

            Button delete = makeDeleteButton(ul, originalList.getUniqueID());

            container.add(delete);
            return container;
          }

          public Button makeDeleteButton(final UserList ul,  final long uniqueID) {
            Button delete = makeDeleteButton(ul);

            delete.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                System.out.println(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);

                deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, npfHelper.npfExerciseList);
              }
            });

            delete.addStyleName("topFiftyMargin");
            return delete;
          }

          @Override
          public void setFields(CommonExercise newUserExercise) {}
        };

      }
    }
    return editableExercise;
  }

  private boolean didICreateThisItem(CommonUserExercise exercise, UserList originalList) {
    String id = exercise.getID();
    long creator = getCreator(originalList, id);
    return creator == controller.getUser();
  }

  private long getCreator(UserList originalList, String id) {
    for (CommonUserExercise ue : originalList.getExercises()) {
      if (ue.getID().equals(id)) {
        return ue.getCreator();
      }
    }
    return -1;
  }

}
