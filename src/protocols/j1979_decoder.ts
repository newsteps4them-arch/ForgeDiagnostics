// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

/**
 * ForgeDiagnostic - SAE J1979 Mode 01 & Mode 03 Decoder
 */

export interface DecodedPid {
  pid: string;
  name: string;
  value: number | string;
  unit: string;
}

function parseHexBytes(hexString: string): number[] | null {
  const clean = hexString.replace(/[\s\r\n>]/g, '').toUpperCase();
  if (!clean || clean.length % 2 !== 0 || !/^[0-9A-F]+$/.test(clean)) return null;

  const bytes: number[] = [];
  for (let i = 0; i < clean.length; i += 2) {
    const byte = Number.parseInt(clean.slice(i, i + 2), 16);
    if (Number.isNaN(byte)) return null;
    bytes.push(byte);
  }

  return bytes;
}

function decodeAsciiPayload(bytes: number[]): string {
  const cleaned = bytes.filter((value) => value !== 0x00);
  return String.fromCharCode(...cleaned);
}

export function decodeMode01Response(hexString: string): DecodedPid | null {
  const clean = hexString.replace(/[\s\r\n>]/g, '').toUpperCase();
  if (!clean.startsWith('41') || clean.length < 6) return null;

  const pid = clean.slice(2, 4);
  const rawBytes = clean.slice(4);
  const bytes = parseHexBytes(rawBytes);
  if (!pid || !bytes || bytes.length === 0) return null;

  const A = bytes[0];
  const B = bytes[1];

  switch (pid) {
    case '0C': // Engine RPM
      if (bytes.length < 2 || A === undefined || B === undefined) return null;
      return { pid: '0C', name: 'Engine RPM', value: ((A * 256) + B) / 4, unit: 'RPM' };
    case '0D': // Vehicle Speed
      if (A === undefined) return null;
      return { pid: '0D', name: 'Vehicle Speed', value: A, unit: 'km/h' };
    case '05': // Coolant Temp
      if (A === undefined) return null;
      return { pid: '05', name: 'Coolant Temperature', value: A - 40, unit: '°C' };
    case '0F': // Intake Air Temp
      if (A === undefined) return null;
      return { pid: '0F', name: 'Intake Air Temp', value: A - 40, unit: '°C' };
    case '04': // Calculated Load
      if (A === undefined) return null;
      return { pid: '04', name: 'Engine Load', value: (A * 100) / 255, unit: '%' };
    case '11': // Throttle Position
      if (A === undefined) return null;
      return { pid: '11', name: 'Throttle Position', value: (A * 100) / 255, unit: '%' };
    case '2F': // Fuel Tank Level
      if (A === undefined) return null;
      return { pid: '2F', name: 'Fuel Tank Level', value: (A * 100) / 255, unit: '%' };
    default:
      if (A === undefined) return null;
      return { pid, name: `PID_${pid}`, value: A, unit: 'raw' };
  }
}

export function decodeMode09Response(hexString: string): DecodedPid | null {
  const clean = hexString.replace(/[\s\r\n>]/g, '').toUpperCase();
  if (!clean.startsWith('49') || clean.length < 6) return null;

  const pid = clean.slice(2, 4);
  const rawBytes = clean.slice(4);
  const bytes = parseHexBytes(rawBytes);
  if (!pid || !bytes || bytes.length === 0) return null;

  const payload = bytes[0] === 0x00 ? bytes.slice(1) : bytes;
  if (payload.length === 0) return null;

  const ascii = decodeAsciiPayload(payload);
  if (!ascii) return null;

  switch (pid) {
    case '02':
      return { pid: '02', name: 'VIN', value: ascii, unit: 'ascii' };
    case '0A':
      return { pid: '0A', name: 'ECU Name', value: ascii, unit: 'ascii' };
    default:
      return { pid, name: `PID_${pid}`, value: ascii, unit: 'ascii' };
  }
}

export function decodeSupportedPidMask(hexMask: string): string[] {
  const bytes = parseHexBytes(hexMask);
  if (!bytes || bytes.length === 0) return [];

  const pids: string[] = [];

  for (let bit = 0; bit < bytes.length * 8; bit += 1) {
    const byteIndex = Math.floor(bit / 8);
    const bitIndex = 7 - (bit % 8);
    const byte = bytes[byteIndex];
    if (byte === undefined) continue;

    const mask = 1 << bitIndex;
    if ((byte & mask) !== 0) {
      const pidNumber = bit + 1;
      pids.push(pidNumber.toString(16).padStart(2, '0').toUpperCase());
    }
  }

  return pids;
}

export function decodeMode03Response(hexString: string): string[] {
  const clean = hexString.replace(/[\s\r\n>]/g, '').toUpperCase();
  if (!clean || !clean.startsWith('43') || !/^[0-9A-F]+$/.test(clean)) return [];

  const payload = clean.slice(2);
  if (!payload || payload.length % 4 !== 0) return [];

  const dtcs: string[] = [];
  for (let index = 0; index + 4 <= payload.length; index += 4) {
    const codeHex = payload.slice(index, index + 4);
    if (codeHex === '0000') continue;

    const firstNibble = Number.parseInt(codeHex[0], 16);
    const group = (() => {
      const firstByte = firstNibble >> 2;
      switch (firstByte) {
        case 0:
          return 'P';
        case 1:
          return 'C';
        case 2:
          return 'B';
        case 3:
          return 'U';
        default:
          return 'P';
      }
    })();

    const digit = (firstNibble & 0x03).toString();
    const tail = codeHex.slice(1);
    dtcs.push(`${group}${digit}${tail}`);
  }

  return dtcs;
}
