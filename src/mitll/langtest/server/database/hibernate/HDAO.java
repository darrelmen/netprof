package mitll.langtest.server.database.hibernate;

import mitll.langtest.server.database.Database;
import org.hibernate.criterion.Projections;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public class HDAO<T> {
  protected final SessionManagement sessionManagement;

  public HDAO(Database database) {
    sessionManagement = database.getSessionManagement();
  }

  public void addAll(Collection<T> allEvents) {
    Class<T> clazz = getSelfClass();

    if (isEmptyForClass(clazz)) {
     // logger.info("Adding initial phones from H2 " + allEvents.size());
      sessionManagement.doInTranscation((session) -> {
        for (T event : allEvents) {
          session.save(event);
        }
      });
    }
  }

  Class<T> getSelfClass() {
    return (Class<T>) ((ParameterizedType) getClass()
        .getGenericSuperclass()).getActualTypeArguments()[0];
  }

  //  @Override
  public boolean add(T phone) {
    sessionManagement.doInTranscation((session) -> {
      session.save(phone);
    });

    return true;
  }

  public boolean isEmpty(String objClass) {
    String s = "select 1 from " +objClass;
    return sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<Boolean>() {
      @Override
      public Boolean get() {
        return session.createQuery(s).setMaxResults(1).list().isEmpty();
      }
    });
  }

/*  public boolean isEmpty() {
    return isEmptyForClass(getSelfClass());
  }*/

  public boolean isEmptyForClass(Class objClass) {
    String s = "select 1 from " +objClass.getName();
    return sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<Boolean>() {
      @Override
      public Boolean get() {
        return session.createQuery(s).setMaxResults(1).list().isEmpty();
      }
    });
  }

  public <T> List<T> getAll(String objClass) {
    return sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<List<T>>() {
      @Override
      public List<T> get() {
        return session.createQuery("from "+objClass).list();
      }
    });
  }

  public Number getNumRowsByClass(Class objClass) {
    return sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<Number>() {
      @Override
      public Number get() {
        return (Number) session.createCriteria(objClass).setProjection(Projections.rowCount()).uniqueResult();
      }
    });
  }
}
