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
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.copy.VocabFactory;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.PronunciationLookup;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.npdata.dao.*;
import mitll.npdata.dao.userexercise.ExerciseAttributeDAOWrapper;
import mitll.npdata.dao.userexercise.ExerciseAttributeJoinDAOWrapper;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SlickUserExerciseDAO
    extends BaseUserExerciseDAO
    implements IUserExerciseDAO {
  private static final Logger logger = LogManager.getLogger(SlickUserExerciseDAO.class);

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#getTypeOrder
   * @see #addPhoneInfo
   */
  public static final String SOUND = "Sound";
  public static final String DIFFICULTY = "Difficulty";

  /**
   * TODO : need to do something to allow this to scale well - maybe ajax style nested types, etc.
   */
  // private static final boolean ADD_PHONE_LENGTH = false;

  private static final String NEW_USER_EXERCISE = "NEW_USER_EXERCISE";
  private static final String UNKNOWN = "UNKNOWN";
  private static final String ANY = "Any";
  private static final String DEFAULT_FOR_EMPTY = ANY;
  private static final int DESIRED_RANGES = 10;
  public static final boolean ADD_SOUNDS = false;

  private final long lastModified = System.currentTimeMillis();
  private final ExerciseDAOWrapper dao;
  private final RelatedExerciseDAOWrapper relatedExerciseDAOWrapper;
  private final ExerciseAttributeDAOWrapper attributeDAOWrapper;
  private final ExerciseAttributeJoinDAOWrapper attributeJoinDAOWrapper;
  //  private Map<Integer, ExercisePhoneInfo> exToPhones;
  private final IUserDAO userDAO;
  private final IRefResultDAO refResultDAO;
  public static final boolean ADD_PHONE_LENGTH = false;
  private SlickExercise unknownExercise;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickUserExerciseDAO(DatabaseImpl database, DBConnection dbConnection) {
    super(database);
    dao = new ExerciseDAOWrapper(dbConnection);
    relatedExerciseDAOWrapper = new RelatedExerciseDAOWrapper(dbConnection);
    attributeDAOWrapper = new ExerciseAttributeDAOWrapper(dbConnection);
    attributeJoinDAOWrapper = new ExerciseAttributeJoinDAOWrapper(dbConnection);

    userDAO = database.getUserDAO();
    refResultDAO = database.getRefResultDAO();
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param shared
   * @param projectID
   * @param typeOrder
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addUserExercises
   */
  public SlickExercise toSlick(Exercise shared, int projectID, Collection<String> typeOrder) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    //List<String> typeOrder = getTypeOrder();
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

  int spew = 0;

  /**
   * @param shared
   * @param isOverride
   * @param isContext
   * @param typeOrder
   * @return
   * @paramx isPredef
   * @see #toSlick
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addContextExercises
   */
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
    String first  = iterator.hasNext() ? iterator.next() : "";
    String second = iterator.hasNext() ? iterator.next() : "";

    int creator = shared.getCreator();
    if (creator == BaseUserDAO.UNDEFINED_USER) creator = importUserIfNotSpecified;

    long updateTime = shared.getUpdateTime();
    if (updateTime == 0) updateTime = lastModified;

    String firstType  = unitToValue.getOrDefault(first, "");
    if (firstType.isEmpty()) firstType  = unitToValue.getOrDefault(first.toLowerCase(), "");
    String secondType = unitToValue.getOrDefault(second, "");
    if (secondType.isEmpty()) secondType  = unitToValue.getOrDefault(second.toLowerCase(), "");

//    logger.info("toSlick for " + shared.getID() +
//        " : " +first +
//        " = '" + firstType + "' " +
//        "" + second + " = '" + secondType + "'");

    return new SlickExercise(shared.getID() > 0 ? shared.getID() : -1,
        creator,
        shared.getOldID(),
        new Timestamp(updateTime),
        shared.getEnglish(),
        shared.getMeaning(),
        shared.getForeignLanguage(),
        shared.getAltFL(),
        shared.getTransliteration(),
        isOverride,
        firstType,
        secondType,
        projectID,//shared.getProjectID(),  // project id fk
        shared.isPredefined(),
        isContext,
        false,
        shared.getDominoID(),
        false,
        never,
        shared.getNumPhones());
  }

  private final Timestamp never = new Timestamp(0);

  VocabFactory factory = new VocabFactory();

  /**
   * @param slick
   * @return
   * @see #getUserExercises
   */
  private Exercise fromSlick(SlickExercise slick) {
    Map<String, String> unitToValue = getUnitToValue(slick);

    Exercise userExercise = new Exercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        slick.english(),
        slick.foreignlanguage(),
        StringUtils.stripAccents(slick.foreignlanguage()),
        slick.altfl(),
        slick.transliteration(),
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime(),
        slick.numphones(),
        factory.getTokens(slick.foreignlanguage()));
//    logger.info("fromSlick created " + userExercise);
    return userExercise;
  }

  @NotNull
  private Map<String, String> getUnitToValue(SlickExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    Collection<String> typeOrder = userDAO.getDatabase().getTypeOrder(slick.projid());

    if (typeOrder == null || typeOrder.isEmpty()) {
      logger.warn("getUnitToValue no types for exercise " + slick);
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
   *
   * @param slick
   * @param lookup
   * @return
   * @see IUserExerciseDAO#useExToPhones(Map)
   * @see #getExercises
   */
  private List<Pair> addExerciseToSectionHelper(SlickExercise slick,
                                                Collection<String> baseTypeOrder,
                                                ISection<CommonExercise> sectionHelper,
                                                Map<Integer, ExercisePhoneInfo> exToPhones,
                                                PronunciationLookup lookup,
                                                Exercise exercise,
                                                Collection<String> attrTypes) {
    ExercisePhoneInfo exercisePhoneInfo = getExercisePhoneInfo(slick, exToPhones, lookup);

    int numToUse = exercisePhoneInfo.getNumPhones();
    if (numToUse == 0) {
      numToUse = exercisePhoneInfo.getNumPhones2();
//      logger.warn("using back off phone childCount " + numToUse);
    }

    exercise.setNumPhones(numToUse);

    List<Pair> pairs = addPhoneInfo(slick, baseTypeOrder, sectionHelper, exercise, attrTypes);
    return pairs;
  }

  @NotNull
  private Exercise makeExercise(SlickExercise slick) {
    int id = slick.id();
    String noAccentFL = StringUtils.stripAccents(slick.foreignlanguage());
    Exercise exercise = new Exercise(
        id,
        slick.exid(),
        BaseUserDAO.UNDEFINED_USER,
        slick.english(),
        slick.foreignlanguage(),
        noAccentFL,
        slick.altfl(),
        slick.meaning(),
        slick.transliteration(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime(), slick.iscontext());

    List<String> translations = new ArrayList<String>();
    if (!slick.foreignlanguage().isEmpty()) {
      translations.add(slick.foreignlanguage());
    }
    exercise.setRefSentences(translations); // ?
    exercise.setUpdateTime(lastModified);
    exercise.setAltFL(slick.altfl());
    return exercise;
  }

  @NotNull
  private ExercisePhoneInfo getExercisePhoneInfo(SlickExercise slick,
                                                 Map<Integer, ExercisePhoneInfo> exToPhones,
                                                 PronunciationLookup lookup) {
    int id = slick.id();
    ExercisePhoneInfo exercisePhoneInfo = exToPhones.get(id);

    if (exercisePhoneInfo == null) {
      exercisePhoneInfo = getExercisePhoneInfo(slick, lookup);
    }
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
   * @see #getExercisePhoneInfo(SlickExercise, Map, PronunciationLookup)
   */
  @NotNull
  private ExercisePhoneInfo getExercisePhoneInfo(SlickExercise slick, PronunciationLookup lookup) {
    ExercisePhoneInfo exercisePhoneInfo;

    if (slick.numphones() == -1) {
      String foreignlanguage = slick.foreignlanguage();
      String transliteration = slick.transliteration();

      String pronunciations = lookup.getPronunciations(foreignlanguage, transliteration);

      int n2 = lookup.getNumPhones(foreignlanguage, transliteration);

      exercisePhoneInfo = pronunciations.isEmpty() ? new ExercisePhoneInfo() : new ExercisePhoneInfo(pronunciations);
      exercisePhoneInfo.setNumPhones2(n2);

      if (n2 == -1) {
        cantcalc++;
        if (cantcalc % 1000 == 0) {
          logger.debug("getExercisePhoneInfo can't calc num phones for " + cantcalc +
              " exercises, e.g. " + slick.id() + " " + foreignlanguage + "/" + slick.english());
        }
      } else {
        exerciseDAO.updatePhones(slick.id(), n2);
        updated++;
        if (updated % 100 == 0) logger.debug("getExercisePhoneInfo updated " + updated + " exercises with phone info");
      }
    } else {
      exercisePhoneInfo = new ExercisePhoneInfo();
      exercisePhoneInfo.setNumPhones(slick.numphones());
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
   * TODO : What is this doing???
   * TODO : remove this???
   *
   * @param slick
   * @param sectionHelper
   * @param exercise
   * @return
   * @paramx typeOrder
   * @see #addExerciseToSectionHelper
   */
  private List<Pair> addPhoneInfo(SlickExercise slick,
                                  Collection<String> baseTypeOrder,
                                  ISection<CommonExercise> sectionHelper,
                                  Exercise exercise,
                                  Collection<String> attrTypes) {
    List<Pair> pairs = getUnitToValue(slick, baseTypeOrder);

    // Collection<String> phones = exercisePhoneInfo == null ? null : exercisePhoneInfo.getPhones();
    //if (phones == null || phones.isEmpty()) logger.warn("no phones for " + id);
    int max = 15;
    int i = 0;
    boolean addedPhones = false;
    if (slick.ispredef() && !slick.iscontext()) {
      if (exercise.getAttributes() == null) {
        if (spew++ < 10) {
          logger.warn("addPhoneInfo : no exercise attributes for " + exercise.getID());
        }
      } else {
        Map<String, ExerciseAttribute> typeToAtrr = new HashMap<>();

        for (ExerciseAttribute attribute : exercise.getAttributes()) {
          typeToAtrr.put(attribute.getProperty(), attribute);
        }

        for (String attrType : attrTypes) {
          ExerciseAttribute attribute = typeToAtrr.get(attrType);
          if (attribute == null) {
            // missing info for this type, so map it to Any
            pairs.add(new ExerciseAttribute(attrType, ANY));
          } else {
            pairs.add(attribute);
          }
        }
        //       pairs.addAll(exercise.getAttributes());
      }

      //    addExerciseToSectionHelper(sectionHelper, unitToValue, exercise);
      sectionHelper.addPairs(exercise, pairs);
      // allPairs.add(pairs);

      if (true) {//phones == null) {
//        logger.warn("no phones for " + id);
      } else {
        addedPhones = true;

        // TODO : maybe put back phone length later ???
 /*       if (ADD_PHONE_LENGTH) {
          int numPhones = exercisePhoneInfo.getNumPhones2();

          int choice = range.get(0);
          for (int r : range) {
            i++;
            if (numPhones <= r) {
              choice = i;
              break;
            }
          }

          String label = "" + choice;

     *//*     String label = "" + numPhones;
          if (numPhones >= max) {
            label = ">" + (max - 1);
          }*//*
          pairs.add(sectionHelper.getPairForExerciseAndLesson(exercise, DIFFICULTY, label));
        }*/

/*        if (ADD_SOUNDS) {
          // need to make a copy
          for (String phone : phones) {
            List<Pair> copy = new ArrayList<>(pairs);
            copy.add(sectionHelper.getPairForExerciseAndLesson(exercise, SOUND, phone));

  *//*        if (slick.unit().equals("1") && slick.lesson().equals("1")) {
            logger.info("for " + slick.id() + " " + slick.english() + " " + slick.foreignlanguage() + " (" +
                exercisePhoneInfo.getNumPhones() +
                ") " + copy);
          }*//*
            allPairs.add(copy);
          }
        }*/
      }

      // sectionHelper.addAssociations(pairs);
//      return addedPhones;
    } else {
      exercise.setPairs(pairs);
    }
    return pairs;
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

    String first = iterator.hasNext() ? iterator.next() : "Unit";
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

    if (!second.isEmpty()/* && !second.equals(SOUND)*/) {
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

  public int insert(SlickExercise UserExercise) {
    return dao.insert(UserExercise);
  }

  public void addBulk(List<SlickExercise> bulk) {
    dao.addBulk(bulk);
  }

  public int getNumRows() {
    return dao.getNumRows();
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }

  /**
   * @param all
   * @return
   * @see
   * @see IUserExerciseDAO#getOnList(int)
   */
  private List<CommonExercise> getUserExercises(Collection<SlickExercise> all) {
//    logger.info("getUserExercises for " + all.size()+ " exercises");
    List<CommonExercise> copy = new ArrayList<>();
    for (SlickExercise userExercise : all) copy.add(fromSlick(userExercise));
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
    List<List<Pair>> allPairs = new ArrayList<>();

    List<String> baseTypeOrder = getBaseTypeOrder(lookup);

    List<CommonExercise> copy = new ArrayList<>();

    Collection<String> attrTypes = allByProject.values().stream().map(Pair::getProperty).collect(Collectors.toCollection(HashSet::new));

//    logger.info("getExercises attr types " + attrTypes);

    long then = System.currentTimeMillis();
    for (SlickExercise slickExercise : all) {
      Exercise exercise = makeExercise(slickExercise);
      addAttributeToExercise(allByProject, exToAttrs, exercise);
//      logger.info("Attr for " + exercise.getID() + " " + exercise.getAttributes());
      allPairs.add(addExerciseToSectionHelper(slickExercise, baseTypeOrder, sectionHelper, exToPhones, lookup, exercise, attrTypes));
      copy.add(exercise);
    }

    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.info("getExercises took " + (now - then) + " to attach attributes to " + all.size() + " exercises.");
    }

    if (addTypesToSection) {
      logger.info("getExercises type order " + typeOrder);
      sectionHelper.rememberTypesInOrder(typeOrder, allPairs);
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
      List<ExerciseAttribute> attributes = new ArrayList<>();
      for (SlickExerciseAttributeJoin join : slickExerciseAttributeJoins) {
        attributes.add(allByProject.get(join.attrid()));
      }

      exercise.setAttributes(attributes);
      //   logger.info("now " + exercise.getID() + "  " + exercise.getAttributes());
    }
  }

  /**
   * @param userExercise
   * @param isOverride
   * @param isContext
   * @param typeOrder
   * @see IUserListManager#newExercise
   */
  @Override
  public int add(CommonExercise userExercise, boolean isOverride, boolean isContext, Collection<String> typeOrder) {
    int insert = insert(toSlick(userExercise, isOverride, isContext, typeOrder));
    ((Exercise) userExercise).setID(insert);
    return insert;
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
   * @return
   * @see mitll.langtest.server.database.userlist.SlickUserListDAO#populateList(UserList)
   */
  @Override
  public List<CommonShell> getOnList(int listID) {
    List<CommonExercise> userExercises = getCommonExercises(listID);
    List<CommonShell> userExercises2 = new ArrayList<>();
    userExercises2.addAll(userExercises);
    return userExercises2;
  }

  public List<CommonExercise> getCommonExercises(int listID) {
    return getUserExercises(dao.getOnList(listID));
  }

  @Override
  public CommonExercise getByExID(int exid) {
    Collection<SlickExercise> byExid = dao.byID(exid);
    CommonExercise exercise = byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());

    if (exercise != null) {
      //   for (SlickExercise ex:relatedExerciseDAOWrapper.contextExercises(exid)) exercise.getDirectlyRelated().add(fromSlick(ex));
      relatedExerciseDAOWrapper.contextExercises(exid)
          .forEach(ex -> exercise.getDirectlyRelated().add(fromSlick(ex)));
    }
    return exercise;
  }

  @Override
  public CommonExercise getTemplateExercise(int projID) {
    if (templateExercise == null) {
      Collection<SlickExercise> byExid = dao.getByExid(NEW_USER_EXERCISE, projID);
      templateExercise = byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());
    }
    return templateExercise;
  }

  private CommonExercise templateExercise;

  /**
   * @param projID
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
    return id;
  }

  public List<CommonExercise> getAllUserExercises(int projid) {
    return getUserExercises(dao.getAllUserEx(projid));
  }

  /**
   * @param typeOrder
   * @param sectionHelper
   * @param theProject
   * @return
   * @paramx attributeTypes
   * @see DBExerciseDAO#readExercises
   */
  public List<CommonExercise> getByProject(
      List<String> typeOrder,
      ISection<CommonExercise> sectionHelper,
      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject,
      Project theProject,
      Map<Integer, ExerciseAttribute> allByProject,
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs) {
    // TODO : consider getting exercise->phone from the ref result table again
//    logger.info("getByProject type order " + typeOrder);
    int projectid = theProject.getID();
//    Collection<String> attributeTypes = getAttributeTypes(projectid);
    return getExercises(dao.getAllPredefByProject(projectid),
        typeOrder,
        sectionHelper,
        exerciseToPhoneForProject, theProject, allByProject, exToAttrs, /*attributeTypes, */true);
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

    logger.info("getContextByProject For " + projectid + " got " + allContextPredefByProject.size() + " context predef ");

    return getExercises(allContextPredefByProject, typeOrder, sectionHelper,
        exerciseToPhoneForProject, lookup, allByProject, exToAttrs, /*attributeTypes,*/ false);
  }

  @Override
  public Collection<CommonExercise> getOverrides() {
    return getUserExercises(dao.getOverrides());
  }

  @Override
  public Collection<CommonExercise> getByExID(Collection<Integer> exids) {
    return getUserExercises(dao.byIDs(exids));
  }

  /**
   * TODOx : Why so complicated?
   *
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @seex UserListManager#editItem
   * @see mitll.langtest.server.services.ProjectServiceImpl#doUpdate
   */
  @Override
  public boolean update(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder) {
    //logger.info("update : " + userExercise.getID() + " has " + userExercise.getDirectlyRelated().size() + " context");
    SlickExercise slickUserExercise = toSlick(userExercise, true, isContext, typeOrder);

    int rows = dao.update(slickUserExercise);
    boolean didIt = rows > 0;
    {
      String idLabel = userExercise.getID() + "/" + slickUserExercise.id();
      if (rows == 0 /*&& createIfDoesntExist*/) {
//      int insert = dao.insert(slickUserExercise);
//      logger.info("update inserted exercise #" + insert);
        logger.error("update didn't update exercise #" + idLabel);
      } else {
        logger.info("update : updated exercise #" + idLabel);
      }
    }

    // recurse on related context exercises
    for (CommonExercise contextEx : userExercise.getDirectlyRelated()) {
      logger.info("update with context exercise " + contextEx.getID() + " context " + contextEx.getForeignLanguage() + " " + contextEx.getEnglish());
      didIt &= update(contextEx, true, typeOrder);
    }
    return didIt;
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyOneConfig(DatabaseImpl, String, String, int, boolean)
   */
  public boolean isProjectEmpty(int projectid) {
    return dao.isProjectEmpty(projectid);
  }

  /**
   * @return
   * @see DatabaseImpl#createTables
   */
  public IDAO getRelatedExercise() {
    return new IDAO() {
      public void createTable() {
        relatedExerciseDAOWrapper.createTable();
      }

      public String getName() {
        return relatedExerciseDAOWrapper.getName();
      }
    };
  }

  public IDAO getExerciseAttribute() {
    return new IDAO() {
      public void createTable() {
        attributeDAOWrapper.createTable();
      }

      public String getName() {
        return attributeDAOWrapper.getName();
      }
    };
  }

  public IDAO getExerciseAttributeJoin() {
    return new IDAO() {
      public void createTable() {
        attributeJoinDAOWrapper.createTable();
      }

      public String getName() {
        return attributeJoinDAOWrapper.getName();
      }
    };
  }

  /**
   * @param relatedExercises
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addContextExercises
   */
  public void addBulkRelated(List<SlickRelatedExercise> relatedExercises) {
    relatedExerciseDAOWrapper.addBulk(relatedExercises);
  }

  public Collection<SlickRelatedExercise> getAllRelated(int projid) {
    return relatedExerciseDAOWrapper.allByProject(projid);
  }

  public void addContextToExercise(int exid, int contextExid, int projid) {
    relatedExerciseDAOWrapper.insert(new SlickRelatedExercise(-1, exid, contextExid, projid, new Timestamp(System.currentTimeMillis())));
  }

  public int addAttribute(int projid,
                          long now,
                          int userid,
                          ExerciseAttribute attribute) {
    return insertAttribute(projid, now, userid, attribute.getProperty(), attribute.getValue());
  }

  private int insertAttribute(int projid,
                              long now,
                              int userid,
                              String property, String value) {
    return attributeDAOWrapper.insert(new SlickExerciseAttribute(-1,
        projid,
        userid,
        new Timestamp(now),
        property,
        value));
  }

  /**
   * @param projid
   * @return
   * @see SlickUserExerciseDAO#getIDToPair
   */
  private Collection<SlickExerciseAttribute> getAllByProject(int projid) {
    return attributeDAOWrapper.allByProject(projid);
  }

  /**
   * E.g. grammar or topic or sub-topic
   *
   * @param projid
   * @return
   * @see DBExerciseDAO#getAttributeTypes
   */
  public Collection<String> getAttributeTypes(int projid) {
    Set<String> unique = new HashSet<>();
    for (SlickExerciseAttribute attr : attributeDAOWrapper.allByProject(projid)) {
      unique.add(attr.property());
    }
    return new TreeSet<>(unique);
  }

  public Map<Integer, Collection<SlickExerciseAttributeJoin>> getAllJoinByProject(int projid) {
    return attributeJoinDAOWrapper.allByProject(projid);
  }

  /**
   * @param projid
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public Map<Integer, ExerciseAttribute> getIDToPair(int projid) {
    Map<Integer, ExerciseAttribute> pairMap = new HashMap<>();
    Map<String, ExerciseAttribute> known = new HashMap<>();
    getAllByProject(projid).forEach(p -> pairMap.put(p.id(), makeOrGet(known, p)));
    return pairMap;
  }

  @NotNull
  private ExerciseAttribute makeOrGet(Map<String, ExerciseAttribute> known, SlickExerciseAttribute p) {
    String key = p.property() + "-" + p.value();
    ExerciseAttribute attribute;
    if (known.containsKey(key)) {
      attribute = known.get(key);
    } else {
      attribute = new ExerciseAttribute(p.property(), p.value());
      known.put(key, attribute);
    }
    return attribute;
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addExercisesAndAttributes(int, int, SlickUserExerciseDAO, Collection, Collection)
   */
  public Map<String, Integer> getOldToNew(int projectid) {
    Map<String, Integer> oldToNew = new HashMap<>();
    List<SlickExercise> allPredefByProject = dao.getAllPredefByProject(projectid);
    for (SlickExercise exercise : allPredefByProject) {
      Integer before = oldToNew.put(exercise.exid(), exercise.id());
      if (before != null)
        logger.warn("huh? already saw an exercise with id " + exercise.exid() + " replace with " + exercise);
    }
    logger.info("getOldToNew found for project #" + projectid + " " + allPredefByProject.size() + " exercises, " + oldToNew.size() + " old->new");
//    logger.info("old->new for project #" + projectid + " has  " + oldToNew.size());
    return oldToNew;
  }

  /**
   * @param projid
   * @return
   * @see DBExerciseDAO#getIDToFL(int)
   */
  public Map<Integer, String> getIDToFL(int projid) {
    return dao.getIDToFL(projid);
  }

  private List<Integer> range = new ArrayList<>();

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
      int numPhones = exercisePhoneInfo.getNumPhones();
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

  public IRefResultDAO getRefResultDAO() {
    return refResultDAO;
  }

  public SlickExercise getUnknownExercise() {
    return unknownExercise;
  }

  public ExerciseDAOWrapper getDao() {
    return dao;
  }

  public void addBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins) {
    attributeJoinDAOWrapper.addBulk(joins);
  }

  public void removeBulkAttributeJoins(List<SlickExerciseAttributeJoin> joins) {
    attributeJoinDAOWrapper.removeBulk(joins);
  }

  public Map<Integer, SlickExercise> getLegacyToEx(int projectid) {
    return dao.getLegacyToExercise(projectid);
  }
}
