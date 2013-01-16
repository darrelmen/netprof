package mitll.langtest.client.grading;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.ResultManager;
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
import java.util.List;
import java.util.Map;

/**
 * Table that lets the user set grades for the results (answers).  Supports two columns of grades.
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
   * @return
   */
  @Override
  protected TextColumn<Result> addUserPlanExercise(CellTable<Result> table) {
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
    for (int i = 0; i < numGrades; i++) {
      Column<Result, String> col = getGradingColumn(grades, grader, i, i == numGrades - 1);
      String columnHeader = numGrades > 1 ? "Grade #" + (i + 1) : "Grade";
      table.addColumn(col, columnHeader);
    }
  }

  /**
   * Adds second column, on demand
   * <br></br>
   * Hides the grade value from the user for grade #1.   But if it's ungraded it appears "Ungraded"
   * <br></br>
   * Accepts adding a second grade if first isn't done yet.
   *
   * @see #getTable
   * @param grades
   * @param grader who is grading
   * @param editable only the last column is editable
   * @return
   */
  private Column<Result, String> getGradingColumn(Collection<Grade> grades,
                                                  final String grader, final int gradingColumnIndex,
                                                  final boolean editable) {
    final Map<Integer, Grade> resultToGrade = getResultToGrade(grades, gradingColumnIndex);

    AbstractCell selectionCell = editable ? new SelectionCell(GRADING_OPTIONS) : new TextCell();
    Column<Result, String> col = new Column<Result, String>(selectionCell) {
      @Override
      public String getValue(Result result) {
        Grade choice = resultToGrade.get(result.uniqueID);
        if (choice == null) {
         // System.out.println("getGradingColumn getValue no grade for " + result);
          return UNGRADED;
        }
        else {
          Integer grade = choice.grade;
          return (editable ? getStringForGrade(grade) : "--");
        }
      }
    };
    col.setFieldUpdater(new FieldUpdater<Result, String>() {
      public void update(int index, final Result result, String value) {
        int grade = getGrade(value);
        Grade choice = resultToGrade.get(result.uniqueID);
        if (choice == null) {
          String gradeType = gradingColumnIndex == 0 ? "any" : "english-only";
          choice = new Grade(result.uniqueID,grade,grader,gradeType);
          System.out.println("getGradingColumn making new grade " + choice + " for " + result);
          addGrade(result.id, choice, result.uniqueID, resultToGrade);
        }
        else {
          System.out.println("getGradingColumn updating existing grade " + choice + " for " + result);
          choice.grade = grade;
          changeGrade(choice);
        }
      }
    });
    return col;
  }

  private String getStringForGrade(Integer grade) {
    return (grade == -1 ? UNGRADED : (grade == -2 ? SKIP : "" + grade));
  }

  /**
   * Convert a string to a number grade.
   * @param value
   * @return
   */
  private int getGrade(String value) {
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

  /**
   * Make a map of result id -> grade to display for that result
   * @param grades
   * @param gradingColumnIndex controls whether to show "any" or "english-only" grades.
   * @return
   */
  private Map<Integer, Grade> getResultToGrade(Collection<Grade> grades, int gradingColumnIndex) {
    final Map<Integer,Grade> resultToGradeForColumn = new HashMap<Integer, Grade>();

    // make map of result to grade list
    final Map<Integer, List<Grade>> resultToGrade = getResultToGradeList(grades, gradingColumnIndex);

    for (Map.Entry<Integer, List<Grade>> idToGrades : resultToGrade.entrySet()) {
      Integer resultID = idToGrades.getKey();
      List<Grade> gradesForResult = idToGrades.getValue();
      Collections.sort(gradesForResult, new Comparator<Grade>() {
        public int compare(Grade o1, Grade o2) {
          return o1.id < o2.id ? -1 : o1.id > o2.id ? +1 : 0;
        }
      });
      if (gradesForResult.size() == 2) {
        if (gradingColumnIndex < gradesForResult.size()) {
          Grade choice = gradesForResult.get(gradingColumnIndex);
          if (choice == null) {
            System.err.println("no grade at index " + gradingColumnIndex + "?");
          } else {
            resultToGradeForColumn.put(resultID, choice);
          }
        }
      } else if (!gradesForResult.isEmpty()) {
        Grade choice = gradesForResult.get(0);
        if (choice == null) {
          System.err.println("no grade at index " + 0 + "?");
        } else {
          if (gradingColumnIndex == 1) {
            if (choice.gradeType.equals("english-only")) {
              resultToGradeForColumn.put(resultID, choice);
            }
          }
          else {
            resultToGradeForColumn.put(resultID, choice);
          }
        }
      }
    }
    return resultToGradeForColumn;
  }

  /**
   * Filter out grades that aren't of the right type - col 0 = "any", col 1 = "english-only"
   * @param grades
   * @param gradingColumnIndex
   * @return
   */
  private Map<Integer, List<Grade>> getResultToGradeList(Collection<Grade> grades, int gradingColumnIndex) {
    final Map<Integer,List<Grade>> resultToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = resultToGrade.get(g.resultID);
      if (gradesForResult == null) {
        resultToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }

      //System.out.println("Col " + gradingColumnIndex + " Examining " + g);
      String gradeType = g.gradeType;
      if (gradingColumnIndex == 0 && (gradeType.equals("any") || gradeType.length() == 0)) { // for first column, don't include "english-only"
        gradesForResult.add(g);
      }
      else if (gradingColumnIndex == 1 && (gradeType.equals("english-only") || gradeType.length() == 0)) { // for second column, don't include "any"
        gradesForResult.add(g);
      }
      else {
        //System.out.println("\tCol " + gradingColumnIndex + " Skipping " + g);
      }
    }
    return resultToGrade;
  }

  /**
   * Add a new grade -- note that the Grade object that is sent doesn't have a unique id until the response returns.
   * @param exerciseID
   * @param toAdd
   * @param resultID
   * @param resultToGrade
   */
  private void addGrade(String exerciseID, final Grade toAdd,final int resultID ,final Map<Integer, Grade> resultToGrade) {
    System.out.println("addGrade grade " + toAdd + " at " + new Date() + " result->grade has " + resultToGrade.size());

    service.addGrade(exerciseID, toAdd, new AsyncCallback<CountAndGradeID>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(CountAndGradeID result) {
        feedback.showStatus("Now " + result.count + " graded answers.");
        toAdd.id = (int) result.gradeID;
        resultToGrade.put(resultID, toAdd);
        System.out.println("grade added at " + new Date() + " adding " + resultID + " -> " + toAdd + " now " + resultToGrade.size());
      }

      /*
        remainingResults.remove(answerToGrade.uniqueID);
        if (remainingResults.isEmpty()) {
         // panel.recordCompleted(panel);
        }
      */
    });
  }

  /**
   * Change an existing grade.
   * @param toChange
   */
  private void changeGrade(final Grade toChange) {
    service.changeGrade(toChange,new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Void result) {
        feedback.showStatus("Grade changed to " + getStringForGrade(toChange.grade));

       // System.out.println("changeGrade " + toChange);
      }
    }
    );
  }
}
