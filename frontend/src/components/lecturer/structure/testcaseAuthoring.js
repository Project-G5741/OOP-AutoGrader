export const MAX_STEPS = 20;
export const MAX_NAMED_INSTANCES = 10;
export const TEXT_COMPARISON_MODES = ['EXACT', 'TRIMMED', 'NORMALIZED_WHITESPACE'];
/** @deprecated use comparisonModesForAssertion */
export const COMPARISON_MODES = TEXT_COMPARISON_MODES;
export const NUMERIC_COMPARISON_MODES = ['EXACT', 'VALUE_ONLY'];

const NUMERIC_TYPE_NAMES = new Set([
  'byte', 'Byte', 'short', 'Short', 'int', 'Integer', 'long', 'Long',
  'float', 'Float', 'double', 'Double',
]);

export function isPrimitiveNumericType(typeName) {
  const core = coreTypeName(typeName);
  return core != null && NUMERIC_TYPE_NAMES.has(core);
}

export function isNumericAssertion(assertion, step, catalog) {
  if (!assertion || !step || !catalog) return false;
  if (assertion.assertionKind === 'RETURN_VALUE') {
    if (isObjectReturnStep(step, catalog)) return false;
    if (step.invocationKind === 'CONSTRUCTOR') return false;
    const method = catalog.methodsById.get(step.methodId);
    return Boolean(method && isPrimitiveNumericType(method.returnType));
  }
  if (assertion.assertionKind === 'FIELD_STATE' && assertion.fieldId) {
    const field = catalog.fieldsById.get(assertion.fieldId);
    return Boolean(field && isPrimitiveNumericType(field.dataType));
  }
  return false;
}

export function comparisonModesForAssertion(assertion, step, catalog) {
  if (assertion?.assertionKind === 'STDOUT') return TEXT_COMPARISON_MODES;
  if (isNumericAssertion(assertion, step, catalog)) return NUMERIC_COMPARISON_MODES;
  return TEXT_COMPARISON_MODES;
}

/** @param {string} mode */
export function normalizeComparisonMode(mode) {
  if (mode === 'NUMERIC_VALUE') return 'VALUE_ONLY';
  return mode;
}

export function comparisonModeLabel(mode) {
  return normalizeComparisonMode(mode) ?? mode;
}

export const FIELD_CLASS =
  'mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white';
export const FIELD_CLASS_COMPACT =
  'w-full rounded border border-border bg-surface-secondary px-2 py-1 text-sm dark:text-white';

export function emptyInvocation() {
  return {
    id: crypto.randomUUID(),
    invocationKind: 'CONSTRUCTOR',
    constructorId: null,
    methodId: null,
    params: '[]',
    receiverConstructorId: null,
    receiverParams: '[]',
    instanceName: '',
    dispatchClassId: null,
  };
}

export function emptyAssertion(invocationId, assertionKind = 'FIELD_STATE') {
  return {
    id: crypto.randomUUID(),
    invocationId: invocationId || null,
    assertionKind,
    fieldId: null,
    expectedValue: defaultExpectedValue(assertionKind),
    comparisonMode: 'EXACT',
    orderIndex: 0,
  };
}

export function defaultExpectedValue(kind, objectEquals = false) {
  if (kind === 'EXCEPTION') return '"IllegalArgumentException"';
  if (kind === 'STDOUT') return '""';
  if (kind === 'RETURN_VALUE' && objectEquals) {
    return JSON.stringify({ $objectCheck: 'EQUALS', $instance: '' });
  }
  if (kind === 'RETURN_VALUE') return 'null';
  return '0';
}

export function defaultFieldStateExpected(field) {
  if (!field?.dataType) return '0';
  const type = coreTypeName(field.dataType);
  if (type === 'String' || type === 'char' || type === 'Character') return '""';
  if (type === 'boolean' || type === 'Boolean') return 'false';
  return '0';
}

export function parseFieldStateExpected(expectedValue) {
  if (readInstanceExpected(expectedValue) != null) return null;
  try {
    return JSON.parse(expectedValue ?? '0');
  } catch {
    return parseScalarInput(expectedValue);
  }
}

export function writeFieldStateScalar(value) {
  return JSON.stringify(value === undefined ? null : value);
}

