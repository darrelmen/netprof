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

//  private static final int NO_USER_INITIAL = -2;

  public AMASInitialUI(LangTest langTest, UserManager userManager) {
    super(langTest, userManager);
  }

  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel()
   */
/*  private Panel makeFirstTwoRows(Container verticalContainer) {
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel firstRow = new DivWidget();
    verticalContainer.add(firstRow);
    this.firstRow = firstRow;
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }*/

  /**
   * TODO : FIX ME
   *
   * @return
   * @see #populateRootPanel()
   */
/*
  protected Panel makeHeaderRow() {
    logger.info("make header row ---- ");
    headerRow = new FluidRow();
    headerRow.add(new Column(12, flashcard.getNPFHeaderRow()));
    return headerRow;
  }
*/

  /**
   * @return
   */
/*
  protected Container getRootContainer() {
    RootPanel.get().clear();   // necessary?

    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId("root_vertical_container");
    return verticalContainer;
  }
*/
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
    //  firstRow.add(langTest.getFlashRecordPanel());
    langTest.modeSelect();

    //logger.info("populateBelowHeader");

    //  TODO : FIX ME
    learnHelper = new AutoCRTChapterNPFHelper(service, langTest,
        //langTest.getUserManager(),
        null,
        langTest);
    learnHelper.addNPFToContent(firstRow, "");
  }

  @Override
  public void onResize() {
    if (learnHelper != null) learnHelper.onResize();
  }

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   * @see mitll.langtest.client.user.UserManager#login()
   */

/*  public boolean showLogin() {
   // populateRootPanel();
    return true;
  }*/

  /**
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  @Override
  public void reallySetFactory() {
    logger.info("reallySetFactory");
    if (!props.isOdaMode()) {
      int childCount = firstRow.getElement().getChildCount();
      // logger.info("populateRootAfterLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
      if (childCount > 0) {
        Node child = firstRow.getElement().getChild(0);
        Element as = Element.as(child);
        if (as.getId().contains("Login")) {
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
   * @see #onModuleLoad2()
   * @see #showLogin
   * @see #populateRootAfterLogin()
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
