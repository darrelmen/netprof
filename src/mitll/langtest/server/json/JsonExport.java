package mitll.langtest.server.json;

import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.database.exercise.JSONExerciseDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.exercise.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 2/5/16.
 */
public class JsonExport {
  private static final Logger logger = Logger.getLogger(JsonExport.class);

  private static final String NO = "NO";

  private static final String TYPE = "type";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  private static final String CHILDREN = "children";
  private static final String CTMREF = "ctmref";
  private static final String CTFREF = "ctfref";
  private static final String REF = "ref";
  private static final String ID = "id";
  private static final String FL = "fl";
  private static final String TL = "tl";
  private static final String EN = "en";
  private static final String CT = "ct";
  private static final String CTR = "ctr";
  private static final String MN = "mn";
  private static final String COUNT = "Count";
  private static final String UNIT_ORDER = "UnitOrder";
  private static final String UNIT_CHAPTER_NESTING = "UnitChapterNesting";
  private final Map<String, Integer> phoneToCount;
  private final SectionHelper<CommonExercise> sectionHelper;
  private final Collection<Long> preferredVoices;

  /**
   * @param phoneToCount
   * @param sectionHelper
   * @param preferredVoices
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters(boolean)
   */
  public JsonExport(Map<String, Integer> phoneToCount,
                    SectionHelper<CommonExercise> sectionHelper,
                    Collection<Long> preferredVoices) {
    this.phoneToCount = phoneToCount;
    this.sectionHelper = sectionHelper;
    this.preferredVoices = preferredVoices;
  }

  /**
   * @param json
   * @return
   * @see JSONExerciseDAO#readExercises
   */
  public List<CommonExercise> getExercises(String json) {
    JSONObject object = JSONObject.fromObject(json);
    Collection<String> types = getTypes(object);

    JSONArray content = object.getJSONArray(ScoreServlet.CONTENT);
    List<CommonExercise> exercises = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      JSONObject jsonObject = content.getJSONObject(i);
      CommonExercise commonShell = toExercise(jsonObject, types);
      exercises.add(commonShell);
    }

