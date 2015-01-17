package mitll.langtest.server;

import mitll.langtest.server.scoring.CollationSort;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;
import mitll.langtest.shared.custom.UserExercise;

import java.text.Collator;
import java.util.*;

/**
 * Created by GO22670 on 4/30/2014.
 */
public class ExerciseSorter {
  private Collection<String> typeOrder;
  private Map<String, Integer> phoneToCount;

  public ExerciseSorter(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  /**
   * @see mitll.langtest.server.ScoreServlet#doGet
   * @param typeOrder
   * @param phoneToCount
   */
  public ExerciseSorter(Collection<String> typeOrder, Map<String, Integer> phoneToCount) {
    this(typeOrder);
    this.phoneToCount = phoneToCount;
  }

  /**
   * what if the item has no unit/chapter info?
   * what if we have a mixed list of user and predef items?
   *
   * @param toSort
   * @param recordedLast
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
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
            i = getTypeOrder(o1, o2, i);

            // items in same chapter alphabetical by tooltip
            return (i == 0) ? tooltipComp(o1, o2) : i;
          }
        });
      } else {
        sortByTooltip(toSort);
      }
    }
  }

  public void getSortedByUnitThenPhone(List<? extends CommonExercise> toSort, final boolean recordedLast,
                                       final Map<String, Integer> phoneToCount, final boolean checkUnitAndChapter) {
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
      if (!allPredef) System.out.println("not all predef\n\n");
      else {
        //System.out.println("phoneToCount " + phoneToCount);
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
            if (checkUnitAndChapter) {
              i = getTypeOrder(o1, o2, i);
            }

            // items in same chapter alphabetical by tooltip
            return (i == 0) ? phoneCompFirst(o1, o2, phoneToCount) : i;
          }
        });
      } else {
        sortByTooltip(toSort);
      }
    }
  }

  public int phoneCompByFirst(CommonExercise o1, CommonExercise o2) {
    return phoneCompFirst(o1,o2,phoneToCount);
  }
  private int phoneCompFirst(CommonExercise o1, CommonExercise o2, final Map<String, Integer> phoneToCount) {
    List<String> bagOfPhones1 = o1.getFirstPron();
    List<String> bagOfPhones2 = o2.getFirstPron();
    if (bagOfPhones1 == null && bagOfPhones2 != null) return +1;
    if (bagOfPhones1 != null && bagOfPhones2 == null) return -1;
    if (bagOfPhones1 == null && bagOfPhones2 == null) {
      return o1.getForeignLanguage().toLowerCase().compareTo(o2.getForeignLanguage().toLowerCase());
    }
//    if (bagOfPhones2 == null) return +1;

    int o1Num = bagOfPhones1.size();
    int o2Num = bagOfPhones2.size();

    //  if (o1Num < o2Num) return -1;
    //  else if (o1Num > o2Num) return +1;
    //  else {

    int n = Math.min(o1Num, o2Num);
    for (int i = 0; i < n; i++) {
      String a = bagOfPhones1.get(i);
      String b = bagOfPhones2.get(i);
      if (!a.equals(b)) {
        Integer a1 = phoneToCount.get(a);
        Integer b1 = phoneToCount.get(b);
        if (a1 > b1) return -1;
        else if (a1 < b1) return +1;
        else return a.compareTo(b);
      }
    }

    if (o1Num < o2Num) return -1;
    else if (o1Num > o2Num) return +1;
    else {
      return o1.getForeignLanguage().toLowerCase().compareTo(o2.getForeignLanguage().toLowerCase());
    }
  }

  private int phoneComp(CommonExercise o1, CommonExercise o2, final Map<String, Integer> phoneToCount) {
    Set<String> bagOfPhones1 = o1.getBagOfPhones();
    int o1Num = bagOfPhones1.size();
    Set<String> bagOfPhones2 = o2.getBagOfPhones();
    int o2Num = bagOfPhones2.size();
    if (o1Num < o2Num) return -1;
    else if (o1Num > o2Num) return +1;
    else {
//        // e.g. a b c vs a b d or x y z
      List<String> seq1 = new ArrayList<String>(bagOfPhones1);
      Comparator<String> c = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return phoneToCount.get(o2).compareTo(phoneToCount.get(o1));
        }
      };
      Collections.sort(seq1, c);

      List<String> seq2 = new ArrayList<String>(bagOfPhones2);
      Collections.sort(seq2, c);

      for (int i = 0; i < seq1.size(); i++) {
        String a = seq1.get(i);
        String b = seq2.get(i);
        if (a.equals(b)) {
          // next
        } else {
          Integer a1 = phoneToCount.get(a);
          Integer b1 = phoneToCount.get(b);
          if (a1 > b1) return -1;
          else if (a1 < b1) return +1;
        }
      }

      return o1.getForeignLanguage().toLowerCase().compareTo(o2.getForeignLanguage().toLowerCase());
    }
  }

  private int getTypeOrder(CommonExercise o1, CommonExercise o2, int i) {
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

  public <T extends CommonExercise> void sortByForeign(List<T> exerciseShells, CollationSort sort) {  sort.sort(exerciseShells);  }

  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * NOTE:  be careful to use collation order when it's not "english-foreign language"
   * @param exerciseShells
   * @see
   */
  public <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
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
