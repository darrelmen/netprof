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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.collection.Seq;

import java.sql.Timestamp;
import java.util.*;

public class SlickUserExerciseDAO
    extends BaseUserExerciseDAO
    implements IUserExerciseDAO {
  private static final Logger logger = LogManager.getLogger(SlickUserExerciseDAO.class);

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#getTypeOrder
   * @see #addPhoneInfo(SlickExercise, Collection, SectionHelper, int, Exercise)
   */
  public static final String SOUND = "Sound";
  public static final String DIFFICULTY = "Difficulty";

  /**
   * TODO : need to do something to allow this to scale well - maybe ajax style nested types, etc.
   */
  private static final boolean ADD_PHONE_LENGTH = false;
  private static final String NEW_USER_EXERCISE = "NEW_USER_EXERCISE";

  private final long lastModified = System.currentTimeMillis();
  private final ExerciseDAOWrapper dao;
  private final RelatedExerciseDAOWrapper relatedExerciseDAOWrapper;
  private Map<Integer, ExercisePhoneInfo> exToPhones;
  // private final int beforeLoginUser;
  private final IUserDAO userDAO;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   */
  public SlickUserExerciseDAO(DatabaseImpl database, DBConnection dbConnection
  ) {
    super(database);
    dao = new ExerciseDAOWrapper(dbConnection);
    relatedExerciseDAOWrapper = new RelatedExerciseDAOWrapper(dbConnection);
    userDAO = database.getUserDAO();
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
   * @see mitll.langtest.server.database.copy.CopyToPostgres#addUserExercises(DatabaseImpl, Map, int, SlickUserExerciseDAO)
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
        "",
        shared.getTransliteration(),
        shared.isOverride(),
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        projectID,  // project id fk
        false,
        false,
        false,
        shared.getID());
  }

  /**
   * @return
   */
  List<String> getTypeOrder() {
//    logger.info("getTypeOrder : exercise DAO "+ exerciseDAO);
    List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
    if (typeOrder.isEmpty()) typeOrder = exerciseDAO.getTypeOrder();
    return typeOrder;
  }

  /**
   * TODO : we won't do override items soon, since they will just be domino edits...?
   *
   * @param shared
   * @param isOverride
   * @return
   */
  public SlickExercise toSlick(CommonExercise shared, @Deprecated boolean isOverride) {
    return toSlick(shared, isOverride, shared.getProjectID(), false, BaseUserDAO.DEFAULT_USER_ID, false);
  }

  /**
   * @param shared
   * @param isOverride
   * @param isPredef
   * @param isContext
   * @return
   * @see SlickUserExerciseDAO#add(CommonExercise, boolean)
   */
  public SlickExercise toSlick(CommonExercise shared,
                               @Deprecated boolean isOverride,
                               int projectID,
                               boolean isPredef,
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
    return new SlickExercise(-1,
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
        projectID,  // project id fk
        isPredef,
        isContext,
        false,
        -1);
  }

  /**
   * @param slick
   * @return
   * @see #getUserExercises(Collection)
   */
  private Exercise fromSlick(SlickExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    unitToValue.put(first, slick.unit());
    if (!second.isEmpty())
      unitToValue.put(second, slick.lesson());

    Exercise userExercise = new Exercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        slick.english(),
        slick.foreignlanguage(),
        slick.transliteration(),
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime(), slick.projid());

