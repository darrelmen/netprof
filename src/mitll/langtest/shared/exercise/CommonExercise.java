/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by GO22670 on 3/20/2014.
 */
public interface CommonExercise extends CommonShell, AudioAttributeExercise, AnnotationExercise, ScoredExercise {

  String getRefSentence();

  Collection<String> getRefSentences();

  CommonShell getShell();

  //  long getModifiedDateTimestamp();

  /**
   * @see mitll.langtest.server.database.ResultDAO#attachScoreHistory(long, CommonExercise, boolean)
   * @param scoreTotal
   */
  void setScores(List<CorrectAndScore> scoreTotal);

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotationsAndAudio
   * @param v
   */
  void setAvgScore(float v);

  List<String> getFirstPron();

  void setBagOfPhones(Set<String> bagOfPhones);
  void setFirstPron(List<String> phones);

  void setRefSentences(Collection<String> orDefault);

  String getRefAudioIndex();

  boolean isPredefined();
}
