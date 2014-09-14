package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by go22670 on 9/13/14.
 */
public class ExerciseCorrectAndScore implements IsSerializable, Comparable<ExerciseCorrectAndScore> {
  private List<CorrectAndScore> correctAndScores = new ArrayList<CorrectAndScore>();
  private String id;

  public ExerciseCorrectAndScore() {
  }

  public ExerciseCorrectAndScore(String id, List<CorrectAndScore> correctAndScores) {
    this.id = id;
    this.correctAndScores = correctAndScores;
  }

  public ExerciseCorrectAndScore(String id) {
    this.id = id;
  }

  @Override
  public int compareTo(ExerciseCorrectAndScore o) {
    float myCorrect = getAvgCorrect();
    float otherCorrect = o.getAvgCorrect();

    int i = myCorrect < otherCorrect ? -1 : otherCorrect > myCorrect ? +1 : 0;
    if (i == 0) {
      float myScore = getAvgScore();
      float otherScore = o.getAvgScore();

      return myScore < otherScore ? -1 : otherScore > myScore ? +1 : 0;

    } else {
      return i;
    }
  }

  private float getAvgCorrect() {
    float c = 0;
    List<CorrectAndScore> toUse= correctAndScores;
    if (toUse.size() > 5) toUse=toUse.subList(toUse.size()-5,toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.wasCorrect) c++;
    }
    return c / (float) toUse.size();
  }


  private float getAvgScore() {
    float c = 0;
    List<CorrectAndScore> toUse= correctAndScores;
    if (toUse.size() > 5) toUse=toUse.subList(toUse.size()-5,toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
       c += correctAndScore.score;
    }
    return c / (float) toUse.size();
  }


  public void add(CorrectAndScore correctAndScore) {
    correctAndScores.add(correctAndScore);
  }

  public void sort() {
    Collections.sort(correctAndScores);
  }

  public String getId() {
    return id;
  }
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (CorrectAndScore correctAndScore: correctAndScores) { if (correctAndScore.wasCorrect) builder.append("+"); else builder.append("-"); }

    return "id " + id + " " +builder +" pron score " + getAvgScore();
  }
}
