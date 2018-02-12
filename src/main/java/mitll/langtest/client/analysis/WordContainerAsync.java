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

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.RangeChangeEvent;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
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
  private final Logger logger = Logger.getLogger("WordContainer");

  private static final int NARROW_THRESHOLD = 1450;

  private static final int ROWS_TO_SHOW = 8;

  private static final int ITEM_COL_WIDTH = 210;//250;
  private static final int ITEM_COL_WIDTH_NARROW = 190;

  private static final String SCORE = "Score";
  private static final int SCORE_WIDTH = 68;
  private static final int SIGNED_UP = 95;
  private static final String DATE = "Date";

  private final ShowTab learnTab;
  private final Heading heading;

  private final String todayYear;
  private final String todaysDate;
  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");
  private final DateTimeFormat yearShortFormat = DateTimeFormat.getFormat("MMM d yy");
  // private final DateTimeFormat yearShortFormat2 = DateTimeFormat.getFormat("MMM d yy h:mm");

  private WordTable wordTable = new WordTable();

  private int numWords;

  TableSortHelper tableSortHelper = new TableSortHelper();
  AnalysisServiceAsync analysisServiceAsync;
  AnalysisTab.ReqInfo reqInfo;

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
                     ShowTab learnTab,
                     Heading w,
                     int numWords,
                     AnalysisServiceAsync analysisServiceAsync) {
    super(controller, plot);
    this.reqInfo = reqInfo;
    plot.addListener(this);
    this.learnTab = learnTab;
    this.heading = w;

    this.numWords = numWords;
    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - 2);

    this.analysisServiceAsync = analysisServiceAsync;
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
        //logger.info("createProvider asking for " + start +"->" + end);

        StringBuilder builder = tableSortHelper.getColumnSortedState(table);

        int val = req++;
        // logger.info("getResults req " + unitToValue + " user " + userID + " text " + text + " val " + val);
      //  logger.info("createProvider sort " + builder.toString());

        analysisServiceAsync.getWordScoresForUser(
            reqInfo.getUserid(),
            reqInfo.getMinRecordings(),
            reqInfo.getListid(),
            from,
            to,
            start,
            end,
            builder.toString(),
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
/*                  if (numTotal > 0) {
                    WordScore object = result.getResults().get(0);
*//*
                    logger.info("--->getResults req " + result.req +
                            " " + unitToValue + " user " + userID + " text '" + text + "' : " +
                            "got back " + result.results.size() + " of total " + result.numTotal + " selecting "+ object);
*//*
                    cellTable.getSelectionModel().setSelected(object, true);
                  }*/
                }
              }
            });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);
  }

  /**
   * @return
   * @paramx sortedHistory
   * @see AnalysisTab#getWordContainer
   */
  public Panel getTableWithPager(
  ) {
    //  Panel tableWithPager = getTableWithPager(new ListOptions());

    // logger.info("getTableWithPager " +listOptions);
    CellTable<WordScore> wordScoreCellTable = makeCellTable(new ListOptions().isSort());


    createProvider(numWords, wordScoreCellTable);

    // Connect the table to the data provider.
    // dataProvider.addDataDisplay(table);
    // logger.info("still setting up... starting");

    // Create a SimplePager.
    Panel tableWithPager = getTable(new ListOptions());


    tableWithPager.getElement().setId("WordContainerScoreHistory");
    tableWithPager.addStyleName("floatLeftAndClear");

    wordScoreCellTable.addColumnSortHandler(new ColumnSortEvent.AsyncHandler(wordScoreCellTable));


    if (!isPolyglot()) {
      wordScoreCellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(tableSortHelper.getColumn(TIMESTAMP), false));
    } else {
      wordScoreCellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(tableSortHelper.getColumn(SCORE), true));
    }

    //  wordScoreCellTable.setWidth("100%", false);

    addPlayer();

    return tableWithPager;
  }

//  protected void addSelectionModel() {
//    table.setSelectionModel(selectionModel);
//  }

/*  Map<Integer, Map<Long, WordScore>> getExToTimeToAnswer(List<WordScore> sortedHistory) {
    Map<Integer, Map<Long, WordScore>> exToTimeToAnswer = new HashMap<>();
    for (WordScore wordScore : sortedHistory) {
      Map<Long, WordScore> timeToAnswer = exToTimeToAnswer.computeIfAbsent(wordScore.getExid(), k -> new HashMap<>());
      timeToAnswer.computeIfAbsent(wordScore.getTimestamp(), k -> wordScore);
    }
    return exToTimeToAnswer;
  }*/

