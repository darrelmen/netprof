package mitll.langtest.client.analysis;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.AnalysisRequest;

import java.util.List;
import java.util.logging.Logger;

public abstract class PhoneContainerBase extends SimplePagingContainer<PhoneAndStats> {
  private final Logger logger = Logger.getLogger("PhoneContainerBase");
  /**
   * @see #getTableWithPagerForHistory
   */
  private static final int PHONE_CONTAINER_MIN_WIDTH = 220;
  /**
   * @see #setMaxWidth
   */
  private static final int TABLE_WIDTH = 225;
  private static final String COUNT_COL_HEADER = "#";
  /**
   *
   */
  private static final String CURR = "Avg";//"Average";//"Avg. Score";
  private static final int COUNT_COL_WIDTH = 45;
//  private static final String TOOLTIP = "Click to see examples";// and scores over time";
  private static final int SOUND_WIDTH = 65;


  final AnalysisServiceAsync analysisServiceAsync;
  protected final int listid;
  protected final int userid;
  protected long from;
  protected long to;
  protected int reqid = 0;

  PhoneContainerBase(ExerciseController controller, AnalysisServiceAsync analysisServiceAsync, int listid, int userid) {
    super(controller);
    this.analysisServiceAsync = analysisServiceAsync;
    this.listid = listid;
    this.userid = userid;
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", TABLE_WIDTH + "px");
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
    addPhones(getLabel());

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

//    new TooltipHelper().createAddTooltip(table, TOOLTIP, Placement.TOP);
  }

  protected int getPageSize() {
    return 8;
  }

  protected abstract String getLabel();

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
        return PhoneContainerBase.this.getSafeHtml(getScoreMarkup(avg));
      }
    };
  }

  private String getScoreMarkup(int score) {
    return "<span style='margin-left:10px;'>" + score + "</span>";
  }


  private void addPhones(String label) {
    Column<PhoneAndStats, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, SOUND_WIDTH + "px");
    addColumn(itemCol, label);
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
   // logger.info("checkForClick : stats " + object);
    if (BrowserEvents.CLICK.equals(event.getType())) {
      //   clickOnPhone(object.getPhone());
      clickOnPhone2(object.getPhone());
    } else {
      logger.info("got other event " + event.getType());
    }
  }

  abstract void clickOnPhone2(String bigram);

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  /**
   * @param sortedHistory
   * @return
   * @see #getTableWithPager
   */
  Panel getTableWithPagerForHistory(List<PhoneAndStats> sortedHistory) {
    Panel tableWithPager = getTableWithPager(new ListOptions());
    table.getElement().getStyle().setProperty("minWidth", PHONE_CONTAINER_MIN_WIDTH + "px");

    //  tableWithPager.getElement().setId("PhoneContainerTableScoreHistory");
    tableWithPager.addStyleName("floatLeftAndClear");
    tableWithPager.addStyleName("leftTenMargin");

    addItems(sortedHistory);
    return tableWithPager;
  }

   void addItems(List<PhoneAndStats> sortedHistory) {
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
   * Remember to ask to redraw later
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
  void showExamplesForSelectedSound() {
    List<PhoneAndStats> list = getList();
    if (list.isEmpty()) {
      logger.warning("showExamplesForSelectedSound : list empty?");
    } else {
      clickOnPhone2(list.get(0).getPhone());
    }
  }

  AnalysisRequest getAnalysisRequest(long from, long to) {
    return new AnalysisRequest()
        .setUserid(userid)
        .setListid(listid)
        .setFrom(from)
        .setTo(to)
        .setDialogID(new SelectionState().getDialog())
        .setReqid(reqid++);
  }
}
