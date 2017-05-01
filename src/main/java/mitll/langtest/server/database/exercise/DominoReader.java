package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.HTTPClient;
import net.sf.json.JSONObject;

import java.io.IOException;

/**
 * Created by go22670 on 4/5/16.
 * @deprecated
 */
class DominoReader {
  private static final String DOCUMENTS = "documents";

  /**
   * TODO :  Consider reading font from project info
   *
   *     "name": "ARABIC (MODERN STANDARD)",
   "defaultWordSpacing": true,
   "direction": "RTL",
   "systemFontNames": "Simplified Arabic",
   "fontFaceURL": null,
   "bitmapped": false,
   "langCode": "arb",
   "lineHeight": ​125,
   "fontSize": ​14,
   "digraph": "AD",
   "script": "auto",
   "fontWeight": "Normal",
   "trigraph": "ARB"
   * @param serverProps
   */
  void readProjectInfo(ServerProperties serverProps) {
    String baseURL = serverProps.getLessonPlan();

    if (baseURL.endsWith(DOCUMENTS)) {
      baseURL = baseURL.substring(0, baseURL.length() - DOCUMENTS.length());
    }
//    else if (baseURL.endsWith("exam"))
    String projectInfo = null;
    try {
      projectInfo = new HTTPClient().readFromGET(baseURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
    JSONObject jsonObject = JSONObject.fromObject(projectInfo);
    JSONObject language = jsonObject.getJSONObject("language");
    String upperName = language.getString("name");

    // TODO : consider setting language here.
    String npLang = upperName.substring(0,1) + upperName.substring(1).toLowerCase();
    //  logger.info("got language " + npLang);
    boolean isLTR = language.get("direction").equals("LTR");
    String systemFontNames = language.getString("systemFontNames");
    serverProps.setFontFamily(systemFontNames);
    String fontFaceURL = language.getString("fontFaceURL");
    serverProps.setFontFaceURL(fontFaceURL);
    serverProps.setRTL(!isLTR);
  }
}
