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

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.IPronunciationLookup;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.domino.DominoImport;
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.npdata.dao.*;
import mitll.npdata.dao.userexercise.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SlickUserExerciseDAO extends BaseUserExerciseDAO implements IUserExerciseDAO {
  private static final Logger logger = LogManager.getLogger(SlickUserExerciseDAO.class);

  /**
   * TODO : need to do something to allow this to scale well - maybe ajax style nested types, etc.
   */
  // private static final boolean ADD_PHONE_LENGTH = false;
  //public static final boolean ADD_SOUNDS = false;

  private static final String NEW_USER_EXERCISE = "NEW_USER_EXERCISE";
  private static final String UNKNOWN = "UNKNOWN";
  private static final String ANY = "Any";
  private static final String DEFAULT_FOR_EMPTY = ANY;
  private static final String HYDRA = "hydra";
  private static final int DEFAULT_PROJECT = 1;
  private static final boolean WARN_ABOUT_MISSING_PHONES = false;
  private static final String QUOT = "&quot;";
  private static final int MAX_LENGTH = 250;
  private static final String UNKNOWN1 = "unknown";
  /**
   * If we don't have a value for a facet, it's value is "Blank" as opposed to "Any"
   *
   * @see SlickUserExerciseDAO#addPhoneInfo
   */
  private static final String BLANK = "Blank";
  private static final String UNIT = "Unit";

  private final long lastModified = System.currentTimeMillis();
  private final ExerciseDAOWrapper dao;

  private final IAttribute attributeHelper;
  private final IAttributeJoin attributeJoinHelper;
  private final IRelatedExercise relatedExerciseHelper, relatedCoreExerciseHelper;
  //  private Map<Integer, ExercisePhoneInfo> exToPhones;
  private final IUserDAO userDAO;
  private final IRefResultDAO refResultDAO;
  //public static final boolean ADD_PHONE_LENGTH = false;
  private SlickExercise unknownExercise;
  private final boolean hasMediaDir;
  private final String hostName;

  private CommonExercise templateExercise;
  private int unknownExerciseID;
  private DatabaseImpl database;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickUserExerciseDAO(DatabaseImpl database, DBConnection dbConnection) {
    super(database);
    dao = new ExerciseDAOWrapper(dbConnection);
    attributeHelper = new AttributeHelper(new ExerciseAttributeDAOWrapper(dbConnection));
    attributeJoinHelper = new AttributeJoinHelper(new ExerciseAttributeJoinDAOWrapper(dbConnection));
    relatedExerciseHelper = new RelatedExerciseHelper(new RelatedExerciseDAOWrapper(dbConnection));
    relatedCoreExerciseHelper = new RelatedCoreExerciseHelper(new RelatedCoreExerciseDAOWrapper(dbConnection));

    userDAO = database.getUserDAO();
    refResultDAO = database.getRefResultDAO();
    this.database = database;

    String mediaDir = database.getServerProps().getMediaDir();
    hasMediaDir = new File(mediaDir).exists();
    if (!hasMediaDir) {
      logger.info("SlickUserExerciseDAO : no media dir at " + mediaDir + " - this is OK on netprof host.");
    }
    hostName = database.getServerProps().getHostName();
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * Update the netprof 1 exercise ids too, since that's how we'll sync up with domino.
   *
   * @param old
   * @param newprojid
   * @return
   */
  public boolean updateProject(int old, int newprojid) {

    List<SlickExercise> toImport = dao.getAllByProject(old);

    logger.info("updateProject altering " + toImport.size() + " exercises...");
    toImport.forEach(slickExercise -> dao.updateOldEx(slickExercise.id(), old + "-" + slickExercise.exid()));
    logger.info("updateProject DONE altering " + toImport.size() + " exercises...");

    boolean b = dao.updateProject(old, newprojid) > 0;
    if (b) logger.info("updated exercises to            " + newprojid);

    boolean b1 = attributeHelper.updateProject(old, newprojid);
    if (b1) logger.info("updated exercise attributes to " + newprojid);

    boolean b2 = relatedExerciseHelper.updateProject(old, newprojid);
    if (b2) logger.info("updated related exercises  to  " + newprojid);

    return b && b1 && b2;
  }

  @Override
  public boolean updateProjectChinese(int old, int newprojid, List<Integer> justTheseIDs) {
    List<SlickExercise> toImport =
        dao.getAllByProject(old).stream().filter(slickExercise -> slickExercise.exid().startsWith("Custom_")).collect(Collectors.toList());

    logger.info("updateProject altering " + toImport.size() + " exercises...");
    toImport.forEach(slickExercise -> dao.updateOldEx(slickExercise.id(), old + "-" + slickExercise.exid()));
    logger.info("updateProject found    " + toImport.size() + " exercises...");

    justTheseIDs.addAll(toImport.stream().map(SlickExercise::id).collect(Collectors.toList()));
    logger.info("updateProject found    " + justTheseIDs.size() + " ids...");

    int num = dao.updateProjectIn(old, newprojid, justTheseIDs);
    boolean b = num > 0;
    if (b) {
      logger.info("updated exercises to            " + newprojid);
    }
    logger.info("updated " + num +
        "  exercises to            " + newprojid);

    boolean b1 = attributeHelper.updateProject(old, newprojid);
    if (b1) {
      logger.info("updated exercise attributes to " + newprojid);
    }

/*
    boolean b2 = relatedExerciseDAOWrapper.updateProject(old, newprojid) > 0;
    if (b2) logger.info("updated related exercises  to  " + newprojid);
*/

    return b && b1;// && b2;
  }

  /**
   * @param shared
   * @param projectID
   * @param typeOrder
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addUserExercises
   */
  @Override
  public SlickExercise toSlick(Exercise shared, int projectID, Collection<String> typeOrder) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";

    return new SlickExercise(-1,
        shared.getCreator(),
        shared.getOldID(),
        new Timestamp(shared.getUpdateTime()),
        shared.getEnglish(),
        shared.getMeaning(),
        shared.getForeignLanguage(),
        shared.getAltFL(),
        shared.getTransliteration(),
        shared.isOverride(),
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        projectID,  // project id fk
        false,
        false,
        false,
        shared.getID(),
        false,
        never,
        shared.getNumPhones());
  }

  /**
   * TODO : we won't do override items soon, since they will just be domino edits...?
   *
   * @param shared
   * @param isOverride
   * @param isContext
   * @param typeOrder
   * @return
   * @see #add(CommonExercise, boolean, boolean, Collection)
   * @see #update(CommonExercise, boolean, Collection)
   */
  private SlickExercise toSlick(CommonExercise shared,
                                @Deprecated boolean isOverride,
                                boolean isContext,
                                Collection<String> typeOrder) {
    return toSlick(shared, isOverride, shared.getProjectID(), BaseUserDAO.DEFAULT_USER_ID, isContext, typeOrder);
  }

  private int spew = 0;

  /**
   * Deals with &quot; in the fl text.
   *
   * @param shared
   * @param isOverride
   * @param isContext
   * @param typeOrder
   * @return
   * @see #toSlick
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addContextExercises
   */
  @Override
  public SlickExercise toSlick(CommonExercise shared,
                               @Deprecated boolean isOverride,
                               int projectID,
                               int importUserIfNotSpecified,
                               boolean isContext,
                               Collection<String> typeOrder) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    if (typeOrder.isEmpty()) {
      if (spew++ < 100 || spew % 100 == 0)
        logger.error("toSlick type order is empty? (" + spew + " )");
      return null;
    }
    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.hasNext() ? iterator.next() : "";
    String second = iterator.hasNext() ? iterator.next() : "";

    int creator = shared.getCreator();
    if (creator == BaseUserDAO.UNDEFINED_USER) creator = importUserIfNotSpecified;

    long updateTime = shared.getUpdateTime();
    if (updateTime == 0) updateTime = lastModified;

    String firstType = unitToValue.getOrDefault(first, "");
    if (firstType.isEmpty()) firstType = unitToValue.getOrDefault(first.toLowerCase(), "");
    String secondType = unitToValue.getOrDefault(second, "");
    if (secondType.isEmpty()) secondType = unitToValue.getOrDefault(second.toLowerCase(), "");

/*    logger.info("toSlick for " + shared.getID() + " "+shared.getOldID()+
        " : " +first +
        " = '" + firstType + "' " +
        "" + second + " = '" + secondType + "'");*/

    String english = shared.getEnglish();
    if (english.length() > MAX_LENGTH) {
      english = english.substring(0, MAX_LENGTH) + "...";
      logger.warn("toSlick " + shared.getID() + " truncate english " + english);
    }

    return new SlickExercise(shared.getID() > 0 ? shared.getID() : -1,
        creator,
        shared.getOldID(),
        new Timestamp(updateTime),
        english,
        shared.getMeaning(),
        getFL(shared),
        shared.getAltFL(),
        shared.getTransliteration(),
        isOverride,
        firstType,
        secondType,
        projectID,  // project id fk
        shared.isPredefined(),
        isContext,
        false,
        shared.getDominoID(),
        false,
        never,
        shared.getNumPhones());
  }

  private String getFL(CommonExercise shared) {
    String foreignLanguage = shared.getForeignLanguage();
    if (foreignLanguage.contains(QUOT)) {
      String convert = foreignLanguage.replaceAll(QUOT, "\"");
      //logger.info("toSlick : convert\nfrom "+ foreignLanguage + "\nto  " + convert);
      foreignLanguage = convert;
    }
    return foreignLanguage;
  }

  private final Timestamp never = new Timestamp(0);

