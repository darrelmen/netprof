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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;
import org.apache.log4j.Logger;
import scala.collection.Seq;

import java.sql.Timestamp;
import java.util.*;

public class SlickUserExerciseDAO
    extends BaseUserExerciseDAO implements IUserExerciseDAO/*, ISchema<UserExercise, SlickExercise>*/ {
  private static final Logger logger = Logger.getLogger(SlickUserExerciseDAO.class);

  private final ExerciseDAOWrapper dao;
  private final RelatedExerciseDAOWrapper relatedExerciseDAOWrapper;

  public SlickUserExerciseDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new ExerciseDAOWrapper(dbConnection);
    relatedExerciseDAOWrapper = new RelatedExerciseDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  //  @Override
  public SlickExercise toSlick(UserExercise shared, int projectID) {
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
        shared.getTransliteration(),
        shared.isOverride(),
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        projectID,  // project id fk
        false,
        false,
        shared.getID());
  }

  /**
   * TODO : type order depends on project
   *
   * @return
   */
  List<String> getTypeOrder() {
    return exerciseDAO.getSectionHelper().getTypeOrder();
  }

  /**
   * TODO : we won't do override items soon, since they will just be domino edits...
   *
   * @param shared
   * @param isOverride
   * @return
   */
  public SlickExercise toSlick(CommonExercise shared, @Deprecated boolean isOverride) {
    return toSlick(shared, isOverride, -1, false, BaseUserDAO.DEFAULT_USER_ID, false);
  }

  /**
   * @param shared
   * @param isOverride
   * @param isPredef
   * @param isContext
   * @return
   * @see SlickUserExerciseDAO#add(CommonExercise, boolean)
   */
  public SlickExercise toSlick(CommonExercise shared, @Deprecated boolean isOverride, int projectID, boolean isPredef,
                               int importUser,
                               boolean isContext) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";

    int creator = shared.getCreator();
    if (creator == BaseUserDAO.UNDEFINED_USER) creator = importUser;

    return new SlickExercise(-1,
        creator,
        shared.getOldID(),
        new Timestamp(shared.getUpdateTime()),
        shared.getEnglish(),
        shared.getMeaning(),
        shared.getForeignLanguage(),
        shared.getTransliteration(),
        isOverride,
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        projectID,  // project id fk
        isPredef,
        isContext,
        -1);
  }

  /**
   * @param slick
   * @return
   * @see #getUserExercises(Collection)
   */
  //  @Override
  private UserExercise fromSlick(SlickExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    unitToValue.put(first, slick.unit());
    if (!second.isEmpty())
      unitToValue.put(second, slick.lesson());

    UserExercise userExercise = new UserExercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        slick.english(),
        slick.foreignlanguage(),
        slick.transliteration(),
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime(), slick.projectid());

//    logger.info("created " + userExercise);
    return userExercise;
  }

  private long lastModified = System.currentTimeMillis();

  /**
   * So we need to not put the context sentences, at least initially, under the unit-chapter hierarchy.
   * TODO : add context sentences
   * TODO : connect context sentences
   *
   * @param slick
   * @return
   */
  private Exercise fromSlickToExercise(SlickExercise slick,
                                       Collection<String> typeOrder,
                                       SectionHelper<CommonExercise> sectionHelper) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    boolean firstEmpty = slick.unit().isEmpty();
    if (firstEmpty && slick.ispredef()) {
      unitToValue.put(first, "1");
      logger.warn("got empty " + first + " for " + slick);

    } else {
      unitToValue.put(first, slick.unit());
    }
    if (!second.isEmpty()) {
      if (slick.ispredef()) {
        boolean empty = slick.lesson().trim().isEmpty();
        unitToValue.put(second, empty ? "1" : slick.lesson());
        if (empty) logger.warn("got empty " + second + " for " + slick);
      }
    }

    Exercise exercise = new Exercise(
        slick.id(),
        slick.exid(),
        slick.english(),
        slick.foreignlanguage(),
        slick.meaning(),
        slick.transliteration(),
        slick.projectid());

    List<String> translations = new ArrayList<String>();
    if (slick.foreignlanguage().length() > 0) {
      translations.add(slick.foreignlanguage());
    }
    exercise.setRefSentences(translations);

