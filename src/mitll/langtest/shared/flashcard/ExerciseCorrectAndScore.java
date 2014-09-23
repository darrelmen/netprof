package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by go22670 on 9/13/14.
 */
public class ExerciseCorrectAndScore implements IsSerializable, Comparable<ExerciseCorrectAndScore> {
  private static final int MAX_TO_USE = 5;
  private List<CorrectAndScore> correctAndScores = new ArrayList<CorrectAndScore>();
  private String id;

  public ExerciseCorrectAndScore() {
  }

/*  public ExerciseCorrectAndScore(String id, List<CorrectAndScore> correctAndScores) {
    this.id = id;
    this.correctAndScores = correctAndScores;
  }*/

  public ExerciseCorrectAndScore(String id) {
    this.id = id;
  }

  @Override
  public int compareTo(ExerciseCorrectAndScore o) {
    if (isEmpty() && o.isEmpty()) {
      return getId().compareTo(o.getId());
    }
    else if (isEmpty()) return -1;
    else if (o.isEmpty()) return +1;
    else { // neither is empty
      int myI = getNumIncorrect();
      int oI = o.getNumIncorrect();
      int i = myI > oI ? -1 : myI < oI ? +1 : 0;
      if (i == 0) {
        int n = getNumCorrect();
        int on = o.getNumCorrect();

        i = n < on ? -1 : n > on ? +1 : 0;

        if (i == 0) {
          float myScore = getAvgScore();
          float otherScore = o.getAvgScore();

          return myScore < otherScore ? -1 : myScore > otherScore ? +1 : getId().compareTo(o.getId());
        } else {
          return i;
        }
      } else {
        return i;
      }
    }
  }

/*  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }*/

  private boolean isEmpty() {  return correctAndScores.isEmpty();  }

  private int getNumCorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse = toUse.subList(toUse.size() - MAX_TO_USE, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c;
  }

  private int getNumIncorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse = toUse.subList(toUse.size() - MAX_TO_USE, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (!correctAndScore.isCorrect()) c++;
    }
    return c;
  }

/*  private float getAvgCorrect() {
    if (isEmpty()) return 0f;
    float c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse = toUse.subList(toUse.size() - MAX_TO_USE, toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c / (float) toUse.size();
  }*/

  private float getAvgScore() {
    if  (isEmpty()) return 0f;

    float c = 0f;
    float n = 0f;
    List<CorrectAndScore> toUse= getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse=toUse.subList(toUse.size()- MAX_TO_USE,toUse.size());
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.getScore() > 0f) {
        c += correctAndScore.getScore();
        n++;
      }
    }
    float v = n > 0f ? (c / n) : 0f;

    return v;
  }

  public int getAvgScorePercent() { return Math.round(getAvgScore()*100f);}

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
    for (CorrectAndScore correctAndScore: getCorrectAndScores()) {
      if (correctAndScore.isCorrect()) builder.append("+"); else builder.append("-");
    }

    return "id " + id + " " +builder +" correct " +getNumCorrect() + "/" + getNumIncorrect() +
        " score " + (int)getAvgScore();
  }
}
