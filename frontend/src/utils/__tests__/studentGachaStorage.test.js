import { describe, expect, it, beforeEach } from 'vitest';
import { purgeLegacyGachaLocalStorage } from '../studentGachaStorage';

describe('studentGachaStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('removes legacy gacha keys from localStorage', () => {
    localStorage.setItem('gacha_demo_crystals', '99');
    localStorage.setItem('oop_student_owned_mascots', '["fox","bunny"]');
    purgeLegacyGachaLocalStorage();
    expect(localStorage.getItem('gacha_demo_crystals')).toBeNull();
    expect(localStorage.getItem('oop_student_owned_mascots')).toBeNull();
  });
});