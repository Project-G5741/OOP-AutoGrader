package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.ClassRelation;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;

/**
 * Neon-RTT-aware structure graph reads: fan out independent SELECTs on separate
 * short read transactions so editor load is limited by the slowest query, not the sum.
 */
@Service
public class LabStructureGraphLoader {

    public record MemberWave(
            List<Field> fields,
            List<Method> methods,
            List<Constructor> constructors,
            List<ClassRelation> relations) {}

    public record ParameterWave(
            List<Parameter> methodParams,
            List<Parameter> constructorParams) {}

    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ClassRelationRepository classRelationRepository;
    private final ParameterRepository parameterRepository;
    private final ExecutorService structureReadExecutor;
    private final LabStructureGraphLoader self;

    public LabStructureGraphLoader(FieldRepository fieldRepository,
                                   MethodRepository methodRepository,
                                   ConstructorRepository constructorRepository,
                                   ClassRelationRepository classRelationRepository,
                                   ParameterRepository parameterRepository,
                                   @Qualifier("lecturerBootstrapExecutor") ExecutorService structureReadExecutor,
                                   @org.springframework.context.annotation.Lazy LabStructureGraphLoader self) {
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.classRelationRepository = classRelationRepository;
        this.parameterRepository = parameterRepository;
        this.structureReadExecutor = structureReadExecutor;
        this.self = self;
    }

    public MemberWave loadMemberWave(List<UUID> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return new MemberWave(List.of(), List.of(), List.of(), List.of());
        }
        try {
            CompletableFuture<List<Field>> fieldsF = CompletableFuture.supplyAsync(
                    () -> self.loadFields(classIds), structureReadExecutor);
            CompletableFuture<List<Method>> methodsF = CompletableFuture.supplyAsync(
                    () -> self.loadMethods(classIds), structureReadExecutor);
            CompletableFuture<List<Constructor>> constructorsF = CompletableFuture.supplyAsync(
                    () -> self.loadConstructors(classIds), structureReadExecutor);
            CompletableFuture<List<ClassRelation>> relationsF = CompletableFuture.supplyAsync(
                    () -> self.loadRelations(classIds), structureReadExecutor);
            CompletableFuture.allOf(fieldsF, methodsF, constructorsF, relationsF).join();
            return new MemberWave(fieldsF.join(), methodsF.join(), constructorsF.join(), relationsF.join());
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw ex;
        }
    }

    public ParameterWave loadParameterWave(List<UUID> methodIds, List<UUID> constructorIds) {
        if ((methodIds == null || methodIds.isEmpty())
                && (constructorIds == null || constructorIds.isEmpty())) {
            return new ParameterWave(List.of(), List.of());
        }
        List<UUID> methods = methodIds != null ? methodIds : List.of();
        List<UUID> constructors = constructorIds != null ? constructorIds : List.of();
        try {
            CompletableFuture<List<Parameter>> methodParamsF = methods.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : CompletableFuture.supplyAsync(() -> self.loadMethodParams(methods), structureReadExecutor);
            CompletableFuture<List<Parameter>> constructorParamsF = constructors.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : CompletableFuture.supplyAsync(
                            () -> self.loadConstructorParams(constructors), structureReadExecutor);
            CompletableFuture.allOf(methodParamsF, constructorParamsF).join();
            return new ParameterWave(methodParamsF.join(), constructorParamsF.join());
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Field> loadFields(List<UUID> classIds) {
        return fieldRepository.findByClassEntityIdInWithDeclaration(classIds);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Method> loadMethods(List<UUID> classIds) {
        return methodRepository.findByClassEntityIdInWithDeclaration(classIds);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Constructor> loadConstructors(List<UUID> classIds) {
        return constructorRepository.findByClassEntityIdInWithDeclaration(classIds);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<ClassRelation> loadRelations(List<UUID> classIds) {
        return classRelationRepository.findByClassEntityIdInWithEndpoints(classIds);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Parameter> loadMethodParams(List<UUID> methodIds) {
        return parameterRepository.findByMethod_IdIn(methodIds);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Parameter> loadConstructorParams(List<UUID> constructorIds) {
        return parameterRepository.findByConstructorEntity_IdIn(constructorIds);
    }
}
