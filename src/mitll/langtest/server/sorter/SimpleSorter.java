package mitll.langtest.server.sorter;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts by chapters, then by tooltip...
 * Created by go22670 on 10/9/15.
 */
public class SimpleSorter extends ExerciseComparator {
  public SimpleSorter(Collection<String> typeOrder) {super(typeOrder); }

  /**
   * what if the item has no unit/chapter info?
   * what if we have a mixed list of user and predef items?
   *
   * @param toSort
   * @param recordedLast
   * @return
   * @see LangTestDatabaseImpl#sortExercises(String, List)
   */
  public void getSortedByUnitThenAlpha(List<? extends CommonExercise> toSort, final boolean recordedLast) {
    if (typeOrder.isEmpty()) {
      sortByTooltip(toSort);
    } else {
/*      boolean allPredef = true;
      // first check if we have a homogenous predefined exercise list
      for (CommonExercise commonExercise : toSort) {
        if (!new UserExercise(commonExercise).isPredefined()) {
          allPredef = false;
          break;
        }
      }
      if (allPredef) {*/
        Collections.sort(toSort, new Comparator<CommonExercise>() {
          @Override
          public int compare(CommonExercise o1, CommonExercise o2) {
           // return SimpleSorter.this.compare(o1, o2, recordedLast);
            return SimpleSorter.this.simpleCompare(o1, o2, recordedLast);
          }
        });
/*      } else {
        sortByTooltip(toSort);
      }*/
    }
  }


  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * NOTE:  be careful to use collation order when it's not "english-foreign language"
   *
   * @param exerciseShells
   * @see mitll.langtest.server.database.AudioExport#writeZip(OutputStream, String, SectionHelper, Collection, String, AudioDAO, String, String, boolean)
   * @see mitll.langtest.server.database.AudioExport#writeZipJustOneAudio(OutputStream, SectionHelper, Collection, String)
   */
  public <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
    Collections.sort(exerciseShells, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return tooltipComp(o1, o2);
      }
    });
  }

}