export function emptyTestcase(orderIndex = 0, testcaseType = 'UNIT') {
  const invocation = emptyInvocation();
  return {
    id: crypto.randomUUID(),
    name: 'New testcase',
    testcaseType: testcaseType === 'COMPOSITION' ? 'COMPOSITION' : 'UNIT',
    orderIndex,
    hidden: false,
    invocation,
    invocations: [invocation],
    instances: [],
    assertions: [emptyAssertion(invocation.id, 'FIELD_STATE')],
  };
}

export function hydrateInvocation(inv) {
  return {
    ...emptyInvocation(),
    ...inv,
    instanceName: inv?.instanceName ?? '',
    dispatchClassId: inv?.dispatchClassId ?? null,
    receiverConstructorId: inv?.receiverConstructorId ?? null,
    params: inv?.params || '[]',
    receiverParams: inv?.receiverParams || '[]',
  };
}

export function resolvedInvocations(tc) {
  if (Array.isArray(tc?.invocations) && tc.invocations.length > 0) {
    return tc.invocations.map(hydrateInvocation);
  }
  if (tc?.invocation) return [hydrateInvocation(tc.invocation)];
  return [];
}

export function isComposition(tc) {
  return tc?.testcaseType === 'COMPOSITION';
}

export function hydrateTestcase(tc) {
  if (!tc) return tc;
  const testcaseType = tc.testcaseType === 'COMPOSITION' ? 'COMPOSITION' : 'UNIT';
  const invocations = resolvedInvocations(tc);
  const firstId = invocations[0]?.id ?? null;
  return {
    ...tc,
    testcaseType,
    comparisonMethod: null,
    instances: [],
    invocations,
    invocation: invocations[0] || null,
    assertions: (tc.assertions || []).map((a) => ({
      ...a,
      invocationId: a.invocationId || firstId,
      assertionKind: a.assertionKind === 'COMPARISON_RESULT' ? 'RETURN_VALUE' : a.assertionKind,
    })),
  };
}

export function switchTestcaseType(tc, nextType) {
  const type = nextType === 'COMPOSITION' ? 'COMPOSITION' : 'UNIT';
  if (tc.testcaseType === type) return tc;
  const fresh = emptyTestcase(tc.orderIndex ?? 0, type);
  return {
    ...tc,
    testcaseType: type,
    comparisonMethod: null,
    instances: [],
    invocation: fresh.invocation,
    invocations: fresh.invocations,
    assertions: fresh.assertions,
  };
}

export function invocationForKindChange(invocation, kind) {
  const next = {
    ...invocation,
    invocationKind: kind,
    instanceName: '',
    params: '[]',
    receiverConstructorId: null,
    receiverParams: '[]',
    dispatchClassId: null,
  };
  if (kind === 'CONSTRUCTOR') {
    next.methodId = null;
  } else {
    next.constructorId = null;
  }
  return next;
}

function stripInstanceRefs(paramsJson) {
  let parsed;
  try {
    parsed = JSON.parse(paramsJson || '[]');
  } catch {
    return '[]';
  }
  if (!Array.isArray(parsed)) return '[]';
  return JSON.stringify(parsed.map((value) => (isInstanceRef(value) ? null : value)));
}

function normalizeInvocation(step, unit) {
  const hydrated = hydrateInvocation(step);
  if (unit) {
    return {
      ...hydrated,
      instanceName: null,
      receiverConstructorId: null,
      receiverParams: '[]',
      dispatchClassId: null,
      params: stripInstanceRefs(hydrated.params),
    };
  }
  return {
    ...hydrated,
    instanceName: hydrated.instanceName?.trim() || null,
    receiverConstructorId: null,
    receiverParams: '[]',
    dispatchClassId: hydrated.dispatchClassId || null,
  };
}

/**
 * Client-side checks before dry-run. Returns an error message or null when runnable.
 */
