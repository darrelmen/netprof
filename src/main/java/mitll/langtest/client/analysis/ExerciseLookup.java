package mitll.langtest.client.analysis;

import mitll.langtest.shared.exercise.CommonShell;

public interface ExerciseLookup<T extends CommonShell> {
  T getShell(int id);
  boolean isKnown(int exid);
}
