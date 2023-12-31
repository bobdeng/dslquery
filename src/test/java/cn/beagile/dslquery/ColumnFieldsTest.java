package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnFieldsTest {
    @View("empty")
    public static class Empty {

    }


    @Test
    public void should_return_empty() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Empty.class));
        List<ColumnField> fields = columnFields.selectFields();
        assertEquals(0, fields.size());
        assertEquals("empty", columnFields.from());
        assertEquals(0, columnFields.joined().size());
    }

    @View("has_one")
    public static class HasOne {
        @Column(name = "f_name")
        private String name;
    }

    @Test
    public void should_read_column() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, HasOne.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("f_name", columnFields.selectFields().get(0).columnName());
        assertEquals("name", columnFields.selectFields().get(0).fieldName());
        assertEquals("name_", columnFields.selectFields().get(0).alias());
        assertEquals("has_one.f_name name_", columnFields.selectFields().get(0).expression());
    }

    @View("has_embedded")
    public static class HasEmbedded {
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "f_name"))
        })
        @Embedded
        private HasOne hasOne;
    }

    @Test
    public void should_read_embedded_column() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, HasEmbedded.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("f_name", columnFields.selectFields().get(0).columnName());
        assertEquals("hasOne.name", columnFields.selectFields().get(0).fieldName());
        assertEquals("hasOne_name_", columnFields.selectFields().get(0).alias());
        assertEquals("has_embedded.f_name hasOne_name_", columnFields.selectFields().get(0).expression());
    }

    @View("has_embedded")
    public static class HasEmbeddedNotOverride {
        @AttributeOverrides({
        })
        @Embedded
        private HasOne hasOne;
    }

    @Test
    public void should_not_read_if_not_override() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, HasEmbeddedNotOverride.class));
        assertEquals(0, columnFields.selectFields().size());
    }

    @View("has_2embedded")
    public static class HasEmbedded2 {
        @AttributeOverrides({
                @AttributeOverride(name = "hasOne.name", column = @Column(name = "name_2"))
        })
        @Embedded
        private HasEmbeddedNotOverride one;
    }

    @Test
    public void should_read_embedded_twice() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, HasEmbedded2.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("name_2", columnFields.selectFields().get(0).columnName());
        assertEquals("one.hasOne.name", columnFields.selectFields().get(0).fieldName());
        assertEquals("one_hasOne_name_", columnFields.selectFields().get(0).alias());
        assertEquals("has_2embedded.name_2 one_hasOne_name_", columnFields.selectFields().get(0).expression());
    }

    @View("t_slave")
    public static class Slave {
        @JoinColumn(name = "master_id", referencedColumnName = "id")
        public Master master;
    }

    @View("t_master")
    public static class Master {
        @Column(name = "id")
        public Long id;
    }

    @Test
    public void should_read_join() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Slave.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("id", columnFields.selectFields().get(0).columnName());
        assertEquals("master.id", columnFields.selectFields().get(0).fieldName());
        assertEquals("master_id_", columnFields.selectFields().get(0).alias());
        assertEquals("master_.id master_id_", columnFields.selectFields().get(0).expression());
        assertEquals("left join t_master master_ on master_.id = t_slave.master_id", columnFields.joins());
    }

    @View("t_tool")
    @DeepJoinIncludes("slave.master")
    public static class Tool {
        @JoinColumn(name = "slave_id", referencedColumnName = "id")
        public Slave slave;
    }

    @Test
    public void should_read_join_twice() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, Tool.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("id", columnFields.selectFields().get(0).columnName());
        assertEquals("slave.master.id", columnFields.selectFields().get(0).fieldName());
        assertEquals("slave_master_id_", columnFields.selectFields().get(0).alias());
        assertEquals("slave_master_.id slave_master_id_", columnFields.selectFields().get(0).expression());
        assertEquals("left join t_slave slave_ on slave_.id = t_tool.slave_id\n" +
                "left join t_master slave_master_ on slave_master_.id = slave_.master_id", columnFields.joins());
    }

    @View("t_tool")
    public static class ToolMaster {
        @JoinColumn(name = "slave_id", referencedColumnName = "id", table = "t_slave")
        @JoinColumn(name = "master_id", referencedColumnName = "id")
        public Master master;
    }

    @Test
    public void should_read_2join() {
        ColumnFields columnFields = new ColumnFields(new DSLQuery<>(null, ToolMaster.class));
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("id", columnFields.selectFields().get(0).columnName());
        assertEquals("master.id", columnFields.selectFields().get(0).fieldName());
        assertEquals("master_id_", columnFields.selectFields().get(0).alias());
        assertEquals("master_.id master_id_", columnFields.selectFields().get(0).expression());
        assertEquals("left join t_slave on t_slave.id = t_tool.slave_id\n" +
                "left join t_master master_ on master_.id = t_slave.master_id", columnFields.joins());
    }

}
