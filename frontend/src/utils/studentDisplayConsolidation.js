/** Collapse repeated student-facing error rows (one message per mistake category). */

const MISSING_VARIABLE = 'Missing variable name or datatype';
const WRONG_VARIABLE = 'Wrong variable name or datatype';
const MISSING_CONSTRUCTOR = 'Missing constructor name or parameters';
const WRONG_CONSTRUCTOR = 'Wrong constructor name or parameters';
const MISSING_METHOD = 'Missing method name or return type';
const WRONG_METHOD = 'Wrong method name or return type';
const WRONG_SCOPE = 'Missing or wrong scope or modifier';
const PLACEHOLDER = '—';

const MMD_MISSING_BY_TYPE = {
  field: MISSING_VARIABLE,
  constructor: MISSING_CONSTRUCTOR,
  method: MISSING_METHOD,
  stereotype: 'Required diagram element',
};

const MMD_WRONG_BY_TYPE = {
  field: WRONG_VARIABLE,
  constructor: WRONG_CONSTRUCTOR,
  method: WRONG_METHOD,
  stereotype: WRONG_VARIABLE,
};

function classMemberErrorKey(item) {
  const name = item.name ?? '';
  const partial = Boolean(item.partial);

  if (name === MISSING_VARIABLE) return 'field:missing';
  if (name === WRONG_VARIABLE) return partial ? 'field:partial' : 'field:wrong';
  if (name === MISSING_CONSTRUCTOR) return 'constructor:missing';
  if (name === WRONG_CONSTRUCTOR) return partial ? 'constructor:partial' : 'constructor:wrong';
  if (name === MISSING_METHOD) return 'method:missing';
  if (name === WRONG_METHOD) return partial ? 'method:partial' : 'method:wrong';

  return `unique:${name}:${item.scope ?? ''}:${item.dataType ?? ''}:${item.params ?? ''}:${item.returnType ?? ''}:${partial}`;
}

function consolidateFailedItems(items, errorKeyFn) {
  const seen = new Set();
  const consolidated = [];

  for (const item of items) {
    const key = errorKeyFn(item);
    if (seen.has(key)) continue;
    seen.add(key);
    consolidated.push(item);
  }

  return consolidated;
}

export function consolidateStudentClassMembers(items) {
  if (!items?.length) return [];

  const passing = items.filter((item) => item.ok);
  const failed = items.filter((item) => !item.ok);

  return [...passing, ...consolidateFailedItems(failed, classMemberErrorKey)];
}

function mmdAttributeErrorKey(attribute) {
  if (attribute.ok) return `ok:${attribute.name}`;

  const type = attribute.type ?? 'unknown';
  const error = attribute.error ?? '';
  const name = attribute.name ?? '';

  if (error && error !== name) {
    return `${type}:${error}`;
  }

  if (Object.values(MMD_MISSING_BY_TYPE).includes(name)) {
    return `${type}:missing`;
  }

  if (Object.values(MMD_WRONG_BY_TYPE).includes(name)) {
    return `${type}:wrong`;
  }

  if (name.startsWith('Missing ')) return `${type}:missing`;
  if (name.startsWith('Wrong ')) return `${type}:wrong`;

  return `${type}:wrong`;
}

function canonicalMmdAttribute(attribute, bucketKey) {
  const type = attribute.type ?? 'unknown';

  if (bucketKey.endsWith(':missing')) {
    const name = MMD_MISSING_BY_TYPE[type] ?? attribute.name;
    return { ...attribute, name, error: name };
  }

  if (bucketKey.endsWith(':wrong')) {
    const name = MMD_WRONG_BY_TYPE[type] ?? attribute.name;
    return { ...attribute, name, error: name };
  }

  return attribute;
}

export function consolidateStudentMmdAttributes(attributes) {
  if (!attributes?.length) return [];

  const passing = attributes.filter((attribute) => attribute.ok);
  const failed = attributes.filter((attribute) => !attribute.ok);
  const seen = new Set();
  const consolidatedFailed = [];

  for (const attribute of failed) {
    const key = mmdAttributeErrorKey(attribute);
    if (seen.has(key)) continue;
    seen.add(key);
    consolidatedFailed.push(canonicalMmdAttribute(attribute, key));
  }

  return [...passing, ...consolidatedFailed];
}

export function consolidateStudentMmdRelations(relations) {
  if (!relations?.length) return [];

  const passing = relations.filter((relation) => relation.ok);
  const failed = relations.filter((relation) => !relation.ok);
  const seen = new Set();
  const consolidatedFailed = [];

  for (const relation of failed) {
    const key = relation.error ?? `${relation.from}|${relation.to}|${relation.relType}`;
    if (seen.has(key)) continue;
    seen.add(key);
    consolidatedFailed.push(relation);
  }

  return [...passing, ...consolidatedFailed];
}

export function isStudentGenericMessage(text) {
  if (!text) return false;
  return text.startsWith('Missing ')
    || text.startsWith('Wrong ')
    || text === 'Required diagram element'
    || text === 'Required relationship'
    || text === 'Relationship mismatch';
}

export function formatStudentFieldLine(field) {
  if (isStudentGenericMessage(field.name) && (!field.dataType || field.dataType === PLACEHOLDER)) {
    return field.name;
  }
  return `${field.name}: ${field.dataType || PLACEHOLDER}`;
}

export function formatStudentConstructorLine(constructor) {
  if (isStudentGenericMessage(constructor.name)
    && (!constructor.params || constructor.params === PLACEHOLDER)) {
    return constructor.name;
  }
  return `${constructor.name}(${constructor.params || PLACEHOLDER})`;
}

export function formatStudentMethodLine(method) {
  if (isStudentGenericMessage(method.name)
    && (!method.returnType || method.returnType === PLACEHOLDER)) {
    return method.name;
  }
  return `${method.name}(): ${method.returnType || PLACEHOLDER}`;
}

export function formatStudentScopeLine(scope) {
  if (!scope || scope === PLACEHOLDER) return null;
  if (isStudentGenericMessage(scope)) return scope;
  return scope;
}
