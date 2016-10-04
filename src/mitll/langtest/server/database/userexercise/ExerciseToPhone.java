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

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.SlickRefResultJson;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExerciseToPhone {
  private static final Logger logger = Logger.getLogger(ExerciseToPhone.class);

  /**
   * @param refResultDAO
   * @return
   * @see DatabaseImpl#configureProjects
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhone(IRefResultDAO refResultDAO) {
    long then = System.currentTimeMillis();
    List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    long now = System.currentTimeMillis();
    logger.info("getExerciseToPhone took " + (now - then) + " millis to get ref results");
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null);

    for (SlickRefResultJson exjson : jsonResults) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(exjson.scorejson());
      List<TranscriptSegment> transcriptSegments = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      int exid = exjson.exid();
      ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
      if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());
      addPhones(phonesForEx, netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT));
      phonesForEx.setNumPhones(exjson.numalignphones());
    }
    logger.info("took " + (System.currentTimeMillis() - then) + " millis to populate ex->phone map");

    return exToPhones;
  }

  /**
   * @param refResultDAO
   * @return
   * @see DatabaseImpl#configureProjects
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhone2(IRefResultDAO refResultDAO, Set<Integer> inProject) {
    long then = System.currentTimeMillis();
    List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    long now = System.currentTimeMillis();
    logger.info("getExerciseToPhone took " + (now - then) + " millis to get ref results");


    return getExToPhonePerProject(inProject, jsonResults);
  }

  @NotNull
  public Map<Integer, ExercisePhoneInfo> getExToPhonePerProject(Set<Integer> inProject, List<SlickRefResultJson> jsonResults) {
    long then = System.currentTimeMillis();
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null);

    Map<Integer, Map<String, List<List<String>>>> exToWordToPronunciations = new HashMap<>();

    // partition into same length sets
    Map<Integer, Set<Info>> lengthToInfos = new HashMap<>();

    logger.info("looking at " +inProject.size() + " exercises and "+ jsonResults.size() + " results");

    for (SlickRefResultJson exjson : jsonResults) {
      int exid = exjson.exid();
      if (inProject.contains(exid)) {
        Map<String, List<List<String>>> wordToProns = exToWordToPronunciations.get(exid);
        if (wordToProns == null)
          exToWordToPronunciations.put(exid, wordToProns = new HashMap<String, List<List<String>>>());

        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJsonAndGetProns(exjson.scorejson(), wordToProns);

        for (Map.Entry<String, List<List<String>>> pair : wordToProns.entrySet()) {
          Info info = new Info(exid, pair.getKey(), pair.getValue());
          for (List<String> pron : info.getPronunciations()) {
            Set<Info> infos = lengthToInfos.get(pron.size());
            if (infos == null) {
              lengthToInfos.put(pron.size(), infos = new HashSet<Info>());
            }
            infos.add(info);
          }
        }

        ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
        if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());

        addPhones(phonesForEx, netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT));

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

      logger.info("n x n " + value.size());

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
                      String first  = pron.get(i);
                      String second = pron2.get(i);
                      if (!first.equals(second)) {
                        subs++;
                        if (subs == 2) break; // only one sub distance away
                      }
                    }
                    if (subs == 1) {
                      //logger.info(pron + " one from " +pron2);

                      info.addNeighbor(other);
                      Map<String, Info> wordToInfo = exToWordToInfo.get(info.exid);
                      if (wordToInfo == null) {
                        exToWordToInfo.put(info.exid, wordToInfo = new HashMap<>());
                      }
                      Info previous = wordToInfo.put(info.word, info);

                      //if (previous != null) logger.warn("found previous " + info);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    for (Map.Entry<Integer, ExercisePhoneInfo> entry : exToPhones.entrySet()) {
      Map<String, Info> wordToInfo = exToWordToInfo.get(entry.getKey());
      if (wordToInfo != null) {
        entry.getValue().setWordToInfo(wordToInfo);
      }
    }


    logger.info("took " + (System.currentTimeMillis() - then) + " millis to populate ex->phone map");

    return exToPhones;
  }

  public static class Info {
    int exid;
    String word;
    private List<List<String>> pronunciations;
    List<Info> oneSubNeighbors = new ArrayList<>();

    public Info(int exid, String word, List<List<String>> pronunciations) {
      this.exid = exid;
      this.word = word;
      this.pronunciations = pronunciations;
    }

    public void addNeighbor(Info neighbor) {
      oneSubNeighbors.add(neighbor);
    }

    public List<List<String>> getPronunciations() {
      return pronunciations;
    }

    public String toString() {
      return "exid " + exid + " : " + word + " prons " + pronunciations.size() + " neighbors " + oneSubNeighbors.size();
    }
  }

  private void addPhones(ExercisePhoneInfo phonesForEx, List<TranscriptSegment> transcriptSegments) {
    Set<String> phones = new HashSet<>();
    for (TranscriptSegment segment : transcriptSegments) phones.add(segment.getEvent());
    phonesForEx.addPhones(phones);
  }
}
