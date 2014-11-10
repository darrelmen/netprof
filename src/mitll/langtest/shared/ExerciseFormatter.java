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
  public static final String TRANSLITERATION = "Transliteration:";
  public static final String TRANSLATION = "Translation:";
  public static final String CONTEXT = "Context:";
  public static final String CONTEXT_TRANSLATION = "Context Translation: ";

  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @see mitll.langtest.shared.custom.UserExercise#toExercise()
   * @param foreignPhrase
   * @param translit
   * @param english
   * @param meaning
   * @param language
   * @return
   */
  public static String getContent(String foreignPhrase, String translit, String english, String meaning, String context, String contextTranslation, String language) {
    return getContent(foreignPhrase, translit, english, meaning, context, contextTranslation,
      language.equalsIgnoreCase("english"),
      language.equalsIgnoreCase("urdu"),
      language.equalsIgnoreCase("pashto"));
  }

  public static String getContent(String foreignPhrase, String translit, String english, String meaning,
                                  boolean isEnglish, boolean isUrdu, boolean isPashto) {
    return getContent(foreignPhrase, translit, english, meaning, "", "", isEnglish, isUrdu, isPashto);
  }

  /**
   * TODO : This is a bad idea -- the client side should do formatting, etc.
   *
   * @param foreignPhrase
   * @param translit
   * @param english
   * @param meaning
   * @param context
   * @param isEnglish
   * @param isUrdu
   * @param isPashto
   * @return
   */
  private static String getContent(String foreignPhrase, String translit, String english, String meaning, String context,
                                  String contextTranslation, boolean isEnglish, boolean isUrdu, boolean isPashto) {
    String arabicHTML = getArabic(foreignPhrase, isUrdu, isPashto, false);
    String translitHTML = translit.length() > 0 ? getSpanWrapper(TRANSLITERATION, translit, false) : "";

    String translationHTML = english != null && english.length() > 0 && !english.equals(foreignPhrase) ?
      getSpanWrapper(TRANSLATION, english, false) : "";

    String meaningHTML = (isEnglish && meaning.length() > 0) ? getSpanWrapper(ENGLISH_PROMPT, meaning, false) : "";

    String contextHTML = (context.length() > 0) ? getSpanWrapper(CONTEXT, "\""+ context + "\"", false) : "";
    
    String contextTranslationHTML = (contextTranslation.length() > 0) ? getSpanWrapper(CONTEXT_TRANSLATION, "\""+ contextTranslation + "\"", false) : "";

    return arabicHTML +
      translitHTML +
      translationHTML +
      meaningHTML +
      contextHTML +
      contextTranslationHTML;
  }

  private static String getSpanWrapper(String rowTitle, String english, boolean includePrompt) {
    String prompt = includePrompt ? "<span class=\"Instruction-title\">" +
      rowTitle +
      "</span>\n" : "";
    String textClasses = "\"Instruction-data" + (rowTitle.equals(CONTEXT) ? " englishFont" : "")+ "\"";
    return "<div class=\"Instruction\">\n" +
      prompt +
      "<span class=" + textClasses + "> " + english +
      "</span>\n" +
      "</div>";
  }

  public static String getArabic(String arabic) {
    return getArabic(arabic, false, false, false);
  }

  public static String getArabic(String arabic, boolean isUrdu, boolean isPashto, boolean includePrompt) {
    String prompt = includePrompt ? "<span class=\"Instruction-title\">" +
      FOREIGN_LANGUAGE_PROMPT +
      "</span>\n" : "";
    return "<div class=\"Instruction\">\n" +
      prompt +
      "<span class=\"" +
      (isUrdu ? "urdufont" : isPashto ? "pashtofont" : "Instruction-data") +
      "\"> " + arabic +
      "</span>\n" +
      "</div>\n";
  }
}
