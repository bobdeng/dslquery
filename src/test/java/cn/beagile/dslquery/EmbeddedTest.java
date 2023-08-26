package cn.beagile.dslquery;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmbeddedTest {
    @View("employee")
    public static class QueryClass {
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "title_name")),
                @AttributeOverride(name = "level", column = @Column(name = "title_level"))
        })
        private JobTitle title;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "appointment_name")),
                @AttributeOverride(name = "level", column = @Column(name = "appointment_level"))
        })
        private JobTitle appointmentTitle;

        public JobTitle getTitle() {
            return title;
        }

        public void setTitle(JobTitle title) {
            this.title = title;
        }

        public JobTitle getAppointmentTitle() {
            return appointmentTitle;
        }

        public void setAppointmentTitle(JobTitle appointmentTitle) {
            this.appointmentTitle = appointmentTitle;
        }
    }

    public static class JobTitle {
        private String name;
        private String level;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    @Test
    public void should_read_job_titles() throws SQLException {
        DefaultResultSetReader<QueryClass> reader = new DefaultResultSetReader<>(new DSLQuery<>(null, QueryClass.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("title_name_")).thenReturn("name1");
        when(resultSet.getString("title_level_")).thenReturn("level1");
        when(resultSet.getString("appointmentTitle_name_")).thenReturn("name2");
        when(resultSet.getString("appointmentTitle_level_")).thenReturn("level2");
        QueryClass result = reader.apply(resultSet);
        System.out.println(new Gson().toJson(result));
        assertEquals("name1", result.title.name);
        assertEquals("name2", result.appointmentTitle.name);
        assertEquals("level1", result.title.level);
        assertEquals("level2", result.appointmentTitle.level);
    }
}
