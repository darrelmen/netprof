package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise implements IsSerializable  {
  private String content;
  private String id;
  private Map<String,QAPair> langToQuestion = new HashMap<String, QAPair>();
  public static class QAPair implements IsSerializable {
    private String question;
    private String answer;
    public QAPair() {}
    public QAPair(String q, String a) { question = q; answer = a;}
 /*   private QAPair(JSONObject o) {
      question = (String) o.get("question");
      answer   = (String) o.get("answerKey");
    }*/
    public String toString() { return "'"+question + "' : '" + answer +"'"; }
  }

  public Exercise() {}
  public Exercise(String id, String content) { this.id = id; this.content = content; }
 /* public Exercise(JSONObject obj) {
    content = (String) obj.get("content");
    id = (String) obj.get("exid");
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) obj.get("qa"), JSONObject.class);
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String k : keys) {
        JSONObject qaForLang = (JSONObject)o.get(k);

        addQuestion(k,(String) qaForLang.get("question"),(String) qaForLang.get("answerKey"));
   //     langToQuestion.put(k, new QAPair((String) qaForLang.get("question"),(String) o.get("answerKey")));
      }
    }
  }*/
  public void addQuestion(String lang, String question, String answer) {
    langToQuestion.put(lang, new QAPair(question, answer));
  }

  public String getID() { return id; }

  public String toString() {
    return "Exercise " + id + " : " + content + "\n\tquestions " + langToQuestion;
  }
}
