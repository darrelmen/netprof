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

  public ExerciseCorrectAndScore() {}

  /**
   * @see mitll.langtest.server.database.ResultDAO#getSortedAVPHistory(java.util.List, java.util.Collection)
   * @param id
   */
  public ExerciseCorrectAndScore(String id) {
    this.id = id;
  }

  @Override
  public int compareTo(ExerciseCorrectAndScore o) {
    if (isEmpty() && o.isEmpty()) {
      return getId().compareTo(o.getId()); // TODO : consider compare on tooltip
    } else if (isEmpty()) return -1;
    else if (o.isEmpty()) return +1;
    else { // neither is empty
      return compScores(o);
    }
  }

  public int compareTo(ExerciseCorrectAndScore o,String fl, String otherFL) {
    if (isEmpty() && o.isEmpty()) {
      return fl.compareTo(otherFL);
    } else if (isEmpty()) return -1;
    else if (o.isEmpty()) return +1;
    else { // neither is empty
      return compScores(o);
    }
  }

  private int compScores(ExerciseCorrectAndScore o) {
    int myI = getDiff();
    int oI = o.getDiff();
    int i = myI < oI ? -1 : myI > oI ? +1 : 0;
    if (i == 0) {
      float myScore = getAvgScore();
      float otherScore = o.getAvgScore();

      return myScore < otherScore ? -1 : myScore > otherScore ? +1 : getId().compareTo(o.getId());
    } else {
      return i;
    }
  }

  private boolean isEmpty() { return correctAndScores.isEmpty();  }

  private int getNumCorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c;
  }

  private int getNumIncorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (!correctAndScore.isCorrect()) c++;
    }
    return c;
  }


  private int getDiff() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
      else c--;
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
    if (isEmpty()) return 0f;

    float c = 0f;
    float n = 0f;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.getScore() > 0f) {
        c += correctAndScore.getScore();
        n++;
      }
    }
    return n > 0f ? (c / n) : 0f;
  }

  public List<CorrectAndScore> getCorrectAndScoresLimited() {
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse = toUse.subList(toUse.size() - MAX_TO_USE, toUse.size());
    return toUse;
  }

  public int getAvgScorePercent() {
    return Math.round(getAvgScore() * 100f);
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

  public List<CorrectAndScore> getCorrectAndScores() { return correctAndScores;  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (CorrectAndScore correctAndScore : getCorrectAndScores()) {
      if (correctAndScore.isCorrect()) builder.append("+");
      else builder.append("-");
    }

    return "id " + id + " " + builder + " correct " + getNumCorrect() + "/" + getNumIncorrect() + " score " + (int) getAvgScore();
  }
}
