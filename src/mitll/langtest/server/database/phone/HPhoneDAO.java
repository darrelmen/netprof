package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.server.database.hibernate.HDAO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public class HPhoneDAO extends HDAO<Phone> implements IPhoneDAO<Phone> {
  private static final Logger logger = Logger.getLogger(HPhoneDAO.class);

  /**
   * @param database
   * @see DatabaseImpl#initializeDAOs
   */
  public HPhoneDAO(Database database) {
    super(database);
  }

/*  public void addAll(Collection<T> allEvents) {
    Class<T> clazz = (Class<T>) ((ParameterizedType) getClass()
        .getGenericSuperclass()).getActualTypeArguments()[0];

    if (isEmptyForClass(clazz)) {
      logger.info("Adding initial phones from H2 " + allEvents.size());
      sessionManagement.doInTranscation((session) -> {
        for (T event : allEvents) {
          session.save(event);
        }
      });
    }
  }*/

  @Override
  public boolean addPhone(Phone phone) {
    return add(phone);
/*
    sessionManagement.doInTranscation((session) -> {
      session.save(phone);
    });

    return true;
*/
  }

/*  public void doJoin() {
    List<Object[]> stuff =
  }

  private String getJoinSQL(long userid, String filterClause) {
    return "select " +

        "results.exid," +
        "results.answer," +
        "results." + ResultDAO.SCORE_JSON + "," +
        "results." + ResultDAO.PRON_SCORE + "," +
        "results.time,  " +

        "word.seq, " +
        "word.word, " +
        "word.score wordscore, " +

        "phone.* " +

        " from " +
        "results, phone, word " +

        "where " +
        "results.id = phone.rid " + "AND " +
        ResultDAO.RESULTS + "." + ResultDAO.USERID + "=" + userid + " AND " +
        filterClause +
        " AND phone.wid = word.id " +
        " order by results.exid, results.time desc";
  }*/
}
