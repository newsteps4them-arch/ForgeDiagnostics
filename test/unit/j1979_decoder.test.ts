// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

import { describe, it, expect } from 'vitest';
import {
  decodeMode01Response,
  decodeMode03Response,
  decodeMode09Response,
  decodeSupportedPidMask,
} from '../../src/protocols/j1979_decoder';

describe('SAE J1979 Protocol Decoder', () => {
  it('should decode Engine RPM correctly (PID 0C)', () => {
    const result = decodeMode01Response('41 0C 0F A0');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0C');
    expect(result?.name).toBe('Engine RPM');
    expect(result?.value).toBe(1000);
    expect(result?.unit).toBe('RPM');
  });

  it('should decode Vehicle Speed correctly (PID 0D)', () => {
    const result = decodeMode01Response('41 0D 37');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0D');
    expect(result?.name).toBe('Vehicle Speed');
    expect(result?.value).toBe(55);
    expect(result?.unit).toBe('km/h');
  });

  it('should decode Coolant Temperature correctly (PID 05)', () => {
    const result = decodeMode01Response('41 05 7B');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('05');
    expect(result?.name).toBe('Coolant Temperature');
    expect(result?.value).toBe(83);
    expect(result?.unit).toBe('°C');
  });

  it.each([
    ['41 04 00', '04', 0],
    ['41 0F FF', '0F', 215],
    ['41 11 FF', '11', 100],
    ['41 2F FF', '2F', 100],
  ])('should decode single-byte PID boundaries: %s', (response, pid, value) => {
    const result = decodeMode01Response(response);
    expect(result?.pid).toBe(pid);
    expect(result?.value).toBeCloseTo(value, 5);
  });

  it('should return raw data for unknown PID', () => {
    const result = decodeMode01Response('41 99 10');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('99');
    expect(result?.name).toBe('PID_99');
    expect(result?.value).toBe(16);
    expect(result?.unit).toBe('raw');
  });

  it('should return null for invalid format', () => {
    expect(decodeMode01Response('INVALID')).toBeNull();
  });

  it('should reject diagnostic noise that contains a response-looking substring', () => {
    expect(decodeMode01Response('NO DATA 410D37')).toBeNull();
    expect(decodeMode01Response('41 0D GG')).toBeNull();
  });

  it('should return null for a truncated multi-byte PID response', () => {
    expect(decodeMode01Response('41 0C 0F')).toBeNull();
  });

  it('should ignore adapter whitespace and prompt framing', () => {
    expect(decodeMode01Response('\r\n41 11 80\r\n>')?.value).toBeCloseTo(50.196, 3);
  });

  it('should decode VIN data from Mode 09 responses', () => {
    const result = decodeMode09Response('4902005445535456494e30313233343536373839');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('02');
    expect(result?.name).toBe('VIN');
    expect(result?.value).toBe('TESTVIN0123456789');
    expect(result?.unit).toBe('ascii');
  });

  it('should decode ECU name data from Mode 09 responses', () => {
    const result = decodeMode09Response('490A000045435553696D');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0A');
    expect(result?.name).toBe('ECU Name');
    expect(result?.value).toBe('ECUSim');
    expect(result?.unit).toBe('ascii');
  });

  it('should decode supported PID bitmasks into PID numbers in J1979 order', () => {
    expect(decodeSupportedPidMask('0000001F')).toEqual(['1C', '1D', '1E', '1F', '20']);
    expect(decodeSupportedPidMask('00000020')).toEqual(['1B']);
  });

  it('should decode stored DTCs from a Mode 03 response', () => {
    expect(decodeMode03Response('43 01 33 03 00 00 00')).toEqual(['P0133', 'P0300']);
  });

  it('should ignore empty DTC entries in a Mode 03 response', () => {
    expect(decodeMode03Response('43 00 00 00 00')).toEqual([]);
  });

  it('should reject malformed Mode 03 payloads', () => {
    expect(decodeMode03Response('43 GG00')).toEqual([]);
    expect(decodeMode03Response('43 01 3')).toEqual([]);
    expect(decodeMode03Response('NO DATA 43 01 33')).toEqual([]);
  });

  it('should reject non-response Mode 09 noise', () => {
    expect(decodeMode09Response('NO DATA 49020054455354')).toBeNull();
  });
});
