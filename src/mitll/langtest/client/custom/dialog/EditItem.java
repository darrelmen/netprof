/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
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
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Coordinates editing an item -
 * makes a panel that has the list and content on the right
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  final Logger logger = Logger.getLogger("EditItem");

  public static final String NEW_ITEM = "*New Item*";
  public static final String NEW_EXERCISE_ID = "NewExerciseID";
  private static final String EDIT_ITEM = "editItem";

  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;

  private final ReloadableContainer predefinedContentList;

  private UserFeedback feedback = null;
  private HasText itemMarker;

  private PagingExerciseList<CommonShell, CommonExercise> exerciseList;
  private final String instanceName;

  /**
   * @param service
   * @param userManager
   * @param controller
   * @see mitll.langtest.client.custom.ListManager#ListManager
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                  ReloadableContainer predefinedContentList, UserFeedback feedback
  ) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
    this.predefinedContentList = predefinedContentList;
    this.feedback = feedback;
    this.instanceName = "EditItem";//instanceName;
  }

  /**
   * @param originalList
   * @param itemMarker
   * @param includeAddItem
   * @return
   * @see mitll.langtest.client.custom.ListManager#showEditItem
   */
  public Panel editItem(UserList<CommonShell> originalList, final HasText itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_" + originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    UserList<CommonShell> copy = makeListOfOnlyYourItems(originalList);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, copy, originalList, includeAddItem);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  public void onResize() {
    if (exerciseList != null) {
      //  logger.info("EditItem onResize");
      exerciseList.onResize();
    } /*else {
      logger.info("EditItem onResize - no exercise list");

    }*/
  }

  private UserList<CommonShell> makeListOfOnlyYourItems(UserList<CommonShell> toCopy) {
    return toCopy.getCopy();
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
  private PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel right,
                                                                           String instanceName,
                                                                           UserList<CommonShell> ul,
                                                                           UserList<CommonShell> originalList,
                                                                           final boolean includeAddItem) {
    //logger.info("EditItem.makeExerciseList - ul = " + ul + " " + includeAddItem);

    if (includeAddItem) {
      CommonExercise newItem = getNewItem();
      //logger.info("makeExerciseList : Adding " + newItem);// + " with " + newItem.getTooltip());
      ul.addExercise(newItem);
    }

    final PagingExerciseList<CommonShell, CommonExercise> exerciseList =
        new NPExerciseList(right, service, feedback, controller,
            true, instanceName, false, false) {
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
              //     logger.info("EditItem.makeExerciseList - askServerForExercise = " + itemID);
              super.askServerForExercise(itemID);
            }
          }

          @Override
          public Collection<CommonShell> rememberExercises(Collection<CommonShell> result) {
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
    // logger.info("setting vertical on " +exerciseList.getElement().getExID());
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
   * TODOx : EVIL how to avoid unchecked cast here???
   *
   * @return
   */
  private CommonExercise getNewItem() {
    return new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");
  }

  private void setFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                          final UserList<CommonShell> ul,
                          final UserList<CommonShell> originalList) {
    final PagingExerciseList<CommonShell, CommonExercise> outer = exerciseList;

    exerciseList.setFactory(new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        Panel panel = new ResizableSimple();
        panel.getElement().setId("EditItemPanel");
        // TODO : do something better here than toCommonUserExercise
        UserExercise userExercise = new UserExercise(e, e.getCreator());
        populatePanel(userExercise, panel, ul, originalList, itemMarker, outer);
        return panel;
      }
    });
  }

  private class ResizableSimple extends SimplePanel implements RequiresResize {

    @Override
    public void onResize() {
      Widget widget = getWidget();
      if (widget instanceof RequiresResize) {
        ((RequiresResize) widget).onResize();
      } else {
        logger.info("skipping " + widget.getElement().getId());
      }
    }
  }

  /**
   * @param ul
   * @param npfExerciseList
   * @see #editItem
   */
  private void rememberAndLoadFirst(final UserList<CommonShell> ul, PagingExerciseList<CommonShell, CommonExercise> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<CommonShell> userExercises = new ArrayList<>();
    Collection<CommonShell> exercises = ul.getExercises();
    for (CommonShell e : exercises) {
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
  private void populatePanel(CommonExercise exercise,
                             final Panel right,
                             final UserList<CommonShell> ul,
                             final UserList<CommonShell> originalList,
                             final HasText itemMarker,
                             final ListInterface<CommonShell> pagingContainer) {
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
   * @see #populatePanel
   */
  private void addEditOrAddPanel(CommonExercise newExercise, HasText itemMarker,
                                 UserList<CommonShell> originalList,
                                 Panel right,
                                 UserList<CommonShell> ul,
                                 ListInterface<CommonShell> pagingContainer,
                                 boolean doNewExercise, boolean setFields) {
    NewUserExercise editableExercise = getAddOrEditPanel(newExercise, itemMarker, originalList, doNewExercise);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    if (setFields) editableExercise.setFields(newExercise);
  }

  void clearNewExercise() {
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
  private NewUserExercise getAddOrEditPanel(CommonExercise exercise, HasText itemMarker,
                                            UserList<CommonShell> originalList, boolean doNewExercise) {
    NewUserExercise editableExercise;
    if (doNewExercise) { // whole new exercise
      editableExercise = new NewUserExercise(service, controller, itemMarker, this, exercise, getInstance(), originalList);
    } else {
      boolean iCreatedThisItem = didICreateThisItem(exercise) || (userManager.isTeacher() && !exercise.isPredefined());  // asked that teachers be able to record audio for other's items
      if (iCreatedThisItem) {  // it's mine!
        editableExercise = new EditableExerciseDialog(service, controller, this, itemMarker, exercise,
            originalList,
            exerciseList,
            predefinedContentList,
            getInstance()
        );
      } else {
        editableExercise = new RemoveFromListOnlyExercise(itemMarker, exercise, originalList);
      }
    }
    return editableExercise;
  }

  private String getInstance() {
    return instanceName;
  }

  /**
   * @param exercise
   * @return
   * @paramx originalList
   * @see EditItem#getAddOrEditPanel(CommonExercise, HasText, UserList, boolean)
   */
  private boolean didICreateThisItem(CommonExercise exercise) {
    boolean isMine = exercise.getCreator() == controller.getUser();
    // logger.info("for " + exercise + " vs " + controller.getUser() + " is Mine " + isMine);
    return isMine;
  }

  /**
   * @see #getAddOrEditPanel(CommonExercise, HasText, UserList, boolean)
   */
  private class RemoveFromListOnlyExercise extends NewUserExercise {
    RemoveFromListOnlyExercise(HasText itemMarker, CommonExercise exercise, UserList<CommonShell> originalList) {
      super(EditItem.this.service, EditItem.this.controller, itemMarker, EditItem.this, exercise, EditItem.this.getInstance(), originalList);
    }

    @Override
    public Panel addNew(UserList<CommonShell> ul,
                        UserList<CommonShell> originalList,
                        ListInterface<CommonShell> listInterface,
                        Panel toAddTo) {
      final FluidContainer container = new FluidContainer();

      this.ul = ul;
      this.originalList = originalList;
      this.listInterface = listInterface;

      Button delete = makeDeleteButton(ul, originalList.getUniqueID());
      container.add(delete);
      return container;
    }

    Button makeDeleteButton(final UserList<CommonShell> ul, final long uniqueID) {
      Button delete = makeDeleteButton(ul);

      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
//          logger.info(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);
          // delete.setEnabled(false);
          String id = newUserExercise.getID();
          logger.info("RemoveFromListOnlyExercise.makeDeleteButton got click to delete " + id);
          deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, predefinedContentList);
        }
      });

      delete.addStyleName("topFiftyMargin");
      return delete;
    }

    @Override
    public <S extends CommonShell & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    }
  }
}
