package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.analysis.PhoneReport;
import net.sf.json.JSONObject;

import java.util.Collection;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T>  extends IDAO {
  boolean addPhone(T phone);

  /**
   * @see mitll.langtest.server.database.JsonSupport#getJsonPhoneReport
   * @param userid
   * @param exids
   * @param language
   * @param project
   * @return
   */
  JSONObject getWorstPhonesJson(int userid, Collection<Integer> exids, String language, Project project);

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReport
   * @param userid
   * @param ids
   * @param project
   * @return
   */
  PhoneReport getWorstPhonesForResults(int userid, Collection<Integer> ids, Project project);
  PhoneReport getWorstPhonesForResultsForPhone(int userid, Collection<Integer> ids, Project project, String phone, long from, long to);

  void removeForResult(int resultid);
}
