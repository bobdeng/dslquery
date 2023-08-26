package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class One2ManyTest {
    @View("master")
    public static class Master {
        @OneToMany(targetEntity = Slave.class)
        @JoinColumn(name = "id", referencedColumnName = "masterId")
        private List<Slave> slaves;
        @Column(name = "id")
        private String id;
    }

    @View("slave")
    public static class Slave {
        @Column(name = "slave_name")
        private String name;
        @Column(name = "master_id")
        private String masterId;

    }

    @Test
    public void should_not_join() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Master.class));
        assertEquals("", columnFields.joins());
    }

    @Test
    public void should_has_one_to_many_filed() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Master.class));
        assertEquals(1, columnFields.oneToManyFields().size());
    }

    @Test
    public void should_fetch_one_to_many() {
        ArgumentCaptor<DefaultResultSetReader<Slave>> readerArgumentCaptor = ArgumentCaptor.forClass(DefaultResultSetReader.class);
        ArgumentCaptor<SQLQuery> sqlQueryArgumentCaptor = ArgumentCaptor.forClass(SQLQuery.class);
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Master.class));
        Master master = new Master();
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        master.id = "1";
        List<Slave> slaves = Arrays.asList(new Slave());
        when(queryExecutor.list(readerArgumentCaptor.capture(), sqlQueryArgumentCaptor.capture())).thenReturn(slaves);

        columnFields.fetchOneToManyFields(master, queryExecutor);

        assertEquals(slaves, master.slaves);
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select slave.slave_name name_,slave.master_id masterId_ from slave\n" +
                "\n" +
                " where ((slave.master_id = :p1))", sqlQuery.getSql().trim());
    }
}