/*  @NotNull
  private Comparator<WordScore> getWordScoreTimeComparator() {
    return Comparator.comparingLong(WordScore::getTimestamp).thenComparingInt(WordScore::getExid);
  }

  @NotNull
  private Comparator<WordScore> getWordScoreTimeComparatorDesc() {
    return (o1, o2) -> {
      int i = -1 * Long.compare(o1.getTimestamp(), o2.getTimestamp());
      return i == 0 ? Integer.compare(o1.getExid(), o2.getExid()) : i;
    };
  }*/

/*
  private void addItems(Collection<WordScore> sortedHistory) {
    clear();
    sortedHistory.forEach(this::addItem);
    flush();
  }
*/

/*  private ColumnSortEvent.ListHandler<WordScore> getEnglishSorter(Column<WordScore, SafeHtml> englishCol,
                                                                  List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              CommonShell shell1 = getShell(o1.getExid());
              CommonShell shell2 = getShell(o2.getExid());
              return sorter.compareStrings(shell1.getForeignLanguage(), shell2.getForeignLanguage());
            }
          }
          return -1;
        });
    return columnSortHandler;
  }*/

 /* private ColumnSortEvent.ListHandler<WordScore> getScoreSorter(Column<WordScore, SafeHtml> scoreCol,
                                                                List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
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
              int i = Float.compare(o1.getPronScore(), o2.getPronScore());
              // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
              if (i == 0) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
              } else {
                return i;
              }
            }
          } else {
            logger.warning("------- o1 is null?");

            return -1;
          }
        });
    return columnSortHandler;
  }*/

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    addReview();

    {
      Column<WordScore, SafeHtml> dateCol = getDateColumn();
      dateCol.setSortable(true);
      addColumn(dateCol, new TextHeader(DATE));
      table.setColumnWidth(dateCol, SIGNED_UP + "px");
//      table.addColumnSortHandler(getDateSorter(dateCol, getList()));

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
/*

      @Override
      public boolean isDefaultSortAscending() {
        return true;
      }
*/

      @Override
      public SafeHtml getValue(WordScore shell) {
        if (shell == null) return getSafeHtml("");
        return getSafeHtml(getVariableInfoDateStamp(shell));
      }
    };
  }

  private String getVariableInfoDateStamp(WordScore shell) {
    Date date = new Date(shell.getTimestamp());
    String signedUp = format.format(date);

    // drop year if this year
    if (signedUp.equals(todaysDate)) {
      signedUp = todayTimeFormat.format(date);
    } else if (todayYear.equals(signedUp.substring(signedUp.length() - 2))) {
      signedUp = signedUp.substring(0, signedUp.length() - 4);
    }
    return signedUp;
  }
/*

  private ColumnSortEvent.ListHandler<WordScore> getDateSorter(Column<WordScore, SafeHtml> englishCol,
                                                               List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getDateCompare);
    return columnSortHandler;
  }
*/

/*
  private int getDateCompare(WordScore o1, WordScore o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
      }
    }
    return -1;
  }
  */

  private void addReview() {
    Column<WordScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    int itemColWidth = isNarrow() ? ITEM_COL_WIDTH_NARROW : ITEM_COL_WIDTH;
    table.setColumnWidth(itemCol, itemColWidth + "px");

    String language = controller.getLanguage();

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

    tableSortHelper.rememberColumn(itemCol, WORD);

//    table.addColumnSortHandler(getEnglishSorter(itemCol, getList()));
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
            logger.warning("getItemColumn no transcript for " + shell);
            columnText = "";
          } else if (transcript.get(NetPronImageType.WORD_TRANSCRIPT) == null) {
            logger.warning("getItemColumn no word transcript for " + shell);
            columnText = "";
          } else {
            columnText = wordTable.makeColoredTableReally(transcript);
          }

     /*     if (columnText.isEmpty()) {
          CommonShell exercise = getShell(shell.getExid());
           logger.warning("getItemColumn : column text empty for id " +
               shell.getExid() + " and found ex " + exercise);
          String foreignLanguage = exercise == null ? "" : exercise.getForeignLanguage();
          if (spanish) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getPronScore());
        } else {
          //logger.info("getItemColumn : Got item id " + shell.getExID() + " "+ columnText );
        }*/
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

  private void redraw() {
    RangeChangeEvent.fire(table, table.getVisibleRange());
  }

  private long from = 0, to = Long.MAX_VALUE;

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
//       logger.info("Starting from " +from + " : " +to);
//      logger.info("timeChanged : from " + yearShortFormat2.format(new Date(from)) + " to " + yearShortFormat2.format(new Date(to)));
      heading.setSubtext(yearShortFormat.format(new Date(from)) + " - " + yearShortFormat.format(new Date(to)));
      this.from = from;
      this.to = to;
    }

    redraw();
  }
}
