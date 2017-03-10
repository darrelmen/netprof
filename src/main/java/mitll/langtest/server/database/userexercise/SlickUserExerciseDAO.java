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
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.*;
import mitll.npdata.dao.userexercise.ExerciseAttributeDAOWrapper;
import mitll.npdata.dao.userexercise.ExerciseAttributeJoinDAOWrapper;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

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

  private final long lastModified = System.currentTimeMillis();
  private final ExerciseDAOWrapper dao;
  private final RelatedExerciseDAOWrapper relatedExerciseDAOWrapper;
  private final ExerciseAttributeDAOWrapper attributeDAOWrapper;
  private final ExerciseAttributeJoinDAOWrapper attributeJoinDAOWrapper;
  //  private Map<Integer, ExercisePhoneInfo> exToPhones;
  private final IUserDAO userDAO;
  private IRefResultDAO refResultDAO;
  public static final boolean ADD_PHONE_LENGTH = true;
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
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addUserExercises
   */
  public SlickExercise toSlick(Exercise shared, int projectID) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    List<String> typeOrder = getTypeOrder();
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
        never);
  }

  private List<String> typeOrder = null;

  /**
   * @return
   */
  List<String> getTypeOrder() {
    if (typeOrder == null) {
//    logger.info("getTypeOrder : exercise DAO "+ exerciseDAO);
      typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
      if (typeOrder.isEmpty()) {
        typeOrder = exerciseDAO.getTypeOrder();
      }
    }
    return typeOrder;
  }

  /**
   * TODO : we won't do override items soon, since they will just be domino edits...?
   *
   * @param shared
   * @param isOverride
   * @param isContext
   * @return
   */
  private SlickExercise toSlick(CommonExercise shared, @Deprecated boolean isOverride, boolean isContext) {
    return toSlick(shared, isOverride, BaseUserDAO.DEFAULT_USER_ID, isContext);
  }

  public SlickExercise toSlick(CommonExercise shared,
                               @Deprecated boolean isOverride,
                               int importUser,
                               boolean isContext) {
    return toSlick(shared, isOverride, shared.getProjectID(), importUser, isContext);
  }

  /**
   * @param shared
   * @param isOverride
   * @param isContext
   * @return
   * @paramx isPredef
   * @see #toSlick
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addContextExercises
   */
  public SlickExercise toSlick(CommonExercise shared,
                               @Deprecated boolean isOverride,
                               int projectID,
                               int importUser,
                               boolean isContext) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    if (getTypeOrder().isEmpty()) {
      logger.error("type order is empty?");
      return null;
    }
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";

    int creator = shared.getCreator();
    if (creator == BaseUserDAO.UNDEFINED_USER) creator = importUser;

    long updateTime = shared.getUpdateTime();
    if (updateTime == 0) updateTime = lastModified;
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
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        projectID,//shared.getProjectID(),  // project id fk
        shared.isPredefined(),
        isContext,
        false,
        -1,
        false,
        never);
  }

  Timestamp never = new Timestamp(0);

  /**
   * @param slick
   * @return
   * @see #getUserExercises(Collection)
   */
  private Exercise fromSlick(SlickExercise slick) {
    Map<String, String> unitToValue = getUnitToValue(slick);

    Exercise userExercise = new Exercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        slick.english(),
        slick.foreignlanguage(),
        slick.altfl(),
        slick.transliteration(),
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime());

