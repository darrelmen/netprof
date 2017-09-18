package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ListBox;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.Comparator;

/**
 * Created by go22670 on 3/22/17.
 */
public class ListSorting<T extends CommonShell, U extends Shell> {
  private final PagingExerciseList<T, U> exerciseList;
  private final String locale;

  private static final String ENGLISH_ASC = "English : A-Z";
  private static final String MEANING_ASC = "Meaning : A-Z";
  private static final String ENGLISH_DSC = "English : Z-A";
  private static final String MEANING_DSC = "Meaning : Z-A";
  private static final String LENGTH_SHORT_TO_LONG = "Length : short to long";
  private static final String LENGTH_LONG_TO_SHORT = "Length : long to short";
  private static final String SCORE_LOW_TO_HIGH = "Score : low to high";
  private static final String SCORE_DSC = "Score : high to low";

  public static final int MAX_TO_SHOW = 4;
  public static final String ANY = "Any";
  public static final String MENU_ITEM = "menuItem";

  /**
   * @param exerciseList
   * @see FacetExerciseList#addSortBox
   */
  ListSorting(PagingExerciseList<T, U> exerciseList) {
    this.exerciseList = exerciseList;
    ProjectStartupInfo projectStartupInfo = exerciseList.controller.getProjectStartupInfo();
    locale = projectStartupInfo == null ? "" : projectStartupInfo.getLocale();
  }

  /**
   * @param language
   * @return
   * @see FacetExerciseList#addSortBox
   */
  ListBox getSortBox(String language) {
    ListBox w1 = new ListBox();

    boolean isEnglish = language.equalsIgnoreCase("English");
    w1.addItem(isEnglish ? MEANING_ASC : ENGLISH_ASC);
    w1.addItem(isEnglish ? MEANING_DSC : ENGLISH_DSC);
    String langASC = language + " : ascending";
    w1.addItem(langASC);
    String langDSC = language + " : descending";
    w1.addItem(langDSC);
    w1.addItem(LENGTH_SHORT_TO_LONG);
    w1.addItem(LENGTH_LONG_TO_SHORT);
    w1.addItem(SCORE_LOW_TO_HIGH);
    w1.addItem(SCORE_DSC);

    w1.addChangeHandler(event -> ListSorting.this.onChange(w1, langASC, langDSC));

    return w1;
  }

  private void onChange(ListBox w1, String langASC, String langDSC) {
    String selectedValue = w1.getSelectedValue();
    if (selectedValue.equals(LENGTH_SHORT_TO_LONG)) {
      sortBy((o1, o2) -> compareShells(o1, o2, compPhones(o1, o2)));
    } else if (selectedValue.equals(LENGTH_LONG_TO_SHORT)) {
      sortBy((o1, o2) -> compareShells(o1, o2, -1 * compPhones(o1, o2)));
    } else if (selectedValue.equals(ENGLISH_ASC)) {
      sortBy(this::compEnglish);
    } else if (selectedValue.equals(ENGLISH_DSC)) {
      sortBy((o1, o2) -> -1 * compEnglish(o1, o2));
    } else if (selectedValue.equals(MEANING_ASC)) {
      sortBy(this::compMeaning);
    } else if (selectedValue.equals(MEANING_DSC)) {
      sortBy((o1, o2) -> -1 * compMeaning(o1, o2));
    } else if (selectedValue.equals(langASC)) {
      sortBy(this::compForeign);
    } else if (selectedValue.equals(langDSC)) {
      sortBy((o1, o2) -> -1 * compForeign(o1, o2));
    } else if (selectedValue.equals(SCORE_LOW_TO_HIGH)) {
      sortBy((o1, o2) -> {
        int i = compareScores(o1, o2);
        return compareShells(o1, o2, i);
      });
    } else if (selectedValue.equals(SCORE_DSC)) {
      sortBy((o1, o2) -> {
        int i = -1 * compareScores(o1, o2);
        return compareShells(o1, o2, i);
      });
    }
  }

  private int compareScores(T o1, T o2) {
    return Float.valueOf(o1.getScore()).compareTo(o2.getScore());
  }

  private void sortBy(Comparator<T> comp) {
//    Scheduler.get().scheduleDeferred(new Command() {
//      public void execute() {
    exerciseList.waitCursorHelper.scheduleWaitTimer();
    exerciseList.sortBy(comp);
    exerciseList.waitCursorHelper.showFinished();
//      }
    //  });
  }

  private int compPhones(CommonShell o1, CommonShell o2) {
    return Integer.compare(o1.getNumPhones(), o2.getNumPhones());
  }

  private int compareShells(CommonShell o1, CommonShell o2, int i) {
    if (i == 0) i = compForeign(o1, o2);
    if (i == 0) i = compEnglish(o1, o2);
    return i;
  }

  public native int compare(String source, String target); /*-{
      return source.localeCompare(target);
  }-*/

  public native int compareWithLocale(String source, String target, String locale); /*-{
      return source.localeCompare(target, locale);
  }-*/


  public static native int compareAgain(String source, String target) /*-{
      return source.localeCompare(target);
  }-*/;

  public static native int compareAgainLocale(String source, String target, String locale) /*-{
      return source.localeCompare(target);
  }-*/;

  private int compForeign(CommonShell o1, CommonShell o2) {
    //   return o1.getForeignLanguage().compareTo(o2.getForeignLanguage());
    //  return compareWithLocale(o1.getForeignLanguage(), o2.getForeignLanguage(), locale);
    // return compareAgain(o1.getForeignLanguage(), o2.getForeignLanguage());
    return compareAgainLocale(o1.getForeignLanguage(), o2.getForeignLanguage(), locale);
  }

  private int compEnglish(CommonShell o1, CommonShell o2) {
    return o1.getEnglish().toLowerCase().compareTo(o2.getEnglish().toLowerCase());
  }

  private int compMeaning(CommonShell o1, CommonShell o2) {
    return o1.getMeaning().toLowerCase().compareTo(o2.getMeaning().toLowerCase());
  }

}
