package mitll.langtest.server.database;

import mitll.langtest.shared.Grade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @see DatabaseImpl#getExercisesGradeBalancing
 */
class ResultAndGrade implements Comparable<ResultAndGrade> {
  private static final int HIGHEST_INCORRECT_GRADE = 3;
  private ResultDAO.SimpleResult result;
  private List<Grade> grades = new ArrayList<Grade>();

  public ResultAndGrade(ResultDAO.SimpleResult result, List<Grade> grades) {
    this.result = result;
    this.grades = grades;
  }

  public void addGrades(List<Grade> grades) {
    this.grades.addAll(grades);
  }

  public float getRatio() {
    int right = 0;
    int wrong = 0;
    for (Grade g : grades) {
      if (g.grade > HIGHEST_INCORRECT_GRADE) right++; else if (g.grade > 0 && g.grade < HIGHEST_INCORRECT_GRADE) wrong++;
    }
    if (right == 0 && wrong == 0) return 0.5f; // items with no valid grades sort to the middle

    float ratio = (float) right / (float) (right + wrong);
  //  if (grades.size() > 100)
 //   System.out.println("num = " + grades.size() +" : right " + right + " wrong " + wrong + " ratio " + ratio);
    return ratio;
  }

/*  public int totalValidGrades() {
    int total = 0;
    for (Grade g : grades) {
       if (g.grade > 0) total++;
    }
    return total;
  }*/

  private int getNumRight() {
    int right = 0;
    for (Grade g : grades) {
      if (g.grade > HIGHEST_INCORRECT_GRADE) right++;
    }
    return right;
  }
  public int getNumWrong() {
    int wrong = 0;
    for (Grade g : grades) {
      if (g.grade > 0 && g.grade < HIGHEST_INCORRECT_GRADE) wrong++;
    }
    return wrong;
  }

  @Override
  public int compareTo(ResultAndGrade o) {
    int numRight = getNumRight();
    int numRight1 = o.getNumRight();

    int numWrong = getNumWrong();
    int numWrong1 = o.getNumWrong();

    float ratio = getRatio();
    float oratio = o.getRatio();
    return ratio < oratio ? +1 : ratio > oratio ? -1 : numRight > numRight1 ? -1 : numRight < numRight1 ? +1 : numWrong > numWrong1 ? -1 : numWrong < numWrong1 ? +1 : 0;
  }

  public ResultDAO.SimpleResult getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "'" + getResult().getID() + "'\tand grades (" + grades.size() + ")" + " ratio " + getRatio() +
        new HashSet<Grade>(grades);
  }
}
