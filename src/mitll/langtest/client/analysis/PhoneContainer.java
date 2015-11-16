package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneStats;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class PhoneContainer extends SimplePagingContainer<PhoneAndStats> {
  public static final int TABLE_WIDTH = 295;
  public static final int SCORE_COL_WIDTH = 60;
  public static final String SOUND = "Sound";
  public static final String SCORE = "Initial";
  public static final String COUNT_COL_HEADER = "#";
  public static final String CURR = "Curr.";
  public static final String DIFF_COL_HEADER = "+/-";
  public static final int COUNT_COL_WIDTH = 45;
  private final Logger logger = Logger.getLogger("PhoneContainer");
  private static final int SOUND_WIDTH = 75;
  private PhoneExampleContainer exampleContainer;
  private final PhonePlot phonePlot;
  boolean isNarrow;

  /**
   * @see AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   * @param controller
   * @param exampleContainer
   * @param phonePlot
   * @param isNarrow
   */
  public PhoneContainer(ExerciseController controller, PhoneExampleContainer exampleContainer,  PhonePlot phonePlot, boolean isNarrow) {
    super(controller);
    this.exampleContainer = exampleContainer;
    this.phonePlot = phonePlot;
    this.isNarrow = isNarrow;
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<PhoneAndStats>();
    table.setSelectionModel(selectionModel);
  }

  protected int getPageSize() {
    return 8;
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", TABLE_WIDTH + "px");
  }

  private PhoneReport phoneReport;

  /**
   * @param phoneReport
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager(PhoneReport phoneReport) {
    List<PhoneAndStats> phoneAndStatses = new ArrayList<>();
    Map<String, PhoneStats> phoneToAvgSorted = phoneReport.getPhoneToAvgSorted();
    if (phoneToAvgSorted == null) {
      logger.warning("huh? phoneToAvgSorted is null ");
    } else {
      for (Map.Entry<String, PhoneStats> ps : phoneToAvgSorted.entrySet()) {
        PhoneStats value = ps.getValue();
        phoneAndStatses.add(new PhoneAndStats(ps.getKey(),
            value.getInitial(),
            value.getCurrent(),
            value.getCount()
        ));
      }
    }
    this.phoneReport = phoneReport;
    return getTableWithPager(phoneAndStatses);
  }

  /**
   * @param sortedHistory
   * @return
   * @see SetCompleteDisplay#getScoreHistory(List, List, ExerciseController)
   */
  private Panel getTableWithPager(List<PhoneAndStats> sortedHistory) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.addStyleName("floatLeft");
    tableWithPager.addStyleName("leftTenMargin");

    addItems(sortedHistory);
    return tableWithPager;
  }

  private void addItems(List<PhoneAndStats> sortedHistory) {
    for (PhoneAndStats ps : sortedHistory) {  addItem(ps); }
    flush();

    try {
      if (!sortedHistory.isEmpty()) {
        table.getSelectionModel().setSelected(sortedHistory.get(0), true);
      }
    } catch (Exception e) {
      logger.warning("Got " + e);
    }
  }

  /**
   * @see AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   */
  public void showExamplesForSelectedSound() {
    List<PhoneAndStats> list = getList();
    if (list.isEmpty()) {
      logger.info("list empty?");
    } else {
      String phone = list.get(0).getPhone();
      List<WordAndScore> wordExamples = phoneReport.getWordExamples(phone);
     // for (WordAndScore ws : wordExamples) logger.info("showExamplesForSelectedSound got " + ws.getScore() + " " + ws.getWord());
      //  logger.info("showExamplesForSelectedSound adding " + phone + " num examples " + wordExamples.size());
      exampleContainer.addItems(phone, wordExamples);
      List<TimeAndScore> timeSeries = phoneReport.getPhoneToAvgSorted().get(phone).getTimeSeries();
      phonePlot.showData(getByTime(timeSeries),phone, isNarrow);
    }
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getEnglishSorter(Column<PhoneAndStats, SafeHtml> englishCol,
                                                                      List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndStats>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<PhoneAndStats>() {
          public int compare(PhoneAndStats o1, PhoneAndStats o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getPhone().compareTo(o2.getPhone());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getScoreSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                    List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndStats>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<PhoneAndStats>() {
          public int compare(PhoneAndStats o1, PhoneAndStats o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                int a1 = o1.getInitial();
                int a2 = o2.getInitial();
                int i = Integer.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return o1.getPhone().compareTo(o2.getPhone());
                } else {
                  return i;
                }
              }
            } else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getCountSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                    List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndStats>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<PhoneAndStats>() {
          public int compare(PhoneAndStats o1, PhoneAndStats o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                int a1 = o1.getCount();
                int a2 = o2.getCount();
                int i = Integer.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return o1.getPhone().compareTo(o2.getPhone());
                } else {
                  return i;
                }
              }
            } else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<PhoneAndStats> getCurrSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                   List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndStats>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<PhoneAndStats>() {
          public int compare(PhoneAndStats o1, PhoneAndStats o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                int a1 = o1.getCurrent();
                int a2 = o2.getCurrent();
                int i = Integer.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return o1.getPhone().compareTo(o2.getPhone());
                } else {
                  return i;
                }
              }
            } else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<PhoneAndStats> getDiffSorter(Column<PhoneAndStats, SafeHtml> scoreCol,
                                                                   List<PhoneAndStats> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndStats>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<PhoneAndStats>() {
          public int compare(PhoneAndStats o1, PhoneAndStats o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                int a1 = o1.getDiff();
                int a2 = o2.getDiff();
                int i = Integer.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return o1.getPhone().compareTo(o2.getPhone());
                } else {
                  return i;
                }
              }
            } else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  private void addReview() {
    Column<PhoneAndStats, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, SOUND_WIDTH + "px");
    addColumn(itemCol, new TextHeader(SOUND));
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<PhoneAndStats> columnSortHandler = getEnglishSorter(itemCol, getList());
    table.addColumnSortHandler(columnSortHandler);
  }

  @Override
  protected void addColumnsToTable() {
    addReview();

    Column<PhoneAndStats, SafeHtml> countColumn = getCountColumn();
    table.setColumnWidth(countColumn, COUNT_COL_WIDTH, Style.Unit.PX);
    table.addColumn(countColumn, COUNT_COL_HEADER);

    ColumnSortEvent.ListHandler<PhoneAndStats> countSorter = getCountSorter(countColumn, getList());
    table.addColumnSortHandler(countSorter);
    countColumn.setSortable(true);

    Column<PhoneAndStats, SafeHtml> scoreColumn = getScoreColumn();
    table.setColumnWidth(scoreColumn, SCORE_COL_WIDTH, Style.Unit.PX);
    table.addColumn(scoreColumn, SCORE);
    scoreColumn.setSortable(true);
    table.addColumnSortHandler(getScoreSorter(scoreColumn, getList()));

    Column<PhoneAndStats, SafeHtml> currentCol = getCurrentCol();
    table.setColumnWidth(currentCol, SCORE_COL_WIDTH, Style.Unit.PX);
    table.addColumn(currentCol, CURR);
    currentCol.setSortable(true);
    table.addColumnSortHandler(getCurrSorter(currentCol, getList()));

    Column<PhoneAndStats, SafeHtml> diffCol = getDiff();
    table.setColumnWidth(diffCol, SCORE_COL_WIDTH, Style.Unit.PX);
    table.addColumn(diffCol, DIFF_COL_HEADER);
    diffCol.setSortable(true);
    table.addColumnSortHandler(getDiffSorter(diffCol, getList()));

    table.setWidth("100%", true);


    new TooltipHelper().createAddTooltip(table, "Click on an item to review.", Placement.RIGHT);
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
        int current = shell.getCurrent();
        float percent = ((float) current)/100f;
        String columnText = new WordTable().getColoredSpan(shell.getPhone(), percent);
        return getSafeHtml(columnText);
      }
    };
  }

  private void checkForClick(PhoneAndStats object, NativeEvent event) {
    if (BrowserEvents.CLICK.equals(event.getType())) {
      gotClickOnItem(object);
    }
  }

  private void gotClickOnItem(final PhoneAndStats e) {
    String phone = e.getPhone();
    List<WordAndScore> wordExamples = phoneReport.getWordExamples(phone);
   // for (WordAndScore ws : wordExamples) logger.info("gotClickOnItem got " + ws.getScore() + " " + ws.getWord());
    exampleContainer.addItems(phone, wordExamples);
    List<TimeAndScore> timeSeries = phoneReport.getPhoneToAvgSorted().get(phone).getTimeSeries();

//    getByTime(timeSeries);
//    DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
 //   for (TimeAndScore ts : timeSeries) logger.info("gotClickOnItem " + format.format(new Date(ts.getTimestamp())) + " " +ts.getScore());
    phonePlot.showData(getByTime(timeSeries),phone, isNarrow);
  }

  private List<TimeAndScore> getByTime(List<TimeAndScore> timeSeries) {
    List<TimeAndScore> copy = new ArrayList<>();
    for (TimeAndScore ts : timeSeries) copy.add(ts);
    Collections.sort(copy, new Comparator<TimeAndScore>() {
      @Override
      public int compare(TimeAndScore o1, TimeAndScore o2) {
        return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
      }
    });
    return copy;
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Column<PhoneAndStats, SafeHtml> getScoreColumn() {
    return new Column<PhoneAndStats, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndStats object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkForClick(object, event);
      }

      @Override
      public SafeHtml getValue(PhoneAndStats shell) {
        int score = shell.getInitial();
        return new SafeHtmlBuilder().appendHtmlConstant(getScoreMarkup(score)).toSafeHtml();
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
        return new SafeHtmlBuilder().appendHtmlConstant(getScoreMarkup(shell.getCurrent())).toSafeHtml();
      }
    };
  }

  private Column<PhoneAndStats, SafeHtml> getDiff() {
    return new Column<PhoneAndStats, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndStats object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkForClick(object, event);
      }

      @Override
      public SafeHtml getValue(PhoneAndStats shell) {
        return new SafeHtmlBuilder().appendHtmlConstant(getScoreMarkup(shell.getDiff())).toSafeHtml();
      }
    };
  }

  public String getScoreMarkup(int score) {
    return "<span " +  "style='" +  "margin-left:10px;" + "'" + ">" + score + "</span>";
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
        String s = "" + shell.getCount();
        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
/*    interface TableStyle extends CellTable.Style {
    }*/

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
