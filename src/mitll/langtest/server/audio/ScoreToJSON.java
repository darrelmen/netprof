package mitll.langtest.server.audio;

import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
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
 * Created by go22670 on 9/3/15.
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
      else {
        logger.warn("no word transcript for " + pretestScore);
      }
    }
    else {
      logger.warn("pretest score is null?");

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
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }
}