export function validateTestcaseForDryRun(tc, catalog) {
  if (!catalog) {
    return 'Challenge structure is still loading. Try again in a moment.';
  }
  const hydrated = hydrateTestcase(tc);
  const composition = isComposition(hydrated);
  const steps = hydrated.invocations;

  if (!steps.length) {
    return 'Complete the testcase steps before running.';
  }

  for (let i = 0; i < steps.length; i += 1) {
    const step = steps[i];
    const label = composition ? `Step ${i + 1}` : 'Testcase';
    if (step.invocationKind === 'CONSTRUCTOR') {
      if (!step.constructorId) {
        return `${label}: choose a constructor.`;
      }
    } else if (!step.methodId) {
      return `${label}: choose a method.`;
    }

    if (composition) {
      const role = compositionNameRole(step, catalog);
      if (role === 'product' && !(step.instanceName?.trim())) {
        return `${label}: enter an instance name.`;
      }
      if (role === 'receiver' && !(step.instanceName?.trim())) {
        return `${label}: choose a receiver instance.`;
      }
    }

    const parameters = stepParameters(step, catalog);
    const values = parseParamsArray(step.params, parameters.length);
    for (let p = 0; p < parameters.length; p += 1) {
      const param = parameters[p];
      const value = values[p];
      const argLabel = param.name ? `${param.name}` : `argument ${p + 1}`;
      const objectArg = isRubricClassType(param.dataType, catalog);
      if (composition && objectArg) {
        if (!isInstanceRef(value) || !String(value.$instance || '').trim()) {
          return `${label}: choose a named instance for ${argLabel}.`;
        }
      } else if (value === null || value === undefined || value === '') {
        return `${label}: enter a value for ${argLabel}.`;
      }
    }
  }

  const assertions = hydrated.assertions || [];
  if (!assertions.length) {
    return 'Add at least one assertion before running.';
  }

  for (const assertion of assertions) {
    if (assertion.assertionKind === 'FIELD_STATE' && !assertion.fieldId) {
      return 'Each field assertion must select a field.';
    }
    if (composition && assertion.assertionKind === 'FIELD_STATE' && assertion.fieldId) {
      const field = catalog.fieldsById.get(assertion.fieldId);
      if (field && isRubricClassType(field.dataType, catalog)) {
        const instance = readInstanceExpected(assertion.expectedValue);
        if (!instance?.trim()) {
          return 'Each object field assertion must choose a named instance.';
        }
      }
    }
  }

  return null;
}

export function normalizeTestcaseForApi(tc) {
  const hydrated = hydrateTestcase(tc);
  const unit = hydrated.testcaseType !== 'COMPOSITION';
  const invocations = resolvedInvocations(hydrated).map((step) => normalizeInvocation(step, unit));
  return {
    id: hydrated.id,
    name: hydrated.name,
    testcaseType: unit ? 'UNIT' : 'COMPOSITION',
    orderIndex: hydrated.orderIndex ?? 0,
    hidden: !!hydrated.hidden,
    invocations,
    invocation: invocations[0] || null,
    instances: [],
    assertions: (hydrated.assertions || []).map((a, idx) => ({
      id: a.id,
      invocationId: a.invocationId || invocations[0]?.id || null,
      assertionKind: a.assertionKind,
      fieldId: a.assertionKind === 'FIELD_STATE' ? (a.fieldId || null) : null,
      expectedValue: a.expectedValue?.trim() ? a.expectedValue.trim() : 'null',
      comparisonMode: normalizeComparisonMode(a.comparisonMode || 'EXACT'),
      orderIndex: a.orderIndex ?? idx,
    })),
  };
}

export function coreTypeName(typeName) {
  if (!typeName || !String(typeName).trim()) return null;
  let trimmed = String(typeName).trim();
  if (trimmed.endsWith('[]')) return coreTypeName(trimmed.slice(0, -2));
  const genericStart = trimmed.indexOf('<');
  const genericEnd = trimmed.lastIndexOf('>');
  if (genericStart > 0 && genericEnd > genericStart) {
    return coreTypeName(trimmed.slice(genericStart + 1, genericEnd));
  }
  const dot = trimmed.lastIndexOf('.');
  return dot < 0 ? trimmed : trimmed.slice(dot + 1);
}

export function isArrayOrListType(typeName) {
  if (!typeName || !String(typeName).trim()) return false;
  const trimmed = String(typeName).trim();
  if (trimmed.endsWith('[]')) return true;
  const raw = trimmed.includes('<') ? trimmed.slice(0, trimmed.indexOf('<')).trim() : trimmed;
  const simple = coreTypeName(raw);
  return ['List', 'ArrayList', 'LinkedList', 'Collection', 'Set', 'HashSet'].includes(simple);
}

