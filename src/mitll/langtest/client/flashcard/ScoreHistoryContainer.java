package mitll.langtest.client.flashcard;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/20/15.
 */
class ScoreHistoryContainer extends SimplePagingContainer<ExerciseCorrectAndScore> {
  private static final int MAX_LENGTH_ID = 30;
  private static final int TABLE_HISTORY_WIDTH = 326;
//  private static final int ONE_TABLE_WIDTH = 275;//-(TABLE_HISTORY_WIDTH/2);//275;
  //private static final int TABLE_WIDTH = 2 * ONE_TABLE_WIDTH;
  private final Map<String, String> idToExercise = new HashMap<String, String>();

  public ScoreHistoryContainer(ExerciseController controller, List<CommonShell> allExercises) {
    super(controller);

    for (CommonShell commonShell : allExercises) {
      idToExercise.put(commonShell.getID(), commonShell.getTooltip());
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

  @Override
  protected void addColumnsToTable() {
    Column<ExerciseCorrectAndScore, SafeHtml> column = getColumn();
    column.setSortable(true);

    table.setWidth("100%", true);
    table.addColumn(column, "Item");
    table.setColumnWidth(column, "180px");

    Column<ExerciseCorrectAndScore, SafeHtml> column2 = getColumn2();
    column2.setSortable(true);
    table.addColumn(column2, "History");
    table.setColumnWidth(column2, "88px");


    Column<ExerciseCorrectAndScore, SafeHtml> column3 = getColumn3();
    table.addColumn(column3, "Score");
    column3.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    column3.setSortable(true);
    table.setColumnWidth(column3, "70" + "px");

    new TooltipHelper().addTooltip(table, "Correct/Incorrect history and average pronunciation score");
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        String columnText = idToExercise.get(shell.getId());
        String html = shell.getId();
        if (columnText != null) {
          if (columnText.length() > MAX_LENGTH_ID)
            columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";

          html = "<span style='float:left;" +
              //"margin-left:-10px;" +
              "'>" + columnText + "</span>";
        }
        return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
      }
    };
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getColumn2() {

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

  private Column<ExerciseCorrectAndScore, SafeHtml> getColumn3() {
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
