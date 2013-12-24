package mitll.langtest.shared;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/7/13
 * Time: 7:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseFormatter {
  public static String getContent(String arabic, String translit, String english, String meaning, String context, String language) {
    return getContent(arabic, translit, english, meaning, context, language.equalsIgnoreCase("english"), language.equalsIgnoreCase("urdu"), language.equalsIgnoreCase("pashto"));
  }

  public static String getContent(String arabic, String translit, String english, String meaning, boolean isEnglish, boolean isUrdu, boolean isPashto) {
    return getContent(arabic, translit, english, meaning, "", isEnglish, isUrdu, isPashto);
  }

  public static String getContent(String arabic, String translit, String english, String meaning, String context, boolean isEnglish, boolean isUrdu, boolean isPashto) {
    String arabicHTML = getArabic(arabic, isUrdu, isPashto);
    String translitHTML = translit.length() > 0 ?
      getSpanWrapper("Transliteration:", translit)
      : "";
    String translationHTML = english.length() > 0 ?
      getSpanWrapper("Translation:", english) : "";
    String meaningHTML = (isEnglish && meaning.length() > 0) ?
      getSpanWrapper("Meaning:", meaning) : "";

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
      "<span class=\"Instruction-title\">Say:</span>\n" +
      "<span class=\"" +
      (isUrdu ? "urdufont" : isPashto ? "pashtofont" : "Instruction-data") +
      "\"> " + arabic +
      "</span>\n" +
      "</div>\n";
  }
}
