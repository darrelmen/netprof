package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import net.sf.json.JSONObject;

import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T>  extends IDAO {
  void addBulkPhones(List<Phone> bulk, int projID);

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

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReportForPhone(int, UserInfo, Project, String, long, long)
   * @param userid
   * @param ids
   * @param project
   * @param phone
   * @param from
   * @param to
   * @return
   */
  PhoneReport getWorstPhonesForResultsForPhone(int userid, Collection<Integer> ids, Project project, String phone, long from, long to);

  void removeForResult(int resultid);

  void deleteForProject(int projID);
}