//    logger.info("fromSlick created " + userExercise);
    return userExercise;
  }

  /**
   * So we need to not put the context sentences, at least initially, under the unit-chapter hierarchy.
   * <p>
   * Use exercise -> phone map to determine phones per exercise...
   *
   * @param slick
   * @return
   * @see IUserExerciseDAO#setExToPhones(Map)
   * @see #getExercises(Collection, Collection, SectionHelper)
   */
  private Exercise fromSlickToExercise(SlickExercise slick,
                                       Collection<String> typeOrder,
                                       SectionHelper<CommonExercise> sectionHelper) {
    int id = slick.id();
    Exercise exercise = new Exercise(
        id,
        slick.exid(),
        BaseUserDAO.UNDEFINED_USER,
        slick.english(),
        slick.foreignlanguage(),
        slick.meaning(),
        slick.transliteration(),
        slick.projid());

    List<String> translations = new ArrayList<String>();
    if (slick.foreignlanguage().length() > 0) {
      translations.add(slick.foreignlanguage());
    }
    exercise.setRefSentences(translations);
    exercise.setUpdateTime(lastModified);
    exercise.setAltFL(slick.altfl());
    boolean added = addPhoneInfo(slick, typeOrder, sectionHelper, id, exercise);

    return exercise;
  }

  /**
   * @see #fromSlickToExercise(SlickExercise, Collection, SectionHelper)
   * @param slick
   * @param typeOrder
   * @param sectionHelper
   * @param id
   * @param exercise
   * @return
   */
  private boolean addPhoneInfo(SlickExercise slick, Collection<String> typeOrder,
                            SectionHelper<CommonExercise> sectionHelper,
                            int id,
                            Exercise exercise) {
    Map<String, String> unitToValue = getUnitToValue(slick, typeOrder);

    ExercisePhoneInfo exercisePhoneInfo = exToPhones.get(id);
    Collection<String> phones = exercisePhoneInfo == null ? null : exercisePhoneInfo.getPhones();
    //if (phones == null || phones.isEmpty()) logger.warn("no phones for " + id);
    int max = 15;
    int i = 0;
    boolean addedPhones = false;
    if (slick.ispredef() && !slick.iscontext()) {
      //    addExerciseToSectionHelper(sectionHelper, unitToValue, exercise);
      List<SectionHelper.Pair> pairs = getPairs(sectionHelper, unitToValue, exercise);
      if (phones == null) {
//        logger.warn("no phones for " + id);
      } else {
        addedPhones = true;
        for (String phone : phones) {
          pairs.add(sectionHelper.getPairForExerciseAndLesson(exercise, SOUND, phone));
        }

        if (ADD_PHONE_LENGTH) {
          int numPhones = exercisePhoneInfo.getNumPhones();

          int choice = range.get(0);
          for (int r : range) {
            i++;
            if (numPhones <= r) {
              choice = i;
              break;
            }
          }
          String label = "" + choice;
//        if (numPhones >= max) {
//          label = ">" + (max - 1);
//        }
          pairs.add(sectionHelper.getPairForExerciseAndLesson(exercise, DIFFICULTY, label));
        }
      }

      sectionHelper.addAssociations(pairs);
      return addedPhones;
    } else {
      exercise.setUnitToValue(unitToValue);
      return true;
    }
  }

  private List<SectionHelper.Pair> getPairs(SectionHelper<CommonExercise> sectionHelper,
                                            Map<String, String> unitToValue,
                                            Exercise exercise) {
    List<SectionHelper.Pair> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : unitToValue.entrySet()) {
      pairs.add(getPair(sectionHelper, exercise, pair));
    }
    return pairs;
  }

  private SectionHelper.Pair getPair(SectionHelper<CommonExercise> sectionHelper,
                                     Exercise exercise,
                                     Map.Entry<String, String> pair) {
    return sectionHelper.addExerciseToLesson(exercise, pair.getKey(), pair.getValue());
  }

  int c = 0;

  /**
   * @see #addPhoneInfo
   * @param slick
   * @param typeOrder
   * @return
   */
  private Map<String, String> getUnitToValue(SlickExercise slick, Collection<String> typeOrder) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = typeOrder.iterator();
    String first  = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    boolean firstEmpty = slick.unit().isEmpty();
    if (firstEmpty && slick.ispredef()) {
      unitToValue.put(first, "1");
      if (c++ < 100 || c % 100 == 0) logger.warn("getUnitToValue (" +c+
          ") got empty " + first + " for " + slick + " type order " + typeOrder);

    } else {
      unitToValue.put(first, slick.unit());
    }
    if (!second.isEmpty()) {
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
    for (SlickExercise userExercise : all) copy.add(fromSlick(userExercise));
    //  logger.info("getUserExercises returned " + copy.size()+ " user exercises");
    return copy;
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @return
   * @see #getByProject(int, List, SectionHelper)
   * @see #getContextByProject(int, List, SectionHelper)
   */
  private List<CommonExercise> getExercises(Collection<SlickExercise> all,
                                            Collection<String> typeOrder,
                                            SectionHelper<CommonExercise> sectionHelper) {
    List<CommonExercise> copy = new ArrayList<>();
    for (SlickExercise userExercise : all) {
      copy.add(fromSlickToExercise(userExercise, typeOrder, sectionHelper));
    }
    return copy;
  }

  /**
   * @param userExercise
   * @param isOverride
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, CommonExercise, String)
   */
  @Override
  public int add(CommonExercise userExercise, boolean isOverride) {
    int insert = insert(toSlick(userExercise, isOverride));
    ((Exercise) userExercise).setID(insert);
    return insert;
  }

  /**
   * @param projID
   */
  private void insertDefault(int projID) {
    int beforeLoginUser = userDAO.getBeforeLoginUser();
    SlickExercise userExercise = new SlickExercise(-1,
        beforeLoginUser,
        NEW_USER_EXERCISE,
        new Timestamp(System.currentTimeMillis()),
        "",
        "",
        "",
        "", "", false, "", "", projID, true, false, false, -1);

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
    Seq<SlickExercise> byExid = dao.byID(exid);
    return byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());
  }

  @Override
  public CommonExercise getTemplateExercise(int projID) {
    if (templateExercise == null) {
      Seq<SlickExercise> byExid = dao.getByExid(NEW_USER_EXERCISE, projID);
      templateExercise = byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());
    }
    return templateExercise;
  }

  private CommonExercise templateExercise;

  public void ensureTemplateExercise(int projID) {
    if (dao.getByExid(NEW_USER_EXERCISE, projID).isEmpty()) {
      insertDefault(projID);
    }
  }

  public List<CommonExercise> getAllUserExercises(int projid) {
    return getUserExercises(dao.getAllUserEx(projid));
  }

  /**
   * @param projectid
   * @param typeOrder
   * @param sectionHelper
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public List<CommonExercise> getByProject(int projectid, List<String> typeOrder,
                                           SectionHelper<CommonExercise> sectionHelper) {
    return getExercises(dao.getAllPredefByProject(projectid), typeOrder, sectionHelper);
  }

  public List<CommonExercise> getContextByProject(int projectid, List<String> typeOrder, SectionHelper<CommonExercise> sectionHelper) {
    return getExercises(dao.getAllContextPredefByProject(projectid), typeOrder, sectionHelper);
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
   * @param userExercise
   * @param createIfDoesntExist
   * @see mitll.langtest.server.database.custom.UserListManager#editItem(CommonExercise, boolean, String)
   */
  @Override
  public void update(CommonExercise userExercise, boolean createIfDoesntExist) {
    SlickExercise slickUserExercise = toSlick(userExercise, true);
    int rows = dao.update(slickUserExercise);
    if (rows == 0 && createIfDoesntExist) {
      dao.insert(slickUserExercise);
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

  public void addBulkRelated(List<SlickRelatedExercise> relatedExercises) {
    relatedExerciseDAOWrapper.addBulk(relatedExercises);
  }

  public Collection<SlickRelatedExercise> getAllRelated(int projid) {
    return relatedExerciseDAOWrapper.allByProject(projid);
  }

  public Map<String, Integer> getOldToNew(int projectid) {
    Map<String, Integer> oldToNew = new HashMap<>();
    for (SlickExercise exercise : dao.getAllPredefByProject(projectid)) oldToNew.put(exercise.exid(), exercise.id());
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

  List<Integer> range = new ArrayList<>();

  /**
   * @param exToPhones
   * @see DatabaseImpl#configureProjects
   */
  @Override
  public void setExToPhones(Map<Integer, ExercisePhoneInfo> exToPhones) {
    this.exToPhones = exToPhones;

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
    logger.info("setExToPhones got range " + range);
  }
}
