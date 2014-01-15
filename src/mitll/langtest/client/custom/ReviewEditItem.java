package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
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
   * @see #setFactory(mitll.langtest.client.list.PagingExerciseList, mitll.langtest.shared.custom.UserList)
   * @param exercise
   * @param right
   * @param ul
   * @param itemMarker
   * @param pagingContainer
   */
  @Override
  protected void populatePanel(UserExercise exercise, Panel right, UserList ul, HTML itemMarker,
                               PagingContainer<T> pagingContainer) {
    ReviewEditableExercise newUserExercise = new ReviewEditableExercise(itemMarker, exercise);
    right.add(newUserExercise.addNew(ul, pagingContainer, right));
    newUserExercise.setFields();
  }

  private class ReviewEditableExercise extends EditableExercise {
    /**
     * @param itemMarker
     * @param changedUserExercise
     * @see #populatePanel(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.client.exercise.PagingContainer)
     */
    public ReviewEditableExercise(HTML itemMarker, UserExercise changedUserExercise) {
      super(itemMarker, changedUserExercise);
    }

    @Override
    protected boolean shouldDisableNext() { return false; }

    @Override
    protected Panel getCreateButton(final UserList ul, final PagingContainer<T> pagingContainer, final Panel toAddTo,
                                    final ControlGroup normalSpeedRecording, String buttonName) {
      Button submit = makeCreateButton(ul, pagingContainer, toAddTo, english, foreignLang, rap, normalSpeedRecording, buttonName);

      Panel row = new DivWidget();
      row.addStyleName("marginBottomTen");
      row.add(submit);
      submit.addStyleName("floatRight");

      Button fixed = new Button(FIXED);
      DOM.setStyleAttribute(fixed.getElement(), "marginRight", "5px");
      fixed.setType(ButtonType.PRIMARY);
      fixed.addStyleName("floatRight");

      row.add(fixed);
      fixed.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          validateThenPost(english, foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, FIXED);
        }
      });
      return row;
    }

    protected void doAfterEditComplete(PagingContainer<T> pagingContainer, String buttonName) {
      if (buttonName.equals(FIXED)) {
        String id = newUserExercise.getID();
        exerciseList.forgetExercise(id);
        if (!ul.remove(newUserExercise)) {
          System.err.println("\n\n\ndoAfterEditComplete : error - didn't remove");
        }
        service.removeReviewed(id, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            predefinedContentList.reload();
          }
        });
      } else {
        super.doAfterEditComplete(pagingContainer, buttonName);
      }
    }
  }
}
