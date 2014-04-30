package mitll.langtest.server;

import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by GO22670 on 4/30/2014.
 */
public class ExerciseSorter {
  Collection<String> typeOrder;

  public ExerciseSorter(Collection<String> typeOrder) {this.typeOrder = typeOrder;}
  /**
   * what if the item has no unit/chapter info?
   * what if we have a mixed list of user and predef items?
   *
   * @param toSort
   * @param recordedLast
   * @return
   */
  public void
  //List<CommonExercise>
  getSortedByUnitThenAlpha(List<? extends CommonExercise> toSort, //final Collection<String> typeOrder,
                                                        final boolean recordedLast) {
    //List<CommonExercise> copy = new ArrayList<CommonExercise>(toSort);

    //logger.debug("sorting " + toSort.size() + " recorded last " +recordedLast);
   // final Collection<String> typeOrder = getTypeOrder();
    if (typeOrder.isEmpty()) {
      sortByTooltip(toSort);
     // return copy;
    }
    else {
      boolean allPredef = true;
      // first check if we have a homogenous predefined exercise list
      for (CommonExercise commonExercise : toSort) {
        if (!commonExercise.toCommonUserExercise().isPredefined()) {
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
              i = type1.compareTo(type2);
              if (i != 0) {
                break;
              }
            }

            // items in same chapter alphabetical by tooltip
            if (i == 0) {
              return tooltipComp(o1, o2);
            } else {
              return i;
            }
          }
        });
      } else {
        sortByTooltip(toSort);
      }
     // return copy;
    }
  }

  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * @param exerciseShells
   */
  private  <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
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
