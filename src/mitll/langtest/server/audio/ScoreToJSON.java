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

package mitll.langtest.server.audio;

import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/3/15.
 */
public class ScoreToJSON {
  private static final Logger logger = Logger.getLogger(ScoreToJSON.class);

  /**
   * We skip sils, since we wouldn't want to show them to the user.
   *
   * @param answer
   * @return
   * @see AudioFileHelper#getAudioAnswerAlignment(String, CommonExercise, int, int, int, String, boolean, boolean, boolean, String, File, AudioCheck.ValidityAndDur, boolean, float, String, String, boolean)
   * @see AudioFileHelper#getAudioAnswerDecoding
   */
  public JSONObject getJsonFromAnswer(AudioAnswer answer) {
    PretestScore pretestScore = answer.getPretestScore();
    return getJsonObject(pretestScore);
  }

  public JSONObject getJsonObject(PretestScore pretestScore) {
    JSONObject jsonObject = new JSONObject();
    if (pretestScore != null) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = pretestScore.getsTypeToEndTimes();
      List<TranscriptSegment> words  = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
      List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
      if (words != null) {
        int windex = 0;
        int pindex = 0;
        JSONArray jsonWords = new JSONArray();

        for (TranscriptSegment segment : words) {
          String event = segment.getEvent();
          if (!event.equals(SLFFile.UNKNOWN_MODEL) && !event.equals("sil")) {
            JSONObject wordJson = new JSONObject();
            String wid = Integer.toString(windex++);
            wordJson.put("id", wid);
            wordJson.put("w", event);
            wordJson.put("s", getScore(segment));
            wordJson.put("str", floatToString(segment.getStart()));
            wordJson.put("end", floatToString(segment.getEnd()));

            JSONArray jsonPhones = new JSONArray();

            for (TranscriptSegment pseg : phones) {
              if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
                String pevent = pseg.getEvent();
                if (!pevent.equals(SLFFile.UNKNOWN_MODEL) && !pevent.equals("sil")) {
                  JSONObject phoneJson = new JSONObject();
                  phoneJson.put("id", Integer.toString(pindex++));
                  phoneJson.put("p", pevent);
                  phoneJson.put("s", getScore(pseg));
                  phoneJson.put("str", floatToString(pseg.getStart()));
                  phoneJson.put("end", floatToString(pseg.getEnd()));
                  jsonPhones.add(phoneJson);
                }
              }
            }
            wordJson.put("phones", jsonPhones);
            jsonWords.add(wordJson);
          }
        }

        jsonObject.put("words", jsonWords);
      }
      else if (pretestScore.getHydecScore() > -0.1f) {
        logger.warn("no word transcript for " + pretestScore);
      }
    }
    else {
//      logger.warn("pretest score is null?");
    }
    return jsonObject;
  }

  private String getScore(TranscriptSegment segment) {
    float score = segment.getScore();
    return floatToString(score);
  }

  private String floatToString(float round) {
    return Float.toString(round(round));
  }


  private static float round(float d) {
    return round(d, 3);
  }

  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d)).
        setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }
}


