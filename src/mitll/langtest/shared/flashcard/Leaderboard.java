package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/5/13
 * Time: 7:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class Leaderboard implements IsSerializable {
  public Map<Integer,List<ScoreInfo>> selectionToScores = new HashMap<Integer,List<ScoreInfo>>();

  public Leaderboard() {}

  public void addScore(ScoreInfo score) {
    int hash = getHashForSelection(score);

    List<ScoreInfo> scoreInfos = selectionToScores.get(hash);
    if (scoreInfos == null) selectionToScores.put(hash, scoreInfos = new ArrayList<ScoreInfo>());
    scoreInfos.add(score);
  }

  private int getHashForSelection(ScoreInfo score) {
    Map<String, Collection<String>> selection = score.selection;

    return getHash(selection);
  }

  public int getHash(Map<String, Collection<String>> selection) {
    int hash = 0;
    for (Map.Entry<String, Collection<String>> pair : selection.entrySet()) {
      hash += pair.getKey().hashCode();
      for (String value : pair.getValue()) hash += value.hashCode();
    }
    return hash;
  }

/*  public ScoreInfo getPrevious(long userid, Map<String, Collection<String>> selection) {
    long latest = 0;
    ScoreInfo lastScore = null;
    List<ScoreInfo> scores = getScores(selection);
    if (scores == null) return null;
    for (ScoreInfo score : scores) {
      if (score.userid == userid && score.timestamp > latest) {
         lastScore = score;
      }
    }
    return lastScore;
  }*/

  public List<ScoreInfo> getScores(Map<String, Collection<String>> selection) {
    return selectionToScores.get(getHash(selection));
  }

}
