package mitll.langtest.server.database.hibernate;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table( name = "EVENTS" )
public class AnnotationEvent {
  private Long id;

  private String title;
  private Date date;

  public AnnotationEvent() {
    // this form used by Hibernate
  }

  public AnnotationEvent(String title, Date date) {
    // for application use, to create new events
    this.title = title;
    this.date = date;
  }

  @Id
  @GeneratedValue(generator="increment")
  @GenericGenerator(name="increment", strategy = "increment")
  public Long getId() {
    return id;
  }

  private void setId(Long id) {
    this.id = id;
  }

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "EVENT_DATE")
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String toString() {
    return 		"Event (" + getDate() + ") : " + getTitle();

  }
}