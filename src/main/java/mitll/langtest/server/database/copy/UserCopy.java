package mitll.langtest.server.database.copy;

import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.UserToCount;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.user.IUserProjectDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickUserProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/26/16.
 */
public class UserCopy {
  private static final Logger logger = LogManager.getLogger(UserCopy.class);

  private static final boolean DEBUG = true;
  private static final boolean MAKE_COLLISION_ACCOUNT = false;
  private static final boolean WARN_ON_COLLISION = true;

  /**
   * What can happen:
   * <p>
   * 1) User id and password match - no action - they're already there
   * 2) User id matches, but password doesn't - go ahead and add the user id and password
   * -- could be multiple users with same userid but different password... trouble later on?
   * 3) User id is new - add a new user
   * <p>
   * Worry about how to merge two or more user tables -
   * find existing ids in postgres...
   * if some from import db aren't there, add them
   * return map of old ids -> new ids
   * <p>
   * For collisions -- must reset password -- someone loses.
   * <p>
   * Also, going forward, we must store emails, since we need to be able to send the sign up message?
   *
   * @param db
   * @param projid
   * @see CopyToPostgres#copyOneConfig
   */
  Map<Integer, Integer> copyUsers(DatabaseImpl db, int projid, IResultDAO oldResultDAO, String optName) throws Exception {
//    SlickUserDAOImpl dominoUserDAO = (SlickUserDAOImpl) db.getUserDAO();
    DominoUserDAOImpl dominoUserDAO = (DominoUserDAOImpl) db.getUserDAO();

    Map<Integer, Integer> oldToNew = new HashMap<>();
    addDefaultUsers(oldToNew, /*(BaseUserDAO)*/ dominoUserDAO);

    IUserProjectDAO slickUserProjectDAO = db.getUserProjectDAO();

    UserDAO userDAO = new UserDAO(db);
    int defectDetector = userDAO.getDefectDetector();
    oldToNew.put(defectDetector, dominoUserDAO.getDefectDetector());

    List<User> importUsers = userDAO.getUsers();
    logger.info("copyUsers h2 importUsers  " + importUsers.size());
    UserToCount userToNumAnswers = oldResultDAO.getUserToNumAnswers();
    Map<Integer, Integer> idToCount = userToNumAnswers.getIdToCount();
    if (DEBUG) logger.info("copyUsers id->count " + idToCount.size() + " values " + idToCount.values().size());

    int collisions = 0;
    int lurker = 0;
    List<ClientUserDetail> added = new ArrayList<>();
    int c= 0;
    for (User toImport : importUsers) {
        c++;
          int importID = toImport.getID();
      String importUserID = toImport.getUserID();
      if (importID != defectDetector && !dominoUserDAO.isDefaultUser(importUserID)) {

        if (importUserID.isEmpty() && idToCount.get(importID) != null && idToCount.get(importID) == 0) {
          logger.info("copyUsers skipping old user " + toImport + " since they have an empty user name and no recordings");
          lurker++;
        } else {
          if (DEBUG) logger.info("copyUsers #" + (c++) + "/" + importUsers.size()+ " : import " + toImport);

          User userByID1 = dominoUserDAO.getUserByID(importUserID);
          if (userByID1 != null) { // user exists
            String passwordHash = toImport.getPasswordHash();
            if (passwordHash == null) passwordHash = "";

            User strictUserWithPass = dominoUserDAO.getUserIfMatchPass(userByID1, importUserID, passwordHash);
            if (strictUserWithPass != null) { // existing user with same password
              // do nothing, but remember id mapping
              oldToNew.put(importID, strictUserWithPass.getID());
            } else {
              if (DEBUG) logger.info("copyUsers found existing user " + importUserID + " : " + userByID1);

              // User "adam" already exists with a different password - what to do?
              // give the person a new id in the name space of the language

              if (MAKE_COLLISION_ACCOUNT) {
                String compoundID = importUserID + "#" + optName;
                User userByCompound = dominoUserDAO.getUserByID(compoundID);

                if (userByCompound != null) {
                  logger.warn("copyUsers already added " + compoundID + " : " +
                      userByCompound +
                      " so moving on...");
                } else {
                  logger.warn("copyUsers no user for '" + compoundID + "'");

                  toImport.setUserID(compoundID);
                  ClientUserDetail e = addUser(dominoUserDAO, oldToNew, toImport, optName);
                  added.add(e);
                }
//              String passwordHash1 = userByID1.getPasswordHash();
//              if (!passwordHash1.isEmpty()) {
//                logger.info("Found existing user " + existingID + " : " + userByID1.getUserID() + " with password hash " + passwordHash1);
//                //dominoUserDAO.changePassword(existingID, "");
//                dominoUserDAO.forgetPassword(existingID);
//              }
//
//              oldToNew.put(importID, existingID);
              } else {
                if (WARN_ON_COLLISION) {
                  logger.info("\n\n\ncopyUsers found existing user with password difference " + importUserID + " : " + userByID1 + "\n\n\n");
                }

                oldToNew.put(importID, userByID1.getID());
              }
              collisions++;
              logger.info("copyUsers user collision to project " + projid + " map " + importID + "->" + userByID1.getID() +
                  " : " + userByID1);
            }
          } else {
            logger.info("copyUsers no existing user id '" + importUserID + "'");
            added.add(addUser(dominoUserDAO, oldToNew, toImport, optName));
          }
        }
      }
    }

    addUserProjectBinding(projid, slickUserProjectDAO, added);
    logger.info("copyUsers after, postgres importUsers " +
        //"num = " + dominoUserDAO.getUsers().size() +
        " added " + added.size() +
        " collisions " + collisions +
        " lurker " + lurker
    );
    return oldToNew;
  }

