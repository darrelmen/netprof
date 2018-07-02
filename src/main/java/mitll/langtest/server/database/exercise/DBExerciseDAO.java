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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.*;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.exercise.Facet.SEMESTER;
import static mitll.langtest.server.database.exercise.SectionHelper.SUBTOPIC_LC;

public class DBExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(DBExerciseDAO.class);

  private static final int SPEW_THRESH = 5;
  private final SlickUserExerciseDAO userExerciseDAO;
  private final SlickProject project;
  private final Project fullProject;
  private static final boolean DEBUG = false;

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#setExerciseDAO
   */
  public DBExerciseDAO(
      ServerProperties serverProps,
      IUserListManager userListManager,
      boolean addDefects,
      SlickUserExerciseDAO userExerciseDAO,
      Project fullProject
  ) {
    super(serverProps, userListManager, addDefects, fullProject.getProject().language());
    this.userExerciseDAO = userExerciseDAO;
    this.project = fullProject.getProject();
    this.fullProject = fullProject;
  }

  /**
   * so first look in the main id->ex map and then in the context->exercise map
   *
   * @param id
   * @return
   */
  @Override
  public CommonExercise getExercise(int id) {
    synchronized (idToExercise) { //?
      CommonExercise commonExercise = idToExercise.get(id);

      if (commonExercise == null) {
        //logger.info("getExercise can't find exercise " + id);
        if (id != userExerciseDAO.getUnknownExerciseID()) {
          commonExercise = idToContextExercise.get(id);
          if (commonExercise == null) {
            logger.warn(this + " getExercise : couldn't find exercise #" + id +
                " in " + idToExercise.size() + " exercises and " + idToContextExercise.size() + " context exercises");
          } else {
            //  logger.info("getExercise found context " + commonExercise.getID());
          }
        }
      } else {
        if (id != commonExercise.getID()) {
          logger.error("getExercise " + id + " != " + commonExercise);
        }
      }
      return commonExercise;
    }
  }

  private final Map<Integer, CommonExercise> idToContextExercise = new HashMap<>();

  /**
   * Make sure the parent is set on the context exercises.
   */
  protected void populateIdToExercise() {
    super.populateIdToExercise();

    getRawExercises()
        .forEach(parent -> parent.getDirectlyRelated()
            .forEach(commonExercise -> {
              idToContextExercise.put(commonExercise.getID(), commonExercise);
              commonExercise.getMutable().setParentExerciseID(parent.getID());
            }));
  }

  /**
   * TODO : remove duplicate
   *
   * @return
   * @see ExcelImport#readFromSheet
   */
  @Override
  public List<String> getTypeOrder() {
    List<String> typeOrder = getSectionHelper().getTypeOrder();
    if (typeOrder.isEmpty()) {
      String first = project.first();
      String second = project.second();
      typeOrder = new ArrayList<>();
      if (first != null && !first.isEmpty()) typeOrder.add(first);
      if (second != null && !second.isEmpty()) typeOrder.add(second);
    }
    return typeOrder;
  }

  /**
   * I don't think we're doing user exercise mask-out overrides anymore...
   *
   * @param removes
   */
  protected void addOverlays(Collection<Integer> removes) {
  }

  /**
   * Does join with related exercise table - maybe better way to do this in scala side?
   *
   * @return
   * @see #getRawExercises
   */
  @Override
  List<CommonExercise> readExercises() {
    try {
      List<String> typeOrder = getTypeOrderFromProject();
      setRootTypes(typeOrder);

      int projid = project.id();

      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject =
          userExerciseDAO.getRefResultDAO().getExerciseToPhoneForProject(projid);

/*
      exerciseToPhoneForProject.forEach((k, v) -> {
        if (v.getNumPhones() < 1) {
          logger.warn("1 ex #" + k + " has no phones?");
        }
      });
      */

      Map<Integer, ExerciseAttribute> allByProject = userExerciseDAO.getExerciseAttribute().getIDToPair(projid);
      logger.info("readExercises" +
          "\n\tread           " + exerciseToPhoneForProject.size() + " ExercisePhoneInfo" +
          "\n\ttype order     " + typeOrder +
          "\n\tnum attributes " + allByProject.size()
      );
      //logger.info("readExercises found " + allByProject.size() + " attributes");

      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs =
          userExerciseDAO.getExerciseAttributeJoin().getAllJoinByProject(projid);

      // do we add attributes to context exercises?
      List<CommonExercise> allNonContextExercises =
          userExerciseDAO.getByProject(
              typeOrder,
              getSectionHelper(),
              exerciseToPhoneForProject,
              fullProject,

              allByProject,
              exToAttrs
          );

      logger.info("readExercises" +
          "\n\tfor        " + projid +
          "\n\tproject    " + project.name() +
          "\n\ttype order " + typeOrder +
          "\n\tread       " + exerciseToPhoneForProject.size() + " ExercisePhoneInfo" +
          "\n\tgot        " + allNonContextExercises.size() + " predef exercises");


//      logger.info(prefix + " readExercises got " + related.size() + " related exercises;");
      Map<Integer, CommonExercise> idToContext =
          getIDToExercise(userExerciseDAO.getContextByProject(
              typeOrder,
              getSectionHelper(),
              exerciseToPhoneForProject, fullProject, allByProject, exToAttrs
          ));
//      logger.info("readExercises project " + project + " idToContext " + idToContext.size());

      attachContextExercises(allNonContextExercises, userExerciseDAO.getRelatedExercise().getAllRelated(projid), idToContext);

      return allNonContextExercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * Don't attach context sentences for relations in dialogs.
   *
   * @param allNonContextExercises
   * @param related
   * @param idToContext
   * @see #readExercises
   */
  private void attachContextExercises(List<CommonExercise> allNonContextExercises,
                                      Collection<SlickRelatedExercise> related,
                                      Map<Integer, CommonExercise> idToContext) {
    //  int attached = 0;
    int c = 0;
    String prefix = "Project " + project.name();

    Map<Integer, CommonExercise> idToEx = getIDToExercise(allNonContextExercises);

/*
    logger.info("attach context " + allNonContextExercises.size());
    logger.info("related        " + related.size());
    logger.info("idToContext    " + idToContext.size());
    logger.info("idToEx         " + idToEx.size());// + " : " + idToEx.keySet());
*/

    for (SlickRelatedExercise relatedExercise : related) {
      CommonExercise root = idToEx.get(relatedExercise.exid());
      if (root != null) {
        CommonExercise context = idToContext.get(relatedExercise.contextexid());
        if (context != null && relatedExercise.dialogid() < 2) {
          root.getMutable().addContextExercise(context);
          //      attached++;
        } else if (c++ < 2) {
          logger.warn("1 " + prefix + " didn't attach " + relatedExercise + "" + " for\n" + root);
        }
      } else if (c++ < SPEW_THRESH || c % 100 == 0) {
        CommonExercise next = allNonContextExercises.isEmpty() ? null : allNonContextExercises.iterator().next();
        logger.warn("attachContextExercises " + prefix + " exid " + relatedExercise.exid() + " context id " + relatedExercise.contextexid() +
            // " didn't attach " + relatedExercise + "" +
            " for, e.g. " + next);
      }
    }
//    logger.info(prefix + " Read " + allNonContextExercises.size() + " exercises from database, attached " + attached);
  }

  /**
   * First basic types, then attribute types...
   * Might want to allow this to be configurable.
   *
   * Added special code for putting semester at the top.
   *
   * @return
   */
  @NotNull
  private List<String> getTypeOrderFromProject() {
    List<String> typeOrder = getBaseTypeOrder();
    Collection<String> attributeTypes = getAttributeTypes();

    if (attributeTypes.contains(SEMESTER.toString())) {
      //  logger.info("found semester ");
      List<String> copy = new ArrayList<>();
      copy.add(SEMESTER.toString());
      copy.addAll(typeOrder);
      typeOrder = copy;
      attributeTypes.remove(SEMESTER.toString());
    }

    typeOrder.addAll(attributeTypes);
    getSectionHelper().reorderTypes(typeOrder);
    return typeOrder;
  }

  @NotNull
  private List<String> getBaseTypeOrder() {
    List<String> typeOrder = new ArrayList<>();
    typeOrder.add(project.first());
    if (!project.second().isEmpty()) {
      typeOrder.add(project.second());
    }

    return typeOrder;
  }

  private void setRootTypes(List<String> typeOrder) {
    Collection<String> attributeTypes = getAttributeTypes();
    if (DEBUG) logger.info("setRootTypes attributeTypes " + attributeTypes);

    Set<String> allTypesExceptSubTopic = removeSubtopic(attributeTypes);

    String firstProjectType = project.first();
    Set<String> rootTypes = new HashSet<>(Collections.singletonList(firstProjectType));
    rootTypes.addAll(allTypesExceptSubTopic);
    if (DEBUG) logger.info("setRootTypes roots " + rootTypes);

    ISection<CommonExercise> sectionHelper = getSectionHelper();
    sectionHelper.setRootTypes(rootTypes);

    Map<String, String> parentToChild = new HashMap<>();
    String second = project.second();
    if (second != null && !second.isEmpty()) {
      parentToChild.put(firstProjectType, second);
    }

    String topic = Facet.TOPIC.toString();
    if (rootTypes.contains(topic)) {
      setParentChild(attributeTypes, parentToChild, topic);
    } else {
      String lowerTopic = topic.toLowerCase();
      if (rootTypes.contains(lowerTopic)) {
        setParentChild(attributeTypes, parentToChild, lowerTopic);
      }
    }

    sectionHelper.setParentToChildTypes(parentToChild);
    if (DEBUG) {
      logger.info("setRootTypes roots " + rootTypes);

    }
    sectionHelper.setPredefinedTypeOrder(typeOrder);
    if (DEBUG) logger.info("parentToChild " + parentToChild);
  }

  private void setParentChild(Collection<String> rootTypes, Map<String, String> parentToChild, String lowerTopic) {
    String subtopic = Facet.SUB_TOPIC.toString();
    if (rootTypes.contains(SUBTOPIC_LC)) {
      parentToChild.put(lowerTopic, SUBTOPIC_LC);
    } else if (rootTypes.contains(subtopic)) {
      parentToChild.put(lowerTopic, subtopic);
    }
  }

  /**
   * @param attributeTypes
   * @return
   * @see #setRootTypes
   */
  private Set<String> removeSubtopic(Collection<String> attributeTypes) {
    return attributeTypes
        .stream()
        .filter(p -> !SectionHelper.SUBTOPICS.contains(p))
        .collect(Collectors.toSet());
  }

  private Collection<String> getAttributeTypes() {
    return userExerciseDAO.getExerciseAttribute().getAttributeTypes(project.id());
  }

  private Map<Integer, CommonExercise> getIDToExercise(Collection<CommonExercise> allExercises) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    allExercises.forEach(commonExercise -> idToEx.put(commonExercise.getID(), commonExercise));
    return idToEx;
  }

  /**
   * @param safe
   * @param unsafe
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSAndCountPhones
   */
  public void markSafeUnsafe(Set<Integer> safe, Set<Integer> unsafe) {
    getDao().updateCheckedBulk(safe, true);
    getDao().updateCheckedBulk(unsafe, false);
  }

  /**
   * @param id
   * @param count
   * @see SlickUserExerciseDAO#getExercisePhoneInfo
   * @deprecated
   */
  public void updatePhones(int id, int count) {
    getDao().updatePhones(id, count);
  }

  @Override
  public void updatePhonesBulk(List<SlickExercisePhone> pairs) {
    getDao().updatePhonesBulk(pairs);
  }

  /**
   * @param pairs
   * @return
   * @see SlickUserExerciseDAO#updateDominoBulk
   */
  public int updateDominoBulk(List<SlickUpdateDominoPair> pairs) {
    return getDao().updateDominoBulk(pairs).toSeq().size();
  }

  @Override
  public int getExIDForDominoID(int projID, int dominoID) {
    SlickExercise byDominoID = userExerciseDAO.getByDominoID(projID, dominoID);
    return byDominoID == null ? -1 : byDominoID.id();
  }

  @Override
  public int getParentFor(int exid) {
    return userExerciseDAO.getRelatedExercise().getParentForContextID(exid);
  }

  private ExerciseDAOWrapper getDao() {
    return userExerciseDAO.getDao();
  }

  public String toString() {
    return "DBExerciseDAO for " + project;
  }
}
