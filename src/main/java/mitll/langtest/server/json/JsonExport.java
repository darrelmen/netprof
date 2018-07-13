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

package mitll.langtest.server.json;

import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.server.database.exercise.Facet.SUB_TOPIC;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/5/16.
 */
public class JsonExport {
  private static final Logger logger = LogManager.getLogger(JsonExport.class);

  private static final String NO = "NO";

  private static final String TYPE = "type";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  /**
   * @see JsonExport#getJsonForNode
   */
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
//  private static final String MN = "mn";

  private static final int MAX_DEPTH = 2;

  private final Map<String, Integer> phoneToCount;
  private final ISection<CommonExercise> sectionHelper;
  private final Collection<Integer> preferredVoices;
  private final boolean isEnglish;

  /**
   * @param phoneToCount
   * @param sectionHelper
   * @param preferredVoices
   * @param isEnglish
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters
   */
  public JsonExport(Map<String, Integer> phoneToCount,
                    ISection<CommonExercise> sectionHelper,
                    Collection<Integer> preferredVoices,
                    boolean isEnglish) {
    this.phoneToCount = phoneToCount;
    this.sectionHelper = sectionHelper;
    this.preferredVoices = preferredVoices;
    this.isEnglish = isEnglish;
  }


  /**
   * @param removeExercisesWithMissingAudio
   * @return
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters
   */
  public JSONArray getContentAsJson(boolean removeExercisesWithMissingAudio) {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<>();

    Collection<SectionNode> sectionNodesForTypes = sectionHelper.getSectionNodesForTypes();

    //logger.info("getContentAsJson : section nodes " + sectionNodesForTypes);
    List<String> typeOrder = sectionHelper.getTypeOrder();
    boolean semester = typeOrder.iterator().next().startsWith("Semester");
    if (semester) {
      logger.info("getContentAsJson first is semester - pashto???");
    }

    int maxDepthToUse = semester ? MAX_DEPTH + 1 : MAX_DEPTH;
    List<String> minimalTypeOrder = typeOrder.size() > maxDepthToUse ? typeOrder.subList(0, maxDepthToUse) : typeOrder;

    for (SectionNode node : sectionNodesForTypes) {
      String type = node.getType();
      //logger.info("\tgetContentAsJson type " + type + " : " + node.getName());
      typeToValues.put(type, Collections.singletonList(node.getName()));
      JSONObject jsonForNode = getJsonForNode(node, typeToValues, removeExercisesWithMissingAudio, minimalTypeOrder);
      typeToValues.remove(type);

      jsonArray.add(jsonForNode);
    }
    return jsonArray;
  }

  /**
   * @param node
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @param firstTypes
   * @return
   * @see #getContentAsJson
   */
  private JSONObject getJsonForNode(SectionNode node,
                                    Map<String, Collection<String>> typeToValues,
                                    boolean removeExercisesWithMissingAudio,
                                    Collection<String> firstTypes) {
    JSONObject jsonForNode = new JSONObject();
    String type = node.getType();
    jsonForNode.put(TYPE, type);
    jsonForNode.put(NAME, node.getName());
    JSONArray jsonArray = new JSONArray();

    {
      // logger.info("getJsonForNode node " + type + " = " + node.getName() + " vs " + firstTypes);

      if (node.isLeaf() || !firstTypes.iterator().next().equalsIgnoreCase(type)) { // stop when get below first types, e.g. unit,chapter
        // logger.info("getJsonForNode leaf " + typeToValues.keySet() + " types");
        jsonForNode.put(ITEMS, getJsonForSelection(typeToValues, removeExercisesWithMissingAudio, true, firstTypes));
      } else {
        List<SectionNode> children = node.getChildren();
        //     logger.info("getJsonForNode node " + node.getType() + " = " + node.getName() + " with " + children.size() + " children");

        for (SectionNode child : children) {
          typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
          //     logger.info("\tgetJsonForNode node " + child.getType() + " = " + child.getName() + " typeToValues " + typeToValues);
          jsonArray.add(getJsonForNode(child, typeToValues, removeExercisesWithMissingAudio, firstTypes));
          typeToValues.remove(child.getType());
        }
      }
      jsonForNode.put(CHILDREN, jsonArray);
    }
    return jsonForNode;
  }

  /**
   * @param typeToValues                    for this unit and chapter
   * @param removeExercisesWithMissingAudio
   * @param firstTypes
   * @return
   * @see #getJsonForNode
   */
  private JSONArray getJsonForSelection(
      Map<String, Collection<String>> typeToValues,
      boolean removeExercisesWithMissingAudio,
      boolean removeCantDecode,
      Collection<String> firstTypes) {
    return getJsonArray(getFilteredExercises(typeToValues, removeExercisesWithMissingAudio, removeCantDecode), firstTypes);
  }

  /**
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @param removeCantDecode
   * @return
   */
  @NotNull
  private List<CommonExercise> getFilteredExercises(Map<String, Collection<String>> typeToValues, boolean removeExercisesWithMissingAudio, boolean removeCantDecode) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    if (exercisesForState.isEmpty()) {
      logger.warn("getJsonForSelection: no exercises for selection " + typeToValues);
    }

    List<CommonExercise> copy = new ArrayList<>(exercisesForState);