export function isVoidReturn(returnType) {
  return returnType == null || String(returnType).trim() === '' || String(returnType).trim().toLowerCase() === 'void';
}

export function isInstanceRef(value) {
  return Boolean(value && typeof value === 'object' && typeof value.$instance === 'string');
}

export function parseParamsArray(paramsJson, count) {
  let parsed;
  try {
    parsed = JSON.parse(paramsJson || '[]');
  } catch {
    parsed = [];
  }
  if (!Array.isArray(parsed)) parsed = [];
  const values = [];
  for (let i = 0; i < count; i += 1) {
    values.push(parsed[i] === undefined ? null : parsed[i]);
  }
  return values;
}

export function serializeParamsArray(values) {
  return JSON.stringify(values.map((value) => (value === undefined ? null : value)));
}

export function parseScalarInput(text) {
  const trimmed = String(text ?? '').trim();
  if (trimmed === '' || trimmed === 'null') return null;
  if (trimmed === 'true') return true;
  if (trimmed === 'false') return false;
  if (/^-?\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
  if (
    (trimmed.startsWith('[') && trimmed.endsWith(']'))
    || (trimmed.startsWith('{') && trimmed.endsWith('}'))
  ) {
    try {
      return JSON.parse(trimmed);
    } catch {
      return trimmed;
    }
  }
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
    try {
      return JSON.parse(trimmed);
    } catch {
      return trimmed.slice(1, -1);
    }
  }
  return trimmed;
}

export function displayScalar(value) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'string') return value;
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

export function paramSignature(parameters) {
  return (parameters || []).map((param) => param.dataType || '?').join(', ');
}

function heritageRelationKind(relationTypeOption) {
  const name = (relationTypeOption?.name || '').toLowerCase();
  if (name.includes('realiz') || name.includes('implement')) return 'heritage';
  if (name.includes('inherit') || name.includes('extend') || name.includes('general')) return 'heritage';
  return null;
}

function buildChildrenByParentId(relations, relationTypeOptions) {
  const childrenByParentId = new Map();
  (relations || []).forEach((relation) => {
    const option = (relationTypeOptions || []).find((item) => item.id === relation.relationTypeId);
    if (!heritageRelationKind(option)) return;
    const parentId = relation.targetClassId;
    const childId = relation.sourceClassId;
    if (!parentId || !childId) return;
    if (!childrenByParentId.has(parentId)) childrenByParentId.set(parentId, new Set());
    childrenByParentId.get(parentId).add(childId);
  });
  return childrenByParentId;
}

function collectDescendantClassIds(rootClassId, childrenByParentId) {
  const descendants = new Set();
  const stack = [...(childrenByParentId.get(rootClassId) || [])];
  while (stack.length > 0) {
    const id = stack.pop();
    if (descendants.has(id)) continue;
    descendants.add(id);
    (childrenByParentId.get(id) || []).forEach((childId) => stack.push(childId));
  }
  return descendants;
}

function buildParentsByChildId(childrenByParentId) {
  const parentsByChildId = new Map();
  (childrenByParentId || new Map()).forEach((children, parentId) => {
    (children || []).forEach((childId) => {
      if (!parentsByChildId.has(childId)) parentsByChildId.set(childId, new Set());
      parentsByChildId.get(childId).add(parentId);
    });
  });
  return parentsByChildId;
}

/** Extends/Implements ancestors of a class (not including itself). */
export function collectAncestorClassIds(rootClassId, childrenByParentId) {
  const parentsByChildId = buildParentsByChildId(childrenByParentId);
  const ancestors = new Set();
  const stack = [...(parentsByChildId.get(rootClassId) || [])];
  while (stack.length > 0) {
    const id = stack.pop();
    if (ancestors.has(id)) continue;
    ancestors.add(id);
    (parentsByChildId.get(id) || []).forEach((parentId) => stack.push(parentId));
  }
  return ancestors;
}

/** Call-as picker options for a named receiver’s concrete class. */
export function callAsOptionsForReceiver(receiverClassName, catalog) {
  const receiverId = catalog?.classIdByName?.get(receiverClassName);
  if (!receiverId) return [];
  const ancestorIds = collectAncestorClassIds(receiverId, catalog.childrenByParentId || new Map());
  const nameById = new Map();
  (catalog.classIdByName || new Map()).forEach((id, name) => {
    nameById.set(id, name);
  });
  return [...ancestorIds]
    .map((id) => ({ id, name: nameById.get(id) }))
    .filter((option) => option.name)
    .sort((a, b) => a.name.localeCompare(b.name));
}

