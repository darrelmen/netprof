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

package mitll.langtest.server.database.refaudio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class BaseRefResultDAO extends DAO {
  private static final String SCORE_JSON = "scoreJSON";

  BaseRefResultDAO(Database database) {
    super(database);
 //   this.dropTable = dropTable;
  }

  void addToJSONs(Map<Integer, List<String>> idToJSONs, Integer exid, String json) {
    List<String> orDefault2 = idToJSONs.get(exid);
    if (orDefault2 == null) {
      idToJSONs.put(exid, orDefault2 = new ArrayList<>());
      orDefault2.add(json);
    }
  }

  void addToAnswers(Map<Integer, List<String>> idToAnswers, Integer exid, String answer) {
    List<String> orDefault = idToAnswers.get(exid);
    if (orDefault == null) {
      idToAnswers.put(exid, orDefault = new ArrayList<>());
      int i = answer.lastIndexOf("/");
      String fileName = (i > -1) ? answer.substring(i + 1) : answer;
      orDefault.add(fileName);
    }
  }

  JsonObject getJsonObject(Map<Integer, List<String>> idToAnswers, Map<Integer, List<String>> idToJSONs) {
    JsonObject jsonObject = new JsonObject();
    for (Map.Entry<Integer, List<String>> pair : idToAnswers.entrySet()) {
      Integer exid = pair.getKey();
      List<String> answers = pair.getValue();
      List<String> jsons = idToJSONs.get(exid);

      JsonArray array = new JsonArray();

      for (int i = 0; i < answers.size(); i++) {
        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("file", answers.get(i));
        jsonObject1.addProperty(SCORE_JSON, jsons.get(i));
        array.add(jsonObject1);
      }
      jsonObject.add(exid.toString(), array);
    }
    return jsonObject;
  }

  String trimPathForWebPage2(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}
