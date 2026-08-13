package com.eiu.capstone.backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "testcase_invocation")
public class TestcaseInvocation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false,
            foreignKey = @ForeignKey(name = "testcase_invocation_testcase_id_fkey"))
    private Testcase testcase;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "invocation_kind", nullable = false, columnDefinition = "invocation_kind")
    private InvocationKind invocationKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constructor_id",
            foreignKey = @ForeignKey(name = "testcase_invocation_constructor_id_fkey"))
    private Constructor constructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id",
            foreignKey = @ForeignKey(name = "testcase_invocation_method_id_fkey"))
    private Method method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", nullable = false, columnDefinition = "jsonb")
    private String params = "[]";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_constructor_id",
            foreignKey = @ForeignKey(name = "testcase_invocation_receiver_constructor_id_fkey"))
    private Constructor receiverConstructor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "receiver_params", nullable = false, columnDefinition = "jsonb")
    private String receiverParams = "[]";

    public TestcaseInvocation() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Testcase getTestcase() { return testcase; }
    public void setTestcase(Testcase testcase) { this.testcase = testcase; }

    public InvocationKind getInvocationKind() { return invocationKind; }
    public void setInvocationKind(InvocationKind invocationKind) { this.invocationKind = invocationKind; }

    public Constructor getConstructor() { return constructor; }
    public void setConstructor(Constructor constructor) { this.constructor = constructor; }

    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public Constructor getReceiverConstructor() { return receiverConstructor; }
    public void setReceiverConstructor(Constructor receiverConstructor) {
        this.receiverConstructor = receiverConstructor;
    }

    public String getReceiverParams() { return receiverParams; }
    public void setReceiverParams(String receiverParams) { this.receiverParams = receiverParams; }
}
