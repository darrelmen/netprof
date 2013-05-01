package mitll.langtest.server.database;

import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @see #getExercisesGradeBalancing(boolean, boolean)
 */
class ResultAndGrade implements Comparable<ResultAndGrade> {
  private Result result;
  private List<Grade> grades = new ArrayList<Grade>();

  public ResultAndGrade(Result result, List<Grade> grades) {
    this.result = result;
    this.grades = grades;
  }

/*    public void addGrade(Grade g) {
    grades.add(g);
  }*/

  public void addGrades(List<Grade> grades) {
    this.grades.addAll(grades);
  }

  public float getRatio() {
    int right = 0;
    int wrong = 0;
    for (Grade g : grades) {
      if (g.grade > 3) right++; else if (g.grade > 0 && g.grade < 3) wrong++;
    }
    if (right == 0 && wrong == 0) return 0.5f; // items with no valid grades sort to the middle

    float ratio = (float) right / (float) (right + wrong);
  //  if (grades.size() > 100)
 //   System.out.println("num = " + grades.size() +" : right " + right + " wrong " + wrong + " ratio " + ratio);
    return ratio;
  }

  public int totalValidGrades() {
    int total = 0;
    for (Grade g : grades) {
       if (g.grade > 0) total++;
    }
    return total;
  }

  private int getNumRight() {
    int right = 0;
    for (Grade g : grades) {
      if (g.grade > 3) right++;
    }
    return right;
  }
  private int getNumWrong() {
    int wrong = 0;
    for (Grade g : grades) {
      if (g.grade > 0 && g.grade < 3) wrong++;
    }
    return wrong;
  }

  @Override
  public int compareTo(ResultAndGrade o) {
    float ratio = getRatio();
    float oratio = o.getRatio();
    int numRight = getNumRight();
    int numRight1 = o.getNumRight();

    int numWrong = getNumWrong();
    int numWrong1 = o.getNumWrong();
    return ratio < oratio ? +1 : ratio > oratio ? -1 : numRight > numRight1 ? -1 : numRight < numRight1 ? +1 : numWrong > numWrong1 ? -1 : numWrong < numWrong1 ? +1 :0;
  }

  public Result getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "'" + getResult() + "'\tand grades (" + grades.size() + ")" + " ratio " + getRatio() +
        new HashSet<Grade>(grades);
  }
}
