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
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.sound.PlayAudioWidget;
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
class WordContainer extends SimplePagingContainer<WordScore> implements AnalysisPlot.TimeChangeListener {
  public static final int NARROW_THRESHOLD = 1450;
  private final Logger logger = Logger.getLogger("WordContainer");

  private static final int ROWS_TO_SHOW = 8;

  private static final int ITEM_COL_WIDTH = 250;
  private static final int ITEM_COL_WIDTH_NARROW = 190;
  private static final String SCORE = "Score";
  private static final int SCORE_WIDTH = 68;
  static final int PLAY_WIDTH = 42;
  private static final int NATIVE_WIDTH = PLAY_WIDTH;
  private static final String NATIVE = "Ref";
  private static final String PLAY = "Play";

  private static final int TABLE_HISTORY_WIDTH = 430; //380
  private static final int TABLE_HISTORY_WIDTH_NARROW = 360; //380
  private final ExerciseComparator sorter;
  private final AnalysisPlot plot;
  private final ShowTab learnTab;
  private final Heading heading;
  private final boolean spanish;
  private List<WordScore> sortedHistory;
  private SortedSet<WordScore> byTime;

  /**
   * @param controller
   * @param plot
   * @see AnalysisTab#getWordScores
   */
  WordContainer(ExerciseController controller, AnalysisPlot plot, ShowTab learnTab, Heading w) {
    super(controller);
    spanish = controller.getLanguage().equalsIgnoreCase("Spanish");
    sorter = new ExerciseComparator();//controller.getProjectStartupInfo().getTypeOrder());
    this.plot = plot;
    plot.addListener(this);
    this.learnTab = learnTab;
    this.heading = w;
  }

  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");
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
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    int tableHistoryWidth = isNarrow() ? TABLE_HISTORY_WIDTH_NARROW : TABLE_HISTORY_WIDTH;

    tableWithPager.setWidth(tableHistoryWidth + "px");
    tableWithPager.addStyleName("floatLeft");

    this.sortedHistory = sortedHistory;

    byTime = new TreeSet<WordScore>(new Comparator<WordScore>() {
      @Override
      public int compare(WordScore o1, WordScore o2) {
        int i = new Long(o1.getTimestamp()).compareTo(o2.getTimestamp());
        return i == 0 ? Integer.valueOf(o1.getId()).compareTo(o2.getId()) : i;
      }
    });

    byTime.addAll(sortedHistory);

//    logger.info("getTableWithPager got " + sortedHistory.size() + " items");
    addItems(sortedHistory);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        PlayAudioWidget.addPlayer();
      }
    });

    return tableWithPager;
  }

  private void addItems(Collection<WordScore> sortedHistory) {
    clear();
    for (WordScore WordScore : sortedHistory) {
      addItem(WordScore);
    }
    flush();
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
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
                CommonShell shell1 = getShell(o1.getId());
                CommonShell shell2 = getShell(o2.getId());
                return sorter.compareStrings(shell1.getForeignLanguage(), shell2.getForeignLanguage());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private CommonShell getShell(int id) {
    return plot.getIdToEx().get(id);
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
  protected void addColumnsToTable() {
    addReview();

    Column<WordScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, SCORE);

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, SCORE_WIDTH + "px");

    Column<WordScore, SafeHtml> column = getPlayAudio();

    SafeHtmlHeader header = new SafeHtmlHeader(new SafeHtml() {
      @Override
      public String asString() {
        return "<span style=\"text-align:left;\">" + PLAY +
            "</span>";
      }
    });

    table.addColumn(column, header);
    table.setColumnWidth(column, PLAY_WIDTH + "px");
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    column = getPlayNativeAudio();
    table.addColumn(column, NATIVE);
    table.setColumnWidth(column, NATIVE_WIDTH + "px");
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

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
        String columnText = new WordTable().toHTML2(shell.getNetPronImageTypeListMap());
        if (columnText.isEmpty()) {
          CommonShell exercise = getShell(shell.getId());
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
    learnTab.showLearnAndItem(e.getId());
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
   * @return
   * @see SimplePagingContainer#addColumnsToTable()
   */
  private Column<WordScore, SafeHtml> getPlayAudio() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        CommonShell exercise = getShell(shell.getId());
       // logger.info("getPlayAudio : Got " + shell.getId() + "  : " + shell.getFileRef());
        String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        return PlayAudioWidget.getAudioTagHTML(shell.getFileRef(), title);
      }
    };
  }

  /**
   * @see #addColumnsToTable
   * @return
   */
  private Column<WordScore, SafeHtml> getPlayNativeAudio() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        CommonShell exercise = getShell(shell.getId());
     //   logger.info("getPlayNativeAudio : Got " +  shell.getId() + "  : " + shell.getNativeAudio());
        String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        if (shell.getNativeAudio() != null) {
          return PlayAudioWidget.getAudioTagHTML(shell.getNativeAudio(), title);
        } else {
          //if  (exercise != null) logger.info("no native audio for " + exercise.getOldID());
          return new SafeHtmlBuilder().toSafeHtml();
        }
      }
    };
  }

  /**
   * @param from
   * @param to
   * @see AnalysisPlot#timeChanged(long, long)
   */
  @Override
  public void timeChanged(long from, long to) {
    if (from == 0) {
      heading.setSubtext("");
      addItems(sortedHistory);
    } else {
      // logger.info("Starting from " +from + " : " +to);
      // logger.info("Starting from " + noYearFormat.format(new Date(from)) + " to " + noYearFormat.format(new Date(to)));
      heading.setSubtext("Starting " + superShortFormat.format(new Date(from)));

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
