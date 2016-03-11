/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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
  public static final String MEANING_PROMPT = "Meaning:";
  public static final String ENGLISH_PROMPT = "Translation:";
  public static final String TRANSLITERATION = "Transliteration:";
  public static final String CONTEXT = "Context:";
  public static final String CONTEXT_TRANSLATION = "Context Translation: ";

  /**
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#getExerciseContent
   * @param arabic
   * @return
   */
  public static String getArabic(String arabic) {
    return getArabic(arabic, false, false, false);
  }

  private static String getArabic(String arabic, boolean isUrdu, boolean isPashto, boolean includePrompt) {
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
