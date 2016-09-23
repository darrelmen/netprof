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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.SlickRefResultJson;
import org.apache.log4j.Logger;

import java.util.*;

public class ExerciseToPhone {
  private static final Logger logger = Logger.getLogger(ExerciseToPhone.class);

  /**
   * @param refResultDAO
   * @return
   * @see #initializeDAOs(PathHelper)
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhone(IRefResultDAO refResultDAO) {
    long then = System.currentTimeMillis();
    List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    long now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " millis to get ref results");
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null);

    for (SlickRefResultJson exjson : jsonResults) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(exjson.scorejson());
      List<TranscriptSegment> transcriptSegments = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      int exid = exjson.exid();
      ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
      if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());

      {
        Set<String> phones = new HashSet<>();
        for (TranscriptSegment segment : transcriptSegments) phones.add(segment.getEvent());
        phonesForEx.addPhones(phones);
      }
      phonesForEx.setNumPhones(exjson.numalignphones());
    }
    logger.info("took " + (System.currentTimeMillis() - then) + " millis to populate ex->phone map");

    return exToPhones;
  }
}