/** True when an instance’s class is the method’s declaring class or a subclass/implementor. */
export function namedInstanceMatchesMethodReceiver(instanceClassName, methodClassName, catalog) {
  if (!instanceClassName || !methodClassName) return false;
  if (coreTypeName(instanceClassName) === coreTypeName(methodClassName)) return true;
  const methodClassId = catalog?.classIdByName?.get(coreTypeName(methodClassName));
  const instanceClassId = catalog?.classIdByName?.get(coreTypeName(instanceClassName));
  if (!methodClassId || !instanceClassId) return false;
  const descendants = collectDescendantClassIds(
    methodClassId,
    catalog.childrenByParentId || new Map(),
  );
  return descendants.has(instanceClassId);
}

/** Whether a named instance's concrete class can be passed where the parameter type is expected. */
export function namedInstanceMatchesParamType(instanceClassName, paramDataType, catalog) {
  const paramCore = coreTypeName(paramDataType);
  const instanceCore = coreTypeName(instanceClassName);
  if (!paramCore || !instanceCore) return false;
  if (paramCore === instanceCore) return true;
  const paramClassId = catalog?.classIdByName?.get(paramCore);
  const instanceClassId = catalog?.classIdByName?.get(instanceCore);
  if (!paramClassId || !instanceClassId) return false;
  const descendants = collectDescendantClassIds(
    paramClassId,
    catalog.childrenByParentId || new Map(),
  );
  return descendants.has(instanceClassId);
}

function declaringTypeKey(declaringTypeId, declaringTypeOptions) {
  const opt = (declaringTypeOptions || []).find((item) => item.id === declaringTypeId);
  return (opt?.name || '').trim().toLowerCase();
}

/** Concrete classes only — excludes interface/enum/annotation shells and abstract classes. */
export function isConcreteRubricClass(cls, declaringTypeOptions = []) {
  if (!cls) return false;
  if (cls.isAbstract) return false;
  const typeKey = declaringTypeKey(cls.declaringTypeId, declaringTypeOptions);
  if (typeKey === 'interface' || typeKey === 'enum' || typeKey === 'annotation') {
    return false;
  }
  return true;
}

export function buildMemberCatalog(
  challenge,
  relationTypeOptions = [],
  declaringTypeOptions = [],
) {
  const classes = challenge?.classes || [];
  const classNames = new Set(classes.map((cls) => cls.name).filter(Boolean));
  const classIdByName = new Map();
  classes.forEach((cls) => {
    if (cls.name) classIdByName.set(cls.name, cls.id);
  });
  const childrenByParentId = buildChildrenByParentId(challenge?.relations, relationTypeOptions);
  const constructors = [];
  const methods = [];
  const fields = [];
  const noArgClassIds = new Set();

  classes.forEach((cls) => {
    const concreteClass = isConcreteRubricClass(cls, declaringTypeOptions);
    (cls.constructors || []).forEach((ctor) => {
      if (!concreteClass) return;
      const parameters = ctor.parameters || [];
      const isNoArg = parameters.length === 0;
      if (isNoArg) noArgClassIds.add(cls.id);
      constructors.push({
        id: ctor.id,
        classId: cls.id,
        className: cls.name,
        label: `${cls.name}(${paramSignature(parameters)})`,
        parameters,
        isNoArg,
      });
    });
    (cls.methods || []).forEach((method) => {
      if (!concreteClass || method.isAbstract) return;
      const parameters = method.parameters || [];
      methods.push({
        id: method.id,
        classId: cls.id,
        className: cls.name,
        name: method.name,
        label: `${cls.name}.${method.name}(${paramSignature(parameters)})`,
        parameters,
        returnType: method.returnType,
        isStatic: !!method.isStatic,
        isAbstract: false,
      });
    });
    (cls.fields || []).forEach((field) => {
      fields.push({
        id: field.id,
        classId: cls.id,
        className: cls.name,
        name: field.name,
        dataType: field.dataType,
        label: `${cls.name}.${field.name}`,
      });
    });
  });

  return {
    classNames,
    classIdByName,
    childrenByParentId,
    noArgClassIds,
    constructors,
    methods,
    fields,
    constructorsById: new Map(constructors.map((item) => [item.id, item])),
    methodsById: new Map(methods.map((item) => [item.id, item])),
    fieldsById: new Map(fields.map((item) => [item.id, item])),
  };
}

