/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Panel;
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
  //private final Logger logger = Logger.getLogger("TwoColumnAnalysis");
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
   * @param users
   * @param pleaseWaitTimer
   * @param controller
   * @see SessionAnalysis#SessionAnalysis
   */
  void showItems(Collection<T> users, Timer pleaseWaitTimer, ExerciseController controller) {
    finishPleaseWait(pleaseWaitTimer, controller.getMessageHelper());

    clear();

    DivWidget bottom = new DivWidget();
    bottom.addStyleName("floatLeft");
    bottom.getElement().setId(getClass().toString()+"_bottom_showItems");

    /*DivWidget rightSide =*/ addTop(users, controller, bottom);

    // bottom.add(rightSide);
    addBottom(bottom);
  }

  /**
   * So we divide the screen - top and bottom
   * the top section is divided into two sections - left and right
   * left side has the table that selects what's shown everywhere else -
   * for analysis it's the user list, for individual scoring it's the session list
   *
   * @param users
   * @param controller
   * @param bottom
   * @return
   */
  private void addTop(Collection<T> users, ExerciseController controller, DivWidget bottom) {
    DivWidget rightSide = getRightSide();
    DivWidget leftSide = getTable(users, controller, bottom, rightSide, getNoDataYetMessage());
    leftSide.addStyleName("cardBorderShadow");
    leftSide.addStyleName("bottomFiveMargin");
    add(getTop(leftSide, rightSide));
  }

  @NotNull
  protected abstract String getNoDataYetMessage();

  protected abstract String getHeaderLabel();

  @NotNull
  protected abstract MemoryItemContainer<T> getItemContainer(ExerciseController controller, DivWidget bottom, DivWidget rightSide);

  @NotNull
  private DivWidget getRightSide() {
    DivWidget rightSide = new DivWidget();
    rightSide.getElement().setId("rightSideForPlot");
    rightSide.setWidth("100%");
    return rightSide;
  }

  private void addBottom(DivWidget bottom) {
    add(bottom);
  }

  private DivWidget getTable(Collection<T> users,
                             ExerciseController controller,
                             DivWidget bottom,
                             DivWidget rightSide,
                             String noDataMessage) {
    if (users.isEmpty()) {
      return showNoData(noDataMessage);
    } else {
      return getContainer(users, controller, bottom, rightSide, "userContainer");
    }
  }

  protected DivWidget getContainerDiv(DivWidget table) {
    String headerLabel = getHeaderLabel();
//    if (headerLabel == null) {
//      logger.info("header label is null for " + this.getClass());
//    }
    return getContainerDiv(table, headerLabel == null ? null : getHeading(headerLabel));
  }

  @NotNull
  private DivWidget showNoData(String text) {
    DivWidget divWidget = new DivWidget();
    divWidget.add(new Label(LabelType.INFO, text));
    return divWidget;
  }

  @NotNull
  private DivWidget getContainer(Collection<T> users,
                                 ExerciseController controller,
                                 DivWidget bottom,
                                 DivWidget rightSide,
                                 String userContainer1) {
    DivWidget sessions = getContainerDiv(getItemContainer(controller, bottom, rightSide).getTable(users));
    sessions.getElement().setId(userContainer1);
    sessions.addStyleName("cardBorderShadow");
    return sessions;
  }


  /**
   * @param leftSide
   * @param rightSide
   * @return
   * @see #addTop(Collection, ExerciseController, DivWidget)
   */
  private DivWidget getTop(DivWidget leftSide, DivWidget rightSide) {
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

  private DivWidget getContainerDiv(Panel tableWithPager, Heading heading) {
    DivWidget wordsContainer = new DivWidget();
    if (heading != null) {
      wordsContainer.add(heading);
    }
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  @NotNull
  private Heading getHeading(String words) {
    Heading wordsTitle = new Heading(3, words);
    wordsTitle.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    wordsTitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return wordsTitle;
  }
}