//    logger.info("fromSlick created " + userExercise);
    return userExercise;
  }

  @NotNull
  private Map<String, String> getUnitToValue(SlickExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    unitToValue.put(first, slick.unit());
    if (!second.isEmpty())
      unitToValue.put(second, slick.lesson());
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
  private Exercise fromSlickToExercise(SlickExercise slick,
                                       Collection<String> typeOrder,
                                       ISection<CommonExercise> sectionHelper,
                                       Map<Integer, ExercisePhoneInfo> exToPhones,
                                       PronunciationLookup lookup,
                                       List<List<Pair>> allPairs) {
    int id = slick.id();
    Exercise exercise = new Exercise(
        id,
        slick.exid(),
        BaseUserDAO.UNDEFINED_USER,
        slick.english(),
        slick.foreignlanguage(),
        slick.altfl(),
        slick.meaning(),
        slick.transliteration(),
        slick.projid(),
        slick.candecode(),
        slick.candecodechecked().getTime());

    List<String> translations = new ArrayList<String>();
    if (!slick.foreignlanguage().isEmpty()) {
      translations.add(slick.foreignlanguage());
    }
    exercise.setRefSentences(translations); // ?
    exercise.setUpdateTime(lastModified);
    exercise.setAltFL(slick.altfl());

    ExercisePhoneInfo exercisePhoneInfo = exToPhones.get(id);

    if (exercisePhoneInfo == null) {
      String pronunciations = lookup.getPronunciations(slick.foreignlanguage(), slick.transliteration());

      int n2 =lookup.getNumPhones(slick.foreignlanguage(), slick.transliteration());

      exercisePhoneInfo = pronunciations.isEmpty() ? new ExercisePhoneInfo() : new ExercisePhoneInfo(pronunciations);

      exercisePhoneInfo.setNumPhones2(n2);

      if (slick.english().equals("address")) {
        logger.info("ex " + slick);
        logger.info("ex " + slick.foreignlanguage());
        logger.info("pronunciations " + pronunciations);
        logger.info("exercisePhoneInfo " + exercisePhoneInfo);
      }
    }

    boolean added = addPhoneInfo(slick, typeOrder, sectionHelper, exercise, exercisePhoneInfo, allPairs);

    return exercise;
  }

  /**
   * @param slick
   * @param typeOrder
   * @param sectionHelper
   * @param exercise
   * @return
   * @see #fromSlickToExercise
   */
  private boolean addPhoneInfo(SlickExercise slick,
                               Collection<String> typeOrder,
                               ISection<CommonExercise> sectionHelper,
                               Exercise exercise,
                               ExercisePhoneInfo exercisePhoneInfo,
                               List<List<Pair>> allPairs) {
    Map<String, String> unitToValue = getUnitToValue(slick, typeOrder);

    Collection<String> phones = exercisePhoneInfo == null ? null : exercisePhoneInfo.getPhones();
    //if (phones == null || phones.isEmpty()) logger.warn("no phones for " + id);
    int max = 15;
    int i = 0;
    boolean addedPhones = false;
    if (slick.ispredef() && !slick.iscontext()) {
      //    addExerciseToSectionHelper(sectionHelper, unitToValue, exercise);
      List<Pair> pairs = getPairs(sectionHelper, unitToValue, exercise);
      if (phones == null) {
//        logger.warn("no phones for " + id);
      } else {
        addedPhones = true;

        // TODO : maybe put back phone length later ???
        if (ADD_PHONE_LENGTH) {
          int numPhones = exercisePhoneInfo.getNumPhones2();

    /*      int choice = range.get(0);
          for (int r : range) {
            i++;
            if (numPhones <= r) {
              choice = i;
              break;
            }
          }*/

          //   String label = "" + choice;

          String label = "" + numPhones;
          if (numPhones >= max) {
            label = ">" + (max - 1);
          }
          pairs.add(sectionHelper.getPairForExerciseAndLesson(exercise, DIFFICULTY, label));
        }

        // need to make a copy
        for (String phone : phones) {
          List<Pair> copy = new ArrayList<>(pairs);
          copy.add(sectionHelper.getPairForExerciseAndLesson(exercise, SOUND, phone));

  /*        if (slick.unit().equals("1") && slick.lesson().equals("1")) {
            logger.info("for " + slick.id() + " " + slick.english() + " " + slick.foreignlanguage() + " (" +
                exercisePhoneInfo.getNumPhones() +
                ") " + copy);
          }*/
          allPairs.add(copy);
        }
      }

      // sectionHelper.addAssociations(pairs);
      return addedPhones;
    } else {
      exercise.setUnitToValue(unitToValue);
      return true;
    }
  }

  /**
   * @param sectionHelper
   * @param unitToValue
   * @param exercise
   * @return
   * @see #addPhoneInfo
   */
  private List<Pair> getPairs(ISection<CommonExercise> sectionHelper,
                              Map<String, String> unitToValue,
                              Exercise exercise) {
    List<Pair> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : unitToValue.entrySet()) {
      pairs.add(getPair(sectionHelper, exercise, pair));
    }
    return pairs;
  }

  private Pair getPair(ISection<CommonExercise> sectionHelper,
                       Exercise exercise,
                       Map.Entry<String, String> pair) {
    return sectionHelper.addExerciseToLesson(exercise, pair.getKey(), pair.getValue());
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
  private Map<String, String> getUnitToValue(SlickExercise slick, Collection<String> typeOrder) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    boolean firstEmpty = slick.unit().isEmpty();

    if (firstEmpty && slick.ispredef()) {
      unitToValue.put(first, "1");
      if (c++ < 100 || c % 100 == 0) logger.warn("getUnitToValue (" + c +
          ") got empty " + first + " for " + slick + " type order " + typeOrder);

    } else {
      unitToValue.put(first, slick.unit());
    }

    if (!second.isEmpty() && !second.equals(SOUND)) {
      if (slick.ispredef()) {
        boolean empty = slick.lesson().trim().isEmpty();
        unitToValue.put(second, empty ? "1" : slick.lesson());
        if (empty) {
          if (c++ < 100) logger.warn("getUnitToValue got empty " + second + " for " + slick);
        }
      }
    }
    return unitToValue;
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

    for (SlickExercise userExercise : all) {
      copy.add(fromSlick(userExercise));
    }
    //  logger.info("getUserExercises returned " + copy.size()+ " user exercises");
    return copy;
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @return
   * @see #getByProject
   * @see #getContextByProject
   */
  private List<CommonExercise> getExercises(Collection<SlickExercise> all,
                                            List<String> typeOrder,
                                            ISection<CommonExercise> sectionHelper,
                                            Map<Integer, ExercisePhoneInfo> exToPhones,
                                            PronunciationLookup lookup) {
    List<CommonExercise> copy = new ArrayList<>();

    //List<String> ordered = new (typeOrder);

    List<List<Pair>> allPairs = new ArrayList<>();
    for (SlickExercise userExercise : all) {
      copy.add(fromSlickToExercise(userExercise, typeOrder, sectionHelper, exToPhones, lookup, allPairs));
    }
    sectionHelper.rememberTypesInOrder(typeOrder, allPairs);
    return copy;
  }

  /**
   * @param userExercise
   * @param isOverride
   * @param isContext
   * @see mitll.langtest.server.database.custom.UserListManager#newExercise
   */
  @Override
  public int add(CommonExercise userExercise, boolean isOverride, boolean isContext) {
    int insert = insert(toSlick(userExercise, isOverride, isContext));
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
        "", "", false, "", "", projID, true, false, false, -1, false, now);

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
   * @param projectid
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public List<CommonExercise> getByProject(int projectid,
                                           List<String> typeOrder,
                                           ISection<CommonExercise> sectionHelper,
                                           Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject,
                                           PronunciationLookup lookup) {

    logger.info("getByProject type order " + typeOrder);
    // Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject = refResultDAO.getExerciseToPhoneForProject(projectid);
    return getExercises(dao.getAllPredefByProject(projectid), typeOrder, sectionHelper, exerciseToPhoneForProject, lookup);
  }

  /**
   * @param projectid
   * @param typeOrder
   * @param sectionHelper
   * @param exerciseToPhoneForProject
   * @param lookup
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public List<CommonExercise> getContextByProject(int projectid,
                                                  List<String> typeOrder,
                                                  ISection<CommonExercise> sectionHelper,
                                                  Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject,
                                                  PronunciationLookup lookup) {
    return getExercises(dao.getAllContextPredefByProject(projectid), typeOrder, sectionHelper, exerciseToPhoneForProject, lookup);
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
   * TODO : Why so complicated?
   *
   * @param userExercise
   * @param isContext
   * @see IUserListManager#editItem
   */
  @Override
  public void update(CommonExercise userExercise, boolean isContext) {
    //logger.info("update : " + userExercise.getID() + " has " + userExercise.getDirectlyRelated().size() + " context");
    SlickExercise slickUserExercise = toSlick(userExercise, true, isContext);

    int rows = dao.update(slickUserExercise);
    String idLabel = userExercise.getID() + "/" + slickUserExercise.id();

    if (rows == 0 /*&& createIfDoesntExist*/) {
//      int insert = dao.insert(slickUserExercise);
//      logger.info("update inserted exercise #" + insert);
      logger.error("update didn't update exercise #" + idLabel);
    } else {
      logger.info("update : updated exercise #" + idLabel);
    }

    // recurse on related context exercises
    for (CommonExercise contextEx : userExercise.getDirectlyRelated()) {
      logger.info("update with context exercise " + contextEx.getID() + " context " + contextEx.getForeignLanguage() + " " + contextEx.getEnglish());
      update(contextEx, true);
    }
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

  public int addAttribute(int projid, long now,
                          int userid, ExerciseAttribute attribute) {
    return insertAttribute(projid, now, userid, attribute.getProperty(), attribute.getValue());
  }

  public int insertAttribute(int projid,
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

  public int insertAttributeJoin(
      long now,
      int userid,
      int exid,
      int attrid) {
    return attributeJoinDAOWrapper.insert(new SlickExerciseAttributeJoin(-1,
        userid,
        new Timestamp(now),
        exid,
        attrid));
  }

/*
  private int insertAttribute(SlickExerciseAttribute attribute) {
    return attributeDAOWrapper.insert(attribute);
  }
*/

  public Collection<SlickExerciseAttribute> getAllByProject(int projid) {
    return attributeDAOWrapper.allByProject(projid);
  }

  public Map<Integer, Collection<SlickExerciseAttributeJoin>> getAllJoinByProject(int projid) {
    return attributeJoinDAOWrapper.allByProject(projid);
  }

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

  public Map<String, Integer> getOldToNew(int projectid) {
    Map<String, Integer> oldToNew = new HashMap<>();
    for (SlickExercise exercise : dao.getAllPredefByProject(projectid)) oldToNew.put(exercise.exid(), exercise.id());
    logger.info("old->new for project #" + projectid + " has  " + oldToNew.size());
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
   * @param exToPhones
   * @seex DatabaseImpl#configureProjects
   */
  @Override
  public void useExToPhones(Map<Integer, ExercisePhoneInfo> exToPhones) {
    //this.exToPhones = exToPhones;

    int total = 0;

    Map<Integer, Integer> binToCount = new TreeMap<>();
    for (ExercisePhoneInfo exercisePhoneInfo : exToPhones.values()) {
      int numPhones = exercisePhoneInfo.getNumPhones();
      binToCount.put(numPhones, binToCount.getOrDefault(numPhones, 0) + 1);
      total++;
    }
    int ranges = 6;
    int desired = total / ranges;
    int window = desired;
    int prev = 0;
    for (Integer bin : binToCount.keySet()) {
      Integer count = binToCount.get(bin);
      window -= count;
      // logger.info(bin + "\t" + count + "\tleft " + window);
      if (window <= 0) {
        range.add(prev);
        window += desired;
      }
      prev = bin;
    }
    if (!range.isEmpty()) range = range.subList(0, range.size() - 1);
    logger.info("useExToPhones got range " + range);
  }

  public IRefResultDAO getRefResultDAO() {
    return refResultDAO;
  }

  public SlickExercise getUnknownExercise() {
    return unknownExercise;
  }

  public ExerciseDAOWrapper getDao() {
    return dao;
  }

  public void addBulkAttributes(List<SlickExerciseAttributeJoin> joins) {
    attributeJoinDAOWrapper.addBulk(joins);
  }
}
