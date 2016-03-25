package mitll.langtest.server.database.hibernate;

import mitll.langtest.server.database.connection.PostgreSQLConnection;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by go22670 on 3/25/16.
 */
public class SessionManagement {
  private static final Logger logger = Logger.getLogger(SessionManagement.class);

  private SessionFactory sessionFactory;

  public SessionManagement(String dbName) {
    makeFactory(dbName);
  }

  private void makeFactory(String dbName) {
    StandardServiceRegistryBuilder configure = new StandardServiceRegistryBuilder()
        .configure();

    //logger.info("using " + dbName);

    doSpecialConfig(dbName, configure);
    final StandardServiceRegistry registry = configure // configures settings from hibernate.cfg.xml
        .build();

  //  logger.debug("built the registry ");

    try {
      Metadata metadata = new MetadataSources(registry).buildMetadata();
      sessionFactory = metadata.buildSessionFactory();
      //logger.info("made factory " + sessionFactory);
    } catch (Exception e) {
      logger.error("got " + e, e);
      // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
      // so destroy it manually.
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  protected void doSpecialConfig(String dbName, StandardServiceRegistryBuilder configure) {
    configure.applySetting("hibernate.connection.url", "jdbc:postgresql://localhost/" + dbName.toLowerCase());
    create(dbName);
  }

  protected void create(String dbName) {
    try {
      Class.forName("org.postgresql.Driver").newInstance();
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
    try {
      String host = "localhost";

      Connection conn = DriverManager.getConnection(
          "jdbc:postgresql://" +
              host +
              ":5432/", PostgreSQLConnection.DEFAULT_USER,
          "");

      String dbName1 = dbName.toLowerCase();
      createDatabaseForgiving(dbName1, conn);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  void createDatabaseForgiving(String dbName, Connection conn) throws SQLException {
    try {
      createDatabase(dbName, conn);
    } catch (SQLException e) {
      if (!e.getMessage().contains("already exists")) {
        throw e;
      }
      else logger.info("Got " +e.getMessage());
    }
  }

  void createDatabase(String dbName, Connection conn) throws SQLException {
    Statement s2 = conn.createStatement();
    int myResult = s2.executeUpdate("CREATE DATABASE " + dbName);
    s2.close();
    conn.close();
    logger.info("Got " + myResult);

  }

  public void close() throws Exception {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
  }

  public void doInTranscation(Consumer<Session> operations) {
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    operations.accept(session);
    session.getTransaction().commit();
    session.close();
  }

  public <T> T getFromTransaction(SessionSupplier<T> supplier) {
    // now lets pull events from the database and list them
    Session session = sessionFactory.openSession();
    session.beginTransaction();
    supplier.setSession(session);
    T t = supplier.get();
//    List result = session.createQuery( "from Event" ).list();
//    for ( Event event : (List<Event>) result ) {
//      System.out.println( "Event (" + event.getDate() + ") : " + event.getTitle() );
//    }
    session.getTransaction().commit();
    session.close();
    return t;
  }

  public abstract static class SessionSupplier<T> implements Supplier<T> {
    protected Session session;

    //    public SessionSupplier() {}
    //public SessionSupplier(Session session) {this.session = session;}
    public abstract T get();

    public void setSession(Session session) {
      this.session = session;
    }
  }

  public static void main(String[] arg) {
    try {
      SessionManagement sessionManagement = new SessionManagement("testagain");

      sessionManagement.doInTranscation((session) -> {
        session.save(new AnnotationEvent("Our very first event!", new Date()));
        session.save(new AnnotationEvent("A follow up event", new Date()));
      });

      AnnotationEvent ret = sessionManagement.getFromTransaction(new SessionSupplier<AnnotationEvent>() {
        @Override
        public AnnotationEvent get() {
          List<AnnotationEvent> result = session.createQuery("from AnnotationEvent").list();
          return result.isEmpty() ? null : result.get(0);
        }
      });

      logger.info("got " + ret);

      List<AnnotationEvent> ret2 = sessionManagement.getFromTransaction(new SessionSupplier<List<AnnotationEvent>>() {
        @Override
        public List<AnnotationEvent> get() {
          List<AnnotationEvent> result = session.createQuery("from AnnotationEvent").list();
          return result;
        }
      });

      logger.info("everything " + ret2);

      sessionManagement.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
