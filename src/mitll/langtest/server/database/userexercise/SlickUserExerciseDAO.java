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
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserExercise;
import mitll.npdata.dao.userexercise.UserExerciseDAOWrapper;
import org.apache.log4j.Logger;
import scala.collection.Seq;

import java.sql.Timestamp;
import java.util.*;

public class SlickUserExerciseDAO
    extends BaseUserExerciseDAO implements IUserExerciseDAO, ISchema<UserExercise, SlickUserExercise> {
  private static final Logger logger = Logger.getLogger(SlickUserExerciseDAO.class);

  private final UserExerciseDAOWrapper dao;

  public SlickUserExerciseDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new UserExerciseDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public SlickUserExercise toSlick(UserExercise shared, String language) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    List<String> typeOrder = getTypeOrder();
    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";

    return new SlickUserExercise(-1,
        shared.getCreator(),
        shared.getID(),
        shared.getEnglish(),
        shared.getForeignLanguage(),
        shared.getTransliteration(),
        shared.isOverride(),
        new Timestamp(shared.getUpdateTime()),
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        (int) shared.getUniqueID());
  }

  List<String> getTypeOrder() {
    return exerciseDAO.getSectionHelper().getTypeOrder();
  }

  public SlickUserExercise toSlick(CommonExercise shared, boolean isOverride) {
    Map<String, String> unitToValue = shared.getUnitToValue();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";

    return new SlickUserExercise(-1,
        shared.getCreator(),
        shared.getID(),
        shared.getEnglish(),
        shared.getForeignLanguage(),
        shared.getTransliteration(),
        isOverride,
        new Timestamp(shared.getUpdateTime()),
        unitToValue.getOrDefault(first, ""),
        unitToValue.getOrDefault(second, ""),
        -1);
  }

  @Override
  public UserExercise fromSlick(SlickUserExercise slick) {
    Map<String, String> unitToValue = new HashMap<>();
    Iterator<String> iterator = getTypeOrder().iterator();
    String first = iterator.next();
    String second = iterator.hasNext() ? iterator.next() : "";
    unitToValue.put(first, slick.unit());
    if (!second.isEmpty())
      unitToValue.put(second, slick.lesson());

    return new UserExercise(
        slick.id(),
        slick.exid(),
        slick.userid(),
        slick.english(),
        slick.foreignlanguage(),
        slick.transliteration(),
        "",
        "",
        slick.isoverride(),
        unitToValue,
        slick.modified().getTime());
  }

  public void insert(SlickUserExercise UserExercise) {
    dao.insert(UserExercise);
  }

  public void addBulk(List<SlickUserExercise> bulk) {
    dao.addBulk(bulk);
  }

  public int getNumRows() {
    return dao.getNumRows();
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }

  private List<CommonExercise> getUserExercises(List<SlickUserExercise> all) {
    List<CommonExercise> copy = new ArrayList<>();
    for (SlickUserExercise UserExercise : all) copy.add(fromSlick(UserExercise));
    return copy;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, CommonExercise, String)
   * @param userExercise
   * @param isOverride
   */
  @Override
  public void add(CommonExercise userExercise, boolean isOverride) {
    logger.info("adding " + userExercise);
    insert(toSlick(userExercise, isOverride));
  }

  @Override
  public List<CommonShell> getOnList(long listID) {
    List<CommonShell> userExercises2 = new ArrayList<>();

    enrichWithPredefInfo(userExercises2, getUserExercises(dao.getOnList((int) listID)));

    return userExercises2;
  }

  @Override
  public CommonExercise getWhere(String exid) {
    exid = exid.replaceAll("\'", "");

    Seq<SlickUserExercise> byExid = dao.getByExid(exid);
    return byExid.isEmpty() ? null : fromSlick(byExid.iterator().next());
  }

  public List<CommonExercise> getAll() {
    return getUserExercises(dao.getAll());
  }

  @Override
  public Collection<CommonExercise> getOverrides() {
    return getUserExercises(dao.getOverrides());
  }

  @Override
  public Collection<CommonExercise> getWhere(Collection<String> exids) {
    return getUserExercises(dao.byExids(exids));
  }

  @Override
  public void update(CommonExercise userExercise, boolean createIfDoesntExist) {
    SlickUserExercise slickUserExercise = toSlick(userExercise, true);
    int rows = dao.update(slickUserExercise);
    if (rows == 0 && createIfDoesntExist) {
      dao.insert(slickUserExercise);
    }
  }

/*  public void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }*/
}
