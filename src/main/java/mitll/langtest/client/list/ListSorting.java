package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ListBox;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.Scored;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * TODO : don't do sorting here on text
 * Created by go22670 on 3/22/17.
 */
class ListSorting<T extends CommonShell & Scored, U extends HasID> {
  private final Logger logger = Logger.getLogger("ListSorting");

  private static final String LANG_ASC = "langASC";
  private static final String LANG_DSC = "langDSC";

  private static final String ASCENDING = "ascending";
  private static final String DESCENDING = "descending";

  private final PagingExerciseList<T, U> exerciseList;
  private final String locale;

  private static final String NATURAL_ORDER = "Natural Order";
  private static final String ENGLISH_ASC = "English : A-Z";
  private static final String MEANING_ASC = "Meaning : A-Z";
  private static final String ENGLISH_DSC = "English : Z-A";
  private static final String MEANING_DSC = "Meaning : Z-A";

  private static final String LENGTH_SHORT_TO_LONG = "Length : short to long";
  private static final String LENGTH_LONG_TO_SHORT = "Length : long to short";

  private static final String SCORE_LOW_TO_HIGH = "Score : low to high";
  private static final String SCORE_DSC = "Score : high to low";
  private static final String SHUFFLE = "Shuffle";
  private static final String LIST_BOX_SETTING = "listBoxSetting";

  private String language;
  private Language languageInfo;
  private final String keyForSorting;

  private static final boolean DEBUG = false;

  /**
   * @param exerciseList
   * @param view
   * @see FacetExerciseList#addSortBox
   */
  ListSorting(PagingExerciseList<T, U> exerciseList, INavigation.VIEWS view) {
    this.exerciseList = exerciseList;
    keyForSorting = LIST_BOX_SETTING + "_" + view.toString();

  //  logger.info("ListSorting: key " + keyForSorting);
    ProjectStartupInfo projectStartupInfo = exerciseList.controller.getProjectStartupInfo();
    if (projectStartupInfo != null) {
      language = exerciseList.controller.getLanguage();
      languageInfo = exerciseList.controller.getLanguageInfo();
    }
    locale = projectStartupInfo == null ? "" : projectStartupInfo.getLocale();
  }

  /**
   * @return
   * @see FacetExerciseList#addSortBox
   */
  ListBox getSortBox() {
    ListBox w1 = new ListBox();

    if (language != null) {
      boolean isEnglish = languageInfo == Language.ENGLISH;
      w1.addItem(NATURAL_ORDER);
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
      w1.addItem(SHUFFLE);

      w1.addChangeHandler(event -> ListSorting.this.onChange(w1, langASC, langDSC));

      makeDropDownReflectStoredValue(w1, langASC, langDSC);
    }

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
    String value = getStoredValue();

    //   logger.info("makeDropDownReflectStoredValue value is " + value);
    if (!value.isEmpty()) {
      value = getNormValue(value, langASC, langDSC);
      //   logger.info("makeDropDownReflectStoredValue norm value is " + value);
      w1.setSelectedValue(value);
      if (!w1.getSelectedValue().equalsIgnoreCase(value)) logger.warning("didn't set " + value);
      //  sortLater(w1, langASC, langDSC, value);
    }
  }

  /**
   * @param toSort
   * @param w1
   * @see FacetExerciseList#resort
   */
  void sortLater(List<T> toSort, ListBox w1) {
    if (w1.getSelectedIndex() != 0 && (toSort.size() > 1)) {
      String langASC = getLangASC(language, ASCENDING);
      String langDSC = getLangASC(language, DESCENDING);
      String selectedValue = w1.getSelectedValue();  // not sure how this can be null but saw it in an exception

      final String fvalue = selectedValue == null ? langASC : getNormValue(selectedValue, langASC, langDSC);
      // logger.info("sortLater sort with" + w1.getSelectedValue() + " norm " + fvalue);
      sortByValue(toSort, fvalue, langASC, langDSC);
    }
//      Scheduler.get().scheduleDeferred((Command) () -> sortByValue(fvalue, langASC, langDSC));
  }

