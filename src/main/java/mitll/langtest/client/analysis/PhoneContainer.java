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
import mitll.langtest.shared.analysis.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class PhoneContainer extends SimplePagingContainer<PhoneAndStats> implements AnalysisPlot.TimeChangeListener {
  private final Logger logger = Logger.getLogger("PhoneContainer");

  /**
   * @see #getTableWithPagerForHistory
   */
  private static final int PHONE_CONTAINER_MIN_WIDTH = 220;

  /**
   * @see #setMaxWidth
   */
  private static final int TABLE_WIDTH = 225;
  // private static final int MAX_EXAMPLES = 25;

  private static final String SOUND = "Sound";
  private static final String COUNT_COL_HEADER = "#";
  /**
   *
   */
  private static final String CURR = "Avg";//"Average";//"Avg. Score";
  private static final int COUNT_COL_WIDTH = 45;
  private static final String TOOLTIP = "Click to see examples";// and scores over time";
  private static final int SOUND_WIDTH = 65;

  private final BigramContainer bigramContainer;
  private final int listid;
  private final int userid;

  private long from;
  private long to;
  private final DateTimeFormat debugShortFormat = DateTimeFormat.getFormat("MMM d yyyy HH:mm:ss");

  private static final boolean DEBUG = false;
  private final AnalysisServiceAsync analysisServiceAsync;

  /**
   * @param controller
   * @param bigramContainer
   * @param listid
   * @param userid
   * @see AnalysisTab#getPhoneReport
   */
  PhoneContainer(ExerciseController controller,
                 BigramContainer bigramContainer,
                 AnalysisServiceAsync analysisServiceAsync,
                 int listid,
                 int userid) {
    super(controller);
    this.bigramContainer = bigramContainer;
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

  PhoneSummary phoneReport;

  /**
   * @param phoneReport
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager(PhoneSummary phoneReport) {
    from = 0;
    to = System.currentTimeMillis();
    this.phoneReport = phoneReport;
    return getTableWithPagerForHistory(getPhoneAndStatsList(phoneReport, from, to));
  }

  private int reqid = 0;

  /**
   * @param from
   * @param to
   * @see AnalysisPlot#timeChanged
   * @see #getTableWithPager
   */
  @Override
  public void timeChanged(long from, long to) {
    if (DEBUG) logger.info("PhoneContainer.timeChanged From " + debugFormat(from) + " : " + debugFormat(to));

    this.from = from;
    this.to = to;
    long then = System.currentTimeMillis();

    AnalysisRequest analysisRequest = getAnalysisRequest(from, to);


    analysisServiceAsync.getPhoneSummary(
        analysisRequest, new AsyncCallback<PhoneSummary>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("\n\n\n-> getPhoneSummary " + caught);
            controller.getMessageHelper().handleNonFatalError("problem getting phone summary", caught);
          }

          @Override
          public void onSuccess(PhoneSummary result) {
            long now = System.currentTimeMillis();
            long total = now - then;
            logger.info("getPhoneSummary userid " + userid + " req " + reqid +
                "\n\ttook   " + total +
                "\n\tserver " + result.getServerTime() +
                "\n\tclient " + (total - result.getServerTime()));


            if (result.getReqid() + 1 != reqid) {
              logger.info("skip stale req");
            } else {
              gotNewPhoneSummary(result);
            }
          }
        });
  }

  /**
   * TODO :  ??? why filter by time twice??
   * @param result
   */
  private void gotNewPhoneSummary(PhoneSummary result) {
    List<PhoneAndStats> phoneAndStatsList = getPhoneAndStatsListForPeriod(result.getPhoneToAvgSorted(), from, to);
    addItems(phoneAndStatsList);
    showExamplesForSelectedSound();
  }


  private List<PhoneAndStats> getPhoneAndStatsList(PhoneSummary phoneReport, long from, long to) {
    return (phoneReport == null) ? Collections.emptyList() :
        getPhoneAndStatsListForPeriod(phoneReport.getPhoneToAvgSorted(), from, to);
  }


  private List<PhoneAndStats> getPhoneAndStatsListForPeriod(Map<String, PhoneStats> phoneToAvgSorted,
                                                            long first,
                                                            long last) {

    List<PhoneAndStats> phoneAndStatsList = new ArrayList<>();
    if (phoneToAvgSorted == null) {
      logger.warning("getPhoneAndStatsListForPeriod huh? phoneToAvgSorted is null ");
    } else {
      if (DEBUG)
        logger.info("getPhoneAndStatsListForPeriod From " + first +
            "/" + debugFormat(first) + " : " + last + "/" + debugFormat(last) + " over " + phoneToAvgSorted.size());

      getPhoneStatuses(phoneAndStatsList, phoneToAvgSorted, first, last);
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
   * @param phoneToAvgSorted
   * @param first
   * @param last
   * @see #timeChanged
   */
  private void getPhoneStatuses(List<PhoneAndStats> phoneAndStatses,
                                Map<String, PhoneStats> phoneToAvgSorted,
                                long first, long last) {
    if (DEBUG) {
      logger.info("getPhoneStatuses From    " + first + "/" + debugFormat(first) + " : " + last + "/" + debugFormat(last));
      logger.info("getPhoneStatuses examine " + phoneToAvgSorted.entrySet().size());
    }

    // if avg score is less than 75 = native
    // get diff, weighted by total
    // this diff is the rank - highest go first
    // then above 75 weighted... how?

    for (Map.Entry<String, PhoneStats> ps : phoneToAvgSorted.entrySet()) {
      PhoneStats value = ps.getValue();
      List<PhoneSession> filtered = getFiltered(first, last, value);
      //    logger.info("key " + ps.getKey() + " value " + filtered.size());
      //  logger.info("Filtered " + filtered.size());

      if (!filtered.isEmpty()) {
        float total = 0;
        long ltotal = 0;
        float avg = 0;
        float ndiff = 0;
        for (PhoneSession session : filtered) {
          long count1 = session.getCount();
          ltotal += count1;
          float fcount = Long.valueOf(count1).floatValue();
          total += fcount;
          double mean = session.getMean();
          float fmean = Double.valueOf(mean).floatValue();
          float diffNative = 0.75F - fmean;
          if (fmean > 0.75) {
            float diffN1 = (1F - fmean) / 100F;
            ndiff += diffN1 * fcount;
          } else {
            float ldiff = (float) diffNative;
            float weight = ldiff * fcount;
            ndiff += weight;
          }

          avg += fmean * fcount;
        }
        float overall = avg / total;

        int v = Float.valueOf(overall * 100).intValue();
        if (DEBUG) {
          logger.info("getPhoneStatuses : overall " + overall + " avg " + avg + " total " + total + " report " + v);
        }

        // int totalCount = value.getTotalCount(filtered);
        String thePhone = ps.getKey();
        //if (SORT_BY_RANK) logger.info(thePhone + " : total " + total + " ndiff " + ndiff);
        phoneAndStatses.add(new PhoneAndStats(thePhone, v, Long.valueOf(ltotal).intValue()));
      }
    }

//    if (SORT_BY_RANK) {
//      phoneAndStatses.sort((o1, o2) -> {
//        int compare = -1 * Float.compare(o1.getNdiff(), o2.getNdiff());
//        return compare == 0 ? o1.compareTo(o2) : compare;
//      });
//
//      for (int i = 0; i < phoneAndStatses.size(); i++) phoneAndStatses.get(i).setRank(i + 1);
//    } else {
    Collections.sort(phoneAndStatses);
//    }
    if (DEBUG) {
      logger.info("getPhoneStatuses returned " + phoneAndStatses.size());
      if (phoneAndStatses.isEmpty()) {
        phoneToAvgSorted.forEach((k, v) -> {
          logger.info(k + " = " + v.getSessions().size() + " sessions");
          v.getSessions()
              .forEach(phoneSession -> logger.info("\t" + k + " = " +
                  debugFormat(phoneSession.getStart()) + "-" + debugFormat(phoneSession.getEnd())));
        });
      }
    }
  }

  /**
   * Get sessions in this time period.
   *
   * @param first
   * @param last
   * @param value
   * @return
   * @see #getPhoneStatuses(List, Map, long, long)
   */
  private List<PhoneSession> getFiltered(long first, long last, PhoneStats value) {
    List<PhoneSession> sessions = value.getSessions();
    if (DEBUG) {
      logger.info("getFiltered " + first + "/" +
          debugFormat(first) + " - " + debugFormat(last) +
          " over " + sessions.size() + " sessions");
    }
    return first == 0 ? sessions : getFiltered(sessions, first, last);
  }

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
   * @see #clickOnPhone2
   * @see #getFiltered(long, long, PhoneStats)
   */
  private List<PhoneSession> getFiltered(List<PhoneSession> orig, long first, long last) {
    if (DEBUG && true) {
      logger.info("getFiltered : over " + orig.size() +
          " From " + first + "/" + debugFormat(first) + " - " + debugFormat(last) + " window dur " + (last - first));
    }

    List<PhoneSession> filtered = new ArrayList<>();
    for (PhoneSession session : orig) {
      //  String window = shortFormat(session.getStart()) + " - " + shortFormat(session.getEnd());
      if (doesSessionOverlap(first, last, session)) {
        filtered.add(session);

        if (DEBUG) {
          logger.info("getFiltered included " + session.getPhone() +
              " " +
              debugFormat(session.getStart()) + "-" +
              debugFormat(session.getEnd())
          );
        }
      } else if (DEBUG) {
        logger.info("getFiltered exclude " + session);
      }
    }
    if (DEBUG) {
      logger.info("getFiltered : over " + orig.size() +
          " From " + first + "/" + debugFormat(first) + " - " + debugFormat(last) +
          " window dur " + (last - first) + " found " + filtered.size());
//      logger.info("getFiltered : found " + filtered.size());
    }
    return filtered;
  }

  private boolean doesSessionOverlap(long first, long last, PhoneSession session) {
    long sessionStart = session.getStart();
    return
        (sessionStart >= first && sessionStart <= last) || // start inside window
            (session.getEnd() >= first && session.getEnd() <= last) ||    // end inside window
            (sessionStart < first && session.getEnd() > last);      // session starts before and ends after window
  }

  /**
   * @param sortedHistory
   * @return
   * @see #getTableWithPager
   */
  private Panel getTableWithPagerForHistory(List<PhoneAndStats> sortedHistory) {
    Panel tableWithPager = getTableWithPager(new ListOptions());
    table.getElement().getStyle().setProperty("minWidth", PHONE_CONTAINER_MIN_WIDTH + "px");

    tableWithPager.getElement().setId("PhoneContainerTableScoreHistory");
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

  private void addPhones(List<PhoneAndStats> sortedHistory) {
    clear();
    sortedHistory.forEach(this::addItem);
    flush();
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
    logger.info("checkForClick : stats " + object);
    if (BrowserEvents.CLICK.equals(event.getType())) {
      //   clickOnPhone(object.getPhone());
      clickOnPhone2(object.getPhone());
    } else {
      logger.info("got other event " + event.getType());
    }
  }

  private void clickOnPhone2(String phone) {
    logger.info("clickOnPhone2 : got click on" +
        "\n\tphone " + phone+
        "\n\tfrom  " + from+
        "\n\tto    " + to+
        "\n\treqid " + reqid
    );

//    this.from = from;
//    this.to = to;

    long then = System.currentTimeMillis();

    AnalysisRequest analysisRequest = getAnalysisRequest(from, to);

    analysisServiceAsync.getPhoneBigrams(
        analysisRequest, new AsyncCallback<PhoneBigrams>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("\n\n\n-> getPhoneSummary " + caught);
            controller.getMessageHelper().handleNonFatalError("problem getting phone summary", caught);
          }

          @Override
          public void onSuccess(PhoneBigrams result) {
            long now = System.currentTimeMillis();
            long total = now - then;
            logger.info("getPhoneBigrams userid " + userid + " req " + reqid +
                "\n\tphone  " + phone +
                "\n\ttook   " + total +
                "\n\tserver " + result.getServerTime() +
                "\n\tclient " + (total - result.getServerTime()));

            if (result.getReqid() + 1 != reqid) {
              logger.info("clickOnPhone2 : skip stale req");
            } else {
              bigramContainer.gotNewPhoneBigrams(result, phone, from, to);
            }
          }
        });
    // bigramContainer.gotNewPhoneSummary(phoneReport, phone, from, to);
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
    PhoneContainer.LocalTableResources.TableStyle cellTableStyle();
  }
}
