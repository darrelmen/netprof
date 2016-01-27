/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonUserExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Coordinates editing an item -
 * makes a panel that has the list and content on the right
 * <p>
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem<T extends CommonUserExercise, UL extends UserList<T>> {
  private final Logger logger = Logger.getLogger("EditItem");

  public static final String NEW_ITEM = "*New Item*";
  public static final String NEW_EXERCISE_ID = "NewExerciseID";
  private static final String EDIT_ITEM = "editItem";

  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  private final UserManager userManager;

  protected final ListInterface<T> predefinedContentList;

  private UserFeedback feedback = null;
  private HasText itemMarker;

  protected PagingExerciseList<T> exerciseList;
  protected final NPFHelper npfHelper;

  /**
   * @param service
   * @param userManager
   * @param controller
   * @param npfHelper
   * @see mitll.langtest.client.custom.Navigation#Navigation
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                  ListInterface<T> listInterface, UserFeedback feedback, NPFHelper<T, UL> npfHelper) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
    this.predefinedContentList = listInterface;
    this.feedback = feedback;
    this.npfHelper = npfHelper;
    //logger.info(getClass() + " : npfHelper " + npfHelper);
  }

  /**
   * @param originalList
   * @param itemMarker
   * @param includeAddItem
   * @return
   * @see mitll.langtest.client.custom.Navigation#showEditItem
   */
  public Panel editItem(UL originalList, final HasText itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_" + originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    UserList<T> copy = makeListOfOnlyYourItems(originalList);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, copy, originalList, includeAddItem);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  public void onResize() {
    if (exerciseList != null) exerciseList.onResize();
  }

  private UserList<T> makeListOfOnlyYourItems(UL toCopy) {
    UserList<T> copy2 = new UserList<>(toCopy);
    //UL copy2 = (UL) tUserList;
    for (T ue : toCopy.getExercises()) {
      copy2.addExercise(ue);
    }
    return copy2;
  }

  /**
   * @param right
   * @param instanceName
   * @param ul
   * @param originalList
   * @param includeAddItem
   * @return
   * @see #editItem
   */
  private PagingExerciseList<T> makeExerciseList(Panel right, String instanceName, UL ul, UL originalList,
                                                 final boolean includeAddItem) {
    logger.info("EditItem.makeExerciseList - ul = " + ul.getName() + " " + includeAddItem);

    if (includeAddItem) {
      T newItem = getNewItem();
      logger.info("makeExerciseList : Adding " + newItem);// + " with " + newItem.getTooltip());
      ul.addExercise(newItem);
    }

    final PagingExerciseList<T> exerciseList =
        new NPExerciseList<T>(right, service, feedback, controller,
            true, instanceName, false) {
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
            } else {
              logger.info("EditItem.makeExerciseList - askServerForExercise = " + itemID);

              super.askServerForExercise(itemID);
            }
          }

          @Override
          public List<T> rememberExercises(List<T> result) {
            clear();
            boolean addNewItem = includeAddItem;

            for (final T es : result) {
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
    // logger.info("setting vertical on " +exerciseList.getElement().getId());
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  /**
   * TODO : consider filling in context and context translation?
   * <p>
   * TODO : EVIL how to avoid unchecked cast here???
   *
   * @return
   */
  private T getNewItem() {
    UserExercise userExercise = new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");
    CommonUserExercise commonUserExercise = userExercise;
    T commonUserExercise2 = (T) commonUserExercise;
    return commonUserExercise2;
  }

  private void setFactory(final PagingExerciseList<T> exerciseList, final UL ul, final UL originalList) {
    final PagingExerciseList outer = exerciseList;

    exerciseList.setFactory(new ExercisePanelFactory<T>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(T e) {
        Panel panel = new SimplePanel();
        panel.getElement().setId("EditItemPanel");
        // TODO : do something better here than toCommonUserExercise
        populatePanel(new UserExercise(e), panel, ul, originalList, itemMarker, outer);
        return panel;
      }
    });
  }

  /**
   * @param ul
   * @param npfExerciseList
   * @see #editItem
   */
  private void rememberAndLoadFirst(final UL ul, PagingExerciseList<T> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<T> userExercises = new ArrayList<>();
    Collection<T> exercises = ul.getExercises();
    for (T e : exercises) {
      userExercises.add(e);  // TODO something better here
    }
    npfExerciseList.rememberAndLoadFirst(userExercises);
  }

  private UserExercise newExercise;

  /**
   * @param exercise
   * @param right
   * @param ul
   * @param originalList
   * @param itemMarker
   * @param pagingContainer
   * @see #setFactory(mitll.langtest.client.list.PagingExerciseList, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList)
   */
  private void populatePanel(T exercise, final Panel right, final UL ul, final UL originalList, final HasText itemMarker,
                             final ListInterface pagingContainer) {
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

  /**
   * TODO : consider filling in context and context translation?
   *
   * @param userid
   * @return
   */
  private UserExercise createNewItem(long userid) {
    return new UserExercise(-1, UserExercise.CUSTOM_PREFIX + Long.MAX_VALUE, userid, "", "", "");
  }

  /**
   * @param newExercise
   * @param itemMarker
   * @param originalList
   * @param right
   * @param ul
   * @param pagingContainer
   * @param doNewExercise
   * @param setFields
   * @see #populatePanel(mitll.langtest.shared.exercise.CommonUserExercise, com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HasText, mitll.langtest.client.list.ListInterface)
   */
  private void addEditOrAddPanel(CommonUserExercise newExercise, HasText itemMarker, UL originalList,
                                 Panel right, UL ul, ListInterface pagingContainer,
                                 boolean doNewExercise, boolean setFields) {
    NewUserExercise editableExercise = getAddOrEditPanel(newExercise, itemMarker, originalList, doNewExercise);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    if (setFields) editableExercise.setFields(newExercise);
  }

  public void clearNewExercise() {
    this.newExercise = null;
  }

  /**
   * We can go three ways:
   * <p>
   * 1) Make a new item
   * 2) Edit an item that I created
   * 3) Edit an item on the list made by someone else - all I can do then is remove it from my list.
   *
   * @param exercise
   * @param itemMarker
   * @param originalList
   * @param doNewExercise
   * @return
   * @see #populatePanel
   */
  private NewUserExercise getAddOrEditPanel(T exercise, HasText itemMarker, UL originalList, boolean doNewExercise) {
    NewUserExercise editableExercise;
    if (doNewExercise) {
      editableExercise = new NewUserExercise<T,UL>(service, controller, itemMarker, this, exercise, getInstance());
    } else {
      boolean iCreatedThisItem = didICreateThisItem(exercise, originalList);
      if (iCreatedThisItem) {
        editableExercise = new EditableExercise<UserExercise,UserList<UserExercise>>(service, controller, this, itemMarker, exercise.toUserExercise(),
            originalList,
            exerciseList,
            predefinedContentList,
            npfHelper);
      } else {
        editableExercise = new NewUserExercise<T,UL>(service, controller, itemMarker, this, exercise, getInstance()) {
          @Override
          public Panel addNew(final UL ul, final UL originalList, ListInterface<T> listInterface, Panel toAddTo) {
            final FluidContainer container = new FluidContainer();

            this.ul = ul;
            this.originalList = originalList;
            this.listInterface = listInterface;

            Button delete = makeDeleteButton(ul, originalList.getUniqueID());

            container.add(delete);
            return container;
          }

          public Button makeDeleteButton(final UL ul, final long uniqueID) {
            Button delete = makeDeleteButton(ul);

            delete.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                logger.info(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);

                deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, npfHelper.npfExerciseList);
              }
            });

            delete.addStyleName("topFiftyMargin");
            return delete;
          }

          @Override
          public void setFields(T newUserExercise) {
          }
        };
      }
    }
    return editableExercise;
  }

  private String getInstance() {
    return npfHelper.getInstanceName();
  }

  private boolean didICreateThisItem(T exercise, UL originalList) {
    String id = exercise.getID();
    long creator = getCreator(originalList, id);
    return creator == controller.getUser();
  }

  private long getCreator(UL originalList, String id) {
    for (T ue : originalList.getExercises()) {
      if (ue.getID().equals(id)) {
        return ue.getCreator();
      }
    }
    return -1;
  }
}
