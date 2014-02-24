package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewEditItem<T extends ExerciseShell> extends EditItem<T> {
  private static final String FIXED = "Fixed";
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
   if (doNewExercise) {
      editableExercise = new ChapterNewExercise<T>(service, controller, itemMarker, this, exercise);
    } else {
      editableExercise = new ReviewEditableExercise(itemMarker, exercise, originalList);
    }
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
      Button fixed = makeFixedButton();

      row.add(fixed);
      fixed.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true);
        }
      });
      return row;
    }

    private Button makeFixedButton() {
      Button fixed = new Button(FIXED);
      DOM.setStyleAttribute(fixed.getElement(), "marginRight", "5px");
      fixed.setType(ButtonType.PRIMARY);
      fixed.addStyleName("floatRight");
      fixed.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          checkForForeignChange();
        }
      });
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
      if (buttonClicked) {
        String id = newUserExercise.getID();
        System.out.println("doAfterEditComplete : forgetting " + id);
        exerciseList.forgetExercise(id);
        if (!ul.remove(newUserExercise)) {
          System.err.println("\n\n\ndoAfterEditComplete : error - didn't remove " + id);
        }
        if (!originalList.remove(newUserExercise)) {
          System.err.println("\n\n\ndoAfterEditComplete : error - didn't remove " + id);
        }
        service.removeReviewed(id, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void result) { predefinedContentList.reload(); }
        });
      }
    }
  }
}
