package mitll.langtest.server.database.copy;

import mitll.hlt.domino.shared.common.SResult;
import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.UserToCount;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.user.IUserProjectDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickUserProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mitll.langtest.server.database.user.DominoUserDAOImpl.NETPROF;

/**
 * Created by go22670 on 10/26/16.
 */
public class UserCopy {
  private static final Logger logger = LogManager.getLogger(UserCopy.class);

  private static final boolean DEBUG = false;
  private static final boolean MAKE_COLLISION_ACCOUNT = false;
  private static final boolean WARN_ON_COLLISION = true;
  private static final String D_ADMIN = "d.admin";

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
   * @param projid       to import into
   * @param oldResultDAO - so we can check if the user ever recorded anything - if not we skip them on import
   * @param optName      if you want to name the project something other than the language
   * @see CopyToPostgres#copyOneConfig
   */
  Map<Integer, Integer> copyUsers(DatabaseImpl db, int projid, IResultDAO oldResultDAO, String optName) throws Exception {
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
    if (DEBUG) logger.info("copyUsers id->childCount " + idToCount.size() + " values " + idToCount.values().size());

    //boolean debug = false;
    int collisions = 0;
    int lurker = 0;
    List<ClientUserDetail> added = new ArrayList<>();
    int c = 0;
    Map<Integer, Long> userToCreation = new HashMap<>();

    for (User toImport : importUsers) {
      c++;

      int importID = toImport.getID();
      String importUserID = toImport.getUserID();

      if (hasSpaces(importUserID)) {
//      logger.info("replacing spaces in " +importUserID);
        importUserID = importUserID.trim().replaceAll("\\s++", "_");
        logger.info("copyUsers now no spaces in " + importUserID);
        toImport.setUserID(importUserID);
      }

      if (DEBUG) logger.info("copying " + importID + " : " + importUserID);

      if (importUserID.isEmpty()) importUserID = "unknown";
      if (importID != defectDetector && !dominoUserDAO.isDefaultUser(importUserID)) {

        if (importUserID.isEmpty() && idToCount.get(importID) != null && idToCount.get(importID) == 0) {
          logger.info("copyUsers skipping old user " + toImport + " since they have an empty user name and no recordings");
          lurker++;
        } else {
          if (DEBUG) logger.info("copyUsers #" + c + "/" + importUsers.size() + " : import " + toImport);
          mitll.hlt.domino.shared.model.user.DBUser dominoDBUser = dominoUserDAO.getDBUser(importUserID);

          if (dominoDBUser == null) { // new user
            logger.info("copyUsers no existing user id '" + importUserID + "'");
            ClientUserDetail newUser = addUser(dominoUserDAO, oldToNew, toImport, optName);
            userToCreation.put(newUser.getDocumentDBID(), newUser.getAcctDetail().getCrTime().getTime());
            added.add(newUser);
          } else { // user exists
            if (dominoDBUser.getUserId().equals(D_ADMIN)) {
              logger.warn("copyUsers found d.admin " + dominoDBUser);
            } else {
              long creationTime = dominoDBUser.getAcctDetail().getCrTime().getTime();
              userToCreation.put(dominoDBUser.getDocumentDBID(), creationTime);
            }

            if (foundExistingUser(projid, optName,
                dominoUserDAO, oldToNew,
                added,
                toImport,
                dominoDBUser)) {
              collisions++;
            }
          }
        }
      }
    }

    addUserProjectBinding(projid, slickUserProjectDAO, userToCreation);
    logger.info("copyUsers after, postgres importUsers " +
        //"num = " + dominoUserDAO.getUsers().size() +
        " added " + added.size() +
        " collisions " + collisions +
        " lurker " + lurker
    );
    return oldToNew;
  }

  private boolean hasSpaces(String s) {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(s);
    return matcher.find();
  }

  /**
   * Checks the password for the import user to see if it's the same as the current one in mongo.
   * <p>
   * Two people with the same user id but different passwords are in a race - first guy gets the password,
   * second guy needs to get a new account.
   *
   * @param projid        only for debugging
   * @param optName       of the project, if not the language
   * @param dominoUserDAO to use to add to or query for existing users
   * @param oldToNew      remember the mapping of old h2 userid to new mongo/domino user id
   * @param added         only used if we make collision accounts
   * @param toImport      old user from h2
   * @param dominoUser    user found in mongo with same user id
   * @return true if a collision - same user id but different password
   * @throws Exception
   */
  private boolean foundExistingUser(int projid,
                                    String optName,
                                    DominoUserDAOImpl dominoUserDAO,
                                    Map<Integer, Integer> oldToNew,
                                    List<ClientUserDetail> added,
                                    User toImport,
                                    mitll.hlt.domino.shared.model.user.DBUser dominoUser
  ) throws Exception {
    String passwordHash = toImport.getPasswordHash();
    if (passwordHash == null) passwordHash = "";
    int dominoDBID = dominoUser.getDocumentDBID();

    int importID = toImport.getID();
    String importUserID = toImport.getUserID();
    boolean didPasswordMatch = dominoUserDAO.isMatchingPassword(importUserID, passwordHash);
    if (didPasswordMatch) { // existing user with same password
      // do nothing, but remember id mapping
      logger.info("copyUsers found existing user '" + importUserID + "' :" +
          "\n\tdomino " + dominoUser + " password matches.");

      checkMatchingGender(dominoUserDAO, toImport, dominoUser);

      oldToNew.put(importID, dominoDBID);
      return false;
    } else {
      if (DEBUG) {
        logger.info("copyUsers found existing user '" + importUserID + "' :" +
            "\n\tdomino " + dominoUser);
      }

      // User "adam" already exists with a different password - what to do?
      if (MAKE_COLLISION_ACCOUNT) {
        // give the person a new id in the name space of the language
        makeCollisionAccount(optName, dominoUserDAO, oldToNew, added, toImport, importUserID);
      } else {
        // second person is out of luck - they need to make a new account
        if (WARN_ON_COLLISION) {
          logger.info("COLLISION : copyUsers found existing user with password difference " + importUserID +
              " : " + dominoUser + "\n");
        }

        checkUserApplications(dominoUserDAO, dominoUser);
        oldToNew.put(importID, dominoDBID);

        checkMatchingGender(dominoUserDAO, toImport, dominoUser);
      }

      logger.info("copyUsers user collision to project " + projid + " map " + importID + "->" + dominoDBID +
          " : " + dominoUser);
      return true;
    }
  }