//  private final VocabFactory factory = new VocabFactory();

  /**
   * @param slick
   * @param shouldSwap
   * @return
   * @see #getUserExercises
   */
  private Exercise fromSlick(SlickExercise slick, boolean shouldSwap) {
    Map<String, String> unitToValue = getUnitToValue(slick);

//    logger.info("from slick " + slick.id() + " " + slick.exid() + " domino " + slick.legacyid());
    String english = slick.english();
    if (english.length() > MAX_LENGTH) {
      english = english.substring(0, MAX_LENGTH) + "...";
      logger.warn("toSlick " + slick.id() + " truncate english " + english);
    }

    String altfl = slick.altfl();
    String foreignlanguage = slick.foreignlanguage();

    Exercise userExercise = new Exercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        english,
        foreignlanguage,
        StringUtils.stripAccents(foreignlanguage),
        altfl,
        slick.transliteration(),
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime(),
        slick.iscontext(),
        slick.numphones(),
        slick.legacyid(),
        shouldSwap);

/*    logger.info("fromSlick " +
        "\n\tfrom    " + slick+
        "\n\tcreated " + userExercise+
        "\n\tcontext " + userExercise.isContext()
    );*/
    return userExercise;
  }

  /**
   * @param slick
   * @return
   */
  @NotNull
  private Map<String, String> getUnitToValue(SlickExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    boolean isDefault = slick.projid() == DEFAULT_PROJECT;
    Collection<String> typeOrder = isDefault ? Collections.emptyList() : userDAO.getDatabase().getTypeOrder(slick.projid());

    if (typeOrder == null || typeOrder.isEmpty()) {
      logger.warn("getUnitToValue no types for exercise " + (isDefault ? " DEFAULT PROJECT " : "") + slick);
    }

    Iterator<String> iterator = typeOrder != null ? typeOrder.iterator() : null;
    String first = iterator != null && iterator.hasNext() ? iterator.next() : "";
    String second = iterator != null && iterator.hasNext() ? iterator.next() : "";
    unitToValue.put(first, slick.unit());
    if (!second.isEmpty()) {
      unitToValue.put(second, slick.lesson());
    }
    return unitToValue;
  }

  /**
   * So we need to not put the context sentences, at least initially, under the unit-chapter hierarchy.
   * <p>
   * Use exercise -> phone map to determine phones per exercise...
   * <p>
   * <p>
   * Don't set phones on netprof machine???
   *
   * @param slick
   * @param baseTypeOrder
   * @param sectionHelper
   * @param exToPhones
   * @param lookup
   * @param exercise
   * @param attrTypes
   * @param pairs
   * @return
   * @see #getExercises
   */
  private List<Pair> addExerciseToSectionHelper(SlickExercise slick,
                                                Collection<String> baseTypeOrder,
                                                ISection<CommonExercise> sectionHelper,
                                                Map<Integer, ExercisePhoneInfo> exToPhones,
                                                IPronunciationLookup lookup,
                                                Exercise exercise,
                                                Collection<String> attrTypes,
                                                List<SlickExercisePhone> pairs) {
    if (exercise.getNumPhones() < 1 &&
        lookup.hasModel() &&
        (hostName.startsWith(HYDRA) ||
            hostName.contains("MITLL"))) {  // for local testing
//      logger.info("addExerciseToSectionHelper ex " + slick.id() + " = " + exercise.getNumPhones());
      ExercisePhoneInfo exercisePhoneInfo = getExercisePhoneInfo(slick, exToPhones, lookup, pairs);

      int numToUse = exercisePhoneInfo.getNumPhones();
      if (numToUse == 0) {
        numToUse = exercisePhoneInfo.getNumPhones2();
        if (numToUse < 1) {
          logger.warn("addExerciseToSectionHelper can't count phones for " + slick.id() + " " + slick.english() + " " + slick.foreignlanguage());
        } else {
          logger.info("addExerciseToSectionHelper using back off phone childCount " + slick.id() + " = " + numToUse);
        }
      }
      exercise.setNumPhones(numToUse);
    } else {
      // logger.info("hostName " + hostName + " host addr " + hostAddress + " : " + exercise.getNumPhones() + " " + lookup.hasModel());
    }
//    logger.info("addExerciseToSectionHelper for " + exercise.getID() + " num phones = " + numToUse);

    return addPhoneInfo(slick, baseTypeOrder, sectionHelper, exercise, attrTypes);
  }

  /**
   * @param slick
   * @param shouldSwap
   * @return
   * @see #getExercises(Collection, List, ISection, Map, Project, Map, Map, boolean)
   */
  @NotNull
  private Exercise makeExercise(SlickExercise slick, boolean shouldSwap) {
    int id = slick.id();
    String foreignlanguage = getTruncated(slick.foreignlanguage());
    String noAccentFL = StringUtils.stripAccents(foreignlanguage);
    String english = getTruncated(slick.english());
    String meaning = getTruncated(slick.meaning());
    Exercise exercise = new Exercise(
        id,
        slick.exid(),
        BaseUserDAO.UNDEFINED_USER,
        english,
        foreignlanguage,
        noAccentFL,
        slick.altfl(),
        meaning,
        slick.transliteration(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime(),
        slick.iscontext(),
        slick.numphones(),
        slick.legacyid(), // i.e. dominoID
        shouldSwap);

    {
      List<String> translations = new ArrayList<>();
      if (!foreignlanguage.isEmpty()) {
        translations.add(foreignlanguage);
      }
      exercise.setRefSentences(translations); // ?
    }

    exercise.setUpdateTime(slick.modified().getTime());
    exercise.setAltFL(slick.altfl());
    return exercise;
  }

  @NotNull
  private String getTruncated(String english2) {
    String english = english2;
    if (english.length() > MAX_LENGTH) english = english.substring(0, MAX_LENGTH) + "...";
    return english;
  }

  /**
   * If the number of phones on an exercise has not been calculated yet, look it up.
   *
   * @param slick
   * @param exToPhones
   * @param lookup
   * @return
   * @see #addExerciseToSectionHelper
   */
  @NotNull
  private ExercisePhoneInfo getExercisePhoneInfo(SlickExercise slick,
                                                 Map<Integer, ExercisePhoneInfo> exToPhones,
                                                 IPronunciationLookup lookup,
                                                 List<SlickExercisePhone> pairs) {
    ExercisePhoneInfo exercisePhoneInfo = exToPhones.get(slick.id());

    if (exercisePhoneInfo == null || exercisePhoneInfo.getNumPhones() < 1) {
/*      if (exercisePhoneInfo == null) {
        logger.info("getExercisePhoneInfo : no phone info for " + slick.id() + " in " + exToPhones.size());
      } else {
        logger.info("getExercisePhoneInfo phone info is " + exercisePhoneInfo.getNumPhones() +
            " for  " + slick.id());
      }*/
      exercisePhoneInfo = getExercisePhoneInfoFromDict(slick, lookup);
    } else {
//      logger.info("OK for " + slick.id() + " " + exercisePhoneInfo);
      // exerciseDAO.updatePhones(slick.id(), exercisePhoneInfo.getNumPhones());
      pairs.add(new SlickExercisePhone(slick.id(), exercisePhoneInfo.getNumPhones()));
    }
/*
    else if (exercisePhoneInfo.getNumPhonesFromDictionaryOrLTS() <1) {
      logger.warn("for " + slick.id() + " found no phones?");
    }
*/
    return exercisePhoneInfo;
  }

  private int updated = 0;
  private int cantcalc = 0;

  /**
   * Writes to table on cache miss.
   * <p>
   * Trying to prevent recalc on startup, which slows
   *
   * @param slick
   * @param lookup
   * @return
   * @see #getExercisePhoneInfo(SlickExercise, Map, IPronunciationLookup, List)
   */
  @NotNull
  private ExercisePhoneInfo getExercisePhoneInfoFromDict(SlickExercise slick, IPronunciationLookup lookup) {
    ExercisePhoneInfo exercisePhoneInfo;

    int numphones = slick.numphones();
    if (numphones < 1) {
//      logger.info("getExercisePhoneInfoFromDict num phones " + numphones + " for exercise " + slick.id());
      String foreignlanguage = slick.foreignlanguage();
      String transliteration = slick.transliteration();

      String pronunciations = lookup.getPronunciationsFromDictOrLTS(foreignlanguage, transliteration);
      exercisePhoneInfo = pronunciations.isEmpty() ? new ExercisePhoneInfo() : new ExercisePhoneInfo(pronunciations);

      {
        int numPhones = exercisePhoneInfo.getNumPhones();

        int n2;
        if (numPhones == 0) {
          n2 = lookup.getNumPhonesFromDictionary(foreignlanguage, transliteration);
          if (n2 > 0) {
            exercisePhoneInfo.setNumPhones2(n2);
          }
        } else {
          n2 = numPhones;
        }

        int id = slick.id();
        if (n2 < 1) {
          cantcalc++;
          if (cantcalc < 100 || cantcalc % 1000 == 0) {
            logger.info("getExercisePhoneInfo can't calc num phones for " + cantcalc +
                " exercises, e.g. " + id + " " + foreignlanguage + "/" + slick.english());
          }
        } else {
          if (hasMediaDir) {
            if (n2 > 0) {
              exerciseDAO.updatePhones(id, n2);
//            pairs.add(new SlickExercisePhone(id, n2));
              updated++;
              if (updated < 10 || updated % 1000 == 0) logger.info("getExercisePhoneInfo (project #" + slick.projid() +
                  ") updated " + updated + " exercises with phone info (e.g. " + id + " = " + n2);
            }
          } else {
            logger.info("getExercisePhoneInfo no media dir so can't update " + id + " with " + n2);
          }
        }
      }
    } else {
      exercisePhoneInfo = new ExercisePhoneInfo();
      exercisePhoneInfo.setNumPhones(numphones);
    }
    /*
    if (slick.english().equals("address")) {
      logger.info("ex " + slick);
      logger.info("ex " + foreignlanguage);
      logger.info("pronunciations " + pronunciations);
      logger.info("exercisePhoneInfo " + exercisePhoneInfo);
    }*/

    return exercisePhoneInfo;
  }
  //private int spew = 0;

  /**
   * Adds exercise attributes to type hierarchy for facets.
   *
   * @param slick
   * @param sectionHelper
   * @param exercise
   * @return
   * @see #addExerciseToSectionHelper
   */
  private List<Pair> addPhoneInfo(SlickExercise slick,
                                  Collection<String> baseTypeOrder,
                                  ISection<CommonExercise> sectionHelper,
                                  Exercise exercise,
                                  Collection<String> attrTypes) {
    List<Pair> pairs = getUnitToValue(slick, baseTypeOrder);

    if (slick.ispredef() && !slick.iscontext()) {
      if (exercise.getAttributes() == null) {
        if (spew++ < 10) {
          logger.warn("addPhoneInfo : no exercise attributes for " + exercise.getID());
        }
      } else {
        addBlanksForMissingInfo(exercise, attrTypes, pairs);
      }

      // logger.info("pairs for " + exercise.getID() + " " + exercise.getOldID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " : " + pairs);
      sectionHelper.addPairs(exercise, pairs);
    } else {
      exercise.setPairs(pairs);
    }
    return pairs;
  }

  /**
   * SectionHelper gets confused if we don't have a complete tree - same number of nodes on path to root
   *
   * @param exercise
   * @param attrTypes
   * @param pairs
   */
  private void addBlanksForMissingInfo(CommonExercise exercise, Collection<String> attrTypes, List<Pair> pairs) {
    Map<String, ExerciseAttribute> typeToAtrr = new HashMap<>();
    exercise.getAttributes().forEach(attribute -> typeToAtrr.put(attribute.getProperty(), attribute));

    for (String attrType : attrTypes) {
      ExerciseAttribute attribute = typeToAtrr.get(attrType);
      if (attribute == null) {
        // missing info for this type, so map it to BLANK
        pairs.add(new ExerciseAttribute(attrType, BLANK));
      } else {
        if (attribute.isFacet()) {
          pairs.add(attribute);
        } else {
          logger.info("Skip attribute not a facet " + attribute);
        }
      }
    }
  }

  private int c = 0;

  /**
   * Assumes at most two levels stored with each exercise.
   *
   * @param slick
   * @param typeOrder
   * @return
   * @see #addPhoneInfo
   */
  private List<Pair> getUnitToValue(SlickExercise slick, Collection<String> typeOrder) {
    List<Pair> pairs = new ArrayList<>();
    Iterator<String> iterator = typeOrder.iterator();

    String first = iterator.hasNext() ? iterator.next() : UNIT;
    String second = iterator.hasNext() ? iterator.next() : "";
    boolean firstEmpty = slick.unit().isEmpty();

    if (firstEmpty && slick.ispredef()) {
      pairs.add(getPair(first, DEFAULT_FOR_EMPTY));
//      unitToValue.put(first, "1");
      if (c++ < 100 || c % 100 == 0) logger.warn("getUnitToValue (" + c +
          ") got empty " + first + " for " + slick + " type order " + typeOrder);

    } else {
      pairs.add(getPair(first, slick.unit()));
    }

    if (!second.isEmpty()) {
      if (slick.ispredef()) {
        boolean empty = slick.lesson().trim().isEmpty();

        if (empty) {
          pairs.add(getPair(second, DEFAULT_FOR_EMPTY));
        } else {
          pairs.add(getPair(second, slick.lesson()));
        }
        if (empty) {
          if (c++ < 100) logger.warn("getUnitToValue got empty " + second + " for " + slick);
        }
      }
    }
    return pairs;
  }

  @NotNull
  private Pair getPair(String first, String value) {
    if (first.isEmpty()) logger.error("huh type is empty " + value, new Exception());
    return new Pair(first, value);
  }

  /**
   * @param bulk
   * @see mitll.langtest.server.database.copy.ExerciseCopy#reallyAddingUserExercises
   */
  @Override
  public void addBulk(List<SlickExercise> bulk) {
    dao.addBulk(bulk);
  }

  /**
   * TODO : All overkill...?  why not just look them back up from exercise dao?
   *
   * @param all
   * @param shouldSwap
   * @return
   * @see
   * @see IUserExerciseDAO#getOnList
   */
  private List<CommonExercise> getUserExercises(Collection<SlickExercise> all, boolean shouldSwap) {
//    logger.info("getUserExercises for " + all.size()+ " exercises");
    List<CommonExercise> copy = new ArrayList<>(all.size());
    all.forEach(slickExercise -> copy.add(fromSlick(slickExercise, shouldSwap)));
    //  logger.info("getUserExercises returned " + copy.size()+ " user exercises");
    return copy;
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @param addTypesToSection
   * @return
   * @see #getByProject
   * @see #getContextByProject
   */
  private List<CommonExercise> getExercises(Collection<SlickExercise> all,
                                            List<String> typeOrder,
                                            ISection<CommonExercise> sectionHelper,
                                            Map<Integer, ExercisePhoneInfo> exToPhones,
                                            Project lookup,
                                            Map<Integer, ExerciseAttribute> allByProject,
                                            Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs,
                                            boolean addTypesToSection) {
    List<List<Pair>> allAttributes = new ArrayList<>();

    List<String> baseTypeOrder = getBaseTypeOrder(lookup);

    List<CommonExercise> copy = new ArrayList<>();

    List<SlickExercisePhone> pairs = new ArrayList<>();

    long then = System.currentTimeMillis();
    {
      Collection<String> allFacetTypes = allByProject
          .values()
          .stream()
          .filter(ExerciseAttribute::isFacet)
          .map(Pair::getProperty)
          .collect(Collectors.toCollection(HashSet::new));

/*
      logger.info("all facet types:");
      allFacetTypes.forEach(type -> logger.info("\t" + type));
*/

//    logger.info("ExToPhones " + exToPhones.size());
      //  logger.info("examining  " + all.size() + " exercises...");

      int n = 0;
      boolean shouldSwap = getShouldSwap(lookup.getID());
      for (SlickExercise slickExercise : all) {
        Exercise exercise = makeExercise(slickExercise, shouldSwap);

        if (WARN_ABOUT_MISSING_PHONES) {
          if (exercise.getNumPhones() == 0 && n++ < 10) {
            logger.info("getExercises no phones for exercise " + exercise.getID());
          }
        }

        addAttributeToExercise(allByProject, exToAttrs, exercise);
//      logger.info("Attr for " + exercise.getID() + " " + exercise.getAttributes());
        List<Pair> e = addExerciseToSectionHelper(slickExercise, baseTypeOrder, sectionHelper, exToPhones, lookup, exercise,
            allFacetTypes, pairs);
        e.forEach(pair -> {
          if (pair.getProperty().startsWith("Speaker")) {
            logger.info("got speaker attr " + pair);
          }
        });

        allAttributes.add(e);
        copy.add(exercise);
      }

      if (!pairs.isEmpty()) {
        logger.info("updating " + pairs.size() + " exercises for num phones.");
      }
    }
    long then2 = System.currentTimeMillis();
    exerciseDAO.updatePhonesBulk(pairs);

    long now = System.currentTimeMillis();
    if (now - then2 > 50) {
      logger.info("getExercises took " + (now - then) + " to update # phones on " + pairs.size() + " exercises.");
    }
    if (now - then > 50) {
      logger.info("getExercises took " + (now - then) + " to attach attributes to " + all.size() + " exercises.");
    }

    if (addTypesToSection) {
      logger.info("getExercises type order " + typeOrder);

      sectionHelper.rememberTypesInOrder(typeOrder, allAttributes);
    }
    //  logger.info("getExercises created " + copy.size() + " exercises");
    return copy;
  }

  /**
   * get the exercise attributes for this exercise by finding the joins, then by the id->attrbute map
   *
   * @param allByProject
   * @param exToAttrs
   * @param exercise
   * @see #getExercises
   */
  private void addAttributeToExercise(Map<Integer, ExerciseAttribute> allByProject,
                                      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs,
                                      CommonExercise exercise) {
    Collection<SlickExerciseAttributeJoin> slickExerciseAttributeJoins = exToAttrs.get(exercise.getID());

    if (slickExerciseAttributeJoins != null) {
      List<ExerciseAttribute> attributes = new ArrayList<>();//slickExerciseAttributeJoins.size());
      slickExerciseAttributeJoins
          .forEach(slickExerciseAttributeJoin -> attributes.add(allByProject.get(slickExerciseAttributeJoin.attrid())));
      exercise.setAttributes(attributes);
//      logger.info("addAttributeToExercise ex " + exercise.getID() + " : " + exercise.getAttributes());
    }
  }

  /**
   * @paramx userExercise
   * @paramxx isOverride
   * @paramx isContext
   * @paramx typeOrder
   * @seex IUserListManager#newExercise
   */
/*
  @Override
  public int add(CommonExercise userExercise, boolean isOverride, boolean isContext, Collection<String> typeOrder) {
    int insert = insert(toSlick(userExercise, isOverride, isContext, typeOrder));
    ((Exercise) userExercise).setID(insert);
    return insert;
  }
*/
  @Override
  public int insert(SlickExercise UserExercise) {
    return dao.insert(UserExercise);
  }

  /**
   * @param projID
   */
  private void insertDefault(int projID) {
    insertDefault(projID, NEW_USER_EXERCISE);
  }

  private void insertDefault(int projID, String newUserExercise) {
    int beforeLoginUser = userDAO.getBeforeLoginUser();
    Timestamp now = new Timestamp(System.currentTimeMillis());
    SlickExercise userExercise = new SlickExercise(-1,
        beforeLoginUser,
        newUserExercise,
        now,
        "",
        "",
        "",
        "", "", false, "", "", projID, true, false,
        false, -1, false, now, 0);

    logger.info("insert default " + userExercise);
    insert(userExercise);
  }

  /**
   * @param listID
   * @param shouldSwap
   * @return
   * @see mitll.langtest.server.database.userlist.SlickUserListDAO#populateList
   */
  @Override
  public List<CommonShell> getOnList(int listID, boolean shouldSwap) {
    return new ArrayList<>(getCommonExercises(listID, shouldSwap));
  }

  /**
   * @param listID
   * @param shouldSwap
   * @return
   * @see #getOnList
   * @see mitll.langtest.server.database.userlist.SlickUserListDAO#populateListEx
   */
  public List<CommonExercise> getCommonExercises(int listID, boolean shouldSwap) {
    // long then = System.currentTimeMillis();
    List<SlickExercise> onList = dao.getOnList(listID);
    // long now = System.currentTimeMillis();
//    logger.info("getCommonExercises took "+ (now-then) + " to get " + onList.size() + " for list #" + listID );
    return getUserExercises(onList, shouldSwap);
  }

  /**
   * Pull out of the database.
   *
   * @param exid
   * @param shouldSwap
   * @return
   */
  @Override
  public CommonExercise getByExID(int exid, boolean shouldSwap) {
    Collection<SlickExercise> byExid = dao.byID(exid);
    CommonExercise exercise = byExid.isEmpty() ? null : fromSlick(byExid.iterator().next(), shouldSwap);

    if (exercise != null) {
      //   for (SlickExercise ex:relatedExerciseDAOWrapper.contextExercises(exid)) exercise.getDirectlyRelated().add(fromSlick(ex));
      relatedExerciseHelper.getContextExercises(exid)
          .forEach(ex -> exercise.getDirectlyRelated().add(fromSlick(ex, shouldSwap)));
    }
    return exercise;
  }

  public SlickExercise getByID(int exid) {
    Collection<SlickExercise> byExid = dao.byID(exid);
    return byExid.isEmpty() ? null : byExid.iterator().next();
  }

  @Override
  public CommonExercise getTemplateExercise(int projID) {
    if (templateExercise == null) {
      templateExercise = getByExOldID(NEW_USER_EXERCISE, projID);
    }
    return templateExercise;
  }

  @Override
  public CommonExercise getByExOldID(String oldid, int projID) {
    Collection<SlickExercise> byExid = dao.getByExid(oldid, projID);
    boolean shouldSwap = getShouldSwap(projID);
    return byExid.isEmpty() ? null : fromSlick(byExid.iterator().next(), shouldSwap);
  }

  @Override
  public int getProjectForExercise(int exid) {
    if (exid == unknownExerciseID) return -1;
    else {
      Collection<Integer> byExid = dao.projForEx(exid);
      return byExid.isEmpty() ? -1 : byExid.iterator().next();
    }
  }

  /**
   * @param projID
   * @see DatabaseImpl#initializeDAOs
   */
  public int ensureTemplateExercise(int projID) {
    int id = 0;
    if (dao.getByExid(NEW_USER_EXERCISE, projID).isEmpty()) {
      insertDefault(projID);
    }

    Collection<SlickExercise> byExid = dao.getByExid(UNKNOWN, projID);
    if (byExid.isEmpty()) {
      insertDefault(projID, UNKNOWN);
    }
    Collection<SlickExercise> again = dao.getByExid(UNKNOWN, projID);
    if (!again.isEmpty()) {
      unknownExercise = again.iterator().next();
      id = unknownExercise.id();
    }
    unknownExerciseID = id;
    //logger.info("unknown ex " + unknownExerciseID);
    return id;
  }

  /**
   * Niche feature for alt chinese to swap primary and alternate...
   *
   *  It's back on! 7/13/18 GWFV
   *
   * @param projid
   * @return
   */
  private boolean getShouldSwap(int projid) {
    String defPropValue = database.getProjectDAO().getDefPropValue(projid, ProjectProperty.SWAP_PRIMARY_AND_ALT);
    return defPropValue.equalsIgnoreCase("TRUE");
  }

  /**
   * @param typeOrder
   * @param sectionHelper
   * @param theProject
   * @return
   * @see DBExerciseDAO#readExercises
   */
  @Override
  public List<CommonExercise> getByProject(
      List<String> typeOrder,
      ISection<CommonExercise> sectionHelper,
      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject,
      Project theProject,
      Map<Integer, ExerciseAttribute> allByProject,
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs) {
    // TODO? : consider getting exercise->phone from the ref result table again
//    logger.info("getByProject type order " + typeOrder);
    List<SlickExercise> allPredefByProject = dao.getAllPredefByProject(theProject.getID());

    //allPredefByProject = allPredefByProject.stream().filter(ex -> ex.exid().startsWith("0")).collect(Collectors.toList());
    // allPredefByProject = allPredefByProject.stream().filter(ex -> ex.unit().startsWith("5")).collect(Collectors.toList());

//    logger.info("getByProject got " + allPredefByProject.size() + " from " + theProject);

    return getExercises(allPredefByProject,
        typeOrder,
        sectionHelper,
        exerciseToPhoneForProject,
        theProject,
        allByProject,
        exToAttrs,
        true);
  }

  @NotNull
  private List<String> getBaseTypeOrder(Project project) {
    List<String> typeOrder = new ArrayList<>();
    SlickProject project1 = project.getProject();
    if (!project1.first().isEmpty()) {
      typeOrder.add(project1.first());
    } else {
      logger.error("huh? project " + project + " first type is empty?");
    }
    if (!project1.second().isEmpty()) {
      typeOrder.add(project1.second());
    }
    return typeOrder;
  }

  /**
   * @param typeOrder
   * @param sectionHelper
   * @param exerciseToPhoneForProject
   * @param lookup
   * @return
   * @paramx attributeTypes
   * @see DBExerciseDAO#readExercises
   */
  @Override
  public List<CommonExercise> getContextByProject(
      List<String> typeOrder,
      ISection<CommonExercise> sectionHelper,
      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject,
      Project lookup,
      Map<Integer, ExerciseAttribute> allByProject,
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs
  ) {
    int projectid = lookup.getID();
    List<SlickExercise> allContextPredefByProject = dao.getAllContextByProject(projectid);
//    logger.info("getContextByProject For " + projectid + " got " + allContextPredefByProject.size() + " context predef ");
    return getExercises(allContextPredefByProject, typeOrder, sectionHelper,
        exerciseToPhoneForProject, lookup, allByProject, exToAttrs, /*attributeTypes,*/ false);
  }

  @Override
  public Collection<CommonExercise> getOverrides(boolean shouldSwap) {
    return getUserExercises(dao.getOverrides(), shouldSwap);
  }

  @Override
  public Collection<CommonExercise> getByExID(Collection<Integer> exids, boolean shouldSwap) {
    return getUserExercises(getExercisesByIDs(exids), shouldSwap);
  }

  @Override
  public List<SlickExercise> getExercisesByIDs(Collection<Integer> exids) {
    return dao.byIDs(exids);
  }

  /**
   * @param exids
   * @see ProjectSync#doDelete
   */
  public void deleteByExID(Collection<Integer> exids) {
    int i = dao.deleteByIDs(exids);
    if (i != exids.size()) {
      logger.warn("deleteByExID tried to delete " + exids.size() + " but only changed " + i + " rows?");
    }
  }

  /**
   * TODOx : Why so complicated?
   * <p>
   * Maybe separately update context exercises.
   *
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @see mitll.langtest.server.domino.ProjectSync#doUpdate
   */
  @Override
  public boolean update(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder) {
    //logger.info("update : " + userExercise.getID() + " has " + userExercise.getDirectlyRelated().size() + " context");
    SlickExercise slickUserExercise = toSlick(userExercise, true, isContext, typeOrder);

    int rows = dao.update(slickUserExercise);
    boolean didIt = rows > 0;
    {
      String idLabel = userExercise.getID() + "/" + slickUserExercise.id() + "/" + slickUserExercise.legacyid() + "/" + slickUserExercise.exid();
      if (rows == 0 /*&& createIfDoesntExist*/) {
//      int insert = dao.insert(slickUserExercise);
//      logger.info("update inserted exercise #" + insert);
        logger.error("update didn't update exercise #" + idLabel);
      } else {
        logger.info("update : updated exercise #" + idLabel);
      }
    }

    // recurse on related context exercises
/*    for (CommonExercise contextEx : userExercise.getDirectlyRelated()) {
      logger.info("update with context exercise " + contextEx.getID() + " context " + contextEx.getForeignLanguage() + " " + contextEx.getEnglish());
      didIt &= update(contextEx, true, typeOrder);
    }*/
    return didIt;
  }

  public boolean updateModified(int exid) {
    return dao.updateModified(exid) > 0;
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyOneConfig
   */
  @Override
  public boolean isProjectEmpty(int projectid) {
    return dao.isProjectEmpty(projectid);
  }

  /**
   * @return
   * @see DatabaseImpl#createTables
   */
  public IRelatedExercise getRelatedExercise() {
    return relatedExerciseHelper;
  }

  /**
   * @return
   * @see DatabaseImpl#createTables
   */
  public IRelatedExercise getRelatedCoreExercise() {
    return relatedCoreExerciseHelper;
  }

  @Override
  public IAttributeJoin getExerciseAttributeJoin() {
    return attributeJoinHelper;
  }

  public IAttribute getExerciseAttribute() {
    return attributeHelper;
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addExercisesAndAttributes
   */
  @Override
  public BothMaps getOldToNew(int projectid) {
    Map<String, Integer> oldToNew = new HashMap<>();

    List<SlickExercise> allPredefByProject = dao.getAllPredefByProject(projectid);
    addToLegacyIdToIdMap(allPredefByProject, oldToNew);

    List<SlickExercise> allContextPredefByProject = dao.getAllContextByProject(projectid);
    addToLegacyIdToIdMap(allContextPredefByProject, oldToNew);

    logger.info("getOldToNew found for" +
        "\n\tproject #" + projectid +
        "\n\t" + allPredefByProject.size() + " predef exercises," +
        "\n\t" + allContextPredefByProject.size() + " context predef exercises," +
        "\n\t" + oldToNew.size() + " old->new mappings");
//    logger.info("old->new for project #" + projectid + " has  " + oldToNew.size());
    return new BothMaps(oldToNew);
  }

  private void addToLegacyIdToIdMap(List<SlickExercise> allPredefByProject, Map<String, Integer> oldToNew) {
    allPredefByProject.forEach(exercise -> {
      String exid = exercise.exid();

      if (!exid.isEmpty() && !exid.equalsIgnoreCase(UNKNOWN1)) {
        if (oldToNew.containsKey(exid)) {
          logger.warn("addToLegacyIdToIdMap : huh? avoid corruption : already saw an exercise with id " +
              "\n\tbefore '" + exid +
              "'," +
              "\n\twas     " + oldToNew.get(exid) + "' would have replaced with" +
              "\n\tex      " + exercise);
        } else {
          Integer before = oldToNew.put(exid, exercise.id());
          if (before != null) {
            logger.warn("addToLegacyIdToIdMap : huh? corruption : already saw an exercise with id before " + before +
                " '" + exid + "' replace with " + exercise);
          }
        }
      }
    });
  }

  /**
   * @param allPredefByProject
   * @param oldToNew
   * @see #getOldToNew
   */
/*  private void addToDominoMap(List<SlickExercise> allPredefByProject, Map<Integer, Integer> oldToNew) {
    allPredefByProject.forEach(exercise -> {
      if (oldToNew.put(exercise.legacyid(), exercise.id()) != null) {
        logger.warn("addToDominoMap : huh? already saw an exercise with id " + exercise.exid() + ", domino id " + exercise.legacyid() +
            " replace with " + exercise);
      }
    });
  }*/

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addExercises
   */
  @Override
  public Map<Integer, String> getIDToFL(int projid) {
    return dao.getIDToFL(projid);
  }

//  private List<Integer> range = new ArrayList<>();

  /**
   * TODO : Nobody calls this just now. Maybe later.
   *
   * @paramx exToPhones
   * @seex DatabaseImpl#configureProjects
   */
/*  @Override
  public void useExToPhones(Map<Integer, ExercisePhoneInfo> exToPhones) {
    int total = 0;

    Map<Integer, Integer> binToCount = new TreeMap<>();
    for (ExercisePhoneInfo exercisePhoneInfo : exToPhones.values()) {
      int numPhones = exercisePhoneInfo.getNumPhonesFromDictionaryOrLTS();
      binToCount.put(numPhones, binToCount.getOrDefault(numPhones, 0) + 1);
      total++;
    }
    int ranges = DESIRED_RANGES;
    int desired = total / ranges;
    int window = desired;
    int prev = 1;

    logger.info("useExToPhones desired " + desired + "\tranges " + ranges);

    for (Integer bin : binToCount.keySet()) {
      Integer count = binToCount.get(bin);
      window -= count;
      logger.info("useExToPhones " + bin + "\t" + count + "\tleft " + window);
      if (window <= 0) {
        range.add(prev);
        window += desired;
      }

      prev = bin;
    }
    if (!range.isEmpty()) range = range.subList(0, range.size() - 1);
    logger.info("useExToPhones got range " + range);
  }*/
  @Override
  public IRefResultDAO getRefResultDAO() {
    return refResultDAO;
  }

  public SlickExercise getUnknownExercise() {
    return unknownExercise;
  }

  @Override
  public ExerciseDAOWrapper getDao() {
    return dao;
  }

  /**
   * @param projectid
   * @return
   * @see ProjectSync#addPending
   */
  @Override
  public Map<Integer, SlickExercise> getDominoToSlickEx(int projectid) {
    return dao.getLegacyToExercise(projectid);
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.database.project.ProjectDAO#update
   * @see DominoImport#getImportFromDomino
   */
  public boolean areThereAnyUnmatched(int projid) {
    return dao.getUnknownDomino(projid).size() > 0 || dao.getUnknownDominoTriplet(projid).size() > 0;
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.database.project.ProjectDAO#setDominoIDOnExercises(int, int)
   */
  public Map<String, Integer> getNpToExID(int projid) {
    Map<String, Integer> unknownDomino = new HashMap<>(dao.getUnknownDomino(projid));
    logger.info("getNpToExID got " + unknownDomino.size() + " for project " + projid);
    Map<String, Integer> unknownDominoTriplet = dao.getUnknownDominoTriplet(projid);
    logger.info("getNpToExID got all " + unknownDominoTriplet.size() + " for project " + projid);
    unknownDomino.putAll(unknownDominoTriplet);
    logger.info("getNpToExID now " + unknownDomino.size() + " for project " + projid);
    return unknownDomino;
  }

  /**
   * @param pairs
   * @return
   * @see mitll.langtest.server.database.project.ProjectDAO#update
   */
  public int updateDominoBulk(List<SlickUpdateDominoPair> pairs) {
    return exerciseDAO.updateDominoBulk(pairs);
  }

  @Override
  public int getUnknownExerciseID() {
    return unknownExerciseID;
  }

  /**
   * TODO : not advised - will lock up database, maybe forever.
   *
   * @param projID
   * @see DatabaseImpl#dropProject
   */
  @Override
  public void deleteForProject(int projID) {
    relatedExerciseHelper.deleteForProject(projID);
    dao.deleteForProject(projID);
  }

  @Override
  public SlickExercise getByDominoID(int projID, int docID) {
    Collection<SlickExercise> byExid = dao.byDominoID(projID, docID);
    return byExid.isEmpty() ? null : byExid.iterator().next();
  }

  @Override
  public Map<Integer, Integer> getDominoIDToExID(int projID) {
    return dao.allByDominoIDPairs(projID);
  }
}