//    if (!context.isEmpty()) {
//      imported.addContext(context,contextTranslation);
//    }
    exercise.setUpdateTime(lastModified);

    if (slick.ispredef()) {
      List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();
      for (Map.Entry<String, String> pair : unitToValue.entrySet()) {
        pairs.add(sectionHelper.addExerciseToLesson(exercise, pair.getKey(), pair.getValue()));
      }
      sectionHelper.addAssociations(pairs);
    } else {
      exercise.setUnitToValue(unitToValue);
    }

    return exercise;
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
    List<CommonExercise> copy = new ArrayList<>();
    for (SlickExercise userExercise : all) copy.add(fromSlick(userExercise));
    return copy;
  }

  private List<CommonExercise> getExercises(Collection<SlickExercise> all,
                                            Collection<String> typeOrder,
                                            SectionHelper<CommonExercise> sectionHelper) {
    List<CommonExercise> copy = new ArrayList<>();
    for (SlickExercise userExercise : all) copy.add(fromSlickToExercise(userExercise, typeOrder, sectionHelper));
    return copy;
  }

  /**
   * @param userExercise
   * @param isOverride
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, CommonExercise, String)
   */
  @Override
  public void add(CommonExercise userExercise, boolean isOverride) {
    //  logger.info("adding " + userExercise);
    insert(toSlick(userExercise, isOverride));
  }

  /**
   * @param listID
   * @return
   * @see mitll.langtest.server.database.userlist.SlickUserListDAO#populateList(UserList)
   */
  @Override
  public List<CommonShell> getOnList(int listID) {
    List<CommonExercise> userExercises = getUserExercises(dao.getOnList(listID));
    List<CommonShell> userExercises2 = new ArrayList<>();
    userExercises2.addAll(userExercises);
    return userExercises2;
//    List<CommonShell> userExercises2 = new ArrayList<>();
//    enrichWithPredefInfo(userExercises2, userExercises);
//    return userExercises2;
  }

  @Override
  public CommonExercise getByExID(int exid) {
    //exid = exid.replaceAll("\'", "");
    Seq<SlickExercise> byExid = dao.byID(exid);
    return byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());
  }

  public List<CommonExercise> getAll() {
    return getUserExercises(dao.getAllUserEx());
  }

  /**
   * @param typeOrder
   * @param sectionHelper
   * @return
   * @see DBExerciseDAO#readExercises()
   */
  public List<CommonExercise> getAllExercises(List<String> typeOrder, SectionHelper<CommonExercise> sectionHelper) {
    return getExercises(dao.getAllPredefEx(), typeOrder, sectionHelper);
  }

  public List<CommonExercise> getByProject(int projectid, List<String> typeOrder, SectionHelper<CommonExercise> sectionHelper) {
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

  public boolean isProjectEmpty(int projectid) {
    return dao.isProjectEmpty(projectid);
  }

//  private RelatedExerciseDAOWrapper getRelatedExerciseDAOWrapper() {
//    return relatedExerciseDAOWrapper;
//  }

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

  public void insertRelated(int id, int contextid) {
    relatedExerciseDAOWrapper.insert(new SlickRelatedExercise(-1, id, contextid));
  }

  public Collection<SlickRelatedExercise> getAllRelated() {
    return relatedExerciseDAOWrapper.all();
  }

  public Map<String, Integer> getOldToNew(int projectid) {
    Map<String, Integer> oldToNew = new HashMap<>();
    for (SlickExercise exercise : dao.getAllPredefByProject(projectid)) oldToNew.put(exercise.exid(), exercise.id());
    return oldToNew;
  }
}
