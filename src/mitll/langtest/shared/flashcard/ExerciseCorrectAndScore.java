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
   // float myCorrect = getAvgCorrect();
   // float otherCorrect = o.getAvgCorrect();
    int myI = getNumIncorrect();
    int oI = o.getNumIncorrect();
    int i = myI < oI ? +1 : myI > oI ? -1 : 0;
    if (i == 0) {
      //if (myCorrect < 0.01 || myCorrect > 0.99) {
        int n = getNumCorrect();
        int on = o.getNumCorrect();

        i = n < on ? -1 : n > on ? +1 : 0;
      //}
      if (i == 0) {
        float myScore = getAvgScore();
        float otherScore = o.getAvgScore();

        return myScore < otherScore ? -1 : otherScore > myScore ? +1 : 0;
      } else {
        return i;
      }
    } else {
      return i;
    }
  }

  private int getNumCorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > 5) toUse = toUse.subList(toUse.size() - 5, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c;
  }

  private int getNumIncorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > 5) toUse = toUse.subList(toUse.size() - 5, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (!correctAndScore.isCorrect()) c++;
    }
    return c;
  }

  private float getAvgCorrect() {
    float c = 0;
    List<CorrectAndScore> toUse= getCorrectAndScores();
    if (toUse.size() > 5) toUse=toUse.subList(toUse.size()-5,toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c / (float) toUse.size();
  }


  private float getAvgScore() {
    float c = 0;
    List<CorrectAndScore> toUse= getCorrectAndScores();
    if (toUse.size() > 5) toUse=toUse.subList(toUse.size()-5,toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
       c += correctAndScore.getScore();
    }
    return c / (float) toUse.size();
  }


  public void add(CorrectAndScore correctAndScore) {
    getCorrectAndScores().add(correctAndScore);
  }

  public void sort() {
    Collections.sort(getCorrectAndScores());
  }

  public String getId() {
    return id;
  }
  public List<CorrectAndScore> getCorrectAndScores() {
    return correctAndScores;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (CorrectAndScore correctAndScore: getCorrectAndScores()) { if (correctAndScore.isCorrect()) builder.append("+"); else builder.append("-"); }

    return "id " + id + " " +builder +" correct " +getAvgCorrect() +
        " score " + (int)getAvgScore();
  }
}
