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
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "testcase_invocation",
        uniqueConstraints = @UniqueConstraint(
                name = "testcase_invocation_testcase_id_order_index_key",
                columnNames = {"testcase_id", "order_index"}
        )
)
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

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "instance_name")
    private String instanceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_class_id",
            foreignKey = @ForeignKey(name = "testcase_invocation_dispatch_class_id_fkey"))
    private ClassEntity dispatchClass;

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

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }

    public ClassEntity getDispatchClass() { return dispatchClass; }
    public void setDispatchClass(ClassEntity dispatchClass) { this.dispatchClass = dispatchClass; }
}
