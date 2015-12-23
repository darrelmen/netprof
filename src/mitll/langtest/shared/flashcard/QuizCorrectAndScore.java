package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Created by go22670 on 5/22/15.
 */
public class QuizCorrectAndScore implements IsSerializable {
  private boolean complete;
  private int total;
  private Collection<CorrectAndScore> correctAndScoreCollection;

  public QuizCorrectAndScore() {}

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoresForUser
   * @param complete
   * @param total
   * @param correctAndScoreCollection
   */
  public QuizCorrectAndScore(boolean complete, int total, Collection<CorrectAndScore> correctAndScoreCollection) {
    this.complete = complete;
    this.total = total;
    this.correctAndScoreCollection = correctAndScoreCollection;
  }

  public boolean isComplete() {
    return complete;
  }

  public int getTotal() { return total;  }

  public Collection<CorrectAndScore> getCorrectAndScoreCollection() {
    return correctAndScoreCollection;
  }
}
