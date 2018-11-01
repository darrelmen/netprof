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

package mitll.langtest.shared.exercise;

import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.server.database.exercise.IPronunciationLookup;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.npdata.dao.SlickExercise;

import java.util.*;

import static mitll.langtest.server.database.user.BaseUserDAO.UNDEFINED_USER;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends AudioExercise implements CommonExercise,
    MutableExercise, MutableAudioExercise, MutableAnnotationExercise, CommonAnnotatable {
  protected String oldid = "";
  private int dominoID = -1;
  private int dominoContextIndex = -1;

  private transient Collection<String> refSentences = new ArrayList<String>();

  private transient List<String> firstPron = new ArrayList<>();
  private long updateTime = 0;

  private List<ClientExercise> directlyRelated = new ArrayList<>();

  private boolean safeToDecode;
  private transient long safeToDecodeLastChecked;

  private int creator = UNDEFINED_USER;

  private boolean isPredef = true;
  /**
   *
   */
  private boolean isOverride;


  /**
   * TODO : why do we need to carry this around?
   */
  protected Map<String, String> unitToValue = new HashMap<>(3);

  protected String transliteration = "";

  /**
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addAttributeToExercise(Map, Map, CommonExercise)
   */
  private List<ExerciseAttribute> attributes = new ArrayList<>();

  private String noAccentFL;

  private int parentExerciseID = -1;

  // for serialization
  public Exercise() {
  }

  /**
   * @param id
   * @param altcontext
   * @param projectid
   * @param updateTime
   * @param noAccentFL
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public Exercise(String id,
                  String context,
                  String altcontext,
                  String contextTranslation,
                  String meaning,
                  int projectid,
                  long updateTime,
                  String noAccentFL) {
    super(-1, projectid, false);
    this.oldid = id;
    this.meaning = meaning;
    this.updateTime = updateTime;
    this.isPredef = true;
    this.noAccentFL = noAccentFL;
    addContext(context, altcontext, contextTranslation);
  }

  /**
   * @param exid
   * @param creator
   * @param english
   * @param projectid
   * @see EditItem#getNewItem
   */
  public Exercise(int exid, int creator, String english, int projectid, boolean isPredef) {
    this.id = exid;
    this.creator = creator;
    this.english = english;
    this.projectid = projectid;
    this.isPredef = isPredef;
  }

  /**
   * @param id
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @param noAccentFL
   * @param projectid
   * @Deprecated - use related exercise join table
   * @see #addContext
   */
  @Deprecated
  private Exercise(String id, String context, String altcontext, String contextTranslation, String noAccentFL, int projectid) {
    super(-1, projectid, true);
    this.foreignLanguage = context;
    this.altfl = altcontext;
    this.english = contextTranslation;
    this.oldid = id;
    this.noAccentFL = noAccentFL;
  }


  /**
   * @param exid
   * @param oldid
   * @param creator
   * @param englishSentence
   * @param foreignLanguage
   * @param noAccentFL
   * @param altFL
   * @param meaning
   * @param transliteration
   * @param projectid
   * @param candecode
   * @param lastChecked
   * @param isContext
   * @param dominoID
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#makeExercise
   */
  public Exercise(int exid,
                  String oldid,
                  int creator,
                  String englishSentence,
                  String foreignLanguage,
                  String noAccentFL,
                  String altFL,
                  String meaning,
                  String transliteration,
                  int projectid,
                  boolean candecode,
                  long lastChecked,
                  boolean isContext,
                  int numPhones,
                  int dominoID) {
    super(exid, projectid, isContext);
    this.oldid = oldid;
    this.creator = creator;
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    this.noAccentFL = noAccentFL;
    setTransliteration(transliteration);
    setAltFL(altFL);
    this.safeToDecode = candecode;
    safeToDecodeLastChecked = lastChecked;

    this.numPhones = numPhones;
    this.dominoID = dominoID;
  }

  /**
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param noAccentFL
   * @param altFL
   * @param transliteration
   * @param isOverride
   * @param modifiedTimestamp
   * @param projectid
   * @param candecode
   * @param lastChecked
   * @param isContext
   * @param dominoID
   * @param shouldSwap
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#fromSlick
   */
  public Exercise(int uniqueID,
                  String exerciseID,
                  int creator,
                  String english,
                  String foreignLanguage,
                  String noAccentFL,
                  String altFL,
                  String transliteration,
                  boolean isOverride,
                  Map<String, String> unitToValue,
                  long modifiedTimestamp,
                  int projectid,
                  boolean candecode,
                  long lastChecked,
                  boolean isContext,
                  int numPhones,
                  int dominoID,
                  boolean shouldSwap) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, noAccentFL, altFL, "", transliteration,
        projectid, candecode, lastChecked, isContext, numPhones, dominoID);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.updateTime = modifiedTimestamp;
    this.safeToDecode = candecode;
    this.numPhones = numPhones;
    //  this.tokens = tokens;
  }

  /**
   * be careful not to lose any fields
   *
   * @param exercise
   * @see FlexListLayout#getFactory(PagingExerciseList)
   */
  public <T extends CommonExercise> Exercise(T exercise) {
    super(exercise.getID(), exercise.getProjectID(), exercise.isContext());
    this.isPredef = exercise.isPredefined();
//    this.isContext = exercise.isContext();
    this.english = exercise.getEnglish();
    this.foreignLanguage = exercise.getForeignLanguage();
    this.transliteration = exercise.getTransliteration();
    this.meaning = exercise.getMeaning();
    this.dominoID = exercise.getDominoID();
    this.oldid = exercise.getOldID();

    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    //   setState(exercise.getState());
    //setSecondState(exercise.getSecondState());

    setAttributes(exercise.getAttributes());

    exercise.getDirectlyRelated().forEach(this::addContextExercise);

    copyAudio(exercise);
    this.creator = exercise.getCreator();
  }

