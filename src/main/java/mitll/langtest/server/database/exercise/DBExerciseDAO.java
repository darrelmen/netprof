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
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.userexercise.SlickUserExerciseDAO.DIFFICULTY;
import static mitll.langtest.server.database.userexercise.SlickUserExerciseDAO.SOUND;

public class DBExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(DBExerciseDAO.class);
  private SlickUserExerciseDAO userExerciseDAO;
  private SlickProject project;
  private Project fullProject;

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
    super(serverProps, userListManager, addDefects, fullProject.getProject().language(), fullProject.getProject().id());
    this.userExerciseDAO = userExerciseDAO;
    this.project = fullProject.getProject();
    this.fullProject = fullProject;
  }

  /**
   * TODO : remove duplicate
   *
   * @return
   * @see SlickUserExerciseDAO#getTypeOrder
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

  public Map<Integer, String> getIDToFL(int projid) {
    return userExerciseDAO.getIDToFL(projid);
  }

  /**
   * Does join with related exercise table - maybe better way to do this in scala side?
   *
   * @return
   * @see #getRawExercises()
   */
  @Override
  List<CommonExercise> readExercises() {
    try {
      List<String> typeOrder = getTypeOrderFromProject();
      getSectionHelper().putSoundAtEnd(typeOrder);
      setRootTypes();

      int projid = project.id();

      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject =
          userExerciseDAO.getRefResultDAO().getExerciseToPhoneForProject(projid);
      //userExerciseDAO.useExToPhones(exerciseToPhoneForProject);

      logger.info("readExercises" +
          "\n\tread " + exerciseToPhoneForProject.size() + " ExercisePhoneInfo" +
          "\n\tfor " + projid +
          "\n\ttype order " + typeOrder);

      Map<Integer, ExerciseAttribute> allByProject = userExerciseDAO.getIDToPair(projid);

      //  logger.info("addExerciseAttributes found " + allByProject.size() + " attributes");
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs = userExerciseDAO.getAllJoinByProject(projid);

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

      logger.info("readExercises project " + project +
          " readExercises got " + allNonContextExercises.size() + " predef exercises");

//      logger.info(prefix + " readExercises got " + related.size() + " related exercises;");
      Map<Integer, CommonExercise> idToContext =
          getIDToExercise(userExerciseDAO.getContextByProject(
              typeOrder,
              getSectionHelper(),
              exerciseToPhoneForProject, fullProject, allByProject, exToAttrs
          ));
      logger.info("project " + project + " idToContext " + idToContext.size());

      attachContextExercises(allNonContextExercises, userExerciseDAO.getAllRelated(projid), idToContext);

      return allNonContextExercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * @param allNonContextExercises
   * @param related
   * @param idToContext
   * @see #readExercises
   */
  private void attachContextExercises(List<CommonExercise> allNonContextExercises,
                                      Collection<SlickRelatedExercise> related,
                                      Map<Integer, CommonExercise> idToContext) {
    int attached = 0;
    int c = 0;
    String prefix = "Project " + project.name();

    Map<Integer, CommonExercise> idToEx = getIDToExercise(allNonContextExercises);

/*    logger.info("attach context " + allNonContextExercises.size());
    logger.info("related        " + related.size());
    logger.info("idToContext    " + idToContext.size());
    logger.info("idToEx         " + idToEx.size());*/

    for (SlickRelatedExercise relatedExercise : related) {
      CommonExercise root = idToEx.get(relatedExercise.exid());
      if (root != null) {
        CommonExercise context = idToContext.get(relatedExercise.contextexid());
        if (context != null) {
          root.getMutable().addContextExercise(context);
          attached++;
        } else if (c++ < 2) {
          logger.warn("1 " + prefix + " didn't attach " + relatedExercise + "" + " for\n" + root);
        }
      } else if (c++ < 10) {
        logger.warn("2 " + prefix + " didn't attach " + relatedExercise + "" +
            " for, e.g. " + allNonContextExercises.iterator().next());
      }
    }
    logger.info(prefix + " Read " + allNonContextExercises.size() + " exercises from database, attached " + attached);
  }

  /**
   * First basic types, then attribute types...
   * Might want to allow this to be configurable.
   * @return
   */
  @NotNull
  private List<String> getTypeOrderFromProject() {
    List<String> typeOrder = getBaseTypeOrder();
    typeOrder.addAll(getAttributeTypes());
    return typeOrder;
  }

  @NotNull
  private List<String> getBaseTypeOrder() {
    List<String> typeOrder = new ArrayList<>();
    typeOrder.add(project.first());
    if (!project.second().isEmpty()) {
      typeOrder.add(project.second());
    }
/*
    typeOrder.add(SOUND);
    if (SlickUserExerciseDAO.ADD_PHONE_LENGTH) {
      typeOrder.add(SlickUserExerciseDAO.DIFFICULTY);
    }
    */
    return typeOrder;
  }

  private void setRootTypes() {
    Collection<String> attributeTypes = getAttributeTypes();
    Set<String> collect = attributeTypes.stream().filter(p -> !p.equals(SectionHelper.SUB_TOPIC)).collect(Collectors.toSet());

    Set<String> rootTypes = new HashSet<>(Arrays.asList(project.first()));
    rootTypes.addAll(collect);

    ISection<CommonExercise> sectionHelper = getSectionHelper();
    sectionHelper.setRootTypes(rootTypes);

    Map<String, String> parentToChild = new HashMap<>();
    if (project.second() != null && !project.second().isEmpty()) {
      parentToChild.put(project.first(), project.second());
    }

    if (rootTypes.contains(SectionHelper.TOPIC)) {
      parentToChild.put(SectionHelper.TOPIC, SectionHelper.SUB_TOPIC);
    }

    sectionHelper.setParentToChildTypes(parentToChild);

//    logger.info("roots " + rootTypes);
    //   logger.info("parentToChild " + parentToChild);
  }

  private Collection<String> getAttributeTypes() {
    return userExerciseDAO.getAttributeTypes(project.id());
  }

  private Map<Integer, CommonExercise> getIDToExercise(Collection<CommonExercise> allExercises) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise ex : allExercises) {
      idToEx.put(ex.getID(), ex);
    }
    return idToEx;
  }

  /**
   * @param safe
   * @param unsafe
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSAndCountPhones
   */
  public void markSafeUnsafe(Set<Integer> safe, Set<Integer> unsafe) {
    userExerciseDAO.getDao().updateCheckedBulk(safe, true);
    userExerciseDAO.getDao().updateCheckedBulk(unsafe, false);
  }

  public void updatePhones(int id, int count) {
    userExerciseDAO.getDao().updatePhones(id, count);
  }

  public String toString() {
    return "DBExerciseDAO for " + project;
  }
}
