/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.scoring;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.regex.Pattern;

public class TextNormalizer {
  //private static final Logger logger = LogManager.getLogger(TextNormalizer.class);

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
  private final boolean removeAccents;
  private final boolean isTurkish;
  private static final String TURKISH = "TURKISH";
  private static final Set<String> ACCENTED = new HashSet<>(Arrays.asList("FRENCH", TURKISH, "SERBIAN", "CROATIAN"));

  public TextNormalizer(String language) {
    this.removeAccents = !ACCENTED.contains(language);
    this.isTurkish = TURKISH.equalsIgnoreCase(language);

//    System.out.println("remove    " + removeAccents);
//    System.out.println("isTurkish " + isTurkish);
    // and leading upside down question marks...
    tickPattern = Pattern.compile("['’\\u00bf]");  // "don't" or "mustn't"
    punctCleaner = Pattern.compile(REMOVE_ME + "|" + P_P);
    internalPunctPattern = Pattern.compile(INTERNAL_PUNCT_REGEX);
    frenchPunct = Pattern.compile(FRENCH_PUNCT);

    spacepattern = Pattern.compile(P_Z);
    oepattern = Pattern.compile(REPLACE_ME_OE);
  }

  public String getNorm(String raw) {
    return String.join(" ", getTokens(raw, removeAccents, false));
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
 /*   if (sentence.isEmpty()) {
      logger.warn("huh? empty sentence?");
    }*/
    String trimmedSent = getTrimmedSent(sentence, removeAllAccents);

  /*  boolean b = DEBUG || debug;
    if (b && !sentence.equalsIgnoreCase(trimmedSent)) {
      logger.info("getTokens " +
          "\n\tremoveAllAccents " + removeAllAccents + "'" +
          "\n\tbefore           '" + sentence + "'" +
          "\n\tafter trim       '" + trimmedSent + "'");
    }*/

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

/*
    if (b) logger.info("getTokens " +
        "\n\tbefore     '" + sentence + "'" +
        "\n\tafter trim '" + trimmedSent + "'" +
        "\n\tall        (" + all.size() + ")" + all
    );
*/

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

    if (isTurkish) {
      alt = alt
          .replaceAll(TURKISH_CAP_I, NORMAL_I)
          .trim();
    } else {
      alt = alt.trim();
    }
    return alt;
  }

  private void convertFile(String infile, String outfile) {
    try {
      File fileDir = new File(infile);
      if (!fileDir.exists()) System.err.println("can't find " + infile + " at "+ fileDir.getAbsolutePath());
      else {
        // TextNormalizer textNormalizer = new TextNormalizer(lang.toUpperCase());
        BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(fileDir), StandardCharsets.UTF_8));

        if (outfile.isEmpty()) {
          String str;

          while ((str = in.readLine()) != null) {
            System.out.println(getNorm(str));
          }
        } else {
          BufferedWriter outw = new BufferedWriter(
              new OutputStreamWriter(
                  new FileOutputStream(outfile), StandardCharsets.UTF_8));

          String str;

          while ((str = in.readLine()) != null) {
            outw.write(getNorm(str));
            outw.write("\n");
          }
          outw.close();
        }

        in.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("usage : language infile outfile (opt)");

    } else {
      String lang = args[0];
      try {
        String infile = args[1];
        String outfile = args.length == 3 ? args[2] : "";

        new TextNormalizer(lang.toUpperCase()).convertFile(infile, outfile);
//        //  Language language = Language.valueOf(lang.toUpperCase());
//        StringBuilder builder = new StringBuilder();
//        for (int i = 1; i < args.length; i++) builder.append(args[i]).append(" ");
//        // logger.info("norm : " + lang + " " + builder);
//        System.out.println(new TextNormalizer(lang.toUpperCase()).getNorm(builder.toString()));
      } catch (IllegalArgumentException e) {
        System.err.println("couldn't parse \"" + lang + "\" as a language");
      }
    }
  }
}
