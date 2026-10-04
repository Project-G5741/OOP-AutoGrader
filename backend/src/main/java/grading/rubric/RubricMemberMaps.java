package com.eiu.capstone.backend.grading.rubric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;

/**
 * Lookup maps for assembling operational testcase rubrics from challenge members.
 */
public record RubricMemberMaps(
        Map<UUID, String> classNameByClassId,
        Map<UUID, String> classNameByConstructorId,
        Map<UUID, List<String>> paramTypesByConstructorId,
        Map<UUID, List<String>> paramTypesByMethodId,
        Map<UUID, String> classNameByMethodId,
        Map<UUID, Method> methodById,
        Map<UUID, Field> fieldById) {

    public static RubricMemberMaps fromEntities(List<ClassEntity> classes,
                                                List<Constructor> constructors,
                                                List<Method> methods,
                                                List<Field> fields,
                                                List<Parameter> constructorParams,
                                                List<Parameter> methodParams) {
        Map<UUID, String> classNameByClassId = new HashMap<>();
        for (ClassEntity cls : classes) {
            classNameByClassId.put(cls.getId(), cls.getName());
        }
        Map<UUID, String> classNameByConstructorId = new HashMap<>();
        for (Constructor constructor : constructors) {
            classNameByConstructorId.put(
                    constructor.getId(),
                    classNameByClassId.get(constructor.getClassEntity().getId()));
        }
        Map<UUID, String> classNameByMethodId = new HashMap<>();
        Map<UUID, Method> methodById = new HashMap<>();
        for (Method method : methods) {
            methodById.put(method.getId(), method);
            classNameByMethodId.put(method.getId(), classNameByClassId.get(method.getClassEntity().getId()));
        }
        Map<UUID, Field> fieldById = new HashMap<>();
        for (Field field : fields) {
            fieldById.put(field.getId(), field);
        }

        return new RubricMemberMaps(
                classNameByClassId,
                classNameByConstructorId,
                RubricParameterMaps.byConstructor(constructorParams),
                RubricParameterMaps.byMethod(methodParams),
                classNameByMethodId,
                methodById,
                fieldById);
    }

    public static RubricMemberMaps empty() {
        return new RubricMemberMaps(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
}