//  @Override
//  public CommonShell getShell() {
//    return new ExerciseShell(english, meaning, foreignLanguage, getID(), numPhones, isContext);
//  }

  public CommonShell asShell() {
    return this;
  }

  private void copyAudio(AudioRefExercise exercise) {
    exercise.getAudioAttributes().forEach(this::addAudio);
  }

  @Override
  public Collection<String> getRefSentences() {
    return refSentences;
  }

  /**
   * @param sentenceRefs
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  @Override
  public void setRefSentences(Collection<String> sentenceRefs) {
    this.refSentences = sentenceRefs;
  }

  @Override
  public boolean isPredefined() {
    return isPredef;
  }

  /**
   * @return
   * @see UserDAO#DEFAULT_USER_ID
   */
  @Override
  public int getCreator() {
    return creator;
  }

  @Override
  public MutableExercise getMutable() {
    return this;
  }

  @Override
  public MutableAudioExercise getMutableAudio() {
    return this;
  }

  @Override
  public MutableAnnotationExercise getMutableAnnotation() {
    return this;
  }

  @Override
  public CommonExercise asCommon() {
    return this;
  }

  /**
   * @param exerciseAttributes
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addAttributeToExercise(Map, Map, CommonExercise)
   */
  @Override
  public void setAttributes(List<ExerciseAttribute> exerciseAttributes) {
    this.attributes = exerciseAttributes;
  }

  /**
   * @return
   * @see mitll.langtest.client.banner.ListenViewHelper#getTurns
   */
  @Override
  public List<ExerciseAttribute> getAttributes() {
    return attributes;
  }

  public boolean addAttribute(ExerciseAttribute attribute) {
    return attributes.add(attribute);
  }

  /**
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @seex mitll.langtest.server.database.exercise.JSONURLExerciseDAO#addContextSentences
   */
  private void addContext(String context, String altcontext, String contextTranslation) {
    if (!context.trim().isEmpty()) {
      Exercise contextExercise = new Exercise("c" + getID(), context, altcontext, contextTranslation, noAccentFL, getProjectID());
      contextExercise.setUpdateTime(getUpdateTime());
      contextExercise.setUnitToValue(getUnitToValue());
      addContextExercise(contextExercise);
      //   logger.info("addContext adding " + contextExercise);
    }
  }

  @Override
  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  /**
   * @param englishSentence
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public void setEnglishSentence(String englishSentence) {
    this.english = englishSentence;
  }


  /**
   * @return
   * @see mitll.langtest.server.sorter.ExerciseSorter#phoneCompFirst
   */
  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

  /**
   * @param firstPron
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   */
  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
  }

  /**
   * @param updateTime
   * @seex mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise
   */
  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  /**
   * @param contextExercise
   * @see #addContext(String, String, String)
   * @see mitll.langtest.server.database.exercise.DBExerciseDAO#attachContextExercises
   */
  public void addContextExercise(ClientExercise contextExercise) {
    directlyRelated.add(contextExercise);
  }

  public boolean hasContext() {
    return !getDirectlyRelated().isEmpty();
  }


  public String getContext() {
    return hasContext() ? getDirectlyRelated().iterator().next().getForeignLanguage() : "";
  }

  public String getContextTranslation() {
    return hasContext() ? getDirectlyRelated().iterator().next().getEnglish() : "";
  }

  public List<ClientExercise> getDirectlyRelated() {
    return directlyRelated;
  }

  /**
   * @return
   * @seex mitll.langtest.client.custom.exercise.ContextCommentNPFExercise#getItemContent
   */
  public boolean isSafeToDecode() {
    return safeToDecode;
  }

  /**
   * @param safeToDecode
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSAndCountPhones
   */
  public void setSafeToDecode(boolean safeToDecode) {
    this.safeToDecode = safeToDecode;
  }

  /**
   * @return
   * @deprecated we don't do overrides any more
   */
  public boolean isOverride() {
    return isOverride;
  }

  public void setCreator(int creator) {
    this.creator = creator;
  }

  public void setID(int uniqueID) {
    this.id = uniqueID;
  }

  /**
   * I would love for this to be deprecated, but...
   *
   * @return
   */
  public String getOldID() {
    return oldid;
  }

  /**
   * @param id
   * @see UserExerciseDAO#add
   */
  public void setOldID(String id) {
    this.oldid = id;
  }

  /**
   * @return
   */
  public Map<String, String> getUnitToValue() {
    return unitToValue;
  }

  /**
   * @param unit
   * @param value
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   */
  public boolean addUnitToValue(String unit, String value) {
    if (value == null) {
      return false;
    } else {
      unitToValue.put(unit, value);
      return true;
    }
  }

  public void addPair(Pair pair) {
    unitToValue.put(pair.getProperty(), pair.getValue());
  }

  /**
   * @param unitToValue
   * @see mitll.langtest.shared.exercise.Exercise#Exercise
   */
  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public void setPairs(List<Pair> pairs) {
    for (Pair pair : pairs) unitToValue.put(pair.getProperty(), pair.getValue());
  }

  @Override
  public String getTransliteration() {
    return transliteration;
  }

  public int getDominoID() {
    return dominoID;
  }

  public long getLastChecked() {
    return safeToDecodeLastChecked;
  }

  /**
   * @param numPhones
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addExerciseToSectionHelper(SlickExercise, Collection, ISection, Map, IPronunciationLookup, Exercise, Collection, List)
   */
  public void setNumPhones(int numPhones) {
    this.numPhones = numPhones;
  }

  public void setPredef(boolean isPredef) {
    this.isPredef = isPredef;
  }


  @Override
  public int getParentExerciseID() {
    return parentExerciseID;
  }

  /**
   * @param parentExerciseID
   */
  @Override
  public void setParentExerciseID(int parentExerciseID) {
    this.parentExerciseID = parentExerciseID;
  }

  /**
   * @param parentDominoID
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#addContextSentences
   */
