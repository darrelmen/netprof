package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates the LM for a small vocab decoder from foreground and background sentences.
 *
 * User: GO22670
 * Date: 2/7/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallVocabDecoder {
  private static final Logger logger = Logger.getLogger(ASRScoring.class);
  private HTKDictionary htkDictionary;

  public SmallVocabDecoder() {}
  public SmallVocabDecoder(HTKDictionary htkDictionary) {
    this.htkDictionary = htkDictionary;
  }

  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   * @see ASRScoring#getUsedTokens
   * @param background sentences
   * @return most frequent vocabulary words
   */
  public List<String> getVocab(List<String> background, int vocabSizeLimit) {
    return getSimpleVocab(background, vocabSizeLimit);
  }

  /**
   * @see ASRScoring#getUniqueTokensInLM
   * @param sentences
   * @param vocabSizeLimit
   * @return
   */
  public List<String> getSimpleVocab(Collection<String> sentences, int vocabSizeLimit) {
    // count the tokens
    final Map<String, Integer> sc = new HashMap<String, Integer>();
    for (String sentence : sentences) {
      for (String token : getTokens(sentence)) {
      //  if (isValid(scoring, token)) {
          Integer c = sc.get(token);
          sc.put(token, (c == null) ? 1 : c + 1);
     /*   } else {
          logger.warn("getSimpleVocab : skipping '" + token + "' which is not in dictionary.");
        }*/
      }
    }

    // sort by frequency
    List<String> vocab = new ArrayList<String>(sc.keySet());
    Collections.sort(vocab, new Comparator<String>() {
      public int compare(String s, String s2) {
        Integer first = sc.get(s);
        Integer second = sc.get(s2);
        return first < second ? +1 : first > second ? -1 : 0;
      }
    });

    // take top n most frequent
    List<String> all = new ArrayList<String>(); // copy list b/c sublist not serializable ???
    if (vocab.size() > vocabSizeLimit) logger.warn("truncating vocab size from " + vocab.size() + " to " + vocabSizeLimit);
    all.addAll(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
    return all;
  }

  public List<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();

    String trimmedSent = sentence.replaceAll("'", "").replaceAll("\\p{P}", " ").replaceAll("\\s+", " ").trim();
    for (String untrimedToken : trimmedSent.split("\\p{Z}+")) { // split on spaces
      //String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }


  public String segmentation(String phrase){  return longest_prefix(phrase, 0);  }

  private String longest_prefix(String phrase, int i){
    if(i == phrase.length())
      return "";
    String prefix = phrase.substring(0, phrase.length() - i);
    if (inDict(prefix)) {
      if(i == 0)
        return phrase;
      String rest = longest_prefix(phrase.substring(phrase.length()-i, phrase.length()), 0);
      if(rest.length() > 0)
        return prefix + " " + rest;
    }
    //else {
    //logger.debug("dict doesn't contain " + prefix);
    //}
    return longest_prefix(phrase, i+1);
  }

  private boolean inDict(String token) {
    scala.collection.immutable.List<String[]> apply = htkDictionary.apply(token);
    return apply != null && !apply.isEmpty();
  }
}