  private void checkMatchingGender(DominoUserDAOImpl dominoUserDAO, User toImport, DBUser dominoUser) {
    MiniUser.Gender realGender = toImport.getRealGender();
    if (dominoUser.getGender() == mitll.hlt.domino.shared.model.user.User.Gender.Unspecified &&
        realGender != MiniUser.Gender.Unspecified) {
      dominoUser.setGender(realGender == MiniUser.Gender.Male ? mitll.hlt.domino.shared.model.user.User.Gender.Male :
          realGender == MiniUser.Gender.Female ? mitll.hlt.domino.shared.model.user.User.Gender.Female : mitll.hlt.domino.shared.model.user.User.Gender.Unspecified);
      dominoUserDAO.updateUser(dominoUser);
      logger.info("checkMatchingGender : update gender for " + dominoUser);
    } else {
/*      logger.info("checkMatchingGender : no change - gender for " + dominoUser.getUserId() +
          " is " + dominoUser.getGender() + " vs " + realGender);*/
    }
  }

  private void checkUserApplications(DominoUserDAOImpl dominoUserDAO, DBUser dominoUser) {//}, int dominoDBID) {
    Set<String> applicationAbbreviations = dominoUser.getApplicationAbbreviations();
    if (applicationAbbreviations.contains(NETPROF)) {
      // logger.info("foundExistingUser : found existing application entry for user #" + dominoDBID);
    } else {
      applicationAbbreviations.add(NETPROF);
//      logger.info("before " + dominoUser.getApplicationAbbreviationsString());
//      logger.info("before " + dominoUser.getApplicationAbbreviations());

      SResult<ClientUserDetail> clientUserDetailSResult = dominoUserDAO.updateUser(dominoUser);
/*
      logger.info("Got back " + clientUserDetailSResult);
      logger.info("Got back " + clientUserDetailSResult.get().getApplicationAbbreviations());
      logger.info("Got back " + clientUserDetailSResult.get().getApplicationAbbreviationsString());
      logger.info("foundExistingUser : updating application entry for user #" + dominoDBID);
      */
    }
  }

  /**
   * For the moment this is never done.
   *
   * @param optName
   * @param dominoUserDAO
   * @param oldToNew
   * @param added
   * @param toImport
   * @param importUserID
   * @throws Exception
   */
  private void makeCollisionAccount(String optName, DominoUserDAOImpl dominoUserDAO,
                                    Map<Integer, Integer> oldToNew,
                                    List<ClientUserDetail> added,
                                    User toImport, String importUserID) throws Exception {
    String compoundID = importUserID + "#" + optName;
    User userByCompound = dominoUserDAO.getUserByID(compoundID);

    if (userByCompound != null) {
      logger.warn("copyUsers already added " + compoundID + " : " +
          userByCompound +
          " so moving on...");
    } else {
      logger.warn("copyUsers no user for '" + compoundID + "'");

      toImport.setUserID(compoundID);
      added.add(addUser(dominoUserDAO, oldToNew, toImport, optName));
    }
//              String passwordHash1 = userByID1.getPasswordHash();
//              if (!passwordHash1.isEmpty()) {
//                logger.info("Found existing user " + existingID + " : " + userByID1.getUserID() + " with password hash " + passwordHash1);
//                //dominoUserDAO.changePassword(existingID, "");
//                dominoUserDAO.forgetPassword(existingID);
//              }
//
//              oldToNew.put(importID, existingID);
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
    logger.info("addUser " + toImport + "\n\twith permissions " + toImport.getPermissions());
    //logger.info("addUser " + toImport.getID()+ " gender " + toImport.getGender() + " " + toImport.getRealGender());
    ClientUserDetail toAdd = dominoUserDAO.toClientUserDetail(toImport, projectName);
    ClientUserDetail addedUser = dominoUserDAO.addAndGet(
        toAdd,
        toImport.getPasswordHash()
    );
    if (toAdd.getGender() != addedUser.getGender()) {
      logger.error("for " + addedUser +
          " lost gender after adding " + addedUser.getGender());
    }
    rememberUser(oldToNew, toImport.getID(), addedUser);
    return addedUser;
  }

  private void rememberUser(Map<Integer, Integer> oldToNew, int id, ClientUserDetail addedUser) throws Exception {
    if (addedUser == null) {
      logger.error("addUser no error returned from domino.");
      throw new Exception("rememberUser couldn't import " + id);
    } else {
      oldToNew.put(id, addedUser.getDocumentDBID());
    }
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
   * @see #copyUsers
   */
  private void addUserProjectBinding(int projid, IUserProjectDAO slickUserProjectDAO, Map<Integer, Long> added) {
    logger.info("addUserProjectBinding adding user->project for " + projid);
    // Timestamp modified = new Timestamp(System.currentTimeMillis());

    List<SlickUserProject> toAdd = new ArrayList<>();
    added.forEach((userID, modified) -> toAdd.add(new SlickUserProject(-1, userID, projid, new Timestamp(modified))));
    slickUserProjectDAO.addBulk(toAdd);
  }
}
