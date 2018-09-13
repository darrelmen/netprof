/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.analysis.Bigram;
import mitll.langtest.shared.analysis.PhoneBigrams;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class BigramContainer extends SimplePagingContainer<PhoneAndStats> {
  private final Logger logger = Logger.getLogger("BigramContainer");

  /**
   * @see #getTableWithPagerForHistory
   */
  private static final int PHONE_CONTAINER_MIN_WIDTH = 220;

  /**
   * @see #setMaxWidth
   */
  private static final int TABLE_WIDTH = 225;
  private static final int MAX_EXAMPLES = 25;

  private static final String SOUND = "Context";
  private static final String COUNT_COL_HEADER = "#";
  /**
   *
   */
  private static final String CURR = "Avg";
  private static final int COUNT_COL_WIDTH = 45;
  private static final String TOOLTIP = "Click to see examples";
  private static final int SOUND_WIDTH = 65;

  private final PhoneExampleContainer exampleContainer;
  private final int listid;
  private final int userid;

  private long from;
  private long to;
  //  private final DateTimeFormat debugShortFormat = DateTimeFormat.getFormat("MMM d yyyy HH:mm:ss");
  private final AnalysisServiceAsync analysisServiceAsync;

  //  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param exampleContainer
   * @param listid
   * @param userid
   * @see AnalysisTab#getPhoneReport
   */
  BigramContainer(ExerciseController controller,
                  PhoneExampleContainer exampleContainer,
                  AnalysisServiceAsync analysisServiceAsync,
                  int listid,
                  int userid) {
    super(controller);
    this.exampleContainer = exampleContainer;
    this.analysisServiceAsync = analysisServiceAsync;
    this.listid = listid;
    this.userid = userid;
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
  }

  protected int getPageSize() {
    return 8;
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", TABLE_WIDTH + "px");
  }

  /**
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager() {
    from = 0;
    to = System.currentTimeMillis();
    return getTableWithPagerForHistory(new ArrayList<>());
  }

  private int reqid = 0;
  private String phone;

  /**
   * @param result
   * @param phone
   * @param from
   * @param to
   * @see PhoneContainer#clickOnPhone2
   */
  void gotNewPhoneBigrams(PhoneBigrams result, String phone, long from, long to) {
    this.from = from;
    this.to = to;

    List<Bigram> bigrams = result.getPhoneToBigrams().get(phone);

    {
      List<PhoneAndStats> phoneAndStatsList;
      if (bigrams == null) {
        logger.warning("no bigrams for phone " + phone);
        phoneAndStatsList = new ArrayList<>();
      } else {
    //    logger.info("gotNewPhoneReport Got " + bigrams.size() + " for " + phone);
        phoneAndStatsList = getPhoneAndStatsListForPeriod(bigrams);
      }
      //   logger.info("gotNewPhoneReport Got " + phoneAndStatsList.size() + " items for " + phone);

      addItems(phoneAndStatsList);
    }

    this.phone = phone;
    showExamplesForSelectedSound();
  }

  private List<PhoneAndStats> getPhoneAndStatsListForPeriod(List<Bigram> bigrams) {
    List<PhoneAndStats> phoneAndStatsList = new ArrayList<>();
    if (bigrams == null) {
      logger.warning("getPhoneAndStatsListForPeriod huh? phoneToAvgSorted is null ");
    } else {

      getPhoneStatuses(phoneAndStatsList, bigrams);
      if (phoneAndStatsList.isEmpty()) {
        logger.warning("getPhoneAndStatsListForPeriod phoneAndStatsList is empty? ");
      }
    }
    return phoneAndStatsList;
  }

  /**
   * Recalculate an average score for those sessions within the time period first to last.
   *
   * @param phoneAndStatses
   * @paramx first
   * @paramx last
   * @seex #timeChanged
   */
  private void getPhoneStatuses(List<PhoneAndStats> phoneAndStatses,
                                List<Bigram> bigrams) {
    bigrams.forEach(bigram ->
        phoneAndStatses.add(new PhoneAndStats(
            bigram.getBigram(),
            Math.round(100F * bigram.getScore()),
            bigram.getCount()))
    );
  }

  /**
   * @param sortedHistory
   * @return
   * @see #getTableWithPager
   */
  private Panel getTableWithPagerForHistory(List<PhoneAndStats> sortedHistory) {
    Panel tableWithPager = getTableWithPager(new ListOptions());
    table.getElement().getStyle().setProperty("minWidth", PHONE_CONTAINER_MIN_WIDTH + "px");

    //  tableWithPager.getElement().setId("PhoneContainerTableScoreHistory");
    tableWithPager.addStyleName("floatLeftAndClear");
    tableWithPager.addStyleName("leftTenMargin");

    addItems(sortedHistory);
    return tableWithPager;
  }

  private void addItems(List<PhoneAndStats> sortedHistory) {
    addPhones(sortedHistory);

    try {
      if (!sortedHistory.isEmpty()) {
        table.getSelectionModel().setSelected(sortedHistory.get(0), true);
      }
    } catch (Exception e) {
      logger.warning("Got " + e);
    }
  }

  /**
   * TODO : add common base class
   *
   * @param sortedHistory
   */
  private void addPhones(List<PhoneAndStats> sortedHistory) {
    clear();

    sortedHistory.forEach(this::addItem);

    flush();

    Scheduler.get().scheduleDeferred(() -> table.redraw());

  }

  /**
   * @see AnalysisTab#getPhoneReport
   */
  private void showExamplesForSelectedSound() {
    List<PhoneAndStats> list = getList();
    if (list.isEmpty()) {
      logger.warning("showExamplesForSelectedSound : list empty?");
    } else {
      clickOnPhone2(list.get(0).getPhone());
    }
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getPhoneSorter(Column<PhoneAndStats, SafeHtml> englishCol,
                                                                    List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              return compPhones(o1, o2);
            }
          }
          return -1;
        });
    return columnSortHandler;
  }

  private int compPhones(PhoneAndStats o1, PhoneAndStats o2) {
    return o1.getPhone().compareTo(o2.getPhone());
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getCountSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                    List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(scoreCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          if (o1 != null) {
            if (o2 == null) {
              logger.warning("------- o2 is null?");
              return -1;
            } else {
              return compIntThenPhone(o1, o2, o1.getCount(), o2.getCount());
            }
          } else {
            logger.warning("------- o1 is null?");

            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getCurrSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                   List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(scoreCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          if (o1 != null) {
            if (o2 == null) {
              // logger.warning("------- o2 is null?");
              return -1;
            } else {
              return compIntThenPhone(o1, o2, o1.getAvg(), o2.getAvg());
            }
          } else {
            // logger.warning("------- o1 is null?");
            return -1;
          }
        });
    return columnSortHandler;
  }

  private int compIntThenPhone(PhoneAndStats o1, PhoneAndStats o2, int a1, int a2) {
    int i = Integer.compare(a1, a2);
    return (i == 0) ? compPhones(o1, o2) : i;
  }

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    addPhones();

    {
      Column<PhoneAndStats, SafeHtml> countColumn = getCountColumn();
      table.setColumnWidth(countColumn, COUNT_COL_WIDTH, Style.Unit.PX);
      table.addColumn(countColumn, COUNT_COL_HEADER);
      table.addColumnSortHandler(getCountSorter(countColumn, getList()));
      countColumn.setSortable(true);
    }

    {
      Column<PhoneAndStats, SafeHtml> currentCol = getCurrentCol();
      table.setColumnWidth(currentCol, COUNT_COL_WIDTH, Style.Unit.PX);
      table.addColumn(currentCol, CURR);
      currentCol.setSortable(true);
      table.addColumnSortHandler(getCurrSorter(currentCol, getList()));

      table.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(currentCol, true));
    }

    table.setWidth("100%", true);

    new TooltipHelper().createAddTooltip(table, TOOLTIP, Placement.TOP);
  }

  private void addPhones() {
    Column<PhoneAndStats, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, SOUND_WIDTH + "px");
    addColumn(itemCol, SOUND);
    table.setWidth("100%", true);
    table.addColumnSortHandler(getPhoneSorter(itemCol, getList()));
  }

  private Column<PhoneAndStats, SafeHtml> getItemColumn() {
    return new Column<PhoneAndStats, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndStats object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkForClick(object, event);
      }

      @Override
      public SafeHtml getValue(PhoneAndStats shell) {
        int current = shell.getAvg();
        float percent = ((float) current) / 100f;
        String columnText = new WordTable().getColoredSpan(shell.getPhone(), percent);
        return getSafeHtml(columnText);
      }
    };
  }

  private void checkForClick(PhoneAndStats object, NativeEvent event) {
    if (BrowserEvents.CLICK.equals(event.getType())) {
      clickOnPhone2(object.getPhone());
    } else {
      logger.info("checkForClick got other event " + event.getType());
    }
  }

  /**
   * TODO : common base class
   *
   * @param bigram
   * @see #checkForClick
   */
  private void clickOnPhone2(String bigram) {
    //   logger.info("clickOnPhone2 bigram = " + bigram);
    analysisServiceAsync.getPerformanceReportForUserForPhone(
        getAnalysisRequest(from, to)
            .setPhone(phone)
            .setBigram(bigram),
        new AsyncCallback<List<WordAndScore>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("getting performance report for user and phone", caught);
          }

          @Override
          public void onSuccess(List<WordAndScore> filteredWords) {
            if (filteredWords == null) {
              logger.warning("clickOnPhone2 no result for " + phone + " " + bigram);
              exampleContainer.addItems(phone, bigram, Collections.emptyList(), MAX_EXAMPLES);
            } else {
     /*     filteredWords.forEach(wordAndScore -> logger.info("clickOnPhone2 : for " + phone + " and bigram " + bigram +
              "  got " + wordAndScore));
          */

              exampleContainer.addItems(phone,
                  bigram, filteredWords.subList(0, Math.min(filteredWords.size(), MAX_EXAMPLES)),
                  MAX_EXAMPLES);
            }
          }
        });
  }

  private AnalysisRequest getAnalysisRequest(long from, long to) {
    return new AnalysisRequest()
        .setUserid(userid)
        .setListid(listid)
        .setFrom(from)
        .setTo(to)
        .setDialogID(new SelectionState().getDialog())
        .setReqid(reqid++);
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Column<PhoneAndStats, SafeHtml> getCurrentCol() {
    return new Column<PhoneAndStats, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndStats object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkForClick(object, event);
      }

      @Override
      public SafeHtml getValue(PhoneAndStats shell) {
        int avg = shell.getAvg();
        return new SafeHtmlBuilder().appendHtmlConstant(getScoreMarkup(avg)).toSafeHtml();
      }
    };
  }

  private String getScoreMarkup(int score) {
    return "<span style='margin-left:10px;'>" + score + "</span>";
  }

  private Column<PhoneAndStats, SafeHtml> getCountColumn() {
    return new Column<PhoneAndStats, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndStats object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkForClick(object, event);
      }

      @Override
      public SafeHtml getValue(PhoneAndStats shell) {
        return new SafeHtmlBuilder().appendHtmlConstant("" + shell.getCount()).toSafeHtml();
      }
    };
  }

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    BigramContainer.LocalTableResources.TableStyle cellTableStyle();
  }
}
