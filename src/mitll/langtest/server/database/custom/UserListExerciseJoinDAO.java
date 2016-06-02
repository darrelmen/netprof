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

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListExerciseJoinDAO extends DAO {
  private static final Logger logger = Logger.getLogger(UserListExerciseJoinDAO.class);

  private static final String USERLISTID = "userlistid";
  private static final String EXERCISEID = "exerciseid";
  private static final String UNIQUEID = "uniqueid";

  public static final String USER_EXERCISE_LIST_EXERCISE = "userexerciselist_exercise";

  /**
   * @param database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public UserListExerciseJoinDAO(Database database) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  private void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
        USER_EXERCISE_LIST_EXERCISE +
        " (" +
        UNIQUEID +
        " IDENTITY, " +
        USERLISTID +
        " LONG, " +
        EXERCISEID +
        " VARCHAR , " +
        "FOREIGN KEY(" +
        USERLISTID +
        ") REFERENCES " +
        UserListDAO.USER_EXERCISE_LIST +
        "(" +
        UNIQUEID +
        ")" +    // user lists can be combinations of predefined and user defined exercises
 /*       "," +
      "FOREIGN KEY(exerciseid) REFERENCES " +
      UserExerciseDAO.USEREXERCISE +
      "(uniqueid)" +*/
        ")");
    finish(database, connection, statement);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#addItemToList
   */
  public void add(UserList userList, String uniqueID) {
    try {
      // there are much better ways of doing this...
      logger.info("UserListExerciseJoinDAO.add :userList #" + userList.getUniqueID() + " exercise id '" + uniqueID +"'");

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + USER_EXERCISE_LIST_EXERCISE +
              "(" +
              USERLISTID +
              "," +
              EXERCISEID +
              ") " +
              "VALUES(?,?);");
      int i = 1;
      statement.setLong(i++, userList.getUniqueID());
      statement.setString(i++, uniqueID);

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");

      finish(connection, statement);

      logger.debug("\tUserListExerciseJoinDAO.add : now " + getCount(USER_EXERCISE_LIST_EXERCISE) + " and user exercise is " + userList);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void removeListRefs(long listid) {
    remove(USER_EXERCISE_LIST_EXERCISE, USERLISTID, listid);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see UserListManager#deleteItemFromList(long, String, java.util.Collection)
   */
  public boolean remove(long listid, String exid) {
    String sql = getRemoveSQL(listid, exid);
    return doSqlOn(sql, USER_EXERCISE_LIST_EXERCISE, true);
  }

  private String getRemoveSQL(long listid, String exid) {
    return "DELETE FROM " + USER_EXERCISE_LIST_EXERCISE + " WHERE " +
        USERLISTID +
        "=" + listid +
        " AND " +
        EXERCISEID +
        "='" + exid +
        "'; ";
  }
}