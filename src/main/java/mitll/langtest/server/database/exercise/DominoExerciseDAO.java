package mitll.langtest.server.database.exercise;

import mitll.hlt.domino.server.util.Mongo;
import mitll.hlt.domino.shared.model.SimpleHeadDocumentRevision;
import mitll.hlt.domino.shared.model.document.*;
import mitll.hlt.domino.shared.model.metadata.Language;
import mitll.hlt.domino.shared.model.project.ProjectDescriptor;
import mitll.hlt.domino.shared.model.user.UserDescriptor;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.tools.cmd.gen.AnyVals;

import javax.json.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

import static mitll.hlt.domino.shared.model.metadata.MetadataTypes.SkillType.Vocabulary;

/**
 * Created by go22670 on 5/2/17.
 */
public class DominoExerciseDAO {
  private static final Logger logger = LogManager.getLogger(DominoExerciseDAO.class);
  private JSONSerializer ser;

  /**
   * @param serializer
   */
  public DominoExerciseDAO(JSONSerializer serializer) {
    this.ser = serializer;
  }

  public static class Info {
    private Date createTime;
    private Date modifiedTime;
    private List<CommonExercise> exercises;

    private String language;
    private mitll.langtest.shared.project.Language lang;

    private int dominoID;

    public Info(Date exportTime, Date updateTime,
                List<CommonExercise> exercises,
                String language,
                mitll.langtest.shared.project.Language lang,
                int dominoID) {
      this.createTime = exportTime;
      this.modifiedTime = updateTime;
      this.exercises = exercises;
      this.language = language;
      this.lang = lang;
      this.dominoID = dominoID;
    }

    public Date getExportTime() {
      return createTime;
    }

    public List<CommonExercise> getExercises() {
      return exercises;
    }

    public String getLanguage() {
      return language;
    }

    public mitll.langtest.shared.project.Language getLang() {
      return lang;
    }

    public int getDominoID() {
      return dominoID;
    }

    public Date getModifiedTime() {
      return modifiedTime;
    }

    public String toString() {
      return "lang " + lang + " " + lang + " " + getDominoID() + " " + getExportTime() + " num " + getExercises().size();
    }
  }