  /**
   * @param dominoUserDAO
   * @param oldToNew
   * @param toImport
   * @param projectName
   * @return
   * @see #copyUsers
   */
  private ClientUserDetail addUser(DominoUserDAOImpl dominoUserDAO,
                                   Map<Integer, Integer> oldToNew,
                                   User toImport,
                                   String projectName) throws Exception {
    logger.info("addUser " + toImport + " with " + toImport.getPermissions());
    ClientUserDetail user = dominoUserDAO.toClientUserDetail(toImport, projectName);
    ClientUserDetail addedUser = dominoUserDAO.addAndGet(
        user,
        toImport.getPasswordHash()
    );
    if (addedUser == null) {
      logger.error("addUser no error returned from domino.");
      throw new Exception("couldn't import " + toImport);
    } else {
      oldToNew.put(toImport.getID(), addedUser.getDocumentDBID());
    }
    return addedUser;
  }

  private void addDefaultUsers(Map<Integer, Integer> oldToNew, BaseUserDAO dominoUserDAO) {
    oldToNew.put(BaseUserDAO.DEFAULT_USER_ID, dominoUserDAO.getDefaultUser());
    oldToNew.put(BaseUserDAO.DEFAULT_MALE_ID, dominoUserDAO.getDefaultMale());
    oldToNew.put(BaseUserDAO.DEFAULT_FEMALE_ID, dominoUserDAO.getDefaultFemale());
  }

  /**
   * @param projid
   * @param slickUserProjectDAO
   * @param added
   * @see #copyUsers(DatabaseImpl, int, IResultDAO)
   */
  private void addUserProjectBinding(int projid, IUserProjectDAO slickUserProjectDAO, List<ClientUserDetail> added) {
    logger.info("addUserProjectBinding adding user->project for " + projid);
    List<SlickUserProject> toAdd = new ArrayList<>();
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    for (ClientUserDetail user : added) {
      if (user == null) {
        logger.warn("skipping invalid user...");
      } else {
        toAdd.add(new SlickUserProject(-1, user.getDocumentDBID(), projid, modified));
      }
    }
    slickUserProjectDAO.addBulk(toAdd);
  }
}
