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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.*;
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

  private static final String CTID = "ctid";
  private static final String CT = "ct";
  private static final String CTR = "ctr";
//  private static final String MN = "mn";

  private static final int MAX_DEPTH = 2;
  public static final String SEMESTER = "Semester";

  private final Map<String, Integer> phoneToCount;
  private final ISection<CommonExercise> sectionHelper;
  private final Collection<Integer> preferredVoices;
  private final boolean isEnglish;
  AudioFileHelper audioFileHelper;

  /**
   * @param phoneToCount
   * @param sectionHelper
   * @param preferredVoices
   * @param isEnglish
   * @param audioFileHelper
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters
   */
  public JsonExport(Map<String, Integer> phoneToCount,
                    ISection<CommonExercise> sectionHelper,
                    Collection<Integer> preferredVoices,
                    boolean isEnglish,
                    AudioFileHelper audioFileHelper) {
    this.phoneToCount = phoneToCount;
    this.sectionHelper = sectionHelper;
    this.preferredVoices = preferredVoices;
    this.isEnglish = isEnglish;
    this.audioFileHelper = audioFileHelper;
  }

  /**
   * @param removeExercisesWithMissingAudio
   * @param justContext
   * @return
   * @see mitll.langtest.server.ScoreServlet#getJsonNestedChapters
   */
  public JsonArray getContentAsJson(boolean removeExercisesWithMissingAudio, boolean justContext) {
    JsonArray jsonArray = new JsonArray();
    Map<String, Collection<String>> typeToValues = new HashMap<>();

    List<String> minimalTypeOrder = getMinimalTypeOrder();

    for (SectionNode node : sectionHelper.getSectionNodesForTypes()) {
      String type = node.getType();
      String name = node.getName();

      typeToValues.put(type, Collections.singletonList(name));
      JsonObject jsonForNode = getJsonForNode(node, typeToValues, removeExercisesWithMissingAudio, justContext, minimalTypeOrder);
      typeToValues.remove(type);

      if (jsonForNode != null) {
        jsonArray.add(jsonForNode);
      } else logger.info("no content for " + type + " = " + name);
    }
    return jsonArray;
  }

  public JsonArray getContentAsJsonFor(boolean removeMissingAudio, List<CommonExercise> exercises) {
    return getJsonExerciseArray(
        getFilteredExercises(removeMissingAudio, true, exercises),
        getMinimalTypeOrder());
  }

  @NotNull
  private List<String> getMinimalTypeOrder() {
    List<String> typeOrder = sectionHelper.getTypeOrder();
    boolean semester = typeOrder.iterator().next().startsWith(SEMESTER);
    if (semester) {
      logger.info("getContentAsJson first is semester - pashto???");
    }

    int maxDepthToUse = semester ? MAX_DEPTH + 1 : MAX_DEPTH;
    return typeOrder.size() > maxDepthToUse ? typeOrder.subList(0, maxDepthToUse) : typeOrder;
  }

  /**
   * @param node
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @param justContext
   * @param firstTypes
   * @return null if no content
   * @see #getContentAsJson
   */
  private JsonObject getJsonForNode(SectionNode node,
                                    Map<String, Collection<String>> typeToValues,
                                    boolean removeExercisesWithMissingAudio,
                                    boolean justContext,
                                    Collection<String> firstTypes) {
    String type = node.getType();
    String name = node.getName();

    JsonObject jsonForNode = new JsonObject();
    jsonForNode.addProperty(TYPE, type);
    jsonForNode.addProperty(NAME, name);
    JsonArray jsonArray = new JsonArray();

    {
      // logger.info("getJsonForNode node " + type + " = " + node.getName() + " vs " + firstTypes);

      boolean leaf = node.isLeaf();
      if (leaf || !firstTypes.iterator().next().equalsIgnoreCase(type)) { // stop when get below first types, e.g. unit,chapter
        // logger.info("getJsonForNode leaf " + typeToValues.keySet() + " types");
        JsonArray jsonForSelection = getJsonForSelection(typeToValues, removeExercisesWithMissingAudio, true, justContext, firstTypes);
        jsonForNode.add(ITEMS, jsonForSelection);
        if (jsonForSelection.size() == 0) {
          logger.info("getJsonForNode no leaf content for " + type + " = " + name);
          jsonForNode = null;
        }
//        else {
//          logger.info("getJsonForNode leaf content for " + type + " = " + name + " : " +jsonForSelection.size());
//        }
      } else {
        List<SectionNode> children = node.getChildren();
        //     logger.info("getJsonForNode node " + node.getType() + " = " + node.getName() + " with " + children.size() + " children");

        for (SectionNode child : children) {
          String type1 = child.getType();
          String name1 = child.getName();
          typeToValues.put(type1, Collections.singletonList(name1));

//          logger.info("\tgetJsonForNode node " + child.getType() + " = " + child.getName() + " typeToValues " + typeToValues);

          JsonObject jsonForNode1 = getJsonForNode(child, typeToValues, removeExercisesWithMissingAudio, justContext, firstTypes);

          if (jsonForNode1 != null) {
            jsonArray.add(jsonForNode1);
          } else {
            logger.info("getJsonForNode no content for " + type1 + " = " + name1);
          }
          typeToValues.remove(type1);
        }
      }

      if (!leaf && jsonForNode != null) {
        jsonForNode.add(CHILDREN, jsonArray);
        if (jsonArray.size() == 0 ) {
          logger.info("getJsonForNode no content for " + type + " = " + name);

          jsonForNode = null;
        }
      }
    }
    return jsonForNode;
  }

  /**
   * @param typeToValues                    for this unit and chapter
   * @param removeExercisesWithMissingAudio
   * @param justContext
   * @param firstTypes
   * @return
   * @see #getJsonForNode
   */
  private JsonArray getJsonForSelection(
      Map<String, Collection<String>> typeToValues,
      boolean removeExercisesWithMissingAudio,
      boolean removeCantDecode,
      boolean justContext,
      Collection<String> firstTypes) {
    return getJsonExerciseArray(getFilteredExercises(typeToValues, removeExercisesWithMissingAudio, removeCantDecode, justContext), firstTypes);
  }

  /**
   * @param typeToValues
   * @param removeExercisesWithMissingAudio
   * @param removeCantDecode
   * @param justContext
   * @return
   */
  @NotNull
  private List<CommonExercise> getFilteredExercises(Map<String, Collection<String>> typeToValues,
                                                    boolean removeExercisesWithMissingAudio,
                                                    boolean removeCantDecode,
                                                    boolean justContext) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    if (exercisesForState.isEmpty()) {
      logger.warn("getJsonForSelection: no exercises for selection " + typeToValues);
    }

    if (justContext) {
      List<CommonExercise> context = new ArrayList<>();
//      logger.info("for " + typeToValues + " found " + exercisesForState.size());
      exercisesForState.forEach(ex -> {
        ex.getDirectlyRelated().forEach(c -> context.add(c.asCommon()));
      });
  //    logger.info("for " + typeToValues + " context " + context.size());

      exercisesForState = context;
    }
    List<CommonExercise> filteredExercises = getFilteredExercises(removeExercisesWithMissingAudio, removeCantDecode, exercisesForState);
    if (!exercisesForState.isEmpty() && filteredExercises.isEmpty()) {
      logger.warn("getFilteredExercises for " +typeToValues+ " removed all " + exercisesForState.size());
    }
    return filteredExercises;
  }

  @NotNull
  private List<CommonExercise> getFilteredExercises(boolean removeExercisesWithMissingAudio,
                                                    boolean removeCantDecode,
                                                    Collection<CommonExercise> exercisesForState) {
    List<CommonExercise> copy = new ArrayList<>(exercisesForState);

    removeMissingAudio(removeExercisesWithMissingAudio, copy);
    removeCantDecode(removeCantDecode, copy);
    getExerciseSorter().sortedByPronLengthThenPhone(copy, phoneToCount);
    return copy;
  }

  private void removeMissingAudio(boolean removeExercisesWithMissingAudio, List<CommonExercise> copy) {
    if (removeExercisesWithMissingAudio) {
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        if (!next.hasRefAudio()) {
//          logger.warn("removeMissingAudio ex " + next.getID() + " " + next.getForeignLanguage() + " " + next.getEnglish() + " no audio so not returning it.");
          iterator.remove();
        }
      }
    }
  }

  private void removeCantDecode(boolean removeCantDecode, List<CommonExercise> copy) {
    if (removeCantDecode) {
//      logger.info("getJsonForSelection before had " + exercisesForState.size());
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        boolean safeToDecode = next.isSafeToDecode();
        if (!safeToDecode) {
          safeToDecode = audioFileHelper.isValidForeignPhrase(next);
        }
        if (!safeToDecode) {
          logger.warn("removeCantDecode ex " + next.getID() + " " + next.getForeignLanguage() + " " + next.getEnglish() + " not safe to decode so not returning it.");
          iterator.remove();
        }
      }
      //    logger.info("getJsonForSelection after  had " + exercisesForState.size());
    }
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
  private JsonArray getJsonExerciseArray(Collection<CommonExercise> copy, Collection<String> firstTypes) {
    JsonArray exercises = new JsonArray();
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
   * @see #getJsonExerciseArray(Collection, Collection)
   */
  private <T extends CommonExercise> JsonObject getJsonForExercise(T exercise, Collection<String> firstTypes) {
    JsonObject ex = getJsonForCommonExercise(exercise, firstTypes);

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
  private <T extends AudioAttributeExercise> void addContextAudioRefs(T exercise,
                                                                      JsonObject ex,
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
    ex.addProperty(CTMREF, latestContext == null ? NO : latestContext.getAudioRef());
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
    ex.addProperty(CTFREF, latestContext == null ? NO : latestContext.getAudioRef());
    ex.addProperty(REF, exercise.hasRefAudio() ? exercise.getRefAudioWithPrefs(preferredVoices) : NO);
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
  private JsonObject getJsonForCommonExercise(ClientExercise exercise, Collection<String> firstTypes) {
    JsonObject ex = new JsonObject();
    ex.addProperty(ID, exercise.getID());
    ex.addProperty(FL, exercise.getFLToShow());
    ex.addProperty(TL, exercise.getTransliteration() == null ? "" : exercise.getTransliteration());
    ex.addProperty(EN, isEnglish && !exercise.getMeaning().isEmpty() ? exercise.getMeaning() : exercise.getEnglish());

    Map<String, String> unitToValue = exercise.getUnitToValue();
    unitToValue.forEach((k, v) -> {
      if (!firstTypes.contains(k)) {
        String keyToUse = k.equalsIgnoreCase(SUB_TOPIC.getName()) ? SUB_TOPIC.getAlt() : k;
        ex.addProperty(keyToUse, v);
      }
    });
    //  if (addMeaning) ex.addProperty(MN, exercise.getMeaning());

    if (exercise.getDirectlyRelated().isEmpty()) {
      ex.addProperty(CTID, "");
      ex.addProperty(CT, "");
      ex.addProperty(CTR, "");
    } else {
      ClientExercise next = exercise.getDirectlyRelated().iterator().next();
      ex.addProperty(CTID, next.getID());
      ex.addProperty(CT, next.getFLToShow());
      ex.addProperty(CTR, next.getEnglish());
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
  private void addLatestRefs(Collection<Integer> preferredVoices, AudioRefExercise exercise, JsonObject ex) {
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
    ex.addProperty("mrr", mr == null ? NO : mr);

//    if (ms != null) {
//      if (CHECK_FOR_MP3) ensureMP3(ms, foreignLanguage, author);
//    }
    ex.addProperty("msr", ms == null ? NO : ms);

//    if (fr != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fr, foreignLanguage, author);
//    }
    ex.addProperty("frr", fr == null ? NO : fr);

//    if (fs != null) {
//      if (CHECK_FOR_MP3) ensureMP3(fs, foreignLanguage, author);
//    }
    ex.addProperty("fsr", fs == null ? NO : fs);
  }
}