    if (removeExercisesWithMissingAudio) {
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        if (!iterator.next().hasRefAudio()) iterator.remove();
      }
    }

    if (removeCantDecode) {
//      logger.info("getJsonForSelection before had " + exercisesForState.size());
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        if (!next.isSafeToDecode()) iterator.remove();
      }
      //    logger.info("getJsonForSelection after  had " + exercisesForState.size());
    }
    getExerciseSorter().sortedByPronLengthThenPhone(copy, phoneToCount);
    return copy;
  }

  private ExerciseSorter getExerciseSorter() {
    return new ExerciseSorter(phoneToCount);
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
   * @param firstTypes
   * @return
   * @see #getJsonForSelection
   */
  private JSONArray getJsonArray(Collection<CommonExercise> copy, Collection<String> firstTypes) {
    JSONArray exercises = new JSONArray();
    copy.forEach(commonExercise -> exercises.add(getJsonForExercise(commonExercise, firstTypes)));
    return exercises;
  }

  /**
   * Make json for an exercise
   * <p>
   * Prefer recordings by voices on the preferred list.
   *
   * @param exercise
   * @param firstTypes
   * @return
   * @see #getJsonArray(Collection, Collection)
   */
  private <T extends CommonExercise> JSONObject getJsonForExercise(T exercise, Collection<String> firstTypes) {
    JSONObject ex = getJsonForCommonExercise(exercise, firstTypes);

    addContextAudioRefs(exercise, ex, exercise.getDirectlyRelated());
    addLatestRefs(preferredVoices, exercise, ex);

    return ex;
  }

  /**
   * Add male & female context sentence audio and ref audio
   * <p>
   * Used to consider checking for MP3 versions.
   *
   * @param <T>
   * @param exercise
   * @param ex
   * @param directlyRelated
   * @see #getJsonForExercise(CommonExercise, Collection)
   */
  private <T extends AudioAttributeExercise> void addContextAudioRefs(T exercise, JSONObject ex,
                                                                      Collection<ClientExercise> directlyRelated) {
    AudioAttribute latestContext = exercise.getLatestContext(true);

    if (latestContext == null) {
      // logger.info("Found " + latestContext);
      latestContext = directlyRelated
          .stream()
          .findFirst()
          .map(contextSentence -> contextSentence.asCommon().getLatestContext(true))
          .orElse(latestContext);
    }
    //if (latestContext != null) {
    //  String author = latestContext.getUser().getUserID();
    //  if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put(CTMREF, latestContext == null ? NO : latestContext.getAudioRef());
    latestContext = exercise.getLatestContext(false);

    if (latestContext == null) {
      latestContext = directlyRelated
          .stream()
          .findFirst()
          .map(contextSentence -> contextSentence.asCommon().getLatestContext(false))
          .orElse(latestContext);
    }

    // if (latestContext != null) {
    // String author = latestContext.getUser().getUserID();
    // if (CHECK_FOR_MP3) ensureMP3(latestContext.getAudioRef(), exercise.getContext(), author);
    // }
    ex.put(CTFREF, latestContext == null ? NO : latestContext.getAudioRef());
    ex.put(REF, exercise.hasRefAudio() ? exercise.getRefAudioWithPrefs(preferredVoices) : NO);
  }

  /**
   * TODO : add json array of context sentences - be careful to maintain backward compatibility
   *
   * Only adds first context sentence if it's there.
   *
   * Don't redundantly include the unit and chapter markings - we know those from it's position in the nested chapters
   *
   * Darrel doesn't like Sub-topic for some reason.
   *
   * @param exercise
   * @param firstTypes
   * @return
   * @see #getJsonForExercise
   */
  private JSONObject getJsonForCommonExercise(ClientExercise exercise, Collection<String> firstTypes) {
    JSONObject ex = new JSONObject();
    ex.put(ID, exercise.getID());
    ex.put(FL, exercise.getFLToShow());
    ex.put(TL, exercise.getTransliteration() == null ? "" : exercise.getTransliteration());
    ex.put(EN, isEnglish && !exercise.getMeaning().isEmpty() ? exercise.getMeaning() : exercise.getEnglish());

    Map<String, String> unitToValue = exercise.getUnitToValue();
    unitToValue.forEach((k, v) -> {
      if (!firstTypes.contains(k)) {
        String keyToUse = k.equalsIgnoreCase(SUB_TOPIC.getName()) ? SUB_TOPIC.getAlt() : k;
        ex.put(keyToUse, v);
      }
    });
    //  if (addMeaning) ex.put(MN, exercise.getMeaning());

    if (exercise.getDirectlyRelated().isEmpty()) {
      ex.put(CT, "");
      ex.put(CTR, "");
    } else {
      ClientExercise next = exercise.getDirectlyRelated().iterator().next();
      ex.put(CT,  next.getFLToShow());
      ex.put(CTR, next.getEnglish());
    }

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
  private void addLatestRefs(Collection<Integer> preferredVoices, AudioRefExercise exercise, JSONObject ex) {
    String mr = null, ms = null, fr = null, fs = null;
    long mrt = 0, mst = 0, frt = 0, fst = 0;
    AudioAttribute mra = null, msa = null, fra = null, fsa = null;

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      long timestamp = audioAttribute.getTimestamp();
      if (!audioAttribute.isContextAudio()) {
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
