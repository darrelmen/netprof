package mitll.langtest.shared.exercise;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.custom.UserList;

import java.util.Map;

/**
 * Created by go22670 on 2/1/16.
 */
public interface MutableUserExercise {
  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#add(CommonExercise, boolean)
   * @see mitll.langtest.server.database.custom.UserListManager#duplicate(CommonExercise)
   * @param id
   */
  void setID(String id);

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#reallyChange(ListInterface, boolean)
   * @param id
   */
  void setCreator(int id);

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#add(CommonExercise, boolean)
   * @param uniqueID
   */
  void setUniqueID(long uniqueID);

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#add(CommonExercise, boolean)
   * @return
   */
  long getUniqueID();

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getCreateButton(UserList, ListInterface, Panel, ControlGroup)
   * @return
   */
  boolean checkPredef();

  void setUnitToValue(Map<String, String> unitToValue);

}
