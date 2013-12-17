package mitll.langtest.client.custom;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  private PagingContainer<UserExercise> pagingContainer;

  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
  }

  public Panel editItem(final UserList ul, final HTML itemMarker) {
    HorizontalPanel hp = new HorizontalPanel();
    SimplePanel left = new SimplePanel();
    hp.add(left);
    final SimplePanel right = new SimplePanel();
    hp.add(right);

    pagingContainer = new PagingContainer<UserExercise>(controller, 100) {
      @Override
      protected void gotClickOnItem(UserExercise exerciseShell) {
        editItem(exerciseShell, right, ul, itemMarker);
      }
    };

    Panel container = pagingContainer.getTableWithPager();
    left.add(container);
    for (UserExercise es : ul.getExercises()) {
      if (!es.isPredefined()) {
        pagingContainer.addExerciseToList2(es);
      }
    }
    pagingContainer.flush();
    UserExercise exerciseShell = pagingContainer.selectFirst();
    if (exerciseShell == null) {
      System.err.println("huh? nothing first?");
      showPopup("No editable items yet.",container);
    }
    else {
      editItem(exerciseShell, right, ul, itemMarker);
    }
    return hp;
  }

  private void editItem(final UserExercise exerciseShell, SimplePanel right, UserList ul, HTML itemMarker) {
    final PagingContainer<UserExercise> outer = pagingContainer;
    NewUserExercise newUserExercise = new NewUserExercise(service, userManager, controller, itemMarker) {
      @Override
      protected void onClick(final UserList ul, PagingContainer<?> pagingContainer, Panel toAddTo) {
        service.editItem(exerciseShell, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void newExercise) {
            outer.refresh();
            showPopup(exerciseShell.getTooltip() + " has been updated.",submit);
          }
        });
      }
    };
    right.clear();
    right.add(newUserExercise.addNew(ul, pagingContainer, right));
    newUserExercise.setFields(exerciseShell);
  }
  private void showPopup(String toShow, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }
}
