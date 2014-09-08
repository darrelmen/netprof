package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.monitoring.Session;

import java.util.ArrayList;
import java.util.List;

/**
* Created by GO22670 on 3/18/14.
*/
public class AVPHistoryForList implements IsSerializable {
  private float pbCorrect;
  private float top;
  private float totalCorrect;
  private List<Float> yValuesForUser;
  private boolean useCorrect;

  private float totalNotMe = 0f;
  private float numNotMe = 0f;
  private int numScores;
  private List<UserScore> scores = new ArrayList<UserScore>();

  public AVPHistoryForList() {}
  public AVPHistoryForList(List<Session> scores, long userID, boolean useCorrect) {
    this.useCorrect = useCorrect;
    numScores = scores.size();
    calc(scores,userID);
    if (scores.isEmpty()) System.err.println("huh? scores is empty???");
  }

  public float getPbCorrect() {
    return pbCorrect;
  }
  public float getTop() {
    return top;
  }
  public float getTotalCorrect() {
    return totalCorrect;
  }

  public float getClassAvg() {
    if (numNotMe == 0f) {
      return totalCorrect / (float) numScores;
    } else {
      return totalNotMe / numNotMe;
    }
  }

  public List<Float> getyValuesForUser() {
    return yValuesForUser;
  }

  public void calc(List<Session> scores, long userID) {
    pbCorrect = 0;
    top = 0;
    totalCorrect = 0;

    yValuesForUser = new ArrayList<Float>();
    for (SetScore score : scores) {
      float value = getValue(score);

      if (score.getUserid() == userID) {
        if (value > pbCorrect) pbCorrect = value;
        yValuesForUser.add(value);
      //  System.out.println("showLeaderboardPlot : for user " +userID + " got " + score);
      } else {
        //System.out.println("\tshowLeaderboardPlot : for user " +score.getUserid() + " got " + score);
        totalNotMe += value;
        numNotMe++;
      }
      if (value > top) {
        top = value;
        //System.out.println("\tshowLeaderboardPlot : new top score for user " +score.getUserid() + " got " + score);
      }
      totalCorrect += value;
    }

    if (yValuesForUser.isEmpty()) {
      System.err.println("huh? yValuesForUser (" + userID + ") is empty???");
    }
  }

  private float getValue(SetScore score) {
    return useCorrect ? Math.round(score.getCorrectPercent()) : Math.round(100f * score.getAvgScore());
  }

  public boolean isUseCorrect() {
    return useCorrect;
  }

  public int getNumScores() {
    return numScores;
  }

  public List<UserScore> getScores() {
    return scores;
  }

  public void setScores(List<UserScore> scores) {
    this.scores = scores;
  }

  public static class UserScore implements IsSerializable {
    private String user;
    private float score;
    private boolean isCurrent;
    private int index;

    public UserScore() {}
    public UserScore(int index,String user, float score, boolean isCurrent) {
      this.index = index ;
      this.user = user;
      this.score = score;
      this.isCurrent = isCurrent;
    }


    public String getUser() {
      return user;
    }

    public float getScore() {
      return score;
    }

    /**
     * Is this the score for the current session?
     * @see mitll.langtest.client.flashcard.MyFlashcardExercisePanelFactory.StatsPracticePanel#bold(mitll.langtest.shared.flashcard.AVPHistoryForList.UserScore, String)
     * @return
     */
    public boolean isCurrent() {
      return isCurrent;
    }

    public int getIndex() {
      return index;
    }

    public String toString() { return "id " + index + " by " + user + " " + getScore() + (isCurrent() ? " current! " : ""); }
  }

  public String toString() {
    return "History " + numScores + " correct " + getTotalCorrect() + " pb correct " + getPbCorrect() +
      " class avg " + getClassAvg() + " scores " + scores;
  }
}
