package mitll.langtest.server.scoring;

import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class TextNormalizer {
  private static final Logger logger = LogManager.getLogger(TextNormalizer.class);

  static final String ONE_SPACE = " ";
  private static final String OE = "oe";
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char ZERO = '0';
  private static final String TURKISH_CAP_I = "İ";

  /**
   * @see #getTrimmed(String)
   * @see #lcToken
   * <p>
   * remove latin capital letter i with dot above - 0130
   * 0x2026 = three dot elipsis
   * FF01 = full width exclamation
   * FF1B - full semi
   * 002D - hyphen
   */
  private static final String REMOVE_ME = "[\\u0130\\u2022\\u2219\\u2191\\u2193\\u2026\\uFF01\\uFF1B\\u002D;~/']";
  private static final String REPLACE_ME_OE = "[\\u0152\\u0153]";

  private static final String P_Z = "\\p{Z}+";
  /**
   * @see #getTrimmedLeaveAccents
   */
  private static final String FRENCH_PUNCT = "[,.?!]";

  private static final String P_P = "\\p{P}";
  private static final String INTERNAL_PUNCT_REGEX = "(?:(?<!\\S)\\p{Punct}+)|(?:\\p{Punct}+(?!\\S))";

  private static final boolean DEBUG = false;
  private static final String NORMAL_I = "I";


  private final Pattern punctCleaner;
  private final Pattern tickPattern;
  private final Pattern internalPunctPattern;
  private final Pattern frenchPunct;
  private final Pattern spacepattern;
  private final Pattern oepattern;
  private final Language language;

  public TextNormalizer(Language language) {
    this.language = language;
    // and leading upside down question marks...
    tickPattern = Pattern.compile("['’\\u00bf]");  // "don't" or "mustn't"
    punctCleaner = Pattern.compile(REMOVE_ME + "|" + P_P);
    internalPunctPattern = Pattern.compile(INTERNAL_PUNCT_REGEX);
    frenchPunct = Pattern.compile(FRENCH_PUNCT);

    spacepattern = Pattern.compile(P_Z);
    oepattern = Pattern.compile(REPLACE_ME_OE);
  }

  public String getNorm(String raw) {
    return String.join(" ", getTokens(raw,
        language != Language.FRENCH &&
            language != Language.TURKISH &&
            language != Language.CROATIAN &&
            language != Language.SERBIAN, false));
  }

  /**
   * @param sentence
   * @param removeAllAccents
   * @param debug
   * @return
   * @see IPronunciationLookup#getPronunciationsFromDictOrLTS
   * @see mitll.langtest.server.audio.SLFFile#createSimpleSLFFile
   */
  public List<String> getTokens(String sentence, boolean removeAllAccents, boolean debug) {
    List<String> all = new ArrayList<>();
    if (sentence.isEmpty()) {
      logger.warn("huh? empty sentence?");
    }
    String trimmedSent = getTrimmedSent(sentence, removeAllAccents);

    boolean b = DEBUG || debug;
    if (b && !sentence.equalsIgnoreCase(trimmedSent)) {
      logger.info("getTokens " +
          "\n\tremoveAllAccents " + removeAllAccents + "'" +
          "\n\tbefore           '" + sentence + "'" +
          "\n\tafter trim       '" + trimmedSent + "'");
    }

    for (String untrimedToken : spacepattern.split(trimmedSent)) { // split on spaces
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        String trim = token.trim();
        if (!trim.equalsIgnoreCase("–") &&
            !trim.equalsIgnoreCase("؟") &&
            !trim.equalsIgnoreCase("+"))
          all.add(toFull(token));
      }
    }

    if (b) logger.info("getTokens " +
        "\n\tbefore     '" + sentence + "'" +
        "\n\tafter trim '" + trimmedSent + "'" +
        "\n\tall        (" + all.size() + ")" + all
    );

    return all;
  }

  /**
   * @param s
   * @return
   * @see #getTokens(String, boolean, boolean)
   */
  public String toFull(String s) {
    StringBuilder builder = new StringBuilder();

    final CharacterIterator it = new StringCharacterIterator(s);
    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (c >= ZERO && c <= '9') {
        int offset = c - ZERO;
        int full = FULL_WIDTH_ZERO + offset;
        builder.append(Character.valueOf((char) full).toString());
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  private String getTrimmedSent(String sentence, boolean removeAllAccents) {
    return removeAllAccents ?
        getTrimmed(sentence) :
        getTrimmedLeaveAccents(sentence);
  }

  /**
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   * <p>
   * Also removes the chinese unicode bullet character, like in Bill Gates.
   * <p>
   * TODO : really slow - why not smarter?
   *
   * @param sentence
   * @return
   * @see #getTokens
   * @see mitll.langtest.server.trie.ExerciseTrie#addSuffixes
   */
  public String getTrimmed(String sentence) {
    String alt = tickPattern.matcher(sentence).replaceAll("");
    alt = punctCleaner.matcher(alt).replaceAll(ONE_SPACE);
    alt = oepattern.matcher(alt).replaceAll(OE);
    alt = spacepattern.matcher(alt).replaceAll(ONE_SPACE);
    alt = alt.trim();

    return alt;
  }

  /**
   * For the moment we replace the Turkish Cap I with I
   *
   * @param sentence
   * @return
   * @see PronunciationLookup#getPronStringForWord(String, Collection, boolean)
   */
  String getTrimmedLeaveAccents(String sentence) {
    String alt = frenchPunct.matcher(sentence).replaceAll(" ");
    alt = internalPunctPattern.matcher(alt).replaceAll("");
    alt = oepattern.matcher(alt).replaceAll(OE);

    if (language == Language.TURKISH) {
      alt = alt
          .replaceAll(TURKISH_CAP_I, NORMAL_I)
          .trim();
    } else {
      alt = alt.trim();
    }
    return alt;
  }
}
