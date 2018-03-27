package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.analysis.BasicUserContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.FirstLastUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActiveUsersManager {
  private static final int HOUR = 60 * 60 * 1000;
  //private final Logger logger = Logger.getLogger("ActiveUsersManager");

  private final ExerciseController controller;
  private static final int TOP = 56;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.UserMenu.ReportListHandler
   */
  public ActiveUsersManager(ExerciseController controller) {
    this.controller = controller;
  }

  public void show(int hours) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Active Users (last hour)");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = ((Window.getClientWidth()) / 2) - 200;
    dialogBox.setPopupPosition(left, TOP);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");

    controller.getUserService().getUsersSince(System.currentTimeMillis() - hours * HOUR, new AsyncCallback<List<FirstLastUser>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError("getting active users", caught);
      }

      @Override
      public void onSuccess(List<FirstLastUser> result) {
        BasicUserContainer<FirstLastUser> active_users = new BasicUserContainer<FirstLastUser>(controller, "activeUser", "Active User") {
          @Override
          protected String getDateColHeader() {
            return "Last Activity";
          }
        };
        Panel tableWithPager = active_users.getTableWithPager(result);
        dialogVPanel.add(tableWithPager);
        Button ok = new Button("OK");
        ok.setType(ButtonType.SUCCESS);
        ok.setSize(ButtonSize.LARGE);
        ok.addClickHandler(event -> dialogBox.hide());
        DivWidget horiz = new DivWidget();
        horiz.setWidth("100%");
        horiz.add(ok);
        ok.addStyleName("floatRight");
        dialogVPanel.add(horiz);
      }
    });


    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
  }
}
