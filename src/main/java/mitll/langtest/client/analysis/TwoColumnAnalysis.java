package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static mitll.langtest.client.project.ProjectChoices.PLEASE_WAIT;

/**
 * Left side list, then when selected changes what's shown on the right side...
 *
 * @param <T>
 */
public abstract class TwoColumnAnalysis<T extends HasID> extends DivWidget {
  private static final int DELAY_MILLIS = 2000;
  final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);
  private Object waitToken = null;

  @NotNull
  Timer getPleaseWaitTimer(ExerciseController controller) {
    Timer pleaseWaitTimer = new Timer() {
      @Override
      public void run() {
        waitToken = controller.getMessageHelper().startWaiting(PLEASE_WAIT);
      }
    };
    pleaseWaitTimer.schedule(DELAY_MILLIS);
    return pleaseWaitTimer;
  }

  void finishPleaseWait(Timer t, MessageHelper messageHelper) {
    t.cancel();
    if (waitToken != null) {
      messageHelper.stopWaiting(waitToken);
      waitToken = null;
    }
  }

  /**
   * @see SessionAnalysis#SessionAnalysis
   * @param users
   * @param pleaseWaitTimer
   * @param controller
   */
  void showItems(Collection<T> users, Timer pleaseWaitTimer, ExerciseController controller) {
    finishPleaseWait(pleaseWaitTimer, controller.getMessageHelper());

    clear();

    DivWidget bottom = new DivWidget();
    bottom.addStyleName("floatLeft");
    bottom.getElement().setId("StudentAnalysis_bottom");

    /*DivWidget rightSide =*/ addTop(users, controller, bottom);

    addBottom(bottom);
  }

  protected void addBottom(DivWidget bottom) {
    add(bottom);
  }

  protected DivWidget addTop(Collection<T> users, ExerciseController controller, DivWidget bottom) {
    DivWidget rightSide = getRightSide();
    DivWidget table = getTable(users, controller, bottom, rightSide);
    add(getTop(table, rightSide));
    return rightSide;
  }

  @NotNull
  DivWidget getRightSide() {
    DivWidget rightSide = new DivWidget();
    rightSide.getElement().setId("rightSideForPlot");
    rightSide.setWidth("100%");
    return rightSide;
  }

  abstract DivWidget getTable(Collection<T> users,
                              ExerciseController controller,
                              DivWidget bottom,
                              DivWidget rightSide);

  /**
   * @param leftSide
   * @param rightSide
   * @return
   * @see #addTop(Collection, ExerciseController, DivWidget)
   */
   DivWidget getTop(DivWidget leftSide, DivWidget rightSide) {
    DivWidget top = new DivWidget();
    top.addStyleName("inlineFlex");
    top.setWidth("100%");
    top.getElement().setId("top");

    top.add(leftSide);
    top.add(rightSide);

    {
      DivWidget spacer = new DivWidget();
      spacer.getElement().getStyle().setProperty("minWidth", 5 + "px");
      top.add(spacer);
    }
    return top;
  }

  /**
   * TODO : use common key storage
   *
   * @param controller
   * @return
   */
  @NotNull
  String getRememberedSelectedUser(ExerciseController controller) {
    return getSelectedUserKey(controller, controller.getProps().getAppTitle());
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + getStorageKey();
  }

  protected abstract String getStorageKey();

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }
}
