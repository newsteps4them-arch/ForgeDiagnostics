import { describe, it, expect } from 'vitest';
import { decodeMode01Response } from '../../src/protocols/j1979_decoder';

describe('SAE J1979 Protocol Decoder', () => {
  it('should decode Engine RPM correctly (PID 0C)', () => {
    // 41 0C 0F A0 -> 41 (Mode), 0C (PID), 0F A0 (Bytes) -> (39936 + 160) / 4 = 10024 / 4 = 2506? Wait. 0x0F = 15, 0xA0 = 160. (15*256)+160 = 3840 + 160 = 4000. 4000 / 4 = 1000 RPM
    const result = decodeMode01Response('41 0C 0F A0');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0C');
    expect(result?.name).toBe('Engine RPM');
    expect(result?.value).toBe(1000);
    expect(result?.unit).toBe('RPM');
  });

  it('should decode Vehicle Speed correctly (PID 0D)', () => {
    // 41 0D 37 -> 0x37 = 55 km/h
    const result = decodeMode01Response('41 0D 37');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0D');
    expect(result?.name).toBe('Vehicle Speed');
    expect(result?.value).toBe(55);
    expect(result?.unit).toBe('km/h');
  });

  it('should decode Coolant Temperature correctly (PID 05)', () => {
    // 41 05 7B -> 0x7B = 123. 123 - 40 = 83 deg C
    const result = decodeMode01Response('41 05 7B');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('05');
    expect(result?.name).toBe('Coolant Temperature');
    expect(result?.value).toBe(83);
    expect(result?.unit).toBe('°C');
  });

  it('should return raw data for unknown PID', () => {
    // 41 99 10
    const result = decodeMode01Response('41 99 10');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('99');
    expect(result?.name).toBe('PID_99');
    expect(result?.value).toBe(16);
    expect(result?.unit).toBe('raw');
  });

  it('should return null for invalid format', () => {
    const result = decodeMode01Response('INVALID');
    expect(result).toBeNull();
  });
});
