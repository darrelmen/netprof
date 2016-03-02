/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Container;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserManager;

import java.util.logging.Logger;


/**
 * Created by go22670 on 12/21/15.
 */
public class AMASInitialUI extends InitialUI {
  private final Logger logger = Logger.getLogger("AMASInitialUI");

  public AMASInitialUI(LangTest langTest, UserManager userManager) {
    super(langTest, userManager);
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  /**
   * * TODO : FIX ME
   *
   * @param verticalContainer
   * @param firstRow          where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  @Override
  protected void populateBelowHeader(Container verticalContainer, Panel firstRow) {
    RootPanel.get().clear();
    RootPanel.get().add(verticalContainer);
    /**
     * {@link #makeFlashContainer}
     */
    firstRow.add(langTest.getFlashRecordPanel());
    langTest.modeSelect();
    learnHelper = new AutoCRTChapterNPFHelper(service, langTest, null, langTest);
    learnHelper.addNPFToContent(firstRow, "");
  }

  @Override
  public void onResize() {
    if (learnHelper != null) learnHelper.onResize();
  }

  /**
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  @Override
  public void populateRootPanelIfLogin() {
//    logger.info("populateRootPanelIfLogin");
    if (!props.isOdaMode()) {
      int childCount = firstRow.getElement().getChildCount();
      // logger.info("populateRootAfterLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
      if (childCount > 0) {
        Node child = firstRow.getElement().getChild(0);
        Element as = Element.as(child);
        if (as.getId().contains(LOGIN)) {
          //   logger.info("populateRootAfterLogin found login...");
          populateRootPanel();
        }
      }
    }
  }


  /**
   * If in ODA Mode, wait until we start a QUIZ?
   * <p>
   * Then only show the content part of it...
   *
   * @return
   * @see #showLogin
   * @see #populateRootPanelIfLogin()
   */
  private void populateRootPanel() {
    Container verticalContainer = getRootContainer();
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (props.isOdaMode()) {
      headerRow.setVisible(false);
      populateBelowHeader(verticalContainer, firstRow);
    } else if (!showLogin()) {
      populateBelowHeader(verticalContainer, firstRow);
    }
  }

  /**
   * @param userID
   * @return
   * @see #gotUser
   */
  @Override
  public void configureUIGivenUser(long userID) {
    boolean diff = lastUser != userID;
    showUserPermissions(userID);

    if (diff) configureUIGivenUser();
  }

  /**
   * TODO : FIX ME
   */
  private void configureUIGivenUser() {
    if (learnHelper != null && learnHelper.getExerciseList() != null) {
      SingleSelectExerciseList exerciseList = (SingleSelectExerciseList) learnHelper.getExerciseList();
      exerciseList.restoreListFromHistory();
    }
  }
}
