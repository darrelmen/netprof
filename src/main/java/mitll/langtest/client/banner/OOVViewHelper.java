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

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.userlist.TableAndPager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.OOVInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

public class OOVViewHelper extends TableAndPager implements ContentView {
  private final Logger logger = Logger.getLogger("OOVViewHelper");

  private static final int CHUNK = 300;
  private static final INavigation.VIEWS LEARN = INavigation.VIEWS.LEARN;
  private static final String CHECKING_OOV = "Checking OOV...";

  private static final String OOV = "Not In Dict.";
  private static final String ITEMS_WITH_MISSING_WORDS = "Items with Missing Words";
  private static final String ITEMS_WITH_MISSING_WORDS1 = "Items with missing words";

  private static final String CHECK_COMPLETE = "Check complete.";
  private static final String PLEASE_WAIT = "Please wait...";
  /**
   *
   */
  private static final String CHECK_ITEMS_IN_DICT = "Check Items In Dict.";
  private static final String SELECT_AND_ENTER_EQUIVALENT = "Select and enter equivalent.";

  private static final String TITLE = "Words Not In Dictionary";
  private static final String EQUIVALENT = "Equivalent";

  private final ExerciseController controller;
  private MemoryItemContainer<OOV> oovContainer;
  private HTML oov;
  private TextBox equivalent;
  private final HTML message = new HTML();
  private Button checkButton;
  /**
   *
   */
  private OOVInfo oovInfo;

  private int num = 0;
  private int offset = CHUNK;

  OOVViewHelper(ExerciseController controller) {
    this.controller = controller;
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    listContent.clear();

    num = 0;
    offset = CHUNK;

    DivWidget outer = new DivWidget();
    listContent.add(outer);
    addOOVList(outer);
  }

