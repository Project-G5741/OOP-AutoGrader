package com.eiu.capstone.backend.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;

/**
 * Loads expected rubric identifiers for leakage checks only — never for Gemini prompts.
 */
@Component
@Profile("!desktop")
public class AiProhibitedIdentifierLoader {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;

    public AiProhibitedIdentifierLoader(
            ChallengeRepository challengeRepository,
            ClassEntityRepository classEntityRepository,
            FieldRepository fieldRepository,
            MethodRepository methodRepository,
            ConstructorRepository constructorRepository,
            ParameterRepository parameterRepository) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
    }

    public Set<String> loadForLab(UUID labId) {
        Set<String> out = new HashSet<>();
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        if (challenges.isEmpty()) {
            return out;
        }
        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        for (ClassEntity cls : classes) {
            add(out, cls.getName());
        }
        for (Field field : fieldRepository.findByClassEntityInWithDeclaration(classes)) {
            add(out, field.getName());
            if (field.getFieldDeclaration() != null) {
                add(out, field.getFieldDeclaration().getName());
                add(out, field.getFieldDeclaration().getDataType());
            }
        }
        List<Method> methods = methodRepository.findByClassEntityInWithDeclaration(classes);
        for (Method method : methods) {
            add(out, method.getName());
            if (method.getMethodDeclaration() != null) {
                add(out, method.getMethodDeclaration().getName());
                add(out, method.getMethodDeclaration().getReturnType());
            }
        }
        List<Constructor> ctors = constructorRepository.findByClassEntityInWithDeclaration(classes);
        for (Constructor ctor : ctors) {
            add(out, ctor.getName());
            if (ctor.getConstructorDeclaration() != null) {
                add(out, ctor.getConstructorDeclaration().getName());
            }
        }
        for (Parameter parameter : parameterRepository.findByMethodIn(methods)) {
            add(out, parameter.getName());
            add(out, parameter.getDataType());
        }
        for (Parameter parameter : parameterRepository.findByConstructorEntityIn(ctors)) {
            add(out, parameter.getName());
            add(out, parameter.getDataType());
        }
        return out;
    }

    private static void add(Set<String> out, String value) {
        if (value != null && !value.isBlank() && value.length() >= 2) {
            out.add(value.trim());
        }
    }
}
