package mitll.langtest.client.flashcard;

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
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class ScoreHistoryContainer extends SimplePagingContainer<ExerciseCorrectAndScore> {
  private final Logger logger = Logger.getLogger("ScoreHistoryContainer");

  private static final int MAX_LENGTH_ID = 15;
  public static final int TABLE_HISTORY_WIDTH = 420;
  public static final int COL_WIDTH = 120;
  public static final String CORRECT_INCORRECT_HISTORY_AND_AVERAGE_PRONUNCIATION_SCORE = "Correct/Incorrect history and average pronunciation score";

  private final Map<String, CommonShell> idToExercise = new HashMap<String, CommonShell>();
  private ExerciseComparator sorter;

  public ScoreHistoryContainer(ExerciseController controller, List<CommonShell> allExercises) {
    super(controller);
    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());

    for (CommonShell commonShell : allExercises) {
      idToExercise.put(commonShell.getID(), commonShell);
    }
  }

  public Panel getTableWithPager(List<ExerciseCorrectAndScore> sortedHistory) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");

    for (ExerciseCorrectAndScore exerciseCorrectAndScore : sortedHistory) {
      addItem(exerciseCorrectAndScore);
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
    Column<ExerciseCorrectAndScore, SafeHtml> englishCol = getEnglishColumn();
    englishCol.setSortable(true);
    table.setColumnWidth(englishCol, COL_WIDTH +
        "px");

    String language = controller.getLanguage();
    addColumn(englishCol, new TextHeader("English"));

    Column<ExerciseCorrectAndScore, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);
    table.setColumnWidth(flColumn,COL_WIDTH +
        "px");

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(flColumn, new TextHeader(headerForFL));

    List<ExerciseCorrectAndScore> dataList = getList();

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = getEnglishSorter(englishCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);

    // We know that the data is sorted alphabetically by default.
//    table.getColumnSortList().push(englishCol);
  }

  private String truncate(String columnText) {
    if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
    return columnText;
  }

  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getEnglishSorter(Column<ExerciseCorrectAndScore, SafeHtml> englishCol,
                                                                                List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                CommonShell shell1 = idToExercise.get(o1.getId());
                CommonShell shell2 = idToExercise.get(o2.getId());

                return sorter.compareStrings(shell1.getEnglish(), shell2.getEnglish());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getFLSorter(Column<ExerciseCorrectAndScore, SafeHtml> flColumn,
                                                                           List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
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
  }

  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getScoreSorter(Column<ExerciseCorrectAndScore, SafeHtml> scoreCol,
                                                                                List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              }
              else {
                int a1 = o1.getAvgScorePercent();
                int a2 = o2.getAvgScorePercent();
                int i = Integer.valueOf(a1).compareTo(a2);
               // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  CommonShell shell1 = idToExercise.get(o1.getId());
                  CommonShell shell2 = idToExercise.get(o2.getId());
                  if (o1.getId().equals(o2.getId())) logger.warning("same id " + o1.getId());
                  return shell1.getEnglish().compareTo(shell2.getEnglish());
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

    Column<ExerciseCorrectAndScore, SafeHtml> column2 = getHistoryColumn();
    table.addColumn(column2, "History");
    table.setColumnWidth(column2, "88px");

    Column<ExerciseCorrectAndScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, "Score");

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, "70" + "px");
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = getScoreSorter(scoreColumn,  getList());
    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().addTooltip(table, CORRECT_INCORRECT_HISTORY_AND_AVERAGE_PRONUNCIATION_SCORE);
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getEnglishColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        CommonShell shell1 = idToExercise.get(shell.getId());
        String columnText = shell1.getEnglish();
        return getSafeHtml(shell, truncate(columnText));
      }
    };
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getFLColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
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

  private SafeHtml getSafeHtml(ExerciseCorrectAndScore shell, String columnText) {
    String html = shell.getId();
    if (columnText != null) {
      if (columnText.length() > MAX_LENGTH_ID)
        columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
      html = "<span style='float:left;" + "'>" + columnText + "</span>";
    }
    return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getHistoryColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        String history = SetCompleteDisplay.getScoreHistory(shell.getCorrectAndScores());
        String s = shell.getCorrectAndScores().isEmpty() ? "" : "<span style='float:right;" +
            "'>" + history +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getScoreColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        String s = shell.getCorrectAndScores().isEmpty() ? "" : "<span " +
            "style='" +
            "margin-left:10px;" +
            "'" +
            ">" + shell.getAvgScorePercent() +
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
    PagingContainer.TableResources.TableStyle cellTableStyle();
  }
}