  private void updateOOV() {
    List<OOV> dirtyItems = getDirtyItems();
    if (!dirtyItems.isEmpty()) {
      controller.getAudioService().updateOOV(controller.getProjectID(), dirtyItems, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          message.setText("Error - please try again.");
          controller.handleNonFatalError("updateOOV...", caught);
        }

        @Override
        public void onSuccess(Void result) {
          logger.info("updateOOV wrote update for " + dirtyItems.size());
        }
      });
    }
  }

  /**
   * Initial query
   *
   * @param top
   */
  private void addOOVList(DivWidget top) {
    top.add(new HTML("Getting words that are not in the dictionary. Please wait..."));
    top.add(getProgressBarDiv(true));
    oovInfo = new OOVInfo();

    waitAndThenCheck(top);
  }

  private void waitAndThenCheck(DivWidget top) {
    if (controller.getProjectStartupInfo() == null) {
      Timer currentTimer = new Timer() {
        @Override
        public void run() {
//          logger.info("loadNextOnTimer ----> at " + System.currentTimeMillis() + "  firing on " + currentTimer);

          if (controller.getProjectStartupInfo() == null) {
            logger.info("Wait for project startup...");
            waitAndThenCheck(top);
          } else {
            logger.info("checking for oov.");
            checkOOVRepeatedly(top, controller.getProjectID());
          }
        }
      };
      currentTimer.schedule(1000);
    } else {
      checkOOVRepeatedly(top, controller.getProjectID());
    }
  }

  private ProgressBar progressBar;

  private DivWidget getProgressBarDiv(boolean showInitially) {
    ProgressBar scoreProgress = progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    progressBar.setVisible(showInitially);
    return getProgressBarDiv(scoreProgress);
  }

  @NotNull
  private DivWidget getProgressBarDiv(ProgressBar scoreProgress) {
    DivWidget scoreContainer = new DivWidget();

    scoreContainer.addStyleName("topFiveMargin");
    scoreContainer.getElement().getStyle().setMarginBottom(0, PX);
    scoreContainer.add(scoreProgress);

    styleProgressBar(scoreProgress);
    scoreContainer.setWidth("200px");

    return scoreContainer;
  }

  private void styleProgressBar(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, PX);
    style.setMarginLeft(5, PX);
    style.setMarginBottom(0, PX);

    style.setHeight(25, PX);
    style.setFontSize(16, PX);

    progressBar.setWidth(200 + "%");
  }

  /**
   * @param top
   * @param projectID
   * @see #addOOVList(DivWidget)
   */
  private void checkOOVRepeatedly(DivWidget top, int projectID) {
    controller.getAudioService().checkOOV(projectID, num, offset, new AsyncCallback<OOVInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("Checking OOV...", caught);
      }

      @Override
      public void onSuccess(OOVInfo oovInfoForBatch) {
        num += offset;

        oovInfo.add(oovInfoForBatch);

        //logger.info("addOOVList Got " + oovInfo);
        if (num < oovInfo.getTotal()) {
          setProgressPercent(oovInfoForBatch);
          checkOOVRepeatedly(top, projectID);
        } else {
          getOOVs(oovInfo, projectID, top);
        }
      }
    });
  }

  private void setProgressPercent(OOVInfo oovInfoForBatch) {
    float percent = 100F * ((float) num / (float) oovInfoForBatch.getTotal());
//          logger.info("checkOOVRepeatedly : OK now at " + num + " or " + percent);
    progressBar.setPercent(percent);
  }

  private void getOOVs(OOVInfo oovInfo, int projectID, DivWidget top) {
    controller.getScoringService().getOOVs(projectID, new AsyncCallback<List<OOV>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting oovs", caught);
      }

      @Override
      public void onSuccess(List<OOV> result) {
        top.clear();
        logger.info("getOOVs got " + result.size());
        oovContainer = addOOVTable(top, oovInfo);
        sortUnsetToTop(result);
        oovContainer.populateTable(result);
        if (!result.isEmpty()) {
          oovContainer.gotClickOnItem(result.get(0));
        }
        grabFocus();
      }
    });
  }

  private void sortUnsetToTop(List<OOV> result) {
    result.sort((o1, o2) -> {
      boolean o1Empty = o1.getEquivalent().isEmpty();
      boolean o2Empty = o2.getEquivalent().isEmpty();
      if (o1Empty && !o2Empty) {
        return -1;
      } else if (o2Empty && !o1Empty) {
        return +1;
      } else {
        return o1.compareTo(o2);
      }
    });
  }

  private void grabFocus() {
    Scheduler.get().scheduleDeferred((Command) () -> equivalent.setFocus(true));
  }

  private UnsafeItems unsafeItems;
  private DivWidget exampleContainer;

  private MemoryItemContainer<OOV> addOOVTable(DivWidget top, OOVInfo oovInfo) {
    MemoryItemContainer<OOV> oovMemoryItemContainer = new OOVMemoryItemContainer();

    DivWidget leftRight = new DivWidget();
    leftRight.addStyleName("inlineFlex");
    top.add(leftRight);

    {
      DivWidget left = new DivWidget();
      left.getElement().setId("left");
      leftRight.add(left);

      Panel tableWithPager = getTableWithPager(left, oovMemoryItemContainer, TITLE, SELECT_AND_ENTER_EQUIVALENT,
          Placement.TOP);
      tableWithPager.getElement().getStyle().setClear(Style.Clear.BOTH);
      tableWithPager.setWidth("450px");
    }

    {
      DivWidget right = new DivWidget();
      right.addStyleName("leftFiveMargin");
      leftRight.add(right);

      unsafeItems = new UnsafeItems();

      getTableWithPager(right,
          unsafeItems, ITEMS_WITH_MISSING_WORDS1, "example items",
          Placement.TOP).setWidth(550 + "px");
      right.add(getButtonRow());
    }

//    logger.info("showing " + toShow.size());

    showUnsafe(oovInfo);
    oovMemoryItemContainer.getCellTable().addDomHandler(event -> checkForKeyUpDown(event, oovMemoryItemContainer), KeyUpEvent.getType());


    DivWidget below = new DivWidget();
    below.getElement().setId("below");
    below.getElement().getStyle().setClear(Style.Clear.BOTH);
    below.addStyleName("topFiveMargin");

    top.add(below);

    exampleContainer = new DivWidget();

    exampleContainer.addStyleName("topFiveMargin");
    exampleContainer.addStyleName("leftFiveMargin");
    Style style = exampleContainer.getElement().getStyle();
    style.setOverflow(Style.Overflow.HIDDEN);
    style.setProperty("maxWidth", "800px");
    style.setProperty("minHeight", "160px");
    style.setPaddingRight(50, Style.Unit.PX);

    //  below.add(exampleContainer);

    below.add(getOOVAndEquivalent(oovMemoryItemContainer));

    {
      DivWidget buttonRow = new DivWidget();
      below.add(buttonRow);
      buttonRow.add(checkButton = getCheckButton());
    }
    {
      DivWidget buttonRow = new DivWidget();
      below.add(buttonRow);
      buttonRow.add(message);
    }

    below.add(getProgressBarDiv(false));

    return oovMemoryItemContainer;
  }

  /**
   * @return
   * @see #addTable
   */
  @NotNull
  private DivWidget getButtonRow() {
    DivWidget wrapper = new DivWidget();
//    wrapper.addStyleName("floatRight");
    wrapper.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    wrapper.add(getJumpToLearnButton());
    DivWidget child = new DivWidget();
    child.add(wrapper);
    return child;
  }

  private Button getJumpToLearnButton() {
    Button learn = new Button(LEARN.toString());
    learn.addStyleName("topFiveMargin");
    learn.addStyleName("leftTenMargin");
    learn.setType(ButtonType.SUCCESS);

    learn.addClickHandler(event -> {
      learn.setEnabled(false);
      gotClickOnLearn();
    });
    return learn;
    // wrapper.add(learn);
  }

  private void gotClickOnLearn() {
    Wrapper selected1 = unsafeItems.getSelected();

    if (selected1 != null) {
      logger.info("got selection " + selected1);
      controller.getExerciseService().getExerciseIDOrParent(selected1.getID(), new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Integer result) {
          logger.info("gotClickOnLearn OK show " + result + " Vs " + selected1.getID());
          controller.getShowTab(LEARN).showLearnAndItem(result);
        }
      });
