package mitll.langtest.shared.sorter;

import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;

import java.util.Collection;

/**
 * Created by go22670 on 10/9/15.
 */
public class ExerciseComparator {
  protected final Collection<String> typeOrder;
  public ExerciseComparator(Collection<String> typeOrder) { this.typeOrder = typeOrder; }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
  }

  public int simpleCompare(CommonShell o1, CommonShell o2, boolean recordedLast) {
    if (recordedLast) {
      if (o1.getState() != STATE.RECORDED && o2.getState() == STATE.RECORDED) {
        return +1;
      } else if (o1.getState() == STATE.RECORDED && o2.getState() != STATE.RECORDED) {
        return -1;
      }
    }

    // items in same chapter alphabetical by tooltip
    return tooltipComp(o1, o2);
  }

  public int compare(CommonExercise o1, CommonExercise o2, boolean recordedLast) {
    if (recordedLast) {
      if (o1.getState() != STATE.RECORDED && o2.getState() == STATE.RECORDED) {
        return +1;
      } else if (o1.getState() == STATE.RECORDED && o2.getState() != STATE.RECORDED) {
        return -1;
      }
    }

    // compare first by hierarchical order - unit, then chapter, etc.
    int i = getTypeOrder(o1, o2);

    // items in same chapter alphabetical by tooltip
    return (i == 0) ? tooltipComp(o1, o2) : i;
  }

  private int getTypeOrder(CommonExercise o1, CommonExercise o2) {
    int i = 0;
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
    return i;
  }

  protected <T extends CommonShell> int tooltipComp(T o1, T o2) {
    String id1 = o1.getTooltip();
    String id2 = o2.getTooltip();
    return compareStrings(id1, id2);
  }

  public int compareStrings(String id1, String id2) {
    String t = id1.toLowerCase();
    if (t.startsWith("a ")) t = t.substring(2);
    String t1 = id2.toLowerCase();
    if (t1.startsWith("a ")) t1 = t1.substring(2);
    return removePunct(t).compareTo(removePunct(t1));
  }
}
