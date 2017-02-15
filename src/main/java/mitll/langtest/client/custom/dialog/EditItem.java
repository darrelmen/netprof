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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.project.ProjectStartupInfo;

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
  private final Logger logger = Logger.getLogger("EditItem");

  /**
   * @deprecated
   */
  public static final String NEW_ITEM = "*New Item*";
  /**
   * @see #getNewItem
   * @see #makeExerciseList
   */
  public static final int NEW_EXERCISE_ID = -100;
  private static final String EDIT_ITEM = "editItem";

  private final ExerciseController controller;

  private final ReloadableContainer predefinedContentList;

  // private HasText itemMarker;

  private PagingExerciseList<CommonShell, CommonExercise> exerciseList;
  private final String instanceName;
//  private Exercise newExercise;

  /**
   * @param controller
   * @see mitll.langtest.client.custom.ListManager#ListManager
   */
  public EditItem(ExerciseController controller,
                  ReloadableContainer predefinedContentList) {
    this.controller = controller;
    this.predefinedContentList = predefinedContentList;
    this.instanceName = "EditItem";//instanceName;
  }

  /**
   * @param originalList
   * @return
   * @paramx itemMarker
   * @paramx includeAddItem
   * @see mitll.langtest.client.custom.ListManager#showEditItem
   */
  public Panel editItem(UserList<CommonShell> originalList) {//}, final HasText itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_" + originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    //  this.itemMarker = itemMarker; // TODO : something less awkward

    // TODO : why a copy here???
//    UserList<CommonShell> copy = makeListOfOnlyYourItems(originalList);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, originalList);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide());

    rememberAndLoadFirst(originalList, exerciseList);
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

/*
  private UserList<CommonShell> makeListOfOnlyYourItems(UserList<CommonShell> toCopy) {
    return toCopy.getCopy();
  }
*/

  /**
   * @param right
   * @param instanceName
   * @param originalList
   * @return
   * @paramz ul
   * @paramz includeAddItem
   * @see #editItem
   */
  private PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel right,
                                                                           String instanceName,
                                                                           //UserList<CommonShell> ul,
                                                                           UserList<CommonShell> originalList
                                                                           /*,
                                                                           final boolean includeAddItem*/) {
    //logger.info("EditItem.makeExerciseList - ul = " + ul + " " + includeAddItem);

/*
    if (includeAddItem) {
      CommonExercise newItem = getNewItem();
      //logger.info("makeExerciseList : Adding " + newItem);// + " with " + newItem.getTooltip());
      ul.addExercise(newItem);
    }
*/

    exerciseList =
        new EditableExerciseList(controller, this, right, instanceName, /*includeAddItem,*/ originalList);
    setFactory(exerciseList, originalList);
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

//  private EditableExerciseList editableExerciseList;

  /**
   * TODO : don't do it like this!
   * <p>
   * TODO : consider filling in context and context translation?
   * <p>
   *
   * @return
   * @see #makeExerciseList
   */
  public CommonExercise getNewItem() {
    int user = controller.getUserManager().getUser();
    Exercise exercise = new Exercise(
        NEW_EXERCISE_ID,
        user,
        "",
        getProjectid(),
        false);

    addContext(user, exercise);

    return exercise;
  }

  /**
   * TODO : Why two ways of creating a new exercise???
   * <p>
   * TODO : consider filling in context and context translation?
   *
   * @param userid
   * @return
   * @seex #populatePanel
   */
/*  public Exercise createNewItem(int userid) {
    Exercise exercise = new Exercise(-1,
        userid,
        "",
        getProjectid(),
        false);

    addContext(userid, exercise);

    return exercise;
  }*/
  private void addContext(int userid, Exercise exercise) {
    Exercise context = new Exercise(-1,
        userid,
        "",
        getProjectid(),
        false);

    exercise.addContextExercise(context);
  }

  private int getProjectid() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo == null ? -1 : projectStartupInfo.getProjectid();
  }

  private void setFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                          //final UserList<CommonShell> ul,
                          final UserList<CommonShell> originalList) {
    final PagingExerciseList<CommonShell, CommonExercise> outer = exerciseList;

    exerciseList.setFactory(new ExercisePanelFactory<CommonShell, CommonExercise>(
        controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise exercise) {
        Panel panel = new ResizableSimple();
        panel.getElement().setId("EditItemPanel");

        boolean iCreatedThisItem = didICreateThisItem(exercise) ||
            (controller.getUserManager().isTeacher() && !exercise.isPredefined());  // asked that teachers be able to record audio for other's items
        if (iCreatedThisItem) {  // it's mine!
          ReloadableContainer predefinedContentList = EditItem.this.predefinedContentList;
          EditableExerciseDialog editableExercise =
              new EditableExerciseDialog(controller,
                  exercise,
                  originalList,
                  outer,
                  predefinedContentList,
                  getInstance()
              );
          panel.add(editableExercise.addNew(outer, panel));
          editableExercise.setFields(exercise);
        }

        // Exercise userExercise = new Exercise(e);  // copy exercise??? TODO : why???
//        populatePanel(exercise, panel,/* ul,*/ originalList, /*itemMarker,*/ outer);
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
  private void rememberAndLoadFirst(final UserList<CommonShell> ul,
                                    PagingExerciseList<CommonShell, CommonExercise> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getID());
//    List<CommonShell> userExercises = new ArrayList<>();
//    List<CommonShell> exercises = ul.getExercises();
//    for (CommonShell e : exercises) {
//      userExercises.add(e);  // TODO something better here
//    }
    npfExerciseList.rememberAndLoadFirst(ul.getExercises());
  }

/*  private void sortList(List<CommonShell> copy) {
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    ExerciseComparator exerciseComparator = new ExerciseComparator(controller.getTypeOrder());

//    for (CommonShell es:copy) {
//      logger.info("sortList before " +es.getID() + " " + es.getEnglish() + " "+es.getForeignLanguage());
//    }

    Collections.sort(copy, new Comparator<CommonShell>() {
      @Override
      public int compare(CommonShell o1, CommonShell o2) {
        if (o1.getID() == NEW_EXERCISE_ID) return +1;
        else if (o2.getID() == NEW_EXERCISE_ID) return -1;
        else return exerciseComparator.simpleCompare(o1, o2, false, isEnglish);
      }
    });

//    for (CommonShell es:copy) {
//      logger.info("sortList after " +es.getID() + " " + es.getEnglish() + " "+es.getForeignLanguage());
//    }
  }*/


  /**
   * @param exercise
   * @param right
   * @paramx ul
   * @paramx originalList
   * @paramx itemMarker
   * @param pagingContainer
   * @see #setFactory
   */
/*  private void populatePanel(CommonExercise exercise,
                             final Panel right,
                            // final UserList<CommonShell> ul,
                             final UserList<CommonShell> originalList,
                            // final HasText itemMarker,
                             final ListInterface<CommonShell> pagingContainer) {
    if (exercise.getID() == NEW_EXERCISE_ID) {
      if (newExercise == null) {
        newExercise = createNewItem(userManager.getUser());
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, false);
      } else {
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, true);
      }
    } else {
      addEditOrAddPanel(exercise, itemMarker, originalList, right, ul, pagingContainer, false, true);
    }
  }*/


  /**
   * @param newExercise
   * @paramx originalList
   * @paramx right
   * @paramx pagingContainer
   * @paramx setFields
   * @paramx itemMarker
   * @paramx ul
   * @paramx doNewExercise
   * @seex #populatePanel
   */
/*  private void addEditOrAddPanel(CommonExercise newExercise,
                               //  HasText itemMarker,
                                 UserList<CommonShell> originalList,
                                 Panel right,
                                 //UserList<CommonShell> ul,
                                 ListInterface<CommonShell> pagingContainer,
                                 //boolean doNewExercise,
                                 boolean setFields) {
    NewUserExercise editableExercise = getAddOrEditPanel(newExercise, originalList*//*, doNewExercise*//*);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    if (setFields) editableExercise.setFields(newExercise);
  }*/

//  void clearNewExercise() {
//    this.newExercise = null;
//  }

  /**
   * We can go three ways:
   * <p>
   * 1) Make a new item
   * 2) Edit an item that I created
   * 3) Edit an item on the list made by someone else - all I can do then is remove it from my list.
   * <p>
   * TODO : don't do the choice - make these come up as dialogs or something
   *
   * @return
   * @paramx exercise
   * @paramx originalList
   * @paramx doNewExercise
   * @seex #addEditOrAddPanel(CommonExercise, HasText, UserList, Panel, UserList, ListInterface, boolean, boolean)
   */
 /* private NewUserExercise getAddOrEditPanel(CommonExercise exercise,
                                            UserList<CommonShell> originalList
                                            //,
                                            //                                          boolean doNewExercise
  ) {
    NewUserExercise editableExercise;
 *//*   if (doNewExercise) { // whole new exercise
      editableExercise = new NewUserExercise(
          controller,
          this,
          exercise,
          getInstance(),
          originalList);
    } else {*//*
    boolean iCreatedThisItem = didICreateThisItem(exercise) ||
        (controller.getUserManager().isTeacher() && !exercise.isPredefined());  // asked that teachers be able to record audio for other's items
    if (iCreatedThisItem) {  // it's mine!
      editableExercise = new EditableExerciseDialog(controller, this, *//*itemMarker,*//* exercise,
          originalList,
          exerciseList,
          predefinedContentList,
          getInstance()
      );
    } else {
      editableExercise = new RemoveFromListOnlyExercise(exercise, originalList);
    }
    // }
    return editableExercise;
  }*/
  private String getInstance() {
    return instanceName;
  }

  /**
   * @param exercise
   * @return
   * @paramx originalList
   * @seex EditItem#getAddOrEditPanel(CommonExercise, UserList, boolean)
   */
  private boolean didICreateThisItem(CommonExercise exercise) {
    return exercise.getCreator() == controller.getUser();
  }

  /**
   * @see #getAddOrEditPanel(CommonExercise, UserList, boolean)
   */
 /* private class RemoveFromListOnlyExercise extends NewUserExercise {
    RemoveFromListOnlyExercise(CommonExercise exercise, UserList<CommonShell> originalList) {
      super(
          EditItem.this.controller,
          EditItem.this,
          exercise,

          EditItem.this.getInstance(),
          originalList);
    }

    @Override
    public Panel addNew(UserList<CommonShell> ul,
                        UserList<CommonShell> originalList,
                        ListInterface<CommonShell> listInterface,
                        Panel toAddTo) {
      this.ul = ul;
      this.originalList = originalList;
      this.listInterface = listInterface;

      final FluidContainer container = new FluidContainer();
      container.add(makeDeleteButton(ul, originalList.getID()));
      return container;
    }

    Button makeDeleteButton(final UserList<CommonShell> ul, final long uniqueID) {
      Button delete = makeDeleteButton(ul);
      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
//          logger.info(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);
          deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, predefinedContentList);
        }
      });
      delete.addStyleName("topFiftyMargin");
      return delete;
    }

    @Override
    public void setFields(CommonExercise newUserExercise) {
    }
  }*/

}