//      logger.info("gotClickOnLearn OK show " + exid);
//      controller.getShowTab(this.jumpView).showLearnAndItem(exid);
    }
  }

  /**
   * @param oovInfo
   */
  private void showUnsafe(OOVInfo oovInfo) {
    this.oovInfo = oovInfo;
    unsafeItems.populateTable(getWrappers(oovInfo));
  }

  @NotNull
  private DivWidget getOOVAndEquivalent(MemoryItemContainer<OOV> oovMemoryItemContainer) {
    DivWidget oovRow = new DivWidget();
    oovRow.getElement().setId("oovRow");
    {
      oovRow.addStyleName("inlineFlex");
      oovRow.addStyleName("topFiveMargin");
      oov = new HTML();
      Style style = oov.getElement().getStyle();
      style.setProperty("fontSize", "large");
      oov.addStyleName("shadowBorder");
      style.setPaddingLeft(15, Style.Unit.PX);
      style.setPaddingTop(2, Style.Unit.PX);
      style.setPaddingBottom(5, Style.Unit.PX);
      oov.setWidth("100px");
      DivWidget outer = new DivWidget();
      outer.add(oov);
      outer.addStyleName("rightFiveMargin");
      oovRow.add(outer);
      oovRow.add(new HTML("="));
      equivalent = new TextBox();
      equivalent.addBlurHandler(event -> gotBlur(currentOOV));
      equivalent.setVisibleLength(100);
      equivalent.addStyleName("leftFiveMargin");
      oovRow.add(equivalent);

      equivalent.addKeyUpHandler(event -> checkForKeyUpDown(event, oovMemoryItemContainer));
    }
    return oovRow;
  }

  @NotNull
  private List<Wrapper> getWrappers(OOVInfo oovInfo) {
    List<Wrapper> toShow = new ArrayList<>();
//    logger.info("getWrappers got  " + oovInfo.getUnsafe().size());
    oovInfo.getUnsafe().forEach(item -> toShow.add(new Wrapper(item.getID(), item.getForeignLanguage())));
    //  logger.info("getWrappers made " + toShow.size());
    return toShow;
  }

  @NotNull
  private Button getCheckButton() {
    final Button add = new Button(CHECK_ITEMS_IN_DICT, IconType.CHECK);
    add.setEnabled(false);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotCheck(add));
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  private void gotCheck(Button add) {
    int projectID = controller.getProjectID();
    add.setEnabled(false);
    message.setText(PLEASE_WAIT);

    updateAndCheck(projectID, getDirtyItems());
  }

  private void updateAndCheck(int projectID, List<OOV> items) {
    controller.getAudioService().updateOOV(projectID, items, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        message.setText("Error - please try again.");
        controller.handleNonFatalError("updateOOV...", caught);
      }

      @Override
      public void onSuccess(Void result) {
        checkOOVAgain(projectID);
      }
    });
  }

  @NotNull
  private List<OOV> getDirtyItems() {
    return oovContainer.getItems().stream().filter(mitll.langtest.shared.exercise.OOV::isDirty).collect(Collectors.toList());
  }

  @NotNull
  private List<OOV> getUnmatchedItems() {
    return oovContainer.getItems().stream().filter(oov -> oov.getEquivalent().isEmpty()).collect(Collectors.toList());
  }

  private void checkOOVAgain(int projectID) {
    message.setText("Checking dictionary again, please wait...");

    num = 0;
    offset = 100;
    oovInfo = new OOVInfo();
    progressBar.setVisible(true);

    checkOOVAgainRecurse(projectID);
  }

  private void checkOOVAgainRecurse(int projectID) {
    controller.getAudioService().checkOOV(projectID, num, offset, new AsyncCallback<OOVInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(CHECKING_OOV, caught);
      }

      @Override
      public void onSuccess(OOVInfo result) {
        num += offset;
        oovInfo.add(result);
//        logger.info("addOOVList " +num + " " + offset + " vs " + result.getTotal()+
//            "  Got " + result);
        if (num < result.getTotal()) {
          setProgressPercent(result);
          checkOOVAgainRecurse(projectID);
        } else {
          progressBar.setVisible(false);
          showUnsafeAgain(oovInfo, projectID);
        }
      }
    });
  }

  private void showUnsafeAgain(OOVInfo result, int projectID) {
    showUnsafe(result);

    if (result.isNeedsReload()) {
      controller.getExerciseService().reload(projectID, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("updating exercises...", caught);
        }

        @Override
        public void onSuccess(Void result) {
          logger.info("update complete...");
        }
      });
    }
    controller.getScoringService().getOOVs(projectID, new AsyncCallback<List<OOV>>() {
      @Override
      public void onFailure(Throwable caught) {
        message.setText("Error - please try again.");
        controller.handleNonFatalError("getting oovs", caught);
      }

      @Override
      public void onSuccess(List<OOV> result) {
        message.setText(CHECK_COMPLETE);
      }
    });
  }

  private void gotBlur(OOV currentOOV) {
    final String text = equivalent.getText();
    // logger.info("got " + text);
    controller.getScoringService().isValidForeignPhrase(controller.getProjectID(),
        text, "", new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("checking if valid...", caught);
          }

          @Override
          public void onSuccess(Collection<String> result) {
            if (result.isEmpty()) {
              currentOOV.setEquivalent(text);
              oovContainer.redraw();
              checkButton.setEnabled(!getDirtyItems().isEmpty());
              message.setHTML(getUnmatchedItems().size() + " items left.");

              updateOOV();

            }
          }
        });
  }

  /**
   * @param event
   * @param oovMemoryItemContainer
   */
  private void checkForKeyUpDown(KeyUpEvent event, MemoryItemContainer<OOV> oovMemoryItemContainer) {
    // arrow up down Paul suggestion.
    int keyCode = event.getNativeEvent().getKeyCode();

    int index = oovMemoryItemContainer.getIndex(oovMemoryItemContainer.getCurrentSelection());
    if (keyCode == 40) {  // down
      gotBlur(currentOOV);
      goToNext(oovMemoryItemContainer, index);
    } else if (keyCode == 38) {
      gotBlur(currentOOV);
      OOV at;
      if (index == 0) {
        at = oovMemoryItemContainer.getAt(oovMemoryItemContainer.getSize() - 1);
      } else {
        at = oovMemoryItemContainer.getAt(--index);
      }
      oovMemoryItemContainer.markCurrentExercise(at.getID());
      oovMemoryItemContainer.gotClickOnItem(at);
    } else if (keyCode == 13) {
      //  logger.info("got return " + keyCode);
      gotBlur(currentOOV);
      goToNext(oovMemoryItemContainer, index);
    } else {
      //logger.info("got keyCode " + keyCode);
    }
  }

  private void goToNext(MemoryItemContainer<OOV> oovMemoryItemContainer, int index) {
    OOV at;
    if (index == oovMemoryItemContainer.getSize() - 1) {
      at = oovMemoryItemContainer.getAt(0);
    } else {
      at = oovMemoryItemContainer.getAt(++index);
    }
    oovMemoryItemContainer.markCurrentExercise(at.getID());
    oovMemoryItemContainer.gotClickOnItem(at);
  }

  private OOV currentOOV;

  private class OOVMemoryItemContainer extends MemoryItemContainer<OOV> {
    OOVMemoryItemContainer() {
      super(OOVViewHelper.this.controller, "oov", OOV, 10, 10);
    }

    @Override
    public void gotClickOnItem(OOV user) {
      super.gotClickOnItem(user);
      // logger.info("gotClickOnItem got click on " + user);
      oov.setText(user.getOOV());
      equivalent.setText(user.getEquivalent());
      currentOOV = user;

      exampleContainer.clear();
      //getExample(user);
    }

    /**
     * So the trouble is we need to do exact match on the tokens in the item, not the text.
     * i.e. 1 should not match "blah 16 blah"
     *
     * @param exercise
     * @return
     */
/*    private void getExample(OOV user) {
      Set<ClientExercise> unsafe = oovInfo.getUnsafe();

      List<ClientExercise> collect = getMatches(user.getOOV(), unsafe);

      if (collect.isEmpty()) {
        collect = getMatches(new TextNorm().fromFull(user.getOOV()), unsafe);
      }
      if (!collect.isEmpty()) {
        exampleContainer.add(getExample(collect.get(0)));
      } else {
        ExerciseListRequest request = new ExerciseListRequest(0, controller.getUser());
        request.setAddFirst(true);
        request.setPrefix(new TextNorm().fromFull(user.getOOV()));
        request.setExactMatch(true);
        controller.getExerciseService().getExerciseIds(request, new AsyncCallback<ExerciseListWrapper>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper result) {
            ClientExercise firstExercise = result.getFirstExercise();
            logger.info("Got back " + firstExercise);
            if (firstExercise != null) {
              exampleContainer.add(getExample(firstExercise));

            }
          }
        });
        logger.info("no match for " + oovInfo);
      }
    }*/

/*    private Panel getExample(ClientExercise exercise) {
      TwoColumnExercisePanel<ClientExercise> widgets = new TwoColumnExercisePanel<>(exercise,
          controller,
          null,
          new HashMap<>(), true, new IListenView() {
        @Override
        public int getVolume() {
          return 100;
        }

        @Override
        public int getDialogSessionID() {
          return -1;
        }
      },
          false, () -> "");
      widgets.addWidgets(true, false, PhonesChoices.HIDE);
      return widgets;
    }*/

    /**
     * @see SimplePagingContainer#configureTable(boolean)
     */
    @Override
    protected void addColumnsToTable() {
      List<OOV> list = getList();
      addItemID(list, getMaxLengthId());
      addEquivalent(list, 100);
    }

    /**
     * @param list
     * @param maxLength
     */
    private void addEquivalent(List<OOV> list, int maxLength) {
      Column<OOV, SafeHtml> userCol = getEquivColumn(maxLength);
      table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader(EQUIVALENT));
      table.addColumnSortHandler(getSorter(userCol, list));
    }

    private Column<OOV, SafeHtml> getEquivColumn(int maxLength) {
      return getTruncatedCol(maxLength, this::getEquivValue);
    }

    private ColumnSortEvent.ListHandler<OOV> getSorter(Column<OOV, SafeHtml> englishCol,
                                                       List<OOV> dataList) {
      ColumnSortEvent.ListHandler<OOV> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getEquivCompare);
      return columnSortHandler;
    }

    private int getEquivCompare(OOV o1, OOV o2) {
      int i = o1.getEquivalent().compareTo(o2.getEquivalent());
      return i == 0 ? o1.getOOV().compareTo(o2.getOOV()) : i;
    }

    private String getEquivValue(OOV thing) {
      return thing.getEquivalent();
    }

    @Override
    protected int getIDCompare(OOV o1, OOV o2) {
      return Integer.compare(o1.getID(), o2.getID());
    }

    @Override
    protected int getDateCompare(OOV o1, OOV o2) {
      return Long.compare(o1.getModified(), o2.getModified());
    }

    @Override
    protected String getItemLabel(OOV shell) {
      return shell.getOOV();
    }

    @Override
    protected Long getItemDate(OOV shell) {
      return shell.getModified();
    }
  }

