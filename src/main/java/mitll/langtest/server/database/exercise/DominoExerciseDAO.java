package mitll.langtest.server.database.exercise;

import mitll.hlt.domino.server.util.Mongo;
import mitll.hlt.domino.shared.model.metadata.Language;
import mitll.hlt.domino.shared.model.project.ProjectDescriptor;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by go22670 on 5/2/17.
 */
public class DominoExerciseDAO {
  private static final Logger logger = LogManager.getLogger(DominoExerciseDAO.class);
  JSONSerializer ser;

  public DominoExerciseDAO(JSONSerializer serializer) {
    this.ser = serializer;
  }

  public List<CommonExercise> readExercises(String file, List<String> typeOrder) {
    List<CommonExercise> exercises = new ArrayList<>();
    try {
      //JsonParser parser = Json.createParser(new FileReader(file));
      JsonReader reader = Json.createReader(new FileReader(file));
      JsonObject readObj = (JsonObject) reader.read();

      // JSONSerializer ser = Mongo.makeSerializer();
      Date theTime = ser.dateFormat().parse(readObj.getString("exportTime")).get();

      logger.info("got time " + theTime);

      JsonObject langObj = readObj.getJsonObject("language");
      Language l = ser.deserialize(Language.class, langObj.toString());
      logger.info("got l " + l);

      JsonObject projObj = readObj.getJsonObject("project");
      logger.info("got projObj " + projObj);
      ProjectDescriptor pd = ser.deserialize(ProjectDescriptor.class, projObj.toString());
      //  assertThat(pd, notNullValue());
      //  assertThat(pd.getName(), is(expProj.getName()));
      //  assertThat(pd.getContent().getSkill(), is(MetadataTypes.SkillType.Vocabulary));

      JsonArray docArr = readObj.getJsonArray("documents");
      //assertThat(docArr, hasSize(9));
      docArr.forEach(docObj -> {
        logger.info("Got " + docObj);
        //   SimpleHeadDocumentRevision shDoc = ser.deserialize(SimpleHeadDocumentRevision.class, docObj.toString());
        //assertThat(shDoc, notNullValue());
        //assertThat(docIds.contains(shDoc.getId()), is(true));
        //assertThat(shDoc.getDocument() instanceof VocabularyItem, is(true));
      });

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return exercises;
  }

  private CommonExercise toExercise(JsonObject jsonObject, List<String> typeOrder) {
    JsonObject metadata = jsonObject.getJsonObject("metadata");
    JsonObject content = jsonObject.getJsonObject("content");
    String updateTime = jsonObject.getString("updateTime");
    String dominoID = "" + jsonObject.getInt("id");
    boolean isLegacy = metadata.containsKey("npDID");
    String npDID = isLegacy ? metadata.getString("npDID") : dominoID;

    long updateMillis = System.currentTimeMillis();
//    try {
//      Date update = dateFmt.parse(updateTime);
//      updateMillis = update.getTime();
//    } catch (ParseException e) {
//      logger.warn(e.getMessage() + " : can't parse date '" + updateTime + "' for " + npDID);
//    }

    String fl = noMarkup(content.getString("pass"));
    String english = noMarkup(content.getString("trans"));
    String meaning = noMarkup(content.getString("meaning"));
    String transliteration = noMarkup(content.getString("translit"));

    String context = noMarkup(content.getString("context"));
    String contextTranslation = noMarkup(content.getString("context_trans"));

    Exercise exercise = null;
/*    Exercise exercise = new Exercise(
        npDID,
        english,
        fl,
        meaning,
        transliteration,
        context,
        contextTranslation,
        dominoID);
    exercise.setUpdateTime(updateMillis);*/
   // if (!isLegacy) logger.info("NOT LEGACY " + exercise);

/*    for (String type : typeOrder) {
      try {
        exercise.addUnitToValue(type, noMarkup(content.getString(type.toLowerCase())));
      } catch (Exception e) {
        logger.error("couldn't find unit/chapter '" + type + "' in content - see typeOrder property");
      }
    }*/

    return exercise;
  }

  private String noMarkup(String source) {
    return source.replaceAll("\\<.*?>", "");
  }
//  public static void main(String[] arg) {
//    new DominoExerciseDAO().readExercises("SAMPLE-NO-EXAM.json");
//  }
}
