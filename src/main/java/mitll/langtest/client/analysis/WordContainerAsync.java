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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.*;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.result.TableSortHelper;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.client.result.TableSortHelper.TIMESTAMP;
import static mitll.langtest.shared.analysis.WordScore.WORD;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class WordContainerAsync extends AudioExampleContainer<WordScore> implements AnalysisPlot.TimeChangeListener {
  private static final int TABLE_HEIGHT = 215;
  private final Logger logger = Logger.getLogger("WordContainerAsync");

  private static final int NARROW_THRESHOLD = 1450;

  private static final int ROWS_TO_SHOW = 6;

  private static final int ITEM_COL_WIDTH = 210;//250;
  private static final int ITEM_COL_WIDTH_NARROW = 190;

  private static final String SCORE = "Score";
  private static final int SCORE_WIDTH = 68;
  private static final int DATE_WIDTH = 150;
  private static final int WIDE_DATE_WIDTH = 160;

  /**
   * @see #addColumnsToTable
   */
  private static final String DATE = "Date";

  private final Heading heading;

  private final String todayYear;
  private final String todaysDate;
  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");
  private final DateTimeFormat yearShortFormat = DateTimeFormat.getFormat("MMM d yy");
  private final DateTimeFormat longerFormat = DateTimeFormat.getFormat("MMM d h:mm a");
  private final DateTimeFormat longerYearFormat = DateTimeFormat.getFormat("MMM d yy h:mm a");

  private final WordTable wordTable = new WordTable();

  private final int numWords;

  private final TableSortHelper tableSortHelper = new TableSortHelper();
  private final AnalysisServiceAsync analysisServiceAsync;
  private final AnalysisTab.ReqInfo reqInfo;
  private boolean isAllSameDay = false;
  private long from = 0, to = Long.MAX_VALUE;

  /**
   * What sort order do we want?
   *
   * @param controller
   * @param plot
   * @see AnalysisTab#getWordContainer
   */
  WordContainerAsync(AnalysisTab.ReqInfo reqInfo,
                     ExerciseController controller,
                     AnalysisPlot plot,
                     Heading w,
                     int numWords,
                     AnalysisServiceAsync analysisServiceAsync) {
    super(controller, plot);
    this.reqInfo = reqInfo;
    plot.addListener(this);
    this.heading = w;

    this.numWords = numWords;
    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - 2);

    this.analysisServiceAsync = analysisServiceAsync;
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
  }

  private boolean isReview = false;
  private Button review;

  @Override
  protected void addTable(Panel column) {
    DivWidget tableC = new DivWidget();
    tableC.add(table);
    tableC.setHeight(TABLE_HEIGHT + "px");

    column.add(tableC);
    column.add(getButtonRow());
  }


  @NotNull
  private DivWidget getButtonRow() {
    DivWidget child = new DivWidget();
    review = new Button("Review") {
      @Override
      protected void onDetach() {
        super.onDetach();
       // logger.info("got detach ");
        stopAudio();
      }
    };

    review.setWidth("61px");
    review.addStyleName("topFiveMargin");
    review.addStyleName("leftFiveMargin");
    review.setIcon(IconType.PLAY);
    review.setType(ButtonType.SUCCESS);
    review.addClickHandler(event -> gotClickOnReview());


    Button learn = new Button("Learn");
    learn.addStyleName("topFiveMargin");

    learn.setType(ButtonType.SUCCESS);

    learn.addClickHandler(event -> {
      int exid = getSelected().getExid();
      controller.getShowTab().showLearnAndItem(exid);
    });

    DivWidget wrapper = new DivWidget();
    wrapper.addStyleName("floatRight");
    wrapper.add(learn);
    wrapper.add(review);
    child.add(wrapper);
    return child;
  }

  private void gotClickOnReview() {
    isReview = !isReview;
    if (isReview) {
      review.setText("Pause");
      review.setIcon(IconType.PAUSE);

      WordScore selected = getSelected();
      if (selected == null) {
        logger.warning("gotClickOnReview no selection?");
      } else {
        if (onLast() && table.getRowCount() > 1) {
          //logger.info("scrollToVisible first row - selected = " + selected + " table.getRowCount() " + table.getRowCount());
          scrollToVisible(0);
        } else {
//          logger.info("gotClickOnReview playAudio " + selected);
          playAudio(selected);
        }
      }
    } else {
      stopAudio();
      resetReview();
    }
  }

  protected void studentAudioEnded() {
    //  logger.info("studentAudioEnded ");
    if (isReview) {
      WordScore selected = getSelected();
      if (selected == null) {
        logger.warning("studentAudioEnded no selection?");
      } else {
        //        logger.info("studentAudioEnded selected " + selected);
        List<WordScore> visibleItems = table.getVisibleItems();
        int i = visibleItems == null ? -1 : visibleItems.indexOf(selected);

        if (i > -1) {
          //        logger.info("studentAudioEnded index " + i + " in " + visibleItems.size());
          if (i == visibleItems.size() - 1) {
            Range visibleRange = table.getVisibleRange();
            int i1 = visibleRange.getStart() + visibleRange.getLength();
            int rowCount = table.getRowCount();
            //        logger.info("studentAudioEnded next page " + i1 + " row " + rowCount);

            boolean b = i1 > rowCount;
            if (b) {
              resetReview();
            } else {
              scrollToVisible(i1);
            }
          } else {
            //      logger.info("studentAudioEnded next " + (i + 1));
            WordScore wordScore = visibleItems.get(i + 1);
            setSelected(wordScore);
            playAudio(getSelected());
          }
        }
      }
    } else {
      resetReview();
    }
  }


  private boolean onLast() {
    Range visibleRange = table.getVisibleRange();
    int i1 = visibleRange.getStart() + visibleRange.getLength();
    int rowCount = table.getRowCount();
//    logger.info("next page " + i1 + " row " + rowCount);
    return rowCount == i1;
  }

  private void resetReview() {
    review.setText("Review");
    review.setIcon(IconType.PLAY);
    isReview = false;
  }

  protected int getPageSize() {
    return ROWS_TO_SHOW;
  }

  private int req = 0;

  /**
   * Deals with out of order requests, or where the requests outpace the responses
   *
   * @param numResults
   * @param cellTable
   * @return
   * @see #getTableWithPager
   */
  private void createProvider(
      final int numResults,
      final CellTable<WordScore> cellTable) {
    AsyncDataProvider<WordScore> dataProvider = new AsyncDataProvider<WordScore>() {
      @Override
      protected void onRangeChanged(HasData<WordScore> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;

/*        logger.info("createProvider asking for " + start +"->" + end + " num " + numResults);
        logger.info("createProvider asking from " + from + "/"+
            new Date(from) +"->" + to +"/"+new Date(to));*/

        StringBuilder columnSortedState = tableSortHelper.getColumnSortedState(table);

        int val = req++;
        // logger.info("getResults req " + unitToValue + " user " + userID + " text " + text + " val " + val);
        //  logger.info("createProvider sort " + columnSortedState.toString());

        analysisServiceAsync.getWordScoresForUser(
            reqInfo.getUserid(),
            reqInfo.getMinRecordings(),
            reqInfo.getListid(),
            from,
            to,
            start,
            end,
            columnSortedState.toString(),
            val,
            new AsyncCallback<WordsAndTotal>() {

              @Override
              public void onFailure(Throwable caught) {
                // Window.alert("Can't contact server.");
                logger.warning("Got  " + caught);
                controller.handleNonFatalError("getting recordings", caught);
              }

              @Override
              public void onSuccess(final WordsAndTotal result) {
                if (result.getReq() < req - 1) {
/*
              logger.info("->>getResults ignoring response " + result.req + " vs " + req +
                  " --->req " + unitToValue + " user " + userID + " text '" + text + "' : got back " + result.results.size() + " of total " + result.numTotal);
*/
                } else {
                  final int numTotal = result.getNumTotal();  // not the results size - we asked for a page range
                  cellTable.setRowCount(numTotal, true);
                  updateRowData(start, result.getResults());
                  isAllSameDay = result.isAllSameDay();

                  if (isAllSameDay) {
                    table.setColumnWidth(theDateCol, WIDE_DATE_WIDTH + "px");
                  }
                  if (!result.getResults().isEmpty()) {
                    selectFirst(result);
                  }
                }
              }
            });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);
  }

  private void selectFirst(WordsAndTotal result) {
    WordScore toSelect = result.getResults().get(0);
    setSelected(toSelect);
    if (isReview) {
      Scheduler.get().scheduleDeferred(() -> playAudio(toSelect));
    }
  }

  /**
   * @return
   * @paramx sortedHistory
   * @see AnalysisTab#getWordContainer
   */
  public Panel getTableWithPager() {
    // logger.info("getTableWithPager " +listOptions);

    CellTable<WordScore> wordScoreCellTable = makeCellTable(new ListOptions().isSort());

    if (isPolyglot()) {
      wordScoreCellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(tableSortHelper.getColumn(SCORE), true));
    } else {
      wordScoreCellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(tableSortHelper.getColumn(TIMESTAMP), false));
    }

    createProvider(numWords, wordScoreCellTable);

    // Create a SimplePager.
    Panel tableWithPager = getTable(new ListOptions());

    tableWithPager.getElement().setId("WordContainerScoreHistory");
    tableWithPager.addStyleName("floatLeftAndClear");

    wordScoreCellTable.addColumnSortHandler(new ColumnSortEvent.AsyncHandler(wordScoreCellTable));

    addPlayer();

    return tableWithPager;
  }

  private Column<WordScore, SafeHtml> theDateCol;

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    addReview();

    {
      Column<WordScore, SafeHtml> dateCol = getDateColumn();
      this.theDateCol = dateCol;
      dateCol.setSortable(true);
      addColumn(dateCol, new TextHeader(DATE));
      table.setColumnWidth(dateCol, DATE_WIDTH + "px");

      if (!isPolyglot()) {
        table.getColumnSortList().push(dateCol);
      }
      tableSortHelper.rememberColumn(dateCol, TIMESTAMP);
    }

    {
      Column<WordScore, SafeHtml> scoreColumn = getScoreColumn();
      table.addColumn(scoreColumn, SCORE);

      scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      scoreColumn.setSortable(true);
      if (isPolyglot()) {
        table.getColumnSortList().push(scoreColumn);
      }
      table.setColumnWidth(scoreColumn, SCORE_WIDTH + "px");
      //    table.addColumnSortHandler(getScoreSorter(scoreColumn, getList()));
      tableSortHelper.rememberColumn(scoreColumn, SCORE);
    }

    addAudioColumns();

    table.setWidth("100%", true);

    new TooltipHelper().addTooltip(table, PhoneExampleContainer.CLICK_ON);
  }

  private boolean isPolyglot() {
    return controller.getProjectStartupInfo().getProjectType() == ProjectType.POLYGLOT;
  }

  private Column<WordScore, SafeHtml> getDateColumn() {
    return new Column<WordScore, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, WordScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        gotClick(object, event);
      }

      @Override
      public SafeHtml getValue(WordScore shell) {
        if (shell == null) return getSafeHtml("");
        return getNoWrapContent(getVariableInfoDateStamp(shell));
      }
    };
  }

  private SafeHtml getNoWrapContent(String noWrapContent) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<div style='white-space: nowrap;'><span>" +
        noWrapContent +
        "</span>");

    sb.appendHtmlConstant("</div>");
    return sb.toSafeHtml();
  }

  private String getVariableInfoDateStamp(WordScore shell) {
    Date date = new Date(shell.getTimestamp());
    String timeFormatted = format.format(date);

    if (isToday(timeFormatted)) {
      timeFormatted = todayTimeFormat.format(date);
    } else if (isSameYear(timeFormatted)) {    // drop year if this year
      if (isAllSameDay) {
        timeFormatted = longerFormat.format(date);
      } else {
        timeFormatted = yearRemoved(timeFormatted);
      }
    } else {
      if (isAllSameDay) {
        timeFormatted = longerYearFormat.format(date);
      }
    }
    return timeFormatted;
  }

  @NotNull
  private String yearRemoved(String timeFormatted) {
    return timeFormatted.substring(0, timeFormatted.length() - 4);
  }

  private boolean isSameYear(String timeFormatted) {
    return todayYear.equals(timeFormatted.substring(timeFormatted.length() - 2));
  }

  private boolean isToday(String timeFormatted) {
    return timeFormatted.equals(todaysDate);
  }

  private void addReview() {
    Column<WordScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    int itemColWidth = isNarrow() ? ITEM_COL_WIDTH_NARROW : ITEM_COL_WIDTH;
    table.setColumnWidth(itemCol, itemColWidth + "px");

    String language = controller.getLanguage();

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

    tableSortHelper.rememberColumn(itemCol, WORD);
  }

  private boolean isNarrow() {
    return Window.getClientWidth() < NARROW_THRESHOLD;
  }

  /**
   * @return
   * @see #addReview
   */
  private Column<WordScore, SafeHtml> getItemColumn() {
    return new Column<WordScore, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, WordScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        gotClick(object, event);
      }

      @Override
      public SafeHtml getValue(WordScore shell) {
        if (shell == null) {
          logger.warning("huh? shell is null for item column?");
          return getSafeHtml("");
        } else {
          Map<NetPronImageType, List<SlimSegment>> transcript = shell.getTranscript();

          String columnText;
          if (transcript == null) {
//            logger.warning("getItemColumn no transcript for " + shell);
            columnText = "";
          } else if (transcript.get(NetPronImageType.WORD_TRANSCRIPT) == null) {
            //  logger.warning("getItemColumn no word transcript for " + shell);
            columnText = "";
          } else {
            columnText = wordTable.makeColoredTableReally(transcript);
          }

          return getSafeHtml(columnText);
        }
      }
    };
  }

  private Column<WordScore, SafeHtml> getScoreColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        if (shell == null) return getSafeHtml("");
        float v = shell.getPronScore() * 100;
        String s = "<span " + "style='" + "margin-left:10px;" + "'" + ">" + ((int) v) + "</span>";
        return getSafeHtml(s);
      }

      @Override
      public boolean isDefaultSortAscending() {
        return false;
      }
    };
  }

  /**
   * @param from
   * @param to
   * @see AnalysisPlot#timeChanged
   */
  @Override
  public void timeChanged(long from, long to) {
    if (from == 0) {
      heading.setSubtext("");
    } else {
/*       logger.info("timeChanged Starting from " +from + " : " +to);
      logger.info("timeChanged : from " + format.format(new Date(from)) + " to " + format.format(new Date(to)));*/
      heading.setSubtext(yearShortFormat.format(new Date(from)) + " - " + yearShortFormat.format(new Date(to)));
      this.from = from;
      this.to = to;
    }

    redraw();
  }

  private void redraw() {
    RangeChangeEvent.fire(table, table.getVisibleRange());
  }
}