//  @NotNull
//  private List<ClientExercise> getMatches(String oov, Set<ClientExercise> unsafe) {
//    return unsafe.stream().filter(ex -> ex.getForeignLanguage().contains(oov)).collect(Collectors.toList());
//  }

  private static class Wrapper implements HasID {
    int id;
    private String value;

    public Wrapper() {
    }

    Wrapper(int id, String value) {
      this.id = id;
      this.value = value;
    }

    @Override
    public int getID() {
      return id;
    }

    @Override
    public int compareTo(@NotNull HasID o) {
      return Integer.compare(getID(), o.getID());
    }

    String getValue() {
      return value;
    }

    public String toString() {
      return "#" + id + " : " + value;
    }
  }

  private class UnsafeItems extends MemoryItemContainer<Wrapper> {
    UnsafeItems() {
      super(OOVViewHelper.this.controller, "unsafe", ITEMS_WITH_MISSING_WORDS, 10, 10);
    }

//    @Override
//    public void gotClickOnItem(OOV user) {
//      super.gotClickOnItem(user);
//      oov.setText(user.getOOV());
//      equivalent.setText(user.getEquivalent());
//      currentOOV = user;
//    }

    /**
     * @see SimplePagingContainer#configureTable(boolean)
     */
    @Override
    protected void addColumnsToTable() {
      List<Wrapper> list = getList();
      addItemID(list, getMaxLengthId());
      //addEquivalent(list, 100);
    }

    @Override
    protected Column<Wrapper, SafeHtml> getTruncatedCol(int maxLength, GetSafe<Wrapper> getSafe) {
      Column<Wrapper, SafeHtml> column = new Column<Wrapper, SafeHtml>(new ClickableCell()) {
        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, Wrapper object, NativeEvent event) {
          super.onBrowserEvent(context, elem, object, event);
          checkGotClick(object, event);
        }

        @Override
        public SafeHtml getValue(Wrapper shell) {
          return getNoWrapContent(shell.getValue());
        }
      };
      column.setSortable(true);

      return column;
    }

    @Override
    protected ColumnSortEvent.ListHandler<Wrapper> getUserSorter(Column<Wrapper, SafeHtml> englishCol,
                                                                 List<Wrapper> dataList) {
      ColumnSortEvent.ListHandler<Wrapper> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, Comparator.comparing(Wrapper::getValue));
      return columnSortHandler;
    }


    @Override
    protected int getIDCompare(Wrapper o1, Wrapper o2) {
      return Integer.compare(o1.getID(), o2.getID());
    }

    @Override
    protected int getDateCompare(Wrapper o1, Wrapper o2) {
      return 0;
    }

    @Override
    protected String getItemLabel(Wrapper shell) {
      return shell.getValue();
    }

    @Override
    protected Long getItemDate(Wrapper shell) {
      return 0L;
    }
  }
}