  public Info readExercises(String file, InputStream inputStream, int projid, int importUser) {
    List<CommonExercise> exercises = new ArrayList<>();
    try {
      //JsonParser parser = Json.createParser(new FileReader(file));
      JsonReader reader = file == null ?
          Json.createReader(inputStream) :
          Json.createReader(new FileReader(file));
      JsonObject readObj = (JsonObject) reader.read();

      // JSONSerializer ser = Mongo.makeSerializer();
      // Date theTime = ser.dateFormat().parse(readObj.getString("exportTime")).get();

//      logger.info("got time " + theTime);

      JsonObject langObj = readObj.getJsonObject("language");
      Language dominoLang = ser.deserialize(Language.class, langObj.toString());
      //logger.info("got Language " + dominoLang);

      JsonObject projObj = readObj.getJsonObject("project");
      // logger.info("got projObj " + projObj);
      ProjectDescriptor pd = ser.deserialize(ProjectDescriptor.class, projObj.toString());
      UserDescriptor creator1 = pd.getCreator();
      int creator = creator1 != null ? creator1.getDocumentDBID() : importUser;
      logger.info("got " + pd);

      String languageName = pd.getContent().getLanguageName();

      mitll.langtest.shared.project.Language lang = mitll.langtest.shared.project.Language.UNKNOWN;
      try {
        lang = mitll.langtest.shared.project.Language.valueOf(languageName);
        logger.info("Got " + languageName + " " + lang);
      } catch (IllegalArgumentException e) {

      }

      if (pd.getContent().getSkill() != Vocabulary) {
        logger.error("huh? skill type is " + pd.getContent().getSkill());
      }

      JsonArray docArr = readObj.getJsonArray("documents");
      //Set<String> unique = new HashSet<>();
      docArr.forEach(docObj -> {
  //      logger.info("Got json " + docObj);
        Exercise ex = getExercise(projid, creator, docObj);
        //      logger.info("Got " + ex);
        //    logger.info("Got " + ex.getDirectlyRelated());
        exercises.add(ex);
      });
      return new Info(pd.getCreateTime(), pd.getUpdateTime(), exercises, languageName, lang, pd.getId());

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  @NotNull
  private Exercise getExercise(int projid, int creator, JsonValue docObj) {
    SimpleHeadDocumentRevision shDoc = ser.deserialize(SimpleHeadDocumentRevision.class, docObj.toString());
    Date createDate = shDoc.getCreateDate();
    IDocument document = shDoc.getDocument();
    VocabularyItem vocabularyItem = (VocabularyItem) document;
    Exercise ex = getExercise(projid, shDoc.getId(), vocabularyItem, creator);

//        logger.info("Got old id " + shDoc.getId() + " " + ex.getDominoID());
    ex.setUpdateTime(createDate.getTime());

    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      if (name.startsWith("v-") && !name.equals("v-np-id")) {
        name = name.substring(2);
        ex.addUnitToValue(name, field.getDisplayValue());
      }
    }
//        logger.info("Got " + ex.getUnitToValue());
    addContextSentences(projid, creator, shDoc, vocabularyItem, ex);
    return ex;
  }

  private void addContextSentences(int projid, int creator, SimpleHeadDocumentRevision shDoc, VocabularyItem vocabularyItem, Exercise ex) {
    IDocumentComposite samples = vocabularyItem.getSamples();
    for (IDocumentComponent comp : samples.getComponents()) {
      SampleSentence sample = (SampleSentence) comp;
      int compid = shDoc.getId() * 10 + sample.getNum();

//      logger.info("context import id " + compid);

      Exercise context = getExercise(projid, compid, creator,
          sample.getSentenceVal(), sample.getAlternateFormVal(), sample.getTransliterationVal(), sample.getTranslationVal());
      context.setUnitToValue(ex.getUnitToValue());
      ex.getDirectlyRelated().add(context);
    }
  }

  @NotNull
  private Exercise getExercise(int projid,
                               int oldid,
                               VocabularyItem vocabularyItem,
                               int creatorID) {
    String termVal = vocabularyItem.getTermVal();
    String alternateFormVal = vocabularyItem.getAlternateFormVal();
    String transliterationVal = vocabularyItem.getTransliterationVal();
    String meaning = vocabularyItem.getMeaningVal();


    return getExercise(projid, oldid, creatorID, termVal, alternateFormVal, transliterationVal, meaning);
  }

  @NotNull
  private Exercise getExercise(int projid,
                               int oldid, int creatorID,
                               String termVal,
                               String alternateFormVal,
                               String transliterationVal,
                               String meaning) {
    Exercise exercise = new Exercise(-1,
        "" + oldid,
        creatorID,
        meaning,
        termVal,
        StringUtils.stripAccents(termVal),
        alternateFormVal,
        "",
        transliterationVal,
        projid,
        false,
        0,
        false
    );
    exercise.setPredef(true);
    exercise.setDominoID(oldid);
    return exercise;
  }

  /*private CommonExercise toExercise(JsonObject jsonObject, List<String> typeOrder) {
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
*//*    Exercise exercise = new Exercise(
        npDID,
        english,
        fl,
        meaning,
        transliteration,
        context,
        contextTranslation,
        dominoID);
    exercise.setUpdateTime(updateMillis);*//*
    // if (!isLegacy) logger.info("NOT LEGACY " + exercise);

*//*    for (String type : typeOrder) {
      try {
        exercise.addUnitToValue(type, noMarkup(content.getString(type.toLowerCase())));
      } catch (Exception e) {
        logger.error("couldn't find unit/chapter '" + type + "' in content - see typeOrder property");
      }
    }*//*

    return exercise;
  }*/

  private String noMarkup(String source) {
    return source.replaceAll("\\<.*?>", "");
  }
//  public static void main(String[] arg) {
//    new DominoExerciseDAO().readExercises("SAMPLE-NO-EXAM.json");
//  }
}
