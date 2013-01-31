package mitll.langtest.server.database;

import mitll.langtest.shared.TeacherClass;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TeacherClassDAO extends DAO {
  public TeacherClassDAO(Database db) { super(db); }
  public void addClass(String name) {}
  public List<TeacherClass> getTeacherClasses(User user) {
     return new ArrayList<TeacherClass>();
  }
}
