package mitll.langtest.client.analysis;

import mitll.langtest.shared.exercise.CommonShell;

public interface ExerciseLookup {
  CommonShell getShell(int id);
  boolean isKnown(int exid);
}
