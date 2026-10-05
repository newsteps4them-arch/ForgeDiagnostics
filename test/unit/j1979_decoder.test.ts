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

  it('should decode MAP Intake Pressure correctly (PID 0B)', () => {
    const result = decodeMode01Response('41 0B 64');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0B');
    expect(result?.name).toBe('Intake Manifold Pressure');
    expect(result?.value).toBe(100);
    expect(result?.unit).toBe('kPa');
  });

  it('should decode Timing Advance correctly (PID 0E)', () => {
    const result = decodeMode01Response('41 0E A0');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('0E');
    expect(result?.name).toBe('Timing Advance');
    expect(result?.value).toBe(16);
    expect(result?.unit).toBe('°');
  });

  it('should decode MAF Air Flow Rate correctly (PID 10)', () => {
    const result = decodeMode01Response('41 10 09 C4');
    expect(result).not.toBeNull();
    expect(result?.pid).toBe('10');
    expect(result?.name).toBe('MAF Air Flow Rate');
    expect(result?.value).toBe(25);
    expect(result?.unit).toBe('g/s');
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

  it('should reject empty and whitespace-only Mode 01 frames', () => {
    expect(decodeMode01Response('')).toBeNull();
    expect(decodeMode01Response(' \r\n\t> ')).toBeNull();
  });

  it('should reject non-ASCII and odd-length hex input without throwing', () => {
    expect(() => decodeMode01Response('41 0D 37🙂')).not.toThrow();
    expect(decodeMode01Response('41 0D 37🙂')).toBeNull();
    expect(decodeMode01Response('41 0D 3')).toBeNull();
  });

  it('should decode signed and maximum single-byte PID boundaries', () => {
    expect(decodeMode01Response('41 05 00')?.value).toBe(-40);
    expect(decodeMode01Response('41 0F FF')?.value).toBe(215);
    expect(decodeMode01Response('41 0E 00')?.value).toBe(-64);
  });

  it('should reject Mode 09 responses with no usable ASCII payload', () => {
    expect(decodeMode09Response('49 02')).toBeNull();
    expect(decodeMode09Response('49 02 00')).toBeNull();
    expect(decodeMode09Response('49 02 00 00')).toBeNull();
  });

  it('should preserve supported-PID ordering across multiple mask bytes', () => {
    expect(decodeSupportedPidMask('80000001 00000080')).toEqual(['01', '20', '39']);
  });

  it('should reject Mode 03 frames with an incomplete DTC pair', () => {
    expect(decodeMode03Response('43 01 33 03')).toEqual([]);
  });
});