//  @Override
//  public void setParentDominoID(int parentDominoID) {
//  }

  /**
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#getDominoIDToContextExercise
   */
  @Override
  public int getDominoContextIndex() {
    return dominoContextIndex;
  }

  /**
   * @param dominoContextIndex
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#addContextSentences
   */
  public void setDominoContextIndex(int dominoContextIndex) {
    this.dominoContextIndex = dominoContextIndex;
  }

  public String toString() {
    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();

    // warn about attr that have no user
    StringBuilder builder = new StringBuilder();
    for (AudioAttribute attr : audioAttributes1) {
      if (attr.getUser() == null) {
        builder.append("\t").append(attr.toString()).append("\n");
      }
    }

    return "Exercise #" +

        getID() +
        ", domino # " + getDominoID() +
        " np id '" + getOldID() + "'" +
        " context index " + dominoContextIndex +
        " project " + projectid +

        (shouldSwap() ? "\n\tshouldSwap = " + shouldSwap() : "") +

        "'" + getEnglish() +
        "'/'" + getForeignLanguage() + "' " +
        (getAltFL().isEmpty() ? "" : getAltFL()) +
        "meaning '" + getMeaning() +
        "' transliteration '" + getTransliteration() +
        "' context " + getDirectlyRelated() +
        " audio childCount = " + audioAttributes1.size() +
        (builder.toString().isEmpty() ? "" : " \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue() +
        " attr " + getAttributes();
  }

}