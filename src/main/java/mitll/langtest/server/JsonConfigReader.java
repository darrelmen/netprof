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
