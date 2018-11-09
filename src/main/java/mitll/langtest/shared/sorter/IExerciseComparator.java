package mitll.langtest.shared.sorter;

import mitll.langtest.shared.exercise.CommonShell;

public interface IExerciseComparator {
  int simpleCompare(CommonShell o1, CommonShell o2, boolean sortByFL, String searchTerm);
}
