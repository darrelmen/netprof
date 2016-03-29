package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.hibernate.HDAO;
import org.apache.log4j.Logger;

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
}
