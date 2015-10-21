package mitll.langtest.client.analysis;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.AnalysisPlot;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class WordContainer extends SimplePagingContainer<WordScore> {
  private final Logger logger = Logger.getLogger("WordContainer");

  private static final int MAX_LENGTH_ID = 15;
  public static final int TABLE_HISTORY_WIDTH = 420;
  public static final int COL_WIDTH = 120;
  public static final String CORRECT_INCORRECT_HISTORY_AND_AVERAGE_PRONUNCIATION_SCORE = "Correct/Incorrect history and average pronunciation score";

  //private final Map<String, CommonShell> idToExercise = new HashMap<String, CommonShell>();
  private ExerciseComparator sorter;
  //ListInterface listInterface;
  AnalysisPlot plot;
  public WordContainer(ExerciseController controller, AnalysisPlot plot) {
    super(controller);
    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
  this.plot = plot;
    //  this.listInterface = listInterface;
//    for (CommonShell commonShell : allExercises) {
//      idToExercise.put(commonShell.getID(), commonShell);
//    }
  }

  /**
   * @see SetCompleteDisplay#getScoreHistory(List, List, ExerciseController)
   * @param sortedHistory
   * @return
   */
  public Panel getTableWithPager(List<WordScore> sortedHistory) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");

    for (WordScore WordScore : sortedHistory) {
      addItem(WordScore);
    }
    flush();
    return tableWithPager;
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;

    o = GWT.create(LocalTableResources.class);

    return o;
  }

  private void addEnglishAndFL() {
    Column<WordScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
   // table.setColumnWidth(itemCol, COL_WIDTH + "px");

    String language = controller.getLanguage();
 //   addColumn(itemCol, new TextHeader("English"));

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader(headerForFL));

//
//    Column<WordScore, SafeHtml> flColumn = getFLColumn();
//    flColumn.setSortable(true);
//    table.setColumnWidth(flColumn,COL_WIDTH + "px");
//
//    String headerForFL = language.equals("English") ? "Meaning" : language;
//    addColumn(flColumn, new TextHeader(headerForFL));

    List<WordScore> dataList = getList();

    ColumnSortEvent.ListHandler<WordScore> columnSortHandler = getEnglishSorter(itemCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

/*    ColumnSortEvent.ListHandler<WordScore> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);*/

    // We know that the data is sorted alphabetically by default.
//    table.getColumnSortList().push(itemCol);
  }

  private String truncate(String columnText) {
    if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
    return columnText;
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
                CommonShell shell1 = plot.getIdToEx().get(o1.getId());
                CommonShell shell2 = plot.getIdToEx().get(o2.getId());
                return sorter.compareStrings(shell1.getForeignLanguage(), shell2.getForeignLanguage());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

/*  private ColumnSortEvent.ListHandler<WordScore> getFLSorter(Column<WordScore, SafeHtml> flColumn,
                                                                           List<WordScore> dataList) {
    ColumnSortEvent.ListHandler<WordScore> columnSortHandler2 = new ColumnSortEvent.ListHandler<WordScore>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<WordScore>() {
          public int compare(WordScore o1, WordScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                CommonShell shell1 = idToExercise.get(o1.getId());
                CommonShell shell2 = idToExercise.get(o2.getId());

                String id1 = shell1.getForeignLanguage();
                String id2 = shell2.getForeignLanguage();
                return id1.toLowerCase().compareTo(id2.toLowerCase());
              }
            }
            return -1;
          }
        });
    return columnSortHandler2;
  }*/

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
              }
              else {
                float a1 = o1.getPronScore();
                float a2 = o2.getPronScore();
                int i = Float.valueOf(a1).compareTo(a2);
               // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
                }
                else {
                  return i;
                }
              }
            }
            else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  @Override
  protected void addColumnsToTable() {
    addEnglishAndFL();

//    Column<WordScore, SafeHtml> column2 = getHistoryColumn();
//    table.addColumn(column2, "History");
//    table.setColumnWidth(column2, "88px");

    Column<WordScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, "Score");

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, "70" + "px");
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<WordScore> columnSortHandler2 = getScoreSorter(scoreColumn,  getList());
    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().addTooltip(table, "Word Scores");
  }

  private Column<WordScore, SafeHtml> getItemColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
     //   String columnText = new WordTable().toHTML(shell.getNetPronImageTypeListMap());
       // logger.info("col " +columnText);
        String columnText = plot.getIdToEx().get(shell.getId()).getForeignLanguage();
        return getSafeHtml(shell, columnText);
      }
    };
  }

/*
  private Column<WordScore, SafeHtml> getFLColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        CommonShell shell1 = idToExercise.get(shell.getId());
        String toShow = shell1.getForeignLanguage();

        if (controller.getLanguage().equalsIgnoreCase("english")) {
          toShow = shell1.getMeaning();
        }
        String columnText = truncate(toShow);

        return getSafeHtml(shell, columnText);
      }
    };
  }
*/

  private SafeHtml getSafeHtml(WordScore shell, String columnText) {
    String html = shell.getId();
    if (columnText != null) {
      if (columnText.length() > MAX_LENGTH_ID)
        columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
      html = "<span style='float:left;" + "'>" + columnText + "</span>";
    }
    return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
  }

/*
  private Column<WordScore, SafeHtml> getHistoryColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        String history = SetCompleteDisplay.getScoreHistory(shell.getCorrectAndScores());
        String s = shell.getCorrectAndScores().isEmpty() ? "" : "<span style='float:right;" +
            "'>" + history +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }
*/

  private Column<WordScore, SafeHtml> getScoreColumn() {
    return new Column<WordScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordScore shell) {
        float v = shell.getPronScore() * 100;
        String s =   "<span " +
            "style='" +
            "margin-left:10px;" +
            "'" +
            ">" + ((int)v) +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
