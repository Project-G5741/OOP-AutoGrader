export const MAX_STEPS = 20;
export const MAX_NAMED_INSTANCES = 10;
export const COMPARISON_MODES = ['EXACT', 'TRIMMED', 'NORMALIZED_WHITESPACE'];

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

export function defaultExpectedValue(kind, objectCheck = false) {
  if (kind === 'EXCEPTION') return '"IllegalArgumentException"';
  if (kind === 'STDOUT') return '""';
  if (kind === 'RETURN_VALUE' && objectCheck) {
    return JSON.stringify({ $objectCheck: 'TYPE' });
  }
  if (kind === 'RETURN_VALUE') return 'null';
  return '0';
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
    dispatchClassId: null,
  };
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
      comparisonMode: a.comparisonMode || 'EXACT',
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

export function buildMemberCatalog(challenge) {
  const classes = challenge?.classes || [];
  const classNames = new Set(classes.map((cls) => cls.name).filter(Boolean));
  const constructors = [];
  const methods = [];
  const fields = [];
  const noArgClassIds = new Set();

  classes.forEach((cls) => {
    (cls.constructors || []).forEach((ctor) => {
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
  if (target.kind === 'METHOD' && !target.isStatic && !catalog.noArgClassIds.has(target.classId)) {
    return 'Instance methods need a no-arg constructor. Use Composition.';
  }
  return null;
}

export function unitTargets(catalog) {
  const ctorTargets = catalog.constructors.map((ctor) => ({ ...ctor, kind: 'CONSTRUCTOR' }));
  const methodTargets = catalog.methods.map((method) => ({ ...method, kind: 'METHOD' }));
  return [...ctorTargets, ...methodTargets];
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

export function allowedAssertionKinds(step, catalog) {
  if (!step) return ['FIELD_STATE', 'EXCEPTION'];
  if (step.invocationKind === 'CONSTRUCTOR') {
    return ['RETURN_VALUE', 'FIELD_STATE', 'EXCEPTION'];
  }
  const method = catalog.methodsById.get(step.methodId);
  if (method && isVoidReturn(method.returnType)) {
    return ['STDOUT', 'FIELD_STATE', 'EXCEPTION'];
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
      return {
        kind: node.$objectCheck,
        fields: node.fields && typeof node.fields === 'object' ? node.fields : {},
        instance: typeof node.$instance === 'string' ? node.$instance : '',
      };
    }
  } catch {
    // scalar expected values are not object checks
  }
  return null;
}

export function writeObjectCheck(kind, { fields = {}, instance = '' } = {}) {
  if (kind === 'FIELDS') {
    return JSON.stringify({ $objectCheck: 'FIELDS', fields });
  }
  if (kind === 'EQUALS') {
    return JSON.stringify({ $objectCheck: 'EQUALS', $instance: instance });
  }
  return JSON.stringify({ $objectCheck: 'TYPE' });
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
