package mitll.langtest.server.sorter;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by GO22670 on 4/30/2014.
 */
public class ExerciseSorter extends SimpleSorter {
  private static final Logger logger = Logger.getLogger(ExerciseSorter.class);

  private Map<String, Integer> phoneToCount;

  /**
   * @see LangTestDatabaseImpl#sortExercises(String, List)
   * @param typeOrder
   */
  public ExerciseSorter(Collection<String> typeOrder) { super(typeOrder); }

  /**
   * @param typeOrder
   * @param phoneToCount
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
  public ExerciseSorter(Collection<String> typeOrder, Map<String, Integer> phoneToCount) {
    this(typeOrder);
    this.phoneToCount = phoneToCount;
  }

  /**
   * @see ScoreServlet#getJsonForSelection(Map)
   * @param toSort
   * @param phoneToCount
   */
  public void sortedByPronLengthThenPhone(List<? extends CommonExercise> toSort,final Map<String, Integer> phoneToCount) {
    Collections.sort(toSort, new Comparator<CommonExercise>() {
      @Override
      public int compare(CommonExercise o1, CommonExercise o2) {
        // items in same chapter alphabetical by tooltip
        return phoneCompFirst(o1, o2, phoneToCount);
      }
    });
  }

    /**
     * @param o1
     * @param o2
     * @return
     * @see mitll.langtest.server.database.ResultDAO#getSortedAVPHistoryByPhones
     */
  public int phoneCompByFirst(CommonExercise o1, CommonExercise o2) { return phoneCompFirst(o1, o2, phoneToCount);  }

  /**
   * Compare and put shorter pronunciations before longer ones.
   * When they are the same length, compare phones, and when you get to a difference, use their relative occurrence
   * frequency, with rarer phones first.
   * If the phones have the same frequency, compare the phone names (ideally this is unlikely).
   * @param o1
   * @param o2
   * @param phoneToCount
   * @return
   */
  private int phoneCompFirst(CommonExercise o1, CommonExercise o2, final Map<String, Integer> phoneToCount) {
    List<String> pron1 = o1.getFirstPron();
    List<String> pron2 = o2.getFirstPron();

    // these cases should never happen - really defensive
    if (pron1 == null && pron2 != null) {
      logger.warn("missing pron?");
      return +1;
    }
    if (pron1 != null && pron2 == null) {
      logger.warn("missing pron?");
      return -1;
    }
    if (pron1 == null) {
      logger.warn("missing pron?");
      return o1.getForeignLanguage().toLowerCase().compareTo(o2.getForeignLanguage().toLowerCase());
    }

    int o1Num = pron1.size();
    int o2Num = pron2.size();

    int comp = new Integer(o1Num).compareTo(o2Num);
    if (comp != 0) {
      return comp;
    } else {
      int n = Math.min(o1Num, o2Num);
      for (int i = 0; i < n; i++) {
        String a = pron1.get(i);
        String b = pron2.get(i);
        if (!a.equals(b)) {
          Integer a1 = phoneToCount.get(a);
          Integer b1 = phoneToCount.get(b);
          int compt = a1.compareTo(b1);
          return compt == 0 ? a.compareTo(b) :compt;
        }
      }
      return 0;
    }
  }

}