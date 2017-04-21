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
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class WordContainer extends AudioExampleContainer<WordScore> implements AnalysisPlot.TimeChangeListener {
  public static final int NARROW_THRESHOLD = 1450;
  private final Logger logger = Logger.getLogger("WordContainer");

  private static final int ROWS_TO_SHOW = 8;

  private static final int ITEM_COL_WIDTH = 250;
  private static final int ITEM_COL_WIDTH_NARROW = 190;
  private static final String SCORE = "Score";
  private static final int SCORE_WIDTH = 68;
  private static final int PLAY_WIDTH = 42;
//  private static final int NATIVE_WIDTH = PLAY_WIDTH;
//  private static final String NATIVE = "Ref";
//  private static final String PLAY = "Play";

  private static final int TABLE_HISTORY_WIDTH = 430; //380
  private static final int TABLE_HISTORY_WIDTH_NARROW = 360; //380
  private final ExerciseComparator sorter;
  private final ShowTab learnTab;
  private final Heading heading;
  /**
   * Hack for spanish to make it upper case. Why?
   */
  private final boolean spanish;
  private List<WordScore> sortedHistory;
  private SortedSet<WordScore> byTime;

  /**
   * What sort order do we want?
   * @param controller
   * @param plot
   * @see AnalysisTab#getWordScores
   */
  WordContainer(ExerciseController controller, AnalysisPlot plot, ShowTab learnTab, Heading w) {
    super(controller, plot);
    spanish = controller.getLanguage().equalsIgnoreCase("Spanish");
    sorter = new ExerciseComparator();
    plot.addListener(this);
    this.learnTab = learnTab;
    this.heading = w;
  }

//  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");
  private final DateTimeFormat yearShortFormat = DateTimeFormat.getFormat("MMM d yy");
  //private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");

  protected int getPageSize() {
    return ROWS_TO_SHOW;
  }

  /**
   * @param sortedHistory
   * @return
   * @see AnalysisTab#getWordScores
   */
  public Panel getTableWithPager(List<WordScore> sortedHistory) {
    Panel tableWithPager = getTableWithPager(new ListOptions());
    tableWithPager.getElement().setId("WordContainerScoreHistory");
    int tableHistoryWidth = isNarrow() ? TABLE_HISTORY_WIDTH_NARROW : TABLE_HISTORY_WIDTH;

    tableWithPager.setWidth(tableHistoryWidth + "px");
    tableWithPager.addStyleName("floatLeftAndClear");

    this.sortedHistory = sortedHistory;

    byTime = new TreeSet<WordScore>(new Comparator<WordScore>() {
      @Override
      public int compare(WordScore o1, WordScore o2) {
        int i = new Long(o1.getTimestamp()).compareTo(o2.getTimestamp());
        return i == 0 ? Integer.valueOf(o1.getExid()).compareTo(o2.getExid()) : i;
      }
    });

    byTime.addAll(sortedHistory);

//    logger.info("getTableWithPager got " + sortedHistory.size() + " items");
    addItems(sortedHistory);

    addPlayer();

    return tableWithPager;
  }

  private void addItems(Collection<WordScore> sortedHistory) {
    clear();
    for (WordScore wordScore : sortedHistory) {
      addItem(wordScore);
    }
    flush();
  }

  private void addReview() {
    Column<WordScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    int itemColWidth = isNarrow() ? ITEM_COL_WIDTH_NARROW : ITEM_COL_WIDTH;
    table.setColumnWidth(itemCol, itemColWidth + "px");

    String language = controller.getLanguage();

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = getEnglishSorter(itemCol, getList());
    table.addColumnSortHandler(columnSortHandler);
  }

  private boolean isNarrow() {
    return Window.getClientWidth() < NARROW_THRESHOLD;
  }

  private ColumnSortEvent.ListHandler<WordScore> getEnglishSorter(Column<WordScore, SafeHtml> englishCol,
                                                                  List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = new ColumnSortEvent.ListHandler<WordScore>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<WordScore>() {
          public int compare(WordScore o1, WordScore o2) {
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
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<WordScore> getScoreSorter(Column<WordScore, SafeHtml> scoreCol,
                                                                List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = new ColumnSortEvent.ListHandler<WordScore>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<WordScore>() {
          public int compare(WordScore o1, WordScore o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                float a1 = o1.getPronScore();
                float a2 = o2.getPronScore();
                int i = Float.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
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

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    addReview();

    Column<WordScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, SCORE);

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, SCORE_WIDTH + "px");

    addAudioColumns();

    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<WordScore> columnSortHandler2 = getScoreSorter(scoreColumn, getList());
    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().addTooltip(table, "Click on an item to review.");
  }

  private Column<WordScore, SafeHtml> getItemColumn() {
    return new Column<WordScore, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, WordScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(WordScore shell) {
        String columnText = new WordTable().makeColoredTable(shell.getTranscript());
        if (columnText.isEmpty()) {
          CommonShell exercise = getShell(shell.getExid());
          // logger.info("getItemColumn : column text empty for id " + shell.getExID() + " and found ex " + exercise);

          String foreignLanguage = exercise == null ? "" : exercise.getForeignLanguage();
          if (spanish) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getPronScore());
        } else {
          //logger.info("getItemColumn : Got item id " + shell.getExID() + " "+ columnText );
        }
        return getSafeHtml(columnText);
      }
    };
  }

  private void gotClickOnItem(final WordScore e) {
    learnTab.showLearnAndItem(e.getExid());
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Column<WordScore, SafeHtml> getScoreColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        float v = shell.getPronScore() * 100;
        String s = "<span " + "style='" + "margin-left:10px;" + "'" + ">" + ((int) v) + "</span>";
        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
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
      addItems(sortedHistory);
    } else {
      // logger.info("Starting from " +from + " : " +to);
      // logger.info("Starting from " + noYearFormat.format(new Date(from)) + " to " + noYearFormat.format(new Date(to)));

     // Calendar.getInstance().get(Calendar.YEAR);
      heading.setSubtext(yearShortFormat.format(new Date(from)) + " - " +yearShortFormat.format(new Date(to)));

      WordScore fromElement = new WordScore();
      fromElement.setTimestamp(from);
      WordScore toElement = new WordScore();
      toElement.setTimestamp(to);

      SortedSet<WordScore> wordScores = byTime.subSet(fromElement, toElement);
      List<WordScore> filtered = new ArrayList<>(wordScores);

      Collections.sort(filtered); // put sort back to by score first
      addItems(filtered);
    }
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
