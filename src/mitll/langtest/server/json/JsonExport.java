package mitll.langtest.server.json;

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

/**
 * Created by go22670 on 2/5/16.
 */
public class JsonExport {
  private static final String NO = "NO";

  private static final String TYPE = "type";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  private static final String CHILDREN = "children";
  private final Map<String, Integer> phoneToCount;
  private final SectionHelper<CommonExercise> sectionHelper;
  private final Set<Long> preferredVoices;

  public JsonExport(Map<String, Integer> phoneToCount, SectionHelper<CommonExercise> sectionHelper,
                    Set<Long> preferredVoices) {
    this.phoneToCount = phoneToCount;
    this.sectionHelper = sectionHelper;
    this.preferredVoices = preferredVoices;
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
      JSONArray exercises = getJsonForSelection(typeToValues, removeExercisesWithMissingAudio);
      jsonForNode.put(ITEMS, exercises);
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

    //  boolean removeExercisesWithMissingAudio = REMOVE_EXERCISES_WITH_MISSING_AUDIO;
    if (removeExercisesWithMissingAudio) {
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        if (!next.hasRefAudio()) iterator.remove();
      }
    }
//    if (audioFileHelper != null) {
      getExerciseSorter().sortedByPronLengthThenPhone(copy, phoneToCount);// audioFileHelper.getPhoneToCount());
  ///  }
    /* else {
      logger.warn("audioFileHelper not set yet!");
    }*/
    return getJsonArray(copy);
  }

  private ExerciseSorter getExerciseSorter() {
    //Map<String, Integer> phoneToCount = audioFileHelper == null ? new HashMap<>() : audioFileHelper.getPhoneToCount();
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

/*    Map<String, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio();
    String installPath = pathHelper.getInstallPath();*/

    for (CommonExercise exercise : copy) {
/*      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
      }*/
      //if (!debug) ensureMP3s(exercise);
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
  private JSONObject getJsonForExercise(CommonExercise exercise) {
    JSONObject ex = new JSONObject();
    ex.put("id", exercise.getID());
    ex.put("fl", exercise.getForeignLanguage());
    ex.put("tl", exercise.getTransliteration());
    ex.put("en", exercise.getEnglish());
    ex.put("ct", exercise.getContext());
    ex.put("ctr", exercise.getContextTranslation());
    AudioAttribute latestContext = exercise.getLatestContext(true);
    //if (latestContext != null) {
    //  String author = latestContext.getUser().getUserID();
    //  if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put("ctmref", latestContext == null ? NO : latestContext.getAudioRef());
    latestContext = exercise.getLatestContext(false);
    // if (latestContext != null) {
    // String author = latestContext.getUser().getUserID();
    // if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put("ctfref", latestContext == null ? NO : latestContext.getAudioRef());
    ex.put("ref", exercise.hasRefAudio() ? exercise.getRefAudioWithPrefs(preferredVoices) : NO);

    addLatestRefs(preferredVoices, exercise, ex);

    return ex;
  }

  /**
   * Male/female reg/slow speed
   * Prefer voices on the preferred list.
   *
   * @param exercise
   * @param ex
   * @see #getJsonForExercise
   */
  private void addLatestRefs(Set<Long> preferredVoices, AudioRefExercise exercise, JSONObject ex) {
    //Set<Long> preferredVoices = serverProps.getPreferredVoices();

    String mr = null, ms = null, fr = null, fs = null;
    long mrt = 0, mst = 0, frt = 0, fst = 0;
    AudioAttribute mra = null, msa = null, fra = null, fsa = null;

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      long timestamp = audioAttribute.getTimestamp();
      //boolean isPrefVoice = preferredVoices.contains(audioAttribute.getUserid());
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

  /**
   * @param sectionNodes
   * @param removeExercisesWithMissingAudio
   * @return
   * @seexx mitll.langtest.server.ScoreServlet#getJsonLeastRecordedChapters
   */
/*  public JSONArray getContentAsJson2(Collection<SectionNode> sectionNodes, boolean removeExercisesWithMissingAudio) {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<>();

    //if (audioFileHelper != null) {
      for (SectionNode node : sectionNodes) {
        String type = node.getType();
        typeToValues.put(type, Collections.singletonList(node.getName()));
        JSONObject jsonForNode = getJsonForNode2(node, typeToValues, removeExercisesWithMissingAudio);
        typeToValues.remove(type);

        jsonArray.add(jsonForNode);
      }
    //}
    return jsonArray;
  }*/

  /**
   * @param node
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @return
   * @see #getContentAsJson2
   */
/*  private JSONObject getJsonForNode2(SectionNode node, Map<String, Collection<String>> typeToValues, boolean removeExercisesWithMissingAudio) {
    JSONObject jsonForNode = new JSONObject();
    jsonForNode.put(TYPE, node.getType());
    jsonForNode.put(NAME, node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      JSONArray exercises = getJsonForSelection(typeToValues, removeExercisesWithMissingAudio);
      jsonForNode.put(ITEMS, exercises);
    } else {
      List<SectionNode> children = node.getChildren();
      Collections.sort(children); // by avg recorded number

      for (SectionNode child : children) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode2(child, typeToValues, removeExercisesWithMissingAudio));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put(CHILDREN, jsonArray);
    return jsonForNode;
  }*/
}