export function isRubricClassType(typeName, catalog) {
  const core = coreTypeName(typeName);
  return Boolean(core && catalog.classNames.has(core));
}

export function hasRubricClassArgument(parameters, catalog) {
  return (parameters || []).some((param) => isRubricClassType(param.dataType, catalog));
}

export function unitMemberBlockedReason(target, catalog) {
  if (!target) return null;
  if (hasRubricClassArgument(target.parameters, catalog)) {
    return 'Object arguments belong in Composition.';
  }
  return null;
}

export function unitTargets(catalog) {
  const ctorTargets = catalog.constructors.map((ctor) => ({ ...ctor, kind: 'CONSTRUCTOR' }));
  const methodTargets = catalog.methods.map((method) => ({ ...method, kind: 'METHOD' }));
  return [...ctorTargets, ...methodTargets];
}

/** Members lecturers can pick for a UNIT testcase (excludes Composition-only shapes). */
export function unitSelectableTargets(catalog) {
  return unitTargets(catalog).filter((item) => !unitMemberBlockedReason(item, catalog));
}

export function selectedUnitTarget(step, catalog) {
  if (!step) return null;
  if (step.invocationKind === 'CONSTRUCTOR') {
    const ctor = catalog.constructorsById.get(step.constructorId);
    return ctor ? { ...ctor, kind: 'CONSTRUCTOR' } : null;
  }
  const method = catalog.methodsById.get(step.methodId);
  return method ? { ...method, kind: 'METHOD' } : null;
}

export function applyUnitTarget(step, key, catalog) {
  const [kind, id] = String(key).split(':');
  if (kind === 'CONSTRUCTOR') {
    const ctor = catalog.constructorsById.get(id);
    return {
      ...step,
      invocationKind: 'CONSTRUCTOR',
      constructorId: id || null,
      methodId: null,
      instanceName: '',
      receiverConstructorId: null,
      receiverParams: '[]',
      dispatchClassId: null,
      params: serializeParamsArray(parseParamsArray('[]', ctor?.parameters?.length || 0)),
    };
  }
  const method = catalog.methodsById.get(id);
  return {
    ...step,
    invocationKind: 'METHOD',
    methodId: id || null,
    constructorId: null,
    instanceName: '',
    receiverConstructorId: null,
    receiverParams: '[]',
    dispatchClassId: null,
    params: serializeParamsArray(parseParamsArray('[]', method?.parameters?.length || 0)),
  };
}

export function stepParameters(step, catalog) {
  if (step?.invocationKind === 'CONSTRUCTOR') {
    return catalog.constructorsById.get(step.constructorId)?.parameters || [];
  }
  return catalog.methodsById.get(step.methodId)?.parameters || [];
}

export function isObjectReturnStep(step, catalog) {
  if (!step) return false;
  if (step.invocationKind === 'CONSTRUCTOR') return Boolean(step.constructorId);
  const method = catalog.methodsById.get(step.methodId);
  if (!method || isVoidReturn(method.returnType)) return false;
  return isRubricClassType(method.returnType, catalog);
}

export function resultClassName(step, catalog) {
  if (!step) return null;
  if (step.invocationKind === 'CONSTRUCTOR') {
    return catalog.constructorsById.get(step.constructorId)?.className || null;
  }
  const method = catalog.methodsById.get(step.methodId);
  return method ? coreTypeName(method.returnType) : null;
}

export function classIdForClassName(className, catalog) {
  if (!className) return null;
  const fromClass = catalog.classIdByName?.get(className);
  if (fromClass) return fromClass;
  const ctor = catalog.constructors.find((item) => item.className === className);
  if (ctor) return ctor.classId;
  const method = catalog.methods.find((item) => item.className === className);
  return method?.classId ?? null;
}

