package mitll.langtest.client.grading;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;

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
 * Table that lets the user set grades for the results (answers).  Supports two columns of grades.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class GradingResultManager extends ResultManager {
  private static final String ENGLISH_ONLY = "english-only";
  private static final String GRADE_TYPE_ANY = "any";

  private static final List<String> GRADING_OPTIONS = Arrays.asList(UNGRADED, "1", "2", "3", "4", "5", SKIP);
  private final boolean englishOnlyMode;

  /**
   * @see mitll.langtest.client.grading.GradingExercisePanel#showResults
   * @param s
   * @param feedback
   * @param englishOnlyMode
   * @param propertyHandler
   */
  public GradingResultManager(LangTestDatabaseAsync s, UserFeedback feedback, boolean englishOnlyMode, PropertyHandler propertyHandler) {
    super(s,feedback, "Answer",propertyHandler);
    this.englishOnlyMode = englishOnlyMode;
  }

  @Override
  protected CellTable<Result> getResultCellTable(Collection<Result> result,
                                                 boolean showQuestionColumn, Collection<Grade> grades,
                                                 int grader, int numGrades) {
    CellTable<Result> resultCellTable = super.getResultCellTable(result, showQuestionColumn, grades, grader, numGrades);
    String gradingWidth = GRADING_WIDTH + "px"; // todo do something better here?
    resultCellTable.setWidth(gradingWidth +"px");

    showPageWithUngraded(result, grades, numGrades, resultCellTable);
    return resultCellTable;
  }

  /**
   * Set the page to the first one with ungraded results.
   * @param result
   * @param grades
   * @param numGrades
   * @param resultCellTable
   */
  private void showPageWithUngraded(Collection<Result> result, Collection<Grade> grades, int numGrades, CellTable<Result> resultCellTable) {
    Set<Integer> gradedResults = new HashSet<Integer>();
    for (Grade g : grades) {
      if (g.gradeIndex == numGrades-1) {
        gradedResults.add(g.resultID);
      }
    }

    int index = 0;
    //  System.out.println("getResultCellTable num grades to look for " + numGrades + " results " + gradedResults);

    for (Result r : result) {
      if (gradedResults.contains(r.uniqueID)) {
        index++;
      }
      else { break; }
    }

    int page = index / pageSize;
    // System.out.println("getResultCellTable last graded " + index + " page " + page + " page size " + pageSize);
    resultCellTable.setVisibleRange(page * pageSize, Math.min(result.size(),(page+1)* pageSize));
  }

  /**
   * @see mitll.langtest.client.result.ResultManager#addColumnsToTable
   * @param table
   * @return
   */
  @Override
  protected TextColumn<Result> addUserPlanExercise(CellTable<Result> table) {
    return null;
  }

  /**
   * @see mitll.langtest.client.result.ResultManager#addColumnsToTable(boolean, java.util.Collection, int, int, com.google.gwt.user.cellview.client.CellTable)
   * @param grades
   * @param grader
   * @param numGrades
   * @param table
   */
  @Override
  protected void addResultColumn(Collection<Grade> grades, int grader, int numGrades, CellTable<Result> table) {
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
   * @see #addResultColumn
   * @param grades
   * @param grader who is grading
   * @param editable only the last column is editable
   * @return
   */
  private Column<Result, String> getGradingColumn(Collection<Grade> grades,
                                                  final int grader, final int gradingColumnIndex,
                                                  final boolean editable) {
    final Map<Integer, Grade> resultToGrade = getResultToGrade(grades, gradingColumnIndex);

    AbstractCell<String> selectionCell = editable ? new SelectionCell(GRADING_OPTIONS) : new TextCell();
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
          String gradeType = englishOnlyMode ? gradingColumnIndex == 0 ? GRADE_TYPE_ANY : ENGLISH_ONLY : GRADE_TYPE_ANY;
          choice = new Grade(result.uniqueID,grade,grader,gradeType,gradingColumnIndex);
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
        System.err.println("setFieldUpdater : couldn't parse " + value + "??");
      }
    }
    return grade;
  }

  /**
   * Make a map of result id -> grade to display for that result
   * @see #getGradingColumn(java.util.Collection, int, int, boolean)
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
      List<Grade> gradesForResult = getGradesSortedById(idToGrades);
      if (!englishOnlyMode) {

        for (Grade choice : gradesForResult) {
          if (choice.gradeIndex == gradingColumnIndex) {
            resultToGradeForColumn.put(resultID, choice);
          }
        }
      } else {
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
              if (choice.gradeType.equals(ENGLISH_ONLY)) {
                resultToGradeForColumn.put(resultID, choice);
              }
            } else {
              resultToGradeForColumn.put(resultID, choice);
            }
          }
        }
      }
    }
    return resultToGradeForColumn;
  }

  private List<Grade> getGradesSortedById(Map.Entry<Integer, List<Grade>> idToGrades) {
    List<Grade> gradesForResult = idToGrades.getValue();
    Collections.sort(gradesForResult, new Comparator<Grade>() {
      public int compare(Grade o1, Grade o2) {
        return o1.id < o2.id ? -1 : o1.id > o2.id ? +1 : 0;
      }
    });
    return gradesForResult;
  }

  /**
   * Filter out grades that aren't of the right type - col 0 = "any", col 1 = "english-only"
   * @param grades
   * @param gradingColumnIndex
   * @return
   */
  private Map<Integer, List<Grade>> getResultToGradeList(Collection<Grade> grades, int gradingColumnIndex) {
    final Map<Integer, List<Grade>> resultToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = resultToGrade.get(g.resultID);
      if (gradesForResult == null) {
        resultToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }

      if (!englishOnlyMode) {
        gradesForResult.add(g);
      } else {
        //System.out.println("Col " + gradingColumnIndex + " Examining " + g);
        String gradeType = g.gradeType;
        if (gradingColumnIndex == 0 && (gradeType.equals(GRADE_TYPE_ANY) || gradeType.length() == 0)) { // for first column, don't include "english-only"
          gradesForResult.add(g);
        } else if (gradingColumnIndex == 1 && (gradeType.equals(ENGLISH_ONLY) || gradeType.length() == 0)) { // for second column, don't include "any"
          gradesForResult.add(g);
        }/* else {
          //System.out.println("\tCol " + gradingColumnIndex + " Skipping " + g);
        }*/
      }
    }
    return resultToGrade;
  }

  /**
   * Add a new grade -- note that the Grade object that is sent doesn't have a unique id until the response returns.
   * @see #getGradingColumn(java.util.Collection, int, int, boolean)
   * @param exerciseID
   * @param toAdd
   * @param resultID
   * @param resultToGrade
   */
  private void addGrade(String exerciseID, final Grade toAdd, final int resultID, final Map<Integer, Grade> resultToGrade) {
/*    System.out.println("addGrade for exercise " + exerciseID + " : grade " + toAdd + " at " + new Date() +
      " result->grade has " + resultToGrade.size());*/

    service.addGrade(exerciseID, toAdd, new AsyncCallback<CountAndGradeID>() {
      public void onFailure(Throwable caught) { Window.alert("addGrade Couldn't contact server."); }
      public void onSuccess(CountAndGradeID result) {
        feedback.showStatus("Now " + result.count + " graded out of " +result.resultCount +
          " answers (" + (int)(100f*(float)result.count/result.resultCount) + "%)");
        toAdd.id = (int) result.gradeID;
        resultToGrade.put(resultID, toAdd);
        System.out.println("\tgrade added at " + new Date() + " adding result " + resultID + " -> grade " + toAdd +
          ", now " + resultToGrade.size() + " grades for result");
      }
    });
  }

  /**
   * Change an existing grade.
   * @param toChange
   */
  private void changeGrade(final Grade toChange) {
    service.changeGrade(toChange, new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {  Window.alert("changeGrade Couldn't contact server."); }
      public void onSuccess(Void result) {
        feedback.showStatus("Grade changed to " + getStringForGrade(toChange.grade));
      }
    }
    );
  }
}
