package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.Arrays;

/**
* Created by GO22670 on 3/28/2014.
*/
class ReviewEditableExercise extends EditableExercise {
  private static final String FIXED = "Mark Fixed";
  private static final String DUPLICATE = "Duplicate";
  private static final String DELETE = "Delete";
  public static final String DELETE_THIS_ITEM = "Delete this item.";
  public static final String ARE_YOU_SURE = "Are you sure?";
  public static final String REALLY_DELETE_ITEM = "Really delete item?";
  public static final String COPY_THIS_ITEM = "Copy this item.";

  private PagingExerciseList exerciseList;
  private ListInterface predefinedContentList;

  /**
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @param exerciseList
   * @see mitll.langtest.client.custom.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public ReviewEditableExercise(LangTestDatabaseAsync service,
                                ExerciseController controller,
                                HasText itemMarker,
                                CommonUserExercise changedUserExercise,

                                UserList originalList,
                                PagingExerciseList exerciseList,
                                ListInterface predefinedContent,
                                NPFHelper npfHelper) {
    super(service, controller,
      null,
      itemMarker, changedUserExercise, originalList, exerciseList, predefinedContent, npfHelper);
    this.exerciseList = exerciseList;
    this.predefinedContentList = predefinedContent;
  }

  @Override
  protected boolean shouldDisableNext() { return false; }

  /**
   * Add a fixed button, so we know when to clear the comments and remove this item from the reviewed list.
   *
   * @see #addNew
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   */
  @Override
  protected Panel getCreateButton(final UserList ul, final ListInterface pagingContainer, final Panel toAddTo,
                                  final ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.addStyleName("floatLeft");
    row.add(prevNext);

    final Button fixed = makeFixedButton();

    fixed.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true, true);
      }
    });

    if (newUserExercise.checkPredef()) {   // for now, only the owner of the list can remove or add to their list
      row.add(getRemove());
      row.add(getDuplicate());
    }

    row.add(fixed);

    configureButtonRow(row);

    return row;
  }

  private Button getRemove() {
    Button remove = new Button(DELETE);
    remove.setType(ButtonType.WARNING);
    remove.addStyleName("floatRight");
    remove.addStyleName("leftFiveMargin");
    addTooltip(remove, DELETE_THIS_ITEM);

    remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        DialogHelper dialogHelper = new DialogHelper(true);
        dialogHelper.show(ARE_YOU_SURE, Arrays.asList(REALLY_DELETE_ITEM), new DialogHelper.CloseListener() {
          @Override
          public void gotYes() {
            service.deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {}

              @Override
              public void onSuccess(Boolean result) {
                exerciseList.removeExercise(newUserExercise);
                originalList.remove(newUserExercise.getID());
              }
            });
          }

          @Override
          public void gotNo() {}
        });
      }
    });
    return remove;
  }

  private Button getDuplicate() {
    Button duplicate = new Button(DUPLICATE);
    duplicate.setType(ButtonType.SUCCESS);
    duplicate.addStyleName("floatRight");
    addTooltip(duplicate, COPY_THIS_ITEM);

    duplicate.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        duplicateExercise();
      }
    });
    return duplicate;
  }

  private void duplicateExercise() {
    newUserExercise.setCreator(controller.getUser());
    CommonShell commonShell = exerciseList.byID(newUserExercise.getID());
    if (commonShell != null) {
      newUserExercise.setState(commonShell.getState());
      newUserExercise.setSecondState(commonShell.getSecondState());
     // System.out.println("\t using state " + commonShell.getState());
    }
    //System.out.println("to duplicate " + newUserExercise + " state " + newUserExercise.getState());
    service.duplicateExercise(newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(UserExercise result) {
        exerciseList.addExerciseAfter(newUserExercise, result);
        exerciseList.redraw();
        originalList.addExerciseAfter(newUserExercise, result);
      }
    });
  }

  private Button makeFixedButton() {
    Button fixed = new Button(FIXED);
    fixed.setType(ButtonType.PRIMARY);

    fixed.addStyleName("leftFiveMargin");
    fixed.addStyleName("floatLeft");
    fixed.addStyleName("marginRight");

    fixed.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        checkForForeignChange();
      }
    });
    addTooltip(fixed,"Mark item as fixed, clear comments, and remove it from the review list.");
    return fixed;
  }

  @Override
  protected void audioPosted() {  reallyChange(listInterface, false);  }

  @Override
  protected void checkIfNeedsRefAudio() {}

  /**
   * TODO : why do we have to do a sequence of server calls -- how about just one???
   * @param pagingContainer
   * @param buttonClicked
   * @see #reallyChange
   * @see #postEditItem(mitll.langtest.client.list.ListInterface, boolean)
   * @seex #doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   */
  @Override
  protected void doAfterEditComplete(ListInterface pagingContainer, boolean buttonClicked) {
    super.doAfterEditComplete(pagingContainer, buttonClicked);

    if (buttonClicked) {
      final String id = newUserExercise.getID();
      int user = controller.getUser();

      System.out.println("doAfterEditComplete : forgetting " + id + " user " +user);

      if (!ul.remove(newUserExercise)) {
        System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from ul " + ul);
      }
      if (!originalList.remove(newUserExercise)) {
        System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from original " +originalList);
      }

      service.setExerciseState(id, CommonShell.STATE.FIXED, user, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {
          System.out.println("\tdoAfterEditComplete : predefinedContentList reload ");
          //predefinedContentList.removeCompleted(id);
          predefinedContentList.reload();

          CommonShell t = exerciseList.forgetExercise(id);
          System.out.println("\tdoAfterEditComplete : forgot " + t);
        }
      });
    }
    else {
      System.out.println("----> doAfterEditComplete : button not clicked ");

    }
  }
}
