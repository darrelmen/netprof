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

package mitll.langtest.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mitll.langtest.shared.user.Affiliation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by go22670 on 2/3/17.
 */
class JsonConfigReader {
  private static final String AFFLIATIONS = "affliations";
  private static final String ABBREVIATION = "abbreviation";
  private static final String DISPLAY_NAME = "displayName";
  private static final String ID = "_id";

  private static final String CONFIG = "config";
  private static final String CONFIG_JSON = "config.json";

  /**
   * Read json file.
   * @see ServerProperties#useProperties
   * @param configDir
   * @throws FileNotFoundException
   */
  public List<Affiliation> getAffiliations(File configDir) throws FileNotFoundException {
    File file = new File(configDir, CONFIG_JSON);
    if (file.exists()) {
      List<Affiliation> affliations = new ArrayList<>();

      JsonParser parser = new JsonParser();
      JsonObject parse = parser.parse(new FileReader(file)).getAsJsonObject();
      JsonArray config = parse.getAsJsonArray(CONFIG);

      for (JsonElement elem : config) {
        JsonObject asJsonObject = elem.getAsJsonObject();
        if (asJsonObject.has(AFFLIATIONS)) {
          JsonElement affiliations1 = asJsonObject.get(AFFLIATIONS);

          JsonArray affiliations = affiliations1.getAsJsonArray();
          for (JsonElement aff : affiliations) {
            JsonObject asJsonObject1 = aff.getAsJsonObject();
            String abbreviation = asJsonObject1.get(ABBREVIATION).getAsString();
            String displayName = asJsonObject1.get(DISPLAY_NAME).getAsString();
            affliations.add(new Affiliation(asJsonObject1.get(ID).getAsInt(), abbreviation, displayName));
          }
        }
      }
      return affliations;
    } else return Collections.emptyList();
  }
}