  private String getNormValue(String value, String langASC, String langDSC) {
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

  private void onChange(ListBox w1, String langASC, String langDSC) {
    String selectedValue = w1.getSelectedValue();

    String toStore = selectedValue;
    if (toStore.equalsIgnoreCase(langASC)) {
      toStore = LANG_ASC;
    } else if (toStore.equalsIgnoreCase(langDSC)) {
      toStore = LANG_DSC;
    }
    storeValue(toStore);
    if (DEBUG) logger.info("START onChange Sort by " + selectedValue + " to sort is null ");

    sortByValue(null, selectedValue, langASC, langDSC);
    if (DEBUG) logger.info("END   onChange Sort by " + selectedValue + " to sort is null ");
  }

  private void storeValue(String toStore) {
    if (DEBUG)  logger.info("store " + keyForSorting + " = " + toStore);
    getStorage().storeValue(keyForSorting, toStore);
  }

  private String getStoredValue() {
    String value = getStorage().getValue(keyForSorting);
    if (DEBUG) logger.info("get   " + keyForSorting + " = " + value);
    return value;
  }

  private KeyStorage getStorage() {
    return exerciseList.controller.getStorage();
  }

  /**
   * @param toSort
   * @param selectedValue
   * @param langASC
   * @param langDSC
   * @see #sortLater(List, ListBox)
   */
  private void sortByValue(List<T> toSort, String selectedValue, String langASC, String langDSC) {
    if (DEBUG) logger.info("START Sort by " + selectedValue + " to sort is null " + (toSort == null));

    if (selectedValue.equals(LENGTH_SHORT_TO_LONG)) {
      sortBy(toSort, (o1, o2) -> compareShells(o1, o2, compPhones(o1, o2)));
    } else if (selectedValue.equals(LENGTH_LONG_TO_SHORT)) {
      sortBy(toSort, (o1, o2) -> compareShells(o1, o2, -1 * compPhones(o1, o2)));
    } else if (selectedValue.equals(NATURAL_ORDER)) {
      sortBy(toSort, null);
    } else if (selectedValue.equals(ENGLISH_ASC)) {
      sortBy(toSort, this::compEnglish);
    } else if (selectedValue.equals(ENGLISH_DSC)) {
      sortBy(toSort, (o1, o2) -> -1 * compEnglish(o1, o2));
    } else if (selectedValue.equals(MEANING_ASC)) {
      sortBy(toSort, this::compMeaning);
    } else if (selectedValue.equals(MEANING_DSC)) {
      sortBy(toSort, (o1, o2) -> -1 * compMeaning(o1, o2));
    } else if (selectedValue.equals(langASC)) {
      sortBy(toSort, this::compForeign);
    } else if (selectedValue.equals(langDSC)) {
      sortBy(toSort, (o1, o2) -> -1 * compForeign(o1, o2));
    } else if (selectedValue.equals(SCORE_LOW_TO_HIGH)) {
      sortBy(toSort, (o1, o2) -> compareShells(o1, o2, compareScores(o1, o2)));
    } else if (selectedValue.equals(SCORE_DSC)) {
      sortBy(toSort, (o1, o2) -> compareShells(o1, o2, -1 * compareScores(o1, o2)));
    } else if (selectedValue.equals(SHUFFLE)) {
      sortBy(toSort, this::compRand);
    } else {
      logger.warning("sortByValue huh? no match " + selectedValue);
    }

    if (toSort == null) {
      if (DEBUG) logger.info("FINISH Sort by " + selectedValue + " to sort is null ");
    } else {
      if (DEBUG) logger.info("FINISH Sort by " + selectedValue + " to sort has " + (toSort.size()));
    }
  }

  private final Random random = new Random();

  private int compRand(T o1, T o2) {
    int i = random.nextInt(2);
    return (i == 0) ? -1 : 1;
  }

  /**
   * Sort -1 items after un-practiced items.
   *
   * @param o1
   * @param o2
   * @return
   */
  private int compareScores(T o1, T o2) {
    int rawScore1 = o1.getRawScore();
    int rawScore2 = o2.getRawScore();

    if (rawScore1 == -1 && rawScore2 == -1) {
      return 0;
    } else {
//    if (rawScore == -1 && rawScore2 -)
   //  logger.info("compareScores o1 " +o1.getID() + " " + rawScore1 + " vs " + o2.getID() + " " + rawScore2);

      if (rawScore1 == -1 && rawScore2 == 0) return +1;
      else if (rawScore1 == 0 && rawScore2 == -1) return -1;
      else return Integer.compare(rawScore1, rawScore2);
    }
  }

  /**
   * @param toSort
   * @param comp
   * @see
   */
  private void sortBy(List<T> toSort, Comparator<T> comp) {
    if (toSort == null) {
      exerciseList.flushWith(comp);
    } else {
      toSort.sort(comp);
    }
  }

  private int compPhones(CommonShell o1, CommonShell o2) {
    if (o1.getNumPhones() == 0) logger.info("1 no phones for " + o1.getID());
    if (o2.getNumPhones() == 0) logger.info("2 no phones for " + o2.getID());
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
