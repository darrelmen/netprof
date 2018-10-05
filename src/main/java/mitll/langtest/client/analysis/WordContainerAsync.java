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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.*;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.result.TableSortHelper;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.custom.TimeRange;
import mitll.langtest.shared.exercise.CommonShell;
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
  public static final String REVIEW = "Review";

  private final Logger logger = Logger.getLogger("WordContainerAsync");

  private static final int TABLE_HEIGHT = 215;
  private static final String PAUSE = "Pause";

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

  /**
   *
   */
  private final int numWords;

  private final TableSortHelper tableSortHelper = new TableSortHelper();
  private final AnalysisServiceAsync analysisServiceAsync;
  private final AnalysisTab.ReqInfo reqInfo;
  private boolean isAllSameDay = false;
  /**
   *
   */
  private long from, to;
  private int lastPlayed = -1;
  private final INavigation.VIEWS jumpView;
  int itemColumnWidth = -1;

  /**
   * What sort order do we want?
   *
   * @param controller
   * @param plot
   * @param timeRange
   * @param jumpView
   * @see AnalysisTab#getWordContainer
   */
  WordContainerAsync(AnalysisTab.ReqInfo reqInfo,
                     ExerciseController controller,
                     ExerciseLookup<CommonShell> plot,
                     Heading w,
                     int numWords,
                     AnalysisServiceAsync analysisServiceAsync,
                     TimeRange timeRange,
                     INavigation.VIEWS jumpView,
                     int itemColumnWidth) {
    super(controller, plot);
    this.itemColumnWidth = itemColumnWidth;
    this.jumpView = jumpView;
    this.reqInfo = reqInfo;
    this.heading = w;

    this.from = timeRange.getStart();
    this.to = timeRange.getEnd();

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
    tableC.getElement().getStyle().setProperty("minWidth", "502px");  // helps safari in layout
    column.add(tableC);
    column.add(getButtonRow());
  }


  @NotNull
  private DivWidget getButtonRow() {
    DivWidget child = new DivWidget();
    review = new Button(REVIEW) {
      @Override
      protected void onDetach() {
        super.onDetach();
        stopAudio();
      }
    };

    review.setWidth("61px");
    review.addStyleName("topFiveMargin");
    review.addStyleName("leftFiveMargin");
    review.setIcon(IconType.PLAY);
    review.setType(ButtonType.SUCCESS);
    review.addClickHandler(event -> gotClickOnReview());

    DivWidget wrapper = new DivWidget();
    wrapper.addStyleName("floatRight");

    {
      Button learn = new Button(jumpView.toString());
      learn.addStyleName("topFiveMargin");
      learn.setType(ButtonType.SUCCESS);
      learn.addClickHandler(event -> gotClickOnLearn());
      wrapper.add(learn);
    }
    wrapper.add(review);
    child.add(wrapper);
    return child;
  }

  private void gotClickOnLearn() {
    if (getSelected() != null) {
      int exid = getSelected().getExid();
      controller.getShowTab(jumpView).showLearnAndItem(exid);
    }
  }

  private static final boolean DEBUG = false;

  private void gotClickOnReview() {
    isReview = !isReview;
    if (isReview) {
      review.setText(PAUSE);
      review.setIcon(IconType.PAUSE);

      WordScore selected = getSelected();
      if (selected == null) {
        logger.warning("gotClickOnReview no selection?");
      } else {
        if (onLast() && table.getRowCount() > 1) {
          if (DEBUG) {
            logger.info("scrollToVisible first row - selected = " + selected + " table.getRowCount() " + table.getRowCount());
          }

          boolean didScroll = scrollToVisible(0);
          if (!didScroll) {
            WordScore visibleItem = table.getVisibleItem(0);
            setSelected(visibleItem);
            playAudio(visibleItem);
          }
        } else {
          if (DEBUG) logger.info("gotClickOnReview loadAndPlayOrPlayAudio " + selected);
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
        if (DEBUG) logger.info("studentAudioEnded selected " + selected);
        List<WordScore> visibleItems = table.getVisibleItems();

        int i = visibleItems == null ? -1 : visibleItems.indexOf(selected);

        if (i > -1) {
          if (DEBUG) logger.info("studentAudioEnded index " + i + " in " + visibleItems.size());
          if (i == visibleItems.size() - 1) {
            Range visibleRange = table.getVisibleRange();
            int i1 = visibleRange.getStart() + visibleRange.getLength();
            int rowCount = table.getRowCount();
            if (DEBUG) logger.info("studentAudioEnded next page " + i1 + " row " + rowCount);

            boolean b = i1 > rowCount;
            if (b) {
              resetReview();
            } else {
              if (i1 == rowCount) {
                resetReview();
              } else {
                if (DEBUG) logger.info("studentAudioEnded scrollToVisible " + i1 + " row " + rowCount);

                scrollToVisible(i1);
              }
            }
          } else {
            if (DEBUG) logger.info("studentAudioEnded next " + (i + 1));
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


  @Override
  protected void playAudio(WordScore wordScore) {
    lastPlayed = wordScore.getExid();
    super.playAudio(wordScore);
  }

  private boolean onLast() {
    int visibleItemCount = table.getVisibleItemCount();
    if (visibleItemCount == 0) return true;
    else {
      WordScore lastVisible = table.getVisibleItem(visibleItemCount - 1);
      return (lastVisible.getExid() == lastPlayed);
    }
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
   * Unfortunately initially we get two calls here - once when we add the table and again when the plot says the range has changed.
   * Deals with out of order requests, or where the requests outpace the responses
   *
   * @param numResults
   * @param cellTable
   * @return
   * @see #getTableWithPager
   */
  private void createProvider(final int numResults, final CellTable<WordScore> cellTable) {
    AsyncDataProvider<WordScore> dataProvider = new AsyncDataProvider<WordScore>() {
      @Override
      protected void onRangeChanged(HasData<WordScore> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;

/*        logger.info("createProvider asking for " + start +"->" + end + " num " + numResults);
        logger.info("createProvider asking from " + from + "/" +
            new Date(from) +"->" + to +"/"+new Date(to));*/

//        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("create provider " + start + " end " + end));
//        logger.info("logException stack " + exceptionAsString);

        StringBuilder columnSortedState = tableSortHelper.getColumnSortedState(table);

        int val = req++;
        // logger.info("getResults req " + unitToValue + " user " + userID + " text " + text + " val " + val);
        //  logger.info("createProvider sort " + columnSortedState.toString());
        long then = System.currentTimeMillis();

        AnalysisRequest analysisRequest = getAnalysisRequest().setReqid(val);

        if (DEBUG) logger.info("createProvider word scores req " + analysisRequest);
        analysisServiceAsync.getWordScoresForUser(
            analysisRequest,
            start,
            end,
            columnSortedState.toString(),
            new AsyncCallback<WordsAndTotal>() {

              @Override
              public void onFailure(Throwable caught) {
                // Window.alert("Can't contact server.");
                logger.warning("Got  " + caught);
                controller.handleNonFatalError("getting recordings", caught);
              }

              @Override
              public void onSuccess(final WordsAndTotal result) {

                long now = System.currentTimeMillis();
                long total = now - then;
                if (DEBUG) logger.info("createProvider userid " + reqInfo.getUserid() + " req " + req +
                    "\n\ttook   " + total +
                    "\n\tserver " + result.getServerTime() +
                    "\n\tnum    " + result.getNumTotal() +
                    "\n\tws num " + result.getResults().size() +
                    "\n\tclient " + (total - result.getServerTime()));


                if (result.getReq() < req - 1) {
                  logger.warning("ignore request " + result.getReq() + " vs " + req);
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
                  Scheduler.get().scheduleDeferred(cellTable::redraw);
                }
              }
            });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);
  }

  private AnalysisRequest getAnalysisRequest() {
    return new AnalysisRequest()
        .setUserid(reqInfo.getUserid())
        .setListid(reqInfo.getListid())
        .setMinRecordings(reqInfo.getMinRecordings())
        .setFrom(from)
        .setTo(to)
        .setDialogID(reqInfo.getDialogID())
        .setDialogSessionID(reqInfo.getDialogSessionID())
        ;
  }

  private void selectFirst(WordsAndTotal result) {
    List<WordScore> results = result.getResults();

    if (!results.isEmpty()) {

      int next = -1;

      for (int i = 0; i < results.size(); i++) {
        WordScore wordScore = results.get(i);
        if (wordScore.getExid() == lastPlayed) {
          if (DEBUG) {
            logger.info("selectFirst found last " + wordScore.getExid() + " word " + wordScore.getTranscript());
          }

          next = i;
        }
      }

      next = Math.min(results.size() - 1, next + 1);
      WordScore toSelect = results.get(next);
      //logger.info("Select " + next + " " + toSelect.getExid());
      setSelected(toSelect);
      if (isReview) {
        Scheduler.get().scheduleDeferred(() -> playAudio(toSelect));
      }
    }
  }

  /**
   * Only sort with descending score.
   *
   * @return
   * @see AnalysisTab#getWordContainer
   */
  public Panel getTableWithPager() {
    // logger.info("getTableWithPager " +listOptions);
    CellTable<WordScore> wordScoreCellTable = makeCellTable(new ListOptions().isSort());

    wordScoreCellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(tableSortHelper.getColumn(SCORE), true));

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

    //   new TooltipHelper().addTooltip(table, PhoneExampleContainer.CLICK_ON);
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
    int itemColWidth = getItemColWidth();
    table.setColumnWidth(itemCol, itemColWidth + "px");

    String language = controller.getLanguage();

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

    tableSortHelper.rememberColumn(itemCol, WORD);
  }

  private int getItemColWidth() {
    return itemColumnWidth == -1 ?
        (isNarrow() ? ITEM_COL_WIDTH_NARROW : ITEM_COL_WIDTH) : itemColumnWidth;
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
    if (from != this.from || to != this.to) {
//      logger.info("timeChanged     " + this.from + " - " + this.to);
//      logger.info("timeChanged new " + from + " - " + to);
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
    } else {
      logger.info("ignoring redraw for same time period as current.");
    }
  }

  private void redraw() {
    RangeChangeEvent.fire(table, table.getVisibleRange());
  }
}
