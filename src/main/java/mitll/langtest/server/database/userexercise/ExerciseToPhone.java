/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.SlickRefResultJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Figures out the sequence of phones for an exercise.
 * Either by looking up what a decoder has already found, or asking the dictionary.
 */
public class ExerciseToPhone {
  private static final Logger logger = LogManager.getLogger(ExerciseToPhone.class);

  /**
   * TODO: Seems like this could take a long time????
   *
   * @return
   * @paramx refResultDAO
   * @seex DatabaseImpl#configureProjects
   */
/*  public Map<Integer, ExercisePhoneInfo> getExerciseToPhone(IRefResultDAO refResultDAO) {
    long then = System.currentTimeMillis();
    List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    long now = System.currentTimeMillis();
    logger.info("getExerciseToPhone took " + (now - then) + " millis to get ref results");
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null);

    for (SlickRefResultJson exjson : jsonResults) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.readFromJSON(exjson.scorejson());
      *//*List<TranscriptSegment> transcriptSegments =*//* //netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      int exid = exjson.exid();
      ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
      if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());
      addPhones(phonesForEx, netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT));
      phonesForEx.setNumPhones(exjson.numalignphones());
    }
    logger.info("getExerciseToPhone took " + (System.currentTimeMillis() - then) +
        " millis to populate ex->phone map of size " + exToPhones.size());

    return exToPhones;
  }*/

  /**
   * @param jsonResults
   * @return
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#getExerciseToPhoneForProject
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhoneForProject(Collection<SlickRefResultJson> jsonResults) {
    long then = System.currentTimeMillis();
//    long now = System.currentTimeMillis();
//    logger.info("getExerciseToPhone took " + (now - then) + " millis to get ref results");
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();
    // ParseResultJson parseResultJson = new ParseResultJson(null);
    for (SlickRefResultJson exjson : jsonResults) {
//      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.readFromJSON(exjson.scorejson());
      /*List<TranscriptSegment> transcriptSegments =*/ //netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
      int exid = exjson.exid();
      ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
      if (phonesForEx == null) {
        exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());
      }
      // List<TranscriptSegment> transcriptSegments = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
/*      if (transcriptSegments != null) {
        addPhones(phonesForEx, transcriptSegments);
      }*/
      phonesForEx.setNumPhones(exjson.numalignphones());
    }

    long l = System.currentTimeMillis() - then;
    if (l > 20) logger.info("getExerciseToPhone took " + l +
        " millis to populate ex->phone map of size " + exToPhones.size());

    return exToPhones;
  }

  /**
   * @return
   * @paramx refResultDAO
   * @seex DatabaseImpl#configureProjects
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhone2(
      List<SlickRefResultJson> jsonResults,
      Set<Integer> inProject, Language language) {
    long then = System.currentTimeMillis();
    //List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    logger.info("getExerciseToPhone took " + (System.currentTimeMillis() - then) + " millis to get ref results");
    return getExToPhonePerProject(inProject, jsonResults, language);
  }

  Map<Integer, ExercisePhoneInfo> getExToPhonePerProject(Set<Integer> inProject, List<SlickRefResultJson> jsonResults, Language language) {
    long then = System.currentTimeMillis();
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null, language);

    // partition into same length sets
    Map<Integer, Set<Info>> lengthToInfos = new HashMap<>();

    logger.info("getExerciseToPhone2 looking at " + inProject.size() + " exercises and " + jsonResults.size() + " results");

    Map<Integer, Map<String, Set<String>>> exToWordToPron = new HashMap<>();

    for (SlickRefResultJson exjson : jsonResults) {
      int exid = exjson.exid();
      if (inProject.contains(exid)) {
//        Map<String, List<List<String>>> wordToProns = exToWordToPronunciations.get(exid);
//        if (wordToProns == null)
//          exToWordToPronunciations.put(exid, wordToProns = new HashMap<String, List<List<String>>>());
        Map<String, List<List<String>>> wordToProns = new HashMap<String, List<List<String>>>();

        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap =
            parseResultJson.parseJsonAndGetProns(exjson.scorejson(), wordToProns);

        int numWords = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT).size();

        for (Map.Entry<String, List<List<String>>> pair : wordToProns.entrySet()) {
          Map<String, Set<String>> wordToPron = exToWordToPron.get(exid);
          if (wordToPron == null) exToWordToPron.put(exid, wordToPron = new HashMap<String, Set<String>>());


          String word = pair.getKey();
          Set<String> pForWord = wordToPron.get(word);
          if (pForWord == null) wordToPron.put(word, pForWord = new HashSet<String>());

          List<List<String>> pronsForWord = pair.getValue();
          for (List<String> pron : pronsForWord) {
            String pronKey = getPronKey(pron);
            if (!pForWord.contains(pronKey)) {
              pForWord.add(pronKey);

              Info info = new Info(exid, word, pronsForWord, numWords);
              //for (List<String> pron : info.getPronunciationsFromDictOrLTS()) {
              Set<Info> infos = lengthToInfos.get(pron.size());
              if (infos == null) {
                lengthToInfos.put(pron.size(), infos = new HashSet<Info>());
              }
              infos.add(info);
              // }
            }
          }
/*
          Info info = new Info(exid, word, pronsForWord, numWords);
          for (List<String> pron : info.getPronunciationsFromDictOrLTS()) {
            Set<Info> infos = lengthToInfos.get(pron.size());
            if (infos == null) {
              lengthToInfos.put(pron.size(), infos = new HashSet<Info>());
            }
            infos.add(info);
          }
*/
        }

        ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
        if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());

