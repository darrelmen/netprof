package mitll.langtest.server;

import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;
import mitll.langtest.shared.custom.UserExercise;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by GO22670 on 4/30/2014.
 */
public class ExerciseSorter {
  private Collection<String> typeOrder;

  public ExerciseSorter(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  /**
   * what if the item has no unit/chapter info?
   * what if we have a mixed list of user and predef items?
   *
   * @param toSort
   * @param recordedLast
   * @return
   */
  public void getSortedByUnitThenAlpha(List<? extends CommonExercise> toSort, final boolean recordedLast) {
    if (typeOrder.isEmpty()) {
      sortByTooltip(toSort);
    } else {
      boolean allPredef = true;
      // first check if we have a homogenous predefined exercise list
      for (CommonExercise commonExercise : toSort) {
        if (!new UserExercise(commonExercise).isPredefined()) {
          allPredef = false;
          break;
        }
      }
      if (allPredef) {
        Collections.sort(toSort, new Comparator<CommonExercise>() {
          @Override
          public int compare(CommonExercise o1, CommonExercise o2) {
            int i = 0;
            if (recordedLast) {
              if (o1.getState() != STATE.RECORDED && o2.getState() == STATE.RECORDED) {
                return +1;
              } else if (o1.getState() == STATE.RECORDED && o2.getState() != STATE.RECORDED) {
                return -1;
              }
            }

            // compare first by hierarchical order - unit, then chapter, etc.
            for (String type : typeOrder) {
              String type1 = o1.getUnitToValue().get(type);
              String type2 = o2.getUnitToValue().get(type);
              boolean t1Null = type1 == null;
              boolean t2Null = type2 == null;
              if (t1Null) {
                i = t2Null ? 0 : +1;
              } else if (t2Null) {
                i = -1;
              } else {
                i = type1.compareTo(type2);
              }

              if (i != 0) {
                break;
              }
            }

            // items in same chapter alphabetical by tooltip
            return (i == 0) ? tooltipComp(o1, o2) : i;
          }
        });
      } else {
        sortByTooltip(toSort);
      }
    }
  }

  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * @param exerciseShells
   */
  public  <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
    Collections.sort(exerciseShells, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return tooltipComp(o1, o2);
      }
    });
  }

  protected <T extends CommonShell> int tooltipComp(T o1, T o2) {
    String id1 = o1.getTooltip();
    String id2 = o2.getTooltip();
    return id1.toLowerCase().compareTo(id2.toLowerCase());
  }

}
