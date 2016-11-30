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
  private static final boolean DEBUG = false;

  /**
   * What can happen:
   * <p>
   * 1) User id and password match - no action -they're already there
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
   * ??? Also, going forward, we must store emails, since we need to be able to send the sign up message?
   *
   * @param db
   * @param projid
   * @see CopyToPostgres#copyOneConfig
   */
  Map<Integer, Integer> copyUsers(DatabaseImpl db, int projid, IResultDAO oldResultDAO) {
//    SlickUserDAOImpl dominoUserDAO = (SlickUserDAOImpl) db.getUserDAO();
    DominoUserDAOImpl dominoUserDAO = (DominoUserDAOImpl) db.getUserDAO();

    Map<Integer, Integer> oldToNew = new HashMap<>();
    addDefaultUsers(oldToNew, dominoUserDAO);

    IUserProjectDAO slickUserProjectDAO = db.getUserProjectDAO();

    UserDAO userDAO = new UserDAO(db);
    int defectDetector = userDAO.getDefectDetector();
    oldToNew.put(defectDetector, dominoUserDAO.getDefectDetector());

    List<User> importUsers = userDAO.getUsers();
    logger.info("copyUsers h2 importUsers  " + importUsers.size());
    UserToCount userToNumAnswers = oldResultDAO.getUserToNumAnswers();
    Map<Integer, Integer> idToCount = userToNumAnswers.getIdToCount();
    if (DEBUG) logger.info("id->count " + idToCount.size() + " values " + idToCount.values().size());

    int collisions = 0;
    int lurker = 0;
    List<ClientUserDetail> added = new ArrayList<>();
    for (User toImport : importUsers) {
      int importID = toImport.getID();
      if (importID != defectDetector) {
        String importUserID = toImport.getUserID();
        String passwordHash = toImport.getPasswordHash();
        if (passwordHash == null) passwordHash = "";

        if (DEBUG) logger.info("import " + toImport);
        User strictUserWithPass = dominoUserDAO.getStrictUserWithPass(importUserID, passwordHash);

        if (strictUserWithPass != null) {
          // do nothing, but remember id mapping
          oldToNew.put(importID, strictUserWithPass.getID());
        } else {

          if (importUserID.isEmpty() && idToCount.get(importID) != null && idToCount.get(importID) == 0) {
            logger.info("skipping old user " + toImport + " since they have an empty user name and no recordings");
            lurker++;
          } else {
            User userByID1 = dominoUserDAO.getUserByID(importUserID);

            if (userByID1 != null) {
              if (DEBUG) logger.info("found existing user " + importUserID + " : " + userByID1);
              // User "adam" already exists with a different password - what to do?
              // void current password! Force them to set it again when they log in again
              int existingID = userByID1.getID();
              if (!userByID1.getPasswordHash().isEmpty()) {
                dominoUserDAO.changePassword(existingID, "");
              }
              oldToNew.put(importID, existingID);
              collisions++;
              logger.info("user collision to project " + projid + " map " + importID + "->" + existingID +
                  " : " + userByID1);
            } else {
              logger.info("no existing user id '" + importUserID + "'");
              added.add(addUser(dominoUserDAO, oldToNew, toImport));
            }
          }
        }
      }
    }

    addUserProjectBinding(projid, slickUserProjectDAO, added);
    logger.info("after, postgres importUsers " +
        "num = " + dominoUserDAO.getUsers().size() +
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
   * @return
   */
  private ClientUserDetail addUser(DominoUserDAOImpl dominoUserDAO,
                                   Map<Integer, Integer> oldToNew,
                                   User toImport) {
//    logger.info("addUser " + toImport + " with " + toImport.getPermissions());
    ClientUserDetail user = dominoUserDAO.toClientUserDetail(toImport, false);
    ClientUserDetail addedUser = dominoUserDAO.addAndGet(user, toImport.getPasswordHash(), toImport.getPermissions());
    if (addedUser == null) {
      logger.error("no error returned from domino.");
    }

    int add = addedUser.getDocumentDBID();
//    logger.info("addUser id  " + add + " for " + user.id() + " equal " + (user == addedUser));
    //   logger.info("addUser map " + toImport.getID() + " -> " + add);
    oldToNew.put(toImport.getID(), add);
    return addedUser;
  }


  private void addDefaultUsers(Map<Integer, Integer> oldToNew, DominoUserDAOImpl dominoUserDAO) {
    oldToNew.put(BaseUserDAO.DEFAULT_USER_ID, dominoUserDAO.getDefaultUser());
    oldToNew.put(BaseUserDAO.DEFAULT_MALE_ID, dominoUserDAO.getDefaultMale());
    oldToNew.put(BaseUserDAO.DEFAULT_FEMALE_ID, dominoUserDAO.getDefaultFemale());
  }

  private void addUserProjectBinding(int projid, IUserProjectDAO slickUserProjectDAO, List<ClientUserDetail> added) {
    logger.info("adding user->project for " + projid);
    List<SlickUserProject> toAdd = new ArrayList<>();
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    for (ClientUserDetail user : added) {
      toAdd.add(new SlickUserProject(-1, user.getDocumentDBID(), projid, modified));
    }
    slickUserProjectDAO.addBulk(toAdd);
  }

}
