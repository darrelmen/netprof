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
  public long userID;
  public String name;
  public long timestamp;

  public TeacherClass() {}

  public TeacherClass(User userRef, String name) {
    this(userRef.id, name, System.currentTimeMillis());
    this.userRef = userRef;
  }

  public TeacherClass(long userid, String name) {
    this(userid, name, System.currentTimeMillis());
  }

  public TeacherClass(long userid, String name, long timestamp) {
    this.userID = userid;
    this.name = name;
    this.timestamp = timestamp;
  }

  public TeacherClass(long timestamp) {
    this.timestamp = timestamp;
  }
}