/*
        addPhones(phonesForEx, netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT));
*/

        phonesForEx.setNumPhones(exjson.numalignphones());
      }
    }

    logger.info("length->info " + lengthToInfos.size() + " length " + lengthToInfos.keySet());
    Map<Integer, Map<String, Info>> exToWordToInfo = new HashMap<>();

    // n^2
    for (Map.Entry<Integer, Set<Info>> pair : lengthToInfos.entrySet()) {
      Integer length = pair.getKey();

      if (length < 3) continue;
      Set<Info> value = pair.getValue();

      logger.info(length + " : n x n " + value.size());

      int j = 0;
      for (Info info : value) {
        if (j++ % 1000 == 0) logger.info(j + " / " + value.size());
        for (Info other : value) {
          if (info != other) {
            for (List<String> pron : info.getPronunciations()) {
              if (pron.size() == length) {
                for (List<String> pron2 : other.getPronunciations()) {
                  if (pron2.size() == length) {
                    int subs = 0;
                    for (int i = 0; i < length; i++) {
                      String first = pron.get(i);
                      String second = pron2.get(i);
                      if (!first.equals(second)) {
                        subs++;
                        if (subs == 2) break; // only one sub distance away
                      }
                    }
                    if (subs == 1) {   // e.g. l-ah-s => l-o-s
//                      logger.info(pron + " one from " +pron2);
                      addNeighbor(exToWordToInfo, info, other, true);
                      //if (previous != null) logger.warn("found previous " + info);
                    }
                    /*else if (subs == 2){
                      addNeighbor(exToWordToInfo, info, other, false);
                    }*/
                  }
                }
              }
            }
          }
        }
      }
    }

    // Not for now - maybe in the future.
    /*

    for (Map.Entry<Integer, ExercisePhoneInfo> entry : exToPhones.entrySet()) {
      Map<String, Info> wordToInfo = exToWordToInfo.get(entry.getKey());
      if (wordToInfo != null) {
        entry.getValue().setWordToInfo(wordToInfo);
      }
    }
*/

    long diff = System.currentTimeMillis() - then;
    if (diff > 20) logger.info("took " + diff + " millis to populate ex->phone map");

    return exToPhones;
  }

  private void addNeighbor(Map<Integer, Map<String, Info>> exToWordToInfo, Info info, Info other, boolean isOne) {
    info.addNeighbor(other, isOne);
    Map<String, Info> wordToInfo = exToWordToInfo.get(info.exid);
    if (wordToInfo == null) {
      exToWordToInfo.put(info.exid, wordToInfo = new HashMap<>());
    }
    /*Info previous =*/
    wordToInfo.put(info.word, info);
  }

  private String getPronKey(List<String> pron) {
    StringBuilder builder = new StringBuilder();
    for (String phone : pron) builder.append(phone).append("-");
    return builder.toString();
  }

  public static class Info {
    int exid;
    String word;
    private List<List<String>> pronunciations;

    int numWords;

    private Map<String, Info> pronToInfo = new HashMap<>();
    private Map<String, Integer> pronToCount = new HashMap<>();
    private Map<String, Info> pronToInfo2 = new HashMap<>();
    private Map<String, Integer> pronToCount2 = new HashMap<>();

    /**
     * @param exid
     * @param word
     * @param pronunciations
     * @see ExerciseToPhone#getExToPhonePerProject(Set, List, String)
     */
    public Info(int exid, String word, List<List<String>> pronunciations, int numWords) {
      this.exid = exid;
      this.word = word;
      this.pronunciations = pronunciations;
      this.numWords = numWords;
    }

    /**
     * @param neighbor
     * @see ExerciseToPhone#addNeighbor
     */
    void addNeighbor(Info neighbor, boolean isOne) {
      //    oneSubNeighbors.add(neighbor);
      for (List<String> pron : neighbor.getPronunciations()) {
        String pronKey = getPronKey(pron);
        Map<String, Info> pronToInfo = isOne ? this.pronToInfo : pronToInfo2;
        Map<String, Integer> pronToCount = isOne ? this.pronToCount : pronToCount2;

        addNeighbor(neighbor, pronKey, pronToInfo, pronToCount);
      }
    }

    private void addNeighbor(Info neighbor, String pronKey, Map<String, Info> pronToInfo, Map<String, Integer> pronToCount) {
      Info currentInfo = pronToInfo.get(pronKey);
      if (currentInfo == null || currentInfo.numWords > neighbor.numWords) {
        pronToInfo.put(pronKey, neighbor);
        pronToCount.put(pronKey, 1);
//        logger.info("new neighbor " + neighbor);
      } else if (currentInfo.numWords == neighbor.numWords) {
        int value = pronToCount.get(pronKey) + 1;
        pronToCount.put(pronKey, value);

        if (pronKey.equals("libro")) {
          logger.info(pronKey + " " + exid + " - " + neighbor.exid + " value " + value);
        }

        if (value % 100 == 0) {
//            logger.info(pronKey + " = " + value);
        }
      }
    }

    private String getPronKey(List<String> pron) {
      StringBuilder builder = new StringBuilder();
      for (String phone : pron) builder.append(phone).append("-");
      return builder.toString();
    }

    List<List<String>> getPronunciations() {
      return pronunciations;
    }

    public String toString() {
      StringBuilder builder = getProns(pronToInfo);
      StringBuilder builder2 = new StringBuilder();
      for (List<String> pron : pronunciations) builder2.append(getPronKey(pron)).append(", ");

      return "exid " + exid + " : " + word + " prons " + pronunciations.size() + " (" + builder2 +
          ") " +
          "one sub neighbors " + pronToInfo.size() + " : " + builder;// + "\n\ttwo " + pronToInfo2.size() + " " + getProns(pronToInfo2);
    }

    private StringBuilder getProns(Map<String, Info> pronToInfo) {
      StringBuilder builder = new StringBuilder();
      for (String pron : pronToInfo.keySet()) builder.append(pron).append(",");
      return builder;
    }

    public Map<String, Info> getPronToInfo() {
      return pronToInfo;
    }

    public Map<String, Integer> getPronToCount() {
      return pronToCount;
    }
  }

  /**
   * @see #getExToPhonePerProject
   * @param phonesForEx
   * @param transcriptSegments
   */
/*  private void addPhones(ExercisePhoneInfo phonesForEx, List<TranscriptSegment> transcriptSegments) {
    Set<String> phones = new HashSet<>();
    for (TranscriptSegment segment : transcriptSegments) {
      if (segment != null) {
        phones.add(segment.getEvent());
      }
    }
    phonesForEx.addPhones(phones);
  }*/
}
