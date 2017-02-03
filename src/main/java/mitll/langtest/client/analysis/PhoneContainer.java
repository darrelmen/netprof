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
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
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
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.PhoneStats;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class PhoneContainer extends SimplePagingContainer<PhoneAndStats> implements AnalysisPlot.TimeChangeListener {
  private final Logger logger = Logger.getLogger("PhoneContainer");

  private static final int MAX_EXAMPLES = 25;

  private static final int TABLE_WIDTH = 295;
  private static final int SCORE_COL_WIDTH = 60;
  private static final String SOUND = "Sound";
  private static final String SCORE = "Initial";
  private static final String COUNT_COL_HEADER = "#";
  private static final String CURR = "Curr.";
  private static final String DIFF_COL_HEADER = "+/-";
  private static final int COUNT_COL_WIDTH = 45;
  private static final String TOOLTIP = "Click to see examples and scores over time";//"Click on an item to review.";
  private static final int SOUND_WIDTH = 75;
  private final PhoneExampleContainer exampleContainer;
  private final PhonePlot phonePlot;

  private final boolean isNarrow;
  private long from;
  private long to;
  //  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");
  private final DateTimeFormat debugShortFormat = DateTimeFormat.getFormat("MMM d yyyy");

  /**
   * @param controller
   * @param exampleContainer
   * @param phonePlot
   * @param isNarrow
   * @see AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   */
  public PhoneContainer(ExerciseController controller, PhoneExampleContainer exampleContainer,
                        PhonePlot phonePlot, boolean isNarrow) {
    super(controller);
    this.exampleContainer = exampleContainer;
    this.phonePlot = phonePlot;
    this.isNarrow = isNarrow;
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

  private PhoneReport phoneReport;

  /**
   * @param phoneReport
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager(PhoneReport phoneReport) {
    from = 0;
    to = System.currentTimeMillis();
    this.phoneReport = phoneReport;
    return getTableWithPager(getPhoneAndStatsList(from, to));
  }

  /**
   * @param from
   * @param to
   * @see AnalysisPlot#changed(long, long)
   */
  @Override
  public void timeChanged(long from, long to) {
//    logger.info("timeChanged From " + debugFormat(from) + " : " + debugFormat(to));

    this.from = from;
    this.to = to;

    addItems(getPhoneAndStatsList(from, to));
    showExamplesForSelectedSound();
  }

  private List<PhoneAndStats> getPhoneAndStatsList(long from, long to) {
    if (phoneReport == null) {
      return Collections.emptyList();
    }
    else {
      Map<String, PhoneStats> phoneToAvgSorted = phoneReport.getPhoneToAvgSorted();
      return getPhoneAndStatsListForPeriod(phoneToAvgSorted, from, to);
    }
  }

  private List<PhoneAndStats> getPhoneAndStatsListForPeriod(Map<String, PhoneStats> phoneToAvgSorted, long first, long last) {
//    logger.info("timeChanged From " + first+"/"+debugFormat(first) + " : " + last +"/"+debugFormat(last));

    List<PhoneAndStats> phoneAndStatsList = new ArrayList<>();
    if (phoneToAvgSorted == null) {
      logger.warning("getPhoneAndStatsListForPeriod huh? phoneToAvgSorted is null ");
    } else {
      getPhoneStatuses(phoneAndStatsList, phoneToAvgSorted, first, last);
      if (phoneAndStatsList.isEmpty()) {
        logger.warning("getPhoneAndStatsListForPeriod phoneAndStatsList is empty? ");
      }
    }
    return phoneAndStatsList;
  }

  /**
   * @param phoneAndStatses
   * @param phoneToAvgSorted
   * @param first
   * @param last
   * @see #timeChanged(long, long)
   */
  private void getPhoneStatuses(List<PhoneAndStats> phoneAndStatses, Map<String, PhoneStats> phoneToAvgSorted,
                                long first, long last) {
//    logger.info("getPhoneStatuses From " + first+"/"+debugFormat(first) + " : " + last+"/"+debugFormat(last));

    for (Map.Entry<String, PhoneStats> ps : phoneToAvgSorted.entrySet()) {
      PhoneStats value = ps.getValue();
      List<PhoneSession> filtered = getFiltered(first, last, value);
      //    logger.info("key " + ps.getKey() + " value " + filtered.size());
      //  logger.info("Filtered " + filtered.size());
      int initial = value.getInitial(filtered);
      int current = value.getCurrent(filtered);
      int count = value.getCount(filtered);
      if (!filtered.isEmpty()) {
        phoneAndStatses.add(new PhoneAndStats(ps.getKey(),
            initial,
            current,
            count
        ));
      }
    }

    Collections.sort(phoneAndStatses);

//    logger.info("getPhoneStatuses returned " + phoneAndStatses.size());
  }

  /**
   * @param first
   * @param last
   * @param value
   * @return
   * @see #getPhoneStatuses(List, Map, long, long)
   */
  private List<PhoneSession> getFiltered(long first, long last, PhoneStats value) {
    // logger.info("getFiltered " + first + "/" + debugFormat(first) + " - " + debugFormat(last));
    return first == 0 ? value.getSessions() : getFiltered(value.getSessions(), first, last);
  }

//  private String shortFormat(long first) {
//    return superShortFormat.format(new Date(first));
//  }

  private String debugFormat(long first) {
    return debugShortFormat.format(new Date(first));
  }

  /**
   * TODO : this doesn't work properly - should do any sessions that overlap with the window
   *
   * @param orig
   * @param first
   * @param last
   * @return
   * @see #clickOnPhone(String)
   * @see #getFiltered(long, long, PhoneStats)
   */
  private List<PhoneSession> getFiltered(List<PhoneSession> orig, long first, long last) {
    // logger.info("getFiltered : From " + first + "/" + debugFormat(first) + " - " + debugFormat(last) + " window dur " + (last - first));

    List<PhoneSession> filtered = new ArrayList<>();
    for (PhoneSession session : orig) {
      //  String window = shortFormat(session.getStart()) + " - " + shortFormat(session.getEnd());
      if (doesSessionOverlap(first, last, session)
          ) {
        filtered.add(session);
        //  logger.info("included " + window);
      } else {
        //logger.info("Exclude " +window);
      }
    }
    return filtered;
  }

  private boolean doesSessionOverlap(long first, long last, PhoneSession session) {
    return (session.getStart() >= first && session.getStart() <= last) || // start inside window
        (session.getEnd() >= first && session.getEnd() <= last) ||    // end inside window
        (session.getStart() < first && session.getEnd() > last);      // session starts before and ends after window
  }

  /**
   * @param orig
   * @param first
   * @param last
   * @return
   * @see #clickOnPhone(String)
   */
  private List<WordAndScore> getFilteredWords(Collection<WordAndScore> orig, long first, long last) {
    if (first > last) {
      // throw new IllegalArgumentException("getFilteredWords " + orig.size() + " first after last?");
      logger.warning("getFilteredWords " + orig.size() + " first after last?");
    }
    //  logger.info("getFilteredWords From " + debugFormat(first) + " - " + debugFormat(last) + " window dur " + (last - first));

    List<WordAndScore> filtered = new ArrayList<>();
    for (WordAndScore session : orig) {
      //  String window = shortFormat(session.getStart()) + " - " + shortFormat(session.getEnd());
      if (session.getTimestamp() > first && session.getTimestamp() <= last) {
        filtered.add(session);
        //  logger.info("included " + window);
      }
      //else {
      // logger.info("Exclude " +window);
      // }
    }
    return filtered;
  }

  /**
   * @param sortedHistory
   * @return
   * @see #getTableWithPager(PhoneReport)
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
    addPhones(sortedHistory);

    try {
      if (!sortedHistory.isEmpty()) {
        table.getSelectionModel().setSelected(sortedHistory.get(0), true);
      }
    } catch (Exception e) {
      logger.warning("Got " + e);
    }
  }

  private void addPhones(List<PhoneAndStats> sortedHistory) {
    clear();
    for (PhoneAndStats ps : sortedHistory) {
      addItem(ps);
    }
    flush();
  }

  /**
   * @see AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   */
  public void showExamplesForSelectedSound() {
    List<PhoneAndStats> list = getList();
    if (list.isEmpty()) {
      logger.warning("showExamplesForSelectedSound : list empty?");
    } else {
      clickOnPhone(list.get(0).getPhone());
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
                return compPhones(o1, o2);
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
                return compIntThenPhone(o1, o2, a1, a2);
              }
            } else {
              logger.warning("------- o1 is null?");
              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  private int compIntThenPhone(PhoneAndStats o1, PhoneAndStats o2, int a1, int a2) {
    int i = Integer.valueOf(a1).compareTo(a2);
    return (i == 0) ? compPhones(o1, o2) : i;
  }

  private int compPhones(PhoneAndStats o1, PhoneAndStats o2) {
    return o1.getPhone().compareTo(o2.getPhone());
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
                return compIntThenPhone(o1, o2, a1, a2);
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
                return compIntThenPhone(o1, o2, a1, a2);

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
                return compIntThenPhone(o1, o2, a1, a2);
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

    new TooltipHelper().createAddTooltip(table, TOOLTIP, Placement.RIGHT);
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
        float percent = ((float) current) / 100f;
        String columnText = new WordTable().getColoredSpan(shell.getPhone(), percent);
        return getSafeHtml(columnText);
      }
    };
  }

  private void checkForClick(PhoneAndStats object, NativeEvent event) {
    if (BrowserEvents.CLICK.equals(event.getType())) {
      clickOnPhone(object.getPhone());
    } else {
      logger.info("got other event " + event.getType());
    }
  }

  /**
   * TODO : why are the numbers different for word examples vs count???
   *
   * @param phone
   */
  private void clickOnPhone(String phone) {
    PhoneStats stats = phoneReport.getPhoneToAvgSorted().get(phone);
    //  logger.info("clickOnPhone " + debugFormat(from) + " - " + debugFormat(to));
    List<PhoneSession> filtered = getFiltered(stats.getSessions(), from, to);

    SortedSet<WordAndScore> examples = new TreeSet<>();
    for (PhoneSession session : filtered) examples.addAll(session.getExamples());
    List<WordAndScore> filteredWords = new ArrayList<>(examples);

    // TODO: better ways of doing this.
    filteredWords = filteredWords.subList(0, Math.min(filteredWords.size(), MAX_EXAMPLES));
    exampleContainer.addItems(phone, filteredWords, MAX_EXAMPLES);

    phonePlot.showErrorBarData(filtered, phone);
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

  private String getScoreMarkup(int score) {
    return "<span " + "style='" + "margin-left:10px;" + "'" + ">" + score + "</span>";
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

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