function fieldOwnerClassIdsForStep(step, catalog) {
  if (!step) return [];
  if (step.invocationKind === 'CONSTRUCTOR') {
    const classId = catalog.constructorsById.get(step.constructorId)?.classId ?? null;
    return classId ? [classId] : [];
  }
  const method = catalog.methodsById.get(step.methodId);
  if (!method) return [];
  if (isRubricClassType(method.returnType, catalog) && !isVoidReturn(method.returnType)) {
    const returnName = coreTypeName(method.returnType);
    const rootId = classIdForClassName(returnName, catalog);
    if (!rootId) return [];
    const ids = new Set([rootId]);
    collectDescendantClassIds(rootId, catalog.childrenByParentId || new Map())
      .forEach((id) => ids.add(id));
    return [...ids];
  }
  return method.classId ? [method.classId] : [];
}

/** Rubric fields eligible for FIELD_STATE on this step (constructed type, return type, or receiver). */
export function fieldsForStepAssertion(step, catalog) {
  const ownerIds = new Set(fieldOwnerClassIdsForStep(step, catalog));
  if (ownerIds.size === 0) return [];
  return catalog.fields.filter((field) => ownerIds.has(field.classId));
}

export function allowedAssertionKinds(step, catalog, testcaseType = 'UNIT') {
  if (!step) return ['FIELD_STATE', 'EXCEPTION'];
  if (step.invocationKind === 'CONSTRUCTOR') {
    return ['FIELD_STATE', 'EXCEPTION'];
  }
  const method = catalog.methodsById.get(step.methodId);
  if (method && isVoidReturn(method.returnType)) {
    return ['STDOUT', 'FIELD_STATE', 'EXCEPTION'];
  }
  if (isObjectReturnStep(step, catalog)) {
    return ['RETURN_VALUE', 'FIELD_STATE', 'EXCEPTION'];
  }
  return ['RETURN_VALUE', 'STDOUT', 'FIELD_STATE', 'EXCEPTION'];
}

export function namedInstancesBefore(steps, catalog, index) {
  const named = [];
  const used = new Set();
  (steps || []).slice(0, index).forEach((step) => {
    const name = step.instanceName?.trim();
    if (!name || used.has(name)) return;
    if (step.invocationKind === 'CONSTRUCTOR') {
      const ctor = catalog.constructorsById.get(step.constructorId);
      used.add(name);
      named.push({ name, className: ctor?.className || '' });
      return;
    }
    const method = catalog.methodsById.get(step.methodId);
    if (method?.isStatic && isRubricClassType(method.returnType, catalog)) {
      used.add(name);
      named.push({ name, className: coreTypeName(method.returnType) });
    }
  });
  return named;
}

export function compositionNameRole(step, catalog) {
  if (!step) return null;
  if (step.invocationKind === 'CONSTRUCTOR') return 'product';
  const method = catalog.methodsById.get(step.methodId);
  if (!method) return null;
  if (method.isStatic) {
    return isRubricClassType(method.returnType, catalog) ? 'product' : null;
  }
  return 'receiver';
}

export function readObjectCheck(expectedValue) {
  try {
    const node = JSON.parse(expectedValue || '');
    if (node && typeof node === 'object' && typeof node.$objectCheck === 'string') {
      if (node.$objectCheck === 'EQUALS') {
        return {
          kind: 'EQUALS',
          fields: {},
          instance: typeof node.$instance === 'string' ? node.$instance : '',
        };
      }
      return null;
    }
  } catch {
    // scalar expected values are not object checks
  }
  return null;
}

export function writeObjectCheck(kind, { instance = '' } = {}) {
  return JSON.stringify({ $objectCheck: 'EQUALS', $instance: instance });
}

export function readInstanceExpected(expectedValue) {
  try {
    const node = JSON.parse(expectedValue || '');
    if (isInstanceRef(node)) return node.$instance;
  } catch {
    // scalar
  }
  return null;
}

export function fieldsForClass(catalog, className) {
  if (!className) return catalog.fields;
  return catalog.fields.filter((field) => field.className === className);
}

export function unitTargetKey(step) {
  if (!step) return '';
  if (step.invocationKind === 'CONSTRUCTOR') {
    return step.constructorId ? `CONSTRUCTOR:${step.constructorId}` : '';
  }
  return step.methodId ? `METHOD:${step.methodId}` : '';
}
