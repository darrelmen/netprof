/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
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
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class WordContainer extends SimplePagingContainer<WordScore> implements AnalysisPlot.TimeChangeListener {
  private final Logger logger = Logger.getLogger("WordContainer");

  private static final int ROWS_TO_SHOW = 8;

  private static final int ITEM_COL_WIDTH = 250;
  private static final String SCORE = "Score";
  private static final int SCORE_WIDTH = 68;
  public static final int PLAY_WIDTH = 42;
  private static final int NATIVE_WIDTH = PLAY_WIDTH;
  private static final String NATIVE = "Ref";
  private static final String PLAY = "Play";

  private static final int TABLE_HISTORY_WIDTH = 430; //380
  private final ExerciseComparator sorter;
  private final AnalysisPlot plot;
  private final ShowTab learnTab;
  private final Heading heading;

  /**
   * @param controller
   * @param plot
   * @see AnalysisTab#getWordScores(LangTestDatabaseAsync, ExerciseController, int, ShowTab, AnalysisPlot, Panel, int)
   */
  public WordContainer(ExerciseController controller, AnalysisPlot plot, ShowTab learnTab, Heading w) {
    super(controller);
    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
    this.plot = plot;
    plot.addListener(this);
    this.learnTab = learnTab;
    this.heading = w;
  }

  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");


  protected int getPageSize() {
    return ROWS_TO_SHOW;
  }

  private List<WordScore> sortedHistory;

  /**
   * @param sortedHistory
   * @return
   * @see AnalysisTab#getWordScores
   */
  public Panel getTableWithPager(List<WordScore> sortedHistory) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");

    this.sortedHistory = sortedHistory;
    addItems(sortedHistory);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        PlayAudioWidget.addPlayer();
      }
    });

    return tableWithPager;
  }

  private void addItems(List<WordScore> sortedHistory) {
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
    table.setColumnWidth(itemCol, ITEM_COL_WIDTH + "px");

    String language = controller.getLanguage();

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

    List<WordScore> dataList = getList();

    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = getEnglishSorter(itemCol, dataList);
    table.addColumnSortHandler(columnSortHandler);
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
                String id = o1.getId();
                CommonShell shell1 = getShell(id);
                CommonShell shell2 = getShell(o2.getId());
                return sorter.compareStrings(shell1.getForeignLanguage(), shell2.getForeignLanguage());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private CommonShell getShell(String id) {
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
          // logger.info("getItemColumn : column text empty for id " + shell.getId() + " and found ex " + exercise);

          String foreignLanguage = exercise == null ? "" : exercise.getForeignLanguage();
          if (controller.getLanguage().equalsIgnoreCase("Spanish")) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getPronScore());
        } else {
          //logger.info("getItemColumn : Got item id " + shell.getId() + " "+ columnText );
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
   * @see #addColumnsToTable()
   */
  private Column<WordScore, SafeHtml> getPlayAudio() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        CommonShell exercise = getShell(shell.getId());
        // logger.info("getPlayAudio : Got " + shell.getId() + "  : " + exercise);
        String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        return PlayAudioWidget.getAudioTagHTML(shell.getFileRef(), title);
      }
    };
  }

  private Column<WordScore, SafeHtml> getPlayNativeAudio() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        CommonShell exercise = getShell(shell.getId());
        // logger.info("getPlayAudio : Got " + shell.getId() + "  : " + exercise);
        String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        if (shell.getNativeAudio() != null) {
          return PlayAudioWidget.getAudioTagHTML(shell.getNativeAudio(), title);
        } else {
          return new SafeHtmlBuilder().toSafeHtml();
        }
      }
    };
  }

  @Override
  public void timeChanged(long from, long to) {
    List<WordScore> filtered = new ArrayList<>();
    heading.setSubtext("Starting " + superShortFormat.format(new Date(from)));
    for (WordScore wordScore : sortedHistory) {
      if (wordScore.getTimestamp() > from && wordScore.getTimestamp() <= to) filtered.add(wordScore);

    }
    addItems(filtered);
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
//    interface TableStyle extends CellTable.Style {
//    }
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
