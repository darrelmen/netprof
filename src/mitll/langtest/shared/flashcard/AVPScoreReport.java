/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * Created by go22670 on 9/13/14.
 */
public class AVPScoreReport implements IsSerializable {
  private List<AVPHistoryForList> avpHistoryForLists;
  private List<ExerciseCorrectAndScore> sortedHistory;

  public AVPScoreReport() {}
  public AVPScoreReport(List<AVPHistoryForList> historyForLists,
                        List<ExerciseCorrectAndScore> sortedHistory) {
    this.avpHistoryForLists = historyForLists;
    this.sortedHistory=sortedHistory;
  }

  public List<AVPHistoryForList> getAvpHistoryForLists() {
    return avpHistoryForLists;
  }

  public List<ExerciseCorrectAndScore> getSortedHistory() {
    return sortedHistory;
  }
}
