package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Object representing a grader.
 *
 * Grader logs in, registering initially.
 *
 * On the left, questions sorted (not random), each shows how many have been graded (n graders, # complete) and
 * how many responses were collected for the item (which could be a multi part question).
 *
 * E.g. if 2 graders, each could be part way done with responses and so 0 complete.
 *
 * Need a Grade object which will be for each entry in the results table.
 *
 * Either 1-5 scale or that + a Correct? Yes/No scale.  Maybe this is an option in the nature of the exercise?
 *
 * Show question, with table of responses. Note which have been graded so far.
 *
 * UserManager: go22670
 * Date: 5/17/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class Grader implements IsSerializable {
  public long id;
  public String name;
  public String  password;
  public long timestamp;

  public Grader() {} // for serialization
  public Grader(long id, String name, String password, long timestamp) {
     this.id = id;
    this.name = name;
    this.password = password;
    this.timestamp = timestamp;
  }
}
