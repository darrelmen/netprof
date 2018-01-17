package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.logging.Logger;

/**
 * TODO : don't do sorting here on text
 * Created by go22670 on 3/22/17.
 */
class ListSorting<T extends CommonShell, U extends Shell> {
  public static final String LANG_ASC = "langASC";
  public static final String LANG_DSC = "langDSC";
  private final Logger logger = Logger.getLogger("ListSorting");

  private static final String ASCENDING = "ascending";
  private static final String DESCENDING = "descending";
  public static final String LIST_BOX_SETTING = "listBoxSetting";

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
    String langASC = getLangASC(language, ASCENDING);
    w1.addItem(langASC);
    String langDSC = getLangASC(language, DESCENDING);
    w1.addItem(langDSC);
    w1.addItem(LENGTH_SHORT_TO_LONG);
    w1.addItem(LENGTH_LONG_TO_SHORT);
    w1.addItem(SCORE_LOW_TO_HIGH);
    w1.addItem(SCORE_DSC);

    w1.addChangeHandler(event -> ListSorting.this.onChange(w1, langASC, langDSC));

//    makeDropDownReflectStoredValue(w1, langASC, langDSC);
    return w1;
  }

  /**
   * TODO : Can't do it yet - we need to do the sort on the request to the exercise service...
   *
   * @param w1
   * @param langASC
   * @param langDSC
   */
  private void makeDropDownReflectStoredValue(ListBox w1, String langASC, String langDSC) {
    String value = exerciseList.controller.getStorage().getValue(LIST_BOX_SETTING);

    if (value != null) {
      value = getNormValue(langASC, langDSC, value);
      w1.setSelectedValue(value);
      if (!w1.getSelectedValue().equalsIgnoreCase(value)) logger.warning("didn't set " + value);
      //  sortLater(w1, langASC, langDSC, value);
    }
  }

  private String getNormValue(String langASC, String langDSC, String value) {
    if (value.equalsIgnoreCase(LANG_ASC)) {
      value = langASC;
    } else if (value.equalsIgnoreCase(LANG_DSC)) {
      value = langDSC;
    }
    return value;
  }

  @NotNull
  private String getLangASC(String language, String ascending) {
    return language + " : " + ascending;
  }

  public void sortLater(ListBox w1, String lang) {
    if (w1.getSelectedIndex() != 0) {
      String langASC = getLangASC(lang, ASCENDING);
      String langDSC = getLangASC(lang, DESCENDING);
      final String fvalue = getNormValue(w1.getSelectedValue(), langASC, langDSC);

      sortByValue(fvalue, langASC, langDSC);

//      Scheduler.get().scheduleDeferred((Command) () -> sortByValue(fvalue, langASC, langDSC));
    }
  }

  private void onChange(ListBox w1, String langASC, String langDSC) {
    String selectedValue = w1.getSelectedValue();

    String toStore = selectedValue;
    if (toStore.equalsIgnoreCase(langASC)) {
      toStore = LANG_ASC;
    } else if (toStore.equalsIgnoreCase(langDSC)) {
      toStore = LANG_DSC;
    }
    exerciseList.controller.getStorage().storeValue(LIST_BOX_SETTING, toStore);

    sortByValue(selectedValue, langASC, langDSC);
  }

  private void sortByValue(String selectedValue, String langASC, String langDSC) {
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
    return Integer.compare(o1.getRawScore(), o2.getRawScore());
  }

  private void sortBy(Comparator<T> comp) {
    exerciseList.sortBy(comp);
  }

  private int compPhones(CommonShell o1, CommonShell o2) {
//    if (o1.getNumPhones() == 0) logger.warning("1 no phones for " +o1.getID());
//    if (o2.getNumPhones() == 0) logger.warning("2 no phones for " +o2.getID());
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
    return compareAgainLocale(o1.getForeignLanguage(), o2.getForeignLanguage(), locale);
  }

  private int compMeaning(CommonShell o1, CommonShell o2) {
    int i = o1.getMeaning().toLowerCase().compareTo(o2.getMeaning().toLowerCase());
    if (i == 0) i = compEnglish(o1, o2);
    return i;
  }

  private int compEnglish(CommonShell o1, CommonShell o2) {
    return o1.getEnglish().toLowerCase().compareTo(o2.getEnglish().toLowerCase());
  }
}
