package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.analysis.PhoneBigrams;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneSummary;
import mitll.langtest.shared.analysis.UserInfo;
import net.sf.json.JSONObject;

import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T> extends IDAO {
  void addBulkPhones(List<Phone> bulk, int projID);

  /**
   * @param userid
   * @param exids
   * @param language
   * @param project
   * @return
   * @see mitll.langtest.server.database.JsonSupport#getJsonPhoneReport
   */
  JSONObject getWorstPhonesJson(int userid, Collection<Integer> exids, String language, Project project);

  /**
   * @param userid
   * @param ids
   * @param project
   * @return
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReport
   */
  PhoneReport getWorstPhonesForResults(int userid, Collection<Integer> ids, Project project);

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneSummary
   * @param userid
   * @param ids
   * @param project
   * @return
   */
  PhoneSummary getPhoneSummary(int userid, Collection<Integer> ids, Project project);

  PhoneBigrams getPhoneBigrams(int userid, Collection<Integer> ids );

  /**
   * @param userid
   * @param ids
   * @param project
   * @param phone
   * @param from
   * @param to
   * @return
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReportForPhone(int, UserInfo, Project, String, long, long)
   */
  PhoneReport getWorstPhonesForResultsForPhone(int userid, Collection<Integer> ids,
                                               Project project,
                                               String phone, long from, long to);

  PhoneReport getWorstPhonesForResultsForTimeWindow(int userid,
                                                    Collection<Integer> ids,
                                                    Project project,
                                                    long from,
                                                    long to);

  void removeForResult(int resultid);

  void deleteForProject(int projID);

  boolean updateProjectForRID(int rid, int newprojid);

}
