import { describe, it, expect } from 'vitest';
import {
  DIALOG_LEVELS,
  DIALOG_CONFIG,
  resolveDialogConfig,
} from '../../config/dialogConfig';

describe('dialogConfig', () => {
  it('defines 3 distinct severity levels: safety, warning, dangerous', () => {
    expect(DIALOG_LEVELS.SAFETY).toBe('safety');
    expect(DIALOG_LEVELS.WARNING).toBe('warning');
    expect(DIALOG_LEVELS.DANGEROUS).toBe('dangerous');

    expect(DIALOG_CONFIG.safety).toBeDefined();
    expect(DIALOG_CONFIG.warning).toBeDefined();
    expect(DIALOG_CONFIG.dangerous).toBeDefined();
  });

  it('correctly maps safety tier properties', () => {
    const config = resolveDialogConfig('safety');
    expect(config.id).toBe('safety');
    expect(config.label).toBe('Safety');
    expect(config.icon).toBeDefined();
    expect(config.iconColor).toContain('secondary');
    expect(config.borderAccent).toContain('border-l-secondary');
    expect(config.defaultTitle).toBe('Notice');
  });

  it('correctly maps warning tier properties', () => {
    const config = resolveDialogConfig('warning');
    expect(config.id).toBe('warning');
    expect(config.label).toBe('Warning');
    expect(config.icon).toBeDefined();
    expect(config.iconColor).toContain('warning');
    expect(config.borderAccent).toContain('border-l-warning');
    expect(config.confirmButtonClass).toContain('bg-warning');
    expect(config.defaultTitle).toBe('Warning');
  });

  it('correctly maps dangerous tier properties', () => {
    const config = resolveDialogConfig('dangerous');
    expect(config.id).toBe('dangerous');
    expect(config.label).toBe('Dangerous');
    expect(config.icon).toBeDefined();
    expect(config.iconColor).toContain('error');
    expect(config.borderAccent).toContain('border-l-error');
    expect(config.confirmButtonClass).toContain('bg-error');
    expect(config.defaultTitle).toBe('Confirm Action');
  });

  it('normalizes case and handles common aliases', () => {
    expect(resolveDialogConfig('DANGEROUS').id).toBe('dangerous');
    expect(resolveDialogConfig('destructive').id).toBe('dangerous');
    expect(resolveDialogConfig('error').id).toBe('dangerous');
    expect(resolveDialogConfig('danger').id).toBe('dangerous');

    expect(resolveDialogConfig('WARNING').id).toBe('warning');
    expect(resolveDialogConfig('warn').id).toBe('warning');
    expect(resolveDialogConfig('caution').id).toBe('warning');

    expect(resolveDialogConfig('SAFETY').id).toBe('safety');
    expect(resolveDialogConfig('safe').id).toBe('safety');
    expect(resolveDialogConfig('unknown-level').id).toBe('safety');
    expect(resolveDialogConfig(null).id).toBe('safety');
  });
});
