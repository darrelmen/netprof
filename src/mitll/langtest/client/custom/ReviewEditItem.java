package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewEditItem<T extends ExerciseShell> extends EditItem<T> {
  private static final String FIXED = "Mark Fixed";
  private static final String DUPLICATE = "Duplicate";
  private static final String DELETE = "Delete";

  /**
   *
   * @param service
   * @param userManager
   * @param controller
   * @param npfHelper
   * @see Navigation#Navigation
   */
  public ReviewEditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                        ListInterface<? extends ExerciseShell> listInterface, UserFeedback feedback, NPFHelper npfHelper) {
    super(service, userManager, controller, listInterface, feedback, npfHelper);
  }

  /**
   * @see #populatePanel
   * @param exercise
   * @param itemMarker
   * @param originalList
   * @param doNewExercise
   * @return
   */
  @Override
  protected NewUserExercise<T> getAddOrEditPanel(UserExercise exercise, HasText itemMarker, UserList originalList, boolean doNewExercise) {
    NewUserExercise<T> editableExercise;
/*   if (doNewExercise) {
      editableExercise = new ChapterNewExercise<T>(service, controller, itemMarker, this, exercise);
    } else {*/
      editableExercise = new ReviewEditableExercise(itemMarker, exercise, originalList);
  //  }
    return editableExercise;
  }

  private class ReviewEditableExercise extends EditableExercise {
    /**
     *
     * @param itemMarker
     * @param changedUserExercise
     * @param originalList
     * @see EditItem#populatePanel
     */
    public ReviewEditableExercise(HasText itemMarker, UserExercise changedUserExercise, UserList originalList) {
      super(itemMarker, changedUserExercise, originalList);
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
    protected Panel getCreateButton(final UserList ul, final ListInterface<T> pagingContainer, final Panel toAddTo,
                                    final ControlGroup normalSpeedRecording) {
      Panel row = new DivWidget();
      row.addStyleName("marginBottomTen");

      PrevNextList<T> prevNext = getPrevNext(pagingContainer);
      prevNext.addStyleName("floatLeft");
      row.add(prevNext);

      final Button fixed = makeFixedButton();

      fixed.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //fixed.setEnabled(false);
          validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true);
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

    protected Tooltip addTooltip(Widget w, String tip) {
      return createAddTooltip(w, tip, Placement.RIGHT);
    }

    /**
     * @see mitll.langtest.client.custom.NPFExercise#makeAddToList(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController)
     * @param widget
     * @param tip
     * @param placement
     * @return
     */
    private Tooltip createAddTooltip(Widget widget, String tip, Placement placement) {
      Tooltip tooltip = new Tooltip();
      tooltip.setWidget(widget);
      tooltip.setText(tip);
      tooltip.setAnimation(true);
// As of 4/22 - bootstrap 2.2.1.0 -
// Tooltips have an bug which causes the cursor to
// toggle between finger and normal when show delay
// is configured.

      tooltip.setShowDelay(500);
      tooltip.setHideDelay(500);

      tooltip.setPlacement(placement);
      tooltip.reconfigure();
      return tooltip;
    }

    private Button getRemove() {
      Button remove = new Button(DELETE);
      remove.setType(ButtonType.WARNING);
      remove.addStyleName("floatRight");
      remove.addStyleName("leftFiveMargin");
      addTooltip(remove,"Delete this item.");

      remove.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          DialogHelper dialogHelper = new DialogHelper(true);
          dialogHelper.show("Are you sure?", Arrays.asList("Really delete item?"), new DialogHelper.CloseListener() {
            @Override
            public void gotYes() {
              service.deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {}

                @Override
                public void onSuccess(Boolean result) {
                  exerciseList.removeExercise((T) newUserExercise);
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
      addTooltip(duplicate,"Copy this item.");


      duplicate.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          duplicateExercise();
        }
      });
      return duplicate;
    }

    protected void duplicateExercise() {
      newUserExercise.setCreator(controller.getUser());
      service.duplicateExercise(newUserExercise, new AsyncCallback<UserExercise>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(UserExercise result) {

          //System.out.println("Got back " + result);
          T result1 = (T) result;
          exerciseList.addExerciseAfter((T)newUserExercise,result1);
          exerciseList.redraw();
          originalList.addExerciseAfter(newUserExercise, result);
        }
      });
    }

    private Button makeFixedButton() {
      Button fixed = new Button(FIXED);
      fixed.addStyleName("leftFiveMargin");
      fixed.setType(ButtonType.PRIMARY);
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
     * @param pagingContainer
     * @param buttonClicked
     * @see #reallyChange
     */
    @Override
    protected void doAfterEditComplete(ListInterface<T> pagingContainer, boolean buttonClicked) {
      super.doAfterEditComplete(pagingContainer,buttonClicked);

      if (buttonClicked) {
        final String id = newUserExercise.getID();
        System.out.println("doAfterEditComplete : forgetting " + id);
        T t = exerciseList.forgetExercise(id);
        System.out.println("\tdoAfterEditComplete : forgot " + t);

        if (!ul.remove(newUserExercise)) {
          System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from ul " + ul);
        }
        if (!originalList.remove(newUserExercise)) {
          System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from original " +originalList);
        }
        service.removeReviewed(id, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void result) {
            predefinedContentList.removeCompleted(id);
            predefinedContentList.reload();
          }
        });
      }
    }
  }
}
