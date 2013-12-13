package mitll.langtest.shared;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/7/13
 * Time: 7:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseFormatter {
  public static final String FOREIGN_LANGUAGE_PROMPT = "Say:";
  public static final String ENGLISH_PROMPT = "Meaning:";

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @see mitll.langtest.shared.custom.UserExercise#toExercise()
   * @param arabic
   * @param translit
   * @param english
   * @param meaning
   * @param language
   * @return
   */
  public static String getContent(String arabic, String translit, String english, String meaning, String language) {
    return getContent(arabic, translit, english, meaning, "",
      language.equalsIgnoreCase("english"),
      language.equalsIgnoreCase("urdu"),
      language.equalsIgnoreCase("pashto"));
  }

  public static String getContent(String arabic, String translit, String english, String meaning,
                                  boolean isEnglish, boolean isUrdu, boolean isPashto) {
    return getContent(arabic, translit, english, meaning, "", isEnglish, isUrdu, isPashto);
  }

  private static String getContent(String arabic, String translit, String english, String meaning, String context,
                                  boolean isEnglish, boolean isUrdu, boolean isPashto) {
    String arabicHTML = getArabic(arabic, isUrdu, isPashto);
    String translitHTML = translit.length() > 0 ?
      getSpanWrapper("Transliteration:", translit)
      : "";
    String translationHTML = english.length() > 0 ?//(!isEnglish && english.length() > 0) ?
      getSpanWrapper("Translation:", english) : "";
    String meaningHTML = (isEnglish && meaning.length() > 0) ?
      getSpanWrapper(ENGLISH_PROMPT, meaning) : "";

    String contextHTML = (context.length() > 0) ?
      getSpanWrapper("Context:", context) : "";
    return arabicHTML +
      translitHTML +
      translationHTML +
      meaningHTML +
      contextHTML;
  }

  private static String getSpanWrapper(String rowTitle, String english) {
    return "<div class=\"Instruction\">\n" +
      "<span class=\"Instruction-title\">" +
      rowTitle +
      "</span>\n" +
      "<span class=\"Instruction-data\"> " + english +
      "</span>\n" +
      "</div>";
  }

  public static String getArabic(String arabic, boolean isUrdu, boolean isPashto) {
    return "<div class=\"Instruction\">\n" +
      "<span class=\"Instruction-title\">" +
      FOREIGN_LANGUAGE_PROMPT +
      "</span>\n" +
      "<span class=\"" +
      (isUrdu ? "urdufont" : isPashto ? "pashtofont" : "Instruction-data") +
      "\"> " + arabic +
      "</span>\n" +
      "</div>\n";
  }
}
