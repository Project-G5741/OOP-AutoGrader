const INTERACTIVE_ROW_CHILD_SELECTOR =
  'button, a, input, select, textarea, label, [role="button"], [role="checkbox"], [role="combobox"], [role="switch"]';

/**
 * Returns true when an event originated inside an interactive control within a row
 * (such as Delete, Suspend, Toggle, or Distribute buttons) so row-level double-click
 * handlers do not misfire on action controls.
 */
export function isRowInteractiveTarget(target, rowElement = null) {
  let node = target;
  if (node && typeof node.closest !== 'function' && node.parentElement) {
    node = node.parentElement;
  }
  if (!node || typeof node.closest !== 'function') {
    return false;
  }
  const interactive = node.closest(INTERACTIVE_ROW_CHILD_SELECTOR);
  if (!interactive) {
    return false;
  }
  if (rowElement && interactive === rowElement) {
    return false;
  }
  return true;
}

/**
 * Invokes `onEdit` on a row double-click unless the user double-clicked a child control.
 */
export function handleRowDoubleClickEdit(event, onEdit) {
  if (typeof onEdit !== 'function') {
    return;
  }
  if (isRowInteractiveTarget(event?.target, event?.currentTarget)) {
    return;
  }
  onEdit(event);
}
