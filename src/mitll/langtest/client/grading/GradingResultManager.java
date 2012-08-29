package mitll.langtest.client.grading;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.ResultManager;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class GradingResultManager extends ResultManager {
  private static final List<String> GRADING_OPTIONS = Arrays.asList(UNGRADED, "1", "2", "3", "4", "5", SKIP);

  /**
   * @see mitll.langtest.client.grading.GradingExercisePanel#showResults(java.util.Collection, java.util.Collection, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.grading.GradingExercisePanel, boolean, int, int, int, String)
   * @param s
   * @param feedback
   */
  public GradingResultManager(LangTestDatabaseAsync s, UserFeedback feedback) {
    super(s,feedback);
  }

  /**
   * @see #getTable(java.util.Collection, boolean, boolean, java.util.Collection, String, int)
   * @param table
   * @param id
   * @return
   */
  @Override
  protected TextColumn<Result> addUserPlanExercise(CellTable<Result> table, TextColumn<Result> id) {
    return null;
  }

  /**
   * @see #getTable(java.util.Collection, boolean, boolean, java.util.Collection, String, int)
   * @param grades
   * @param grader
   * @param numGrades
   * @param table
   */
  @Override
  protected void addResultColumn(Collection<Grade> grades, String grader, int numGrades, CellTable<Result> table) {
    Map<Integer,Integer> resultToGradeFirstColumn = new HashMap<Integer, Integer>();
    for (int i = 0; i < numGrades; i++) {
      Column<Result, String> col = getGradingColumn(grades, resultToGradeFirstColumn, grader, i, i == numGrades - 1);
      String columnHeader = numGrades > 1 ? "Grade #" + (i + 1) : "Grade";
      table.addColumn(col, columnHeader);
    }
  }

  /**
   * Adds second column, on demand
   *
   * Hides the grade value from the user for grade #1.
   *
   * Rejects adding a second grade if first isn't done yet.
   *
   * @see #getTable
   * @param grades
   * @param grader who is grading
   * @param editable only the last column is editable
   * @return
   */
  private Column<Result, String> getGradingColumn(Collection<Grade> grades,
                                                  final Map<Integer,Integer> resultToGradeFirstColumn,
                                                  final String grader, final int gradingColumnIndex,
                                                  final boolean editable) {
    final Map<Integer, Long> resultToGradeID = new HashMap<Integer, Long>();
    final Map<Integer, Integer> resultToGrade = getResultToGrade(grades,resultToGradeID, gradingColumnIndex);
    if (gradingColumnIndex == 0) resultToGradeFirstColumn.putAll(resultToGrade);

    AbstractCell selectionCell = editable ? new SelectionCell(GRADING_OPTIONS) : new TextCell();
    Column<Result, String> col = new Column<Result, String>(selectionCell) {
      @Override
      public String getValue(Result object) {
        Integer grade = resultToGrade.get(object.uniqueID);
        return grade == null ? UNGRADED : (editable ? (grade == -1 ? UNGRADED : (grade == -2 ? SKIP : "" + grade)) : "--");
      }
    };
    col.setFieldUpdater(new FieldUpdater<Result, String>() {
      public void update(int index, final Result result, String value) {
        if (gradingColumnIndex > 0) {
          Integer grade = resultToGradeFirstColumn.get(result.uniqueID);
          if (grade == null) {
            Window.alert("Please wait until first grade is set.");
            return;
          }
        }
        int grade = getValueToGrade(value);
        resultToGrade.put(result.uniqueID, grade);
        Long gradeID = resultToGradeID.get(result.uniqueID);
        System.out.println("getGradingColumn Found " + gradeID + " for " + result);
        addGrade(result, grade, grader, gradeID == null ? -1 : gradeID, resultToGradeID);
      }
    });
    return col;
  }

  private int getValueToGrade(String value) {
    int grade = -1;
    if (value.equals(UNGRADED)) grade = -1;
    else if (value.equals(SKIP)) grade = -2;
    else {
      try {
        grade = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        System.err.println("setFieldUpdater : couldn't parse " + value +"??");
      }
    }
    return grade;
  }

  private Map<Integer, Integer> getResultToGrade(Collection<Grade> grades,Map<Integer, Long> resultToGradeID, int gradingColumnIndex) {
    final Map<Integer,List<Grade>> resultToGrade = new HashMap<Integer, List<Grade>>();

    final Map<Integer,Integer> resultToGradeForColumn = new HashMap<Integer, Integer>();

    for (Grade g : grades) {
      List<Grade> gradesForResult = resultToGrade.get(g.resultID);
      if (gradesForResult == null) {
        resultToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
    }
    for (Map.Entry<Integer, List<Grade>> idToGrades : resultToGrade.entrySet()) {
      Integer resultID = idToGrades.getKey();
      List<Grade> gradesForResult = idToGrades.getValue();
      if (gradesForResult.size() > 1) {
        //System.out.println(resultID + "->" + gradesForResult);
      }
      Collections.sort(gradesForResult, new Comparator<Grade>() {
        public int compare(Grade o1, Grade o2) {
          return o1.id < o2.id ? -1 : o1.id > o2.id ? +1 : 0;
        }
      });
      if (gradingColumnIndex < gradesForResult.size()) {
        Grade choice = gradesForResult.get(gradingColumnIndex);
        resultToGradeForColumn.put(resultID, choice.grade);
        resultToGradeID.put(resultID,(long)choice.id);
        //System.out.println("\t"+resultID + "->" + choice + " at " + gradingColumnIndex);
      }
    }
    return resultToGradeForColumn;
  }

  /**
   * @see #getGradingColumn
   * @param answerToGrade
   * @param grade
   * @param grader
   * @param gradeID
   */
  private void addGrade(final Result answerToGrade, int grade, String grader, long gradeID, final Map<Integer, Long> resultToGradeID) {
    service.addGrade(answerToGrade.uniqueID, answerToGrade.id, grade, gradeID, true, grader, new AsyncCallback<CountAndGradeID>() {
      public void onFailure(Throwable caught) {}

      public void onSuccess(CountAndGradeID result) {
        feedback.showStatus("Now " + result.count + " graded answers.");
        resultToGradeID.put(answerToGrade.uniqueID, result.gradeID);
      }

 /*
        remainingResults.remove(answerToGrade.uniqueID);
        if (remainingResults.isEmpty()) {
         // panel.recordCompleted(panel);
        }
      */
    });
  }
}
