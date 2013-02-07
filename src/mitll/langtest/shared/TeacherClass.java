package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TeacherClass implements IsSerializable {
  public List<Lesson> lessonList = new ArrayList<Lesson>();
  public User userRef;
  public String name;
  public long timestamp;

  public TeacherClass() {}
  public TeacherClass(User userRef, String name) { this.userRef = userRef; this.name = name;}
}
