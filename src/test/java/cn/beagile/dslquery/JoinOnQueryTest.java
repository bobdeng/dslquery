package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JoinOnQueryTest {
    private final String nullsOrder = "";

    @View("t_user")
    public static class User {
        @Column(name = "tenant_id")
        private Long tenantId;

        @JoinColumn(name = "org_id", referencedColumnName = "id")
        @JoinOn("(and(enabled eq true)(tenantId eq @parent.tenantId))")
        private Org org;
    }

    @View("t_org")
    public static class Org {
        @Column(name = "id")
        private Long id;

        @Column(name = "enabled")
        private Boolean enabled;

        @Column(name = "tenant_id")
        private Long tenantId;

        @Column(name = "type")
        private String type;
    }

    @View("t_user")
    @DeepJoinIncludes("org.area")
    public static class DeepUser {
        @Column(name = "area_code")
        private String areaCode;

        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private DeepOrg org;
    }

    @View("t_org")
    public static class DeepOrg {
        @Column(name = "tenant_id")
        private Long tenantId;

        @JoinColumn(name = "area_id", referencedColumnName = "id")
        @JoinOn("(and(code eq @root.areaCode)(tenantId eq @parent.tenantId))")
        private Area area;
    }

    @View("t_area")
    public static class Area {
        @Column(name = "id")
        private Long id;

        @Column(name = "code")
        private String code;

        @Column(name = "tenant_id")
        private Long tenantId;

        @Column(name = "active")
        private Boolean active;
    }

    @View("t_user")
    public static class InvalidUser {
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        @JoinOn("(and(missing eq true))")
        private Org org;
    }

    @Test
    void should_append_annotation_and_runtime_conditions_to_join_on() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class)
                .joinOn("org", "(and(type eq SALES))");

        DSLSQLBuilder<User> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        SQLQuery sqlQuery = sqlBuilder.build(nullsOrder);

        assertEquals("select t_user.tenant_id tenantId_,org_.id org_id_,org_.enabled org_enabled_,org_.tenant_id org_tenantId_,org_.type org_type_ from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id and ((org_.enabled = :j0_p0) and (org_.tenant_id = t_user.tenant_id)) and ((org_.type = :j0_p2))", sqlQuery.getSql());
        assertEquals(true, sqlQuery.getParams().get("j0_p0"));
        assertEquals("SALES", sqlQuery.getParams().get("j0_p2"));
    }

    @Test
    void should_resolve_parent_and_root_fields_for_deep_join_conditions() {
        DSLQuery<DeepUser> dslQuery = new DSLQuery<>(null, DeepUser.class)
                .joinOn("org.area", "(and(active eq true))");

        DSLSQLBuilder<DeepUser> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        SQLQuery sqlQuery = sqlBuilder.build(nullsOrder);

        assertEquals("select t_user.area_code areaCode_,org_.tenant_id org_tenantId_,org_area_.id org_area_id_,org_area_.code org_area_code_,org_area_.tenant_id org_area_tenantId_,org_area_.active org_area_active_ from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area org_area_ on org_area_.id = org_.area_id and ((org_area_.code = t_user.area_code) and (org_area_.tenant_id = org_.tenant_id)) and ((org_area_.active = :j1_p2))", sqlQuery.getSql());
        assertEquals(true, sqlQuery.getParams().get("j1_p2"));
    }

    @Test
    void should_fail_fast_when_join_on_field_is_unknown() {
        DSLSQLBuilder<InvalidUser> sqlBuilder = new DSLSQLBuilder<>(new DSLQuery<>(null, InvalidUser.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> sqlBuilder.build(nullsOrder));
        assertEquals("field not found: missing", exception.getMessage());
    }

    @Test
    void should_reject_invalid_runtime_join_on_path() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dslQuery.joinOn("org;drop", "(and(type eq SALES))"));

        assertEquals("invalid join path:org;drop", exception.getMessage());
    }

    @Test
    void should_fail_when_runtime_join_on_path_is_not_joined() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class)
                .joinOn("org.area", "(and(active eq true))");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new DSLSQLBuilder<>(dslQuery).build(nullsOrder));
        assertEquals("join path not found: org.area", exception.getMessage());
    }

    @Test
    void should_fail_when_join_on_expression_is_too_deep() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class)
                .joinOn("org", "(and(or(and(or(and(or(and(or(and(or(and(type eq SALES))))))))))))");

        DSLSQLBuilder<User> sqlBuilder = new DSLSQLBuilder<>(dslQuery);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> sqlBuilder.build(nullsOrder));
        assertEquals("join on nesting too deep", exception.getMessage());
    }

    @Test
    void should_fail_when_join_on_expression_is_too_long() {
        String longValue = "a".repeat(2050);
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class)
                .joinOn("org", "(and(type eq " + longValue + "))");

        DSLSQLBuilder<User> sqlBuilder = new DSLSQLBuilder<>(dslQuery);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> sqlBuilder.build(nullsOrder));
        assertEquals("join on expression too long", exception.getMessage());
    }
}