    for (CommonExercise ex : exercises.subList(0, 10)) {
      logger.info("got " + ex);
    }
    return exercises;
  }

  private List<String> getTypes(JSONObject object) {
    JSONArray jsonArray = object.getJSONArray(UNIT_ORDER);

    List<String> types = new ArrayList<>();
    for (int i = 0; i < jsonArray.size(); i++) types.add(jsonArray.getString(i));
    return types;
  }

  /**
   * @param jsonObject
   * @param exercises
   * @param <T>
   * @see ScoreServlet#getJSONExerciseExport
   */
  public <T extends CommonShell/* & AudioAttributeExercise*/> void addJSONExerciseExport(JSONObject jsonObject,
                                                                                         Collection<T> exercises) {
    jsonObject.put(COUNT, exercises.size());
    jsonObject.put(UNIT_ORDER, addUnitsInOrder());
    jsonObject.put(UNIT_CHAPTER_NESTING, addSections(sectionHelper.getSectionNodes()));
    jsonObject.put(ScoreServlet.CONTENT, getExercisesAsJson(exercises));
  }

  private JSONArray addUnitsInOrder() {
    JSONArray value = new JSONArray();
    for (String type : sectionHelper.getTypeOrder()) {
      value.add(type);
    }
    return value;
  }

  private JSONArray addSections(Collection<SectionNode> sectionNodes) {
    JSONArray nesting = new JSONArray();

    List<SectionNode> sorted = new ArrayList<>(sectionNodes);
    Collections.sort(sorted);
    for (SectionNode node : sorted) {
      JSONObject forNode = new JSONObject();
      forNode.put("type", node.getType());
      forNode.put("name", node.getName());
      Collection<SectionNode> children1 = node.getChildren();
      JSONArray children = children1.isEmpty() ? new JSONArray() : addSections(children1);

      forNode.put("children", children);
      nesting.add(forNode);
    }
    return nesting;
  }

  /**
   * @param exercises
   * @param <T>
   * @return
   * @see #addJSONExerciseExport(JSONObject, Collection)
   */
  public <T extends CommonShell/* & AudioAttributeExercise*/> JSONArray getExercisesAsJson(Collection<T> exercises) {
    JSONArray jsonArray = new JSONArray();
    Collection<T> sortedByID = getSortedByID(exercises);

    // int c = 0;
    for (T exercise : sortedByID) {
      JSONObject jsonForCommonExercise = getJsonForCommonExercise(exercise, true);
      addUnitAndChapter(exercise, jsonForCommonExercise);
      jsonArray.add(jsonForCommonExercise);
      //  if (c++ > 10)break;
    }
    return jsonArray;
  }

  private <T extends CommonShell> Collection<T> getSortedByID(Collection<T> exercises) {
    List<T> copy = new ArrayList<>(exercises);
    Collections.sort(copy, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return o1.getID().compareTo(o2.getID());
      }
    });
    return copy;
  }

  private int c = 0;

  private <T extends CommonShell> void addUnitAndChapter(T exercise, JSONObject jsonForCommonExercise) {
    for (String type : sectionHelper.getTypeOrder()) {
      String value = exercise.getUnitToValue().get(type);
      if (value == null) {
        if (c++ < 10)
          logger.warn("huh? no value for " + type + " for " + exercise.getID() + " : " + exercise.getUnitToValue());
        value = "";
      }
      jsonForCommonExercise.put(type, value);
    }
  }

  /**
   * @param removeExercisesWithMissingAudio
   * @return
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters
   */
  public JSONArray getContentAsJson(boolean removeExercisesWithMissingAudio) {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<>();

    for (SectionNode node : sectionHelper.getSectionNodes()) {
      String type = node.getType();
      typeToValues.put(type, Collections.singletonList(node.getName()));
      JSONObject jsonForNode = getJsonForNode(node, typeToValues, removeExercisesWithMissingAudio);
      typeToValues.remove(type);

      jsonArray.add(jsonForNode);
    }
    return jsonArray;
  }

  /**
   * @param node
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @return
   * @see #getContentAsJson
   */
  private JSONObject getJsonForNode(SectionNode node, Map<String, Collection<String>> typeToValues,
                                    boolean removeExercisesWithMissingAudio) {
    JSONObject jsonForNode = new JSONObject();
    jsonForNode.put(TYPE, node.getType());
    jsonForNode.put(NAME, node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      jsonForNode.put(ITEMS, getJsonForSelection(typeToValues, removeExercisesWithMissingAudio));
    } else {
      for (SectionNode child : node.getChildren()) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode(child, typeToValues, removeExercisesWithMissingAudio));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put(CHILDREN, jsonArray);
    return jsonForNode;
  }

  /**
   * @param typeToValues                    for this unit and chapter
   * @param removeExercisesWithMissingAudio
   * @return
   * @see #getJsonForNode
   */
  private JSONArray getJsonForSelection(
      Map<String, Collection<String>> typeToValues, boolean removeExercisesWithMissingAudio) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    List<CommonExercise> copy = new ArrayList<>(exercisesForState);

    if (removeExercisesWithMissingAudio) {
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        if (!next.hasRefAudio()) iterator.remove();
      }
    }
    getExerciseSorter().sortedByPronLengthThenPhone(copy, phoneToCount);
    return getJsonArray(copy);
  }

  private ExerciseSorter getExerciseSorter() {
    return new ExerciseSorter(sectionHelper.getTypeOrder(), phoneToCount);
  }

  /**
   * TODO : consider putting the attachAudio call back on there???
   * <p>
   * This is the json that describes an individual entry.
   * <p>
   * Makes sure to attach audio to exercises (this is especially important for userexercises that mask out
   * exercises with new reference audio).
   *
   * @param copy
   * @return
   * @see #getJsonForSelection(Map, boolean)
   */
  private JSONArray getJsonArray(List<CommonExercise> copy) {
    JSONArray exercises = new JSONArray();
    for (CommonExercise exercise : copy) {
      exercises.add(getJsonForExercise(exercise));
    }
    return exercises;
  }

  /**
   * Make json for an exercise
   * <p>
   * Prefer recordings by voices on the preferred list.
   *
   * @param exercise
   * @return
   */
  private <T extends CommonShell & AudioAttributeExercise> JSONObject getJsonForExercise(T exercise) {
    JSONObject ex = getJsonForCommonExercise(exercise, false);

    addContextAudioRefs(exercise, ex);
    addLatestRefs(preferredVoices, exercise, ex);

    return ex;
  }

  private <T extends AudioAttributeExercise> void addContextAudioRefs(T exercise, JSONObject ex) {
    AudioAttribute latestContext = exercise.getLatestContext(true);
    //if (latestContext != null) {
    //  String author = latestContext.getUser().getUserID();
    //  if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put(CTMREF, latestContext == null ? NO : latestContext.getAudioRef());
    latestContext = exercise.getLatestContext(false);
    // if (latestContext != null) {
    // String author = latestContext.getUser().getUserID();
    // if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put(CTFREF, latestContext == null ? NO : latestContext.getAudioRef());
    ex.put(REF, exercise.hasRefAudio() ? exercise.getRefAudioWithPrefs(preferredVoices) : NO);
  }

  private JSONObject getJsonForCommonExercise(CommonShell exercise, boolean addMeaning) {
    JSONObject ex = new JSONObject();
    ex.put(ID, exercise.getID());
    ex.put(FL, exercise.getForeignLanguage());
    ex.put(TL, exercise.getTransliteration() == null ? "" : exercise.getTransliteration());
    ex.put(EN, exercise.getEnglish());
    if (addMeaning) ex.put(MN, exercise.getMeaning());
    ex.put(CT, exercise.getContext() == null ? "" : exercise.getContext());
    ex.put(CTR, exercise.getContextTranslation() == null ? "" : exercise.getContextTranslation());
    return ex;
  }

  /**
   * @param jsonObject
   * @param types
   * @return
   * @see #getExercises(String)
   */
  private CommonExercise toExercise(JSONObject jsonObject, Collection<String> types) {
    String id = jsonObject.getString(ID);
    CommonExercise exercise = new Exercise(
        id,
        jsonObject.getString(EN),
        jsonObject.getString(FL),
        jsonObject.getString(MN),
        jsonObject.getString(TL),
        jsonObject.getString(CT),
        jsonObject.getString(CTR),
        id);

    try {
      for (String type : types) {
        if (jsonObject.has(type)) {
          String value = jsonObject.getString(type);
          if (value == null) logger.error("toExercise : missing " + type + " on " + exercise.getID());
          else exercise.addUnitToValue(type, value);
        } else {
          exercise.addUnitToValue(type, "");
        }
      }
    } catch (Exception e) {
      logger.warn("toExercise : got " + e + " for " + exercise.getID());
    }
    return exercise;
  }

  /**
   * Male/female reg/slow speed
   * Prefer voices on the preferred list.
   *
   * @param exercise
   * @param ex
   * @see #getJsonForExercise
   */
  private void addLatestRefs(Collection<Long> preferredVoices, AudioRefExercise exercise, JSONObject ex) {
    String mr = null, ms = null, fr = null, fs = null;
    long mrt = 0, mst = 0, frt = 0, fst = 0;
    AudioAttribute mra = null, msa = null, fra = null, fsa = null;

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      long timestamp = audioAttribute.getTimestamp();
      if (audioAttribute.isMale()) {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= mrt) {
            if (mra == null || !preferredVoices.contains(mra.getUserid())) {
              mrt = timestamp;
              mr = audioAttribute.getAudioRef();
              mra = audioAttribute;
            }
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= mst) {
            if (msa == null || !preferredVoices.contains(msa.getUserid())) {
              mst = timestamp;
              ms = audioAttribute.getAudioRef();
              msa = audioAttribute;
            }
          }
        }
      } else {
        if (audioAttribute.isRegularSpeed()) {
          if (timestamp >= frt) {
            if (fra == null || !preferredVoices.contains(fra.getUserid())) {
              frt = timestamp;
              fr = audioAttribute.getAudioRef();
              fra = audioAttribute;
            }
          }
        } else if (audioAttribute.isSlow()) {
          if (timestamp >= fst) {
            if (fsa == null || !preferredVoices.contains(fsa.getUserid())) {
              fst = timestamp;
              fs = audioAttribute.getAudioRef();
              fsa = audioAttribute;
            }
          }
        }
      }
    }

    // male regular speed reference audio (m.r.r.)
    // we want the item text so we can label the mp3 with a title
    //String foreignLanguage = exercise.getForeignLanguage();

//    if (mr != null) {
//      if (CHECK_FOR_MP3) ensureMP3(mr, foreignLanguage, author);
//    }
    ex.put("mrr", mr == null ? NO : mr);

//    if (ms != null) {
//      if (CHECK_FOR_MP3) ensureMP3(ms, foreignLanguage, author);
//    }
    ex.put("msr", ms == null ? NO : ms);

//    if (fr != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fr, foreignLanguage, author);
//    }
    ex.put("frr", fr == null ? NO : fr);

//    if (fs != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fs, foreignLanguage, author);
//    }
    ex.put("fsr", fs == null ? NO : fs);
  }
}
