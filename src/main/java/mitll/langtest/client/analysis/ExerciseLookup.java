package mitll.langtest.client.analysis;

import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonShell;

public interface ExerciseLookup {
  CommonShell getShell(int id);

  WordScore getAnswerPath(int exid, long timestamp);
}
