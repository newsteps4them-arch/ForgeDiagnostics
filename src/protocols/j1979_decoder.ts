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

// Pre-computed lookup tables for hex string formatting ("00".."FF" and "0".."F")
const HEX_BYTE_TABLE: string[] = new Array(256);
const HEX_NIBBLE_TABLE: string[] = new Array(16);

// Fast ASCII lookup array for character parsing:
// -2: whitespace (\s, \r, \n, \t) or prompt delimiter (>)
// -1: invalid non-hex character
// 0..15: hex nibble value
const HEX_CHAR_VAL = new Int8Array(128);
HEX_CHAR_VAL.fill(-1);

// Configure whitespace and delimiter character markers
HEX_CHAR_VAL[32] = -2; // ' '
HEX_CHAR_VAL[13] = -2; // '\r'
HEX_CHAR_VAL[10] = -2; // '\n'
HEX_CHAR_VAL[9] = -2;  // '\t'
HEX_CHAR_VAL[62] = -2; // '>'

for (let i = 0; i < 16; i++) {
  HEX_NIBBLE_TABLE[i] = i.toString(16).toUpperCase();
}

for (let i = 0; i < 256; i++) {
  HEX_BYTE_TABLE[i] = i < 16 ? '0' + i.toString(16).toUpperCase() : i.toString(16).toUpperCase();
}

for (let i = 0; i <= 9; i++) HEX_CHAR_VAL[48 + i] = i; // '0'-'9'
for (let i = 0; i < 6; i++) {
  HEX_CHAR_VAL[65 + i] = 10 + i; // 'A'-'F'
  HEX_CHAR_VAL[97 + i] = 10 + i; // 'a'-'f'
}

/**
 * Fast zero-regex lookup table helper to extract raw hex bytes from OBD-II responses.
 * Ignores whitespace (\s, \r, \n, \t) and prompt character (>).
 * Returns null if non-hex characters are present or if digit count is odd.
 */
function parseHexBytes(hexString: string): number[] | null {
  const len = hexString.length;
  const bytes: number[] = [];
  let highNibble = -1;

  for (let i = 0; i < len; i++) {
    const code = hexString.charCodeAt(i);
    if (code >= 128) return null;
    const val = HEX_CHAR_VAL[code]!;

    if (val === -2) continue; // Fast skip whitespace & delimiter
    if (val === -1) return null; // Invalid character

    if (highNibble === -1) {
      highNibble = val;
    } else {
      bytes.push((highNibble << 4) | val);
      highNibble = -1;
    }
  }

  if (highNibble !== -1) return null;
  return bytes;
}

function decodeAsciiPayload(bytes: number[]): string {
  let result = '';
  for (let i = 0; i < bytes.length; i++) {
    const val = bytes[i]!;
    if (val !== 0) {
      result += String.fromCharCode(val);
    }
  }
  return result;
}

/**
 * High-performance SAE J1979 Mode 01 response decoder using lookup table optimization.
 */
export function decodeMode01Response(hexString: string): DecodedPid | null {
  const bytes = parseHexBytes(hexString);
  if (!bytes || bytes.length < 3 || bytes[0] !== 0x41) return null;

  const pidByte = bytes[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const byteA = bytes[2]!;
  const byteB = bytes[3] ?? 0;

  switch (pid) {
    case '0C': // Engine RPM
      if (bytes.length < 4) return null;
      return { pid: '0C', name: 'Engine RPM', value: ((byteA * 256) + byteB) / 4, unit: 'RPM' };
    case '0D': // Vehicle Speed
      return { pid: '0D', name: 'Vehicle Speed', value: byteA, unit: 'km/h' };
    case '05': // Coolant Temp
      return { pid: '05', name: 'Coolant Temperature', value: byteA - 40, unit: '°C' };
    case '0F': // Intake Air Temp
      return { pid: '0F', name: 'Intake Air Temp', value: byteA - 40, unit: '°C' };
    case '04': // Calculated Load
      return { pid: '04', name: 'Engine Load', value: (byteA * 100) / 255, unit: '%' };
    case '11': // Throttle Position
      return { pid: '11', name: 'Throttle Position', value: (byteA * 100) / 255, unit: '%' };
    case '2F': // Fuel Tank Level
      return { pid: '2F', name: 'Fuel Tank Level', value: (byteA * 100) / 255, unit: '%' };
    default:
      return { pid, name: `PID_${pid}`, value: byteA, unit: 'raw' };
  }
}

export function decodeMode09Response(hexString: string): DecodedPid | null {
  const bytes = parseHexBytes(hexString);
  if (!bytes || bytes.length < 2 || bytes[0] !== 0x49) return null;

  const pidByte = bytes[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const rawPayload = bytes.slice(2);
  if (rawPayload.length === 0) return null;

  const payload = rawPayload[0] === 0x00 ? rawPayload.slice(1) : rawPayload;
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
  const numBytes = bytes.length;

  for (let byteIndex = 0; byteIndex < numBytes; byteIndex++) {
    const byte = bytes[byteIndex]!;
    if (byte === 0) continue;

    for (let bitIndex = 7; bitIndex >= 0; bitIndex--) {
      if ((byte & (1 << bitIndex)) !== 0) {
        const pidNumber = (byteIndex * 8) + (7 - bitIndex) + 1;
        pids.push(HEX_BYTE_TABLE[pidNumber]!);
      }
    }
  }

  return pids;
}

export function decodeMode03Response(hexString: string): string[] {
  const bytes = parseHexBytes(hexString);
  if (!bytes || bytes.length === 0 || bytes[0] !== 0x43) return [];

  const payloadLen = bytes.length - 1;
  if (payloadLen <= 0 || payloadLen % 2 !== 0) return [];

  const dtcs: string[] = [];
  for (let i = 1; i < bytes.length; i += 2) {
    const b1 = bytes[i]!;
    const b2 = bytes[i + 1]!;
    if (b1 === 0 && b2 === 0) continue;

    const firstByteGroup = b1 >> 6;
    let group: string;
    switch (firstByteGroup) {
      case 0: group = 'P'; break;
      case 1: group = 'C'; break;
      case 2: group = 'B'; break;
      case 3: group = 'U'; break;
      default: group = 'P'; break;
    }

    const digit = (b1 >> 4) & 0x03;
    const hex1 = HEX_NIBBLE_TABLE[b1 & 0x0f]!;
    const hex2 = HEX_BYTE_TABLE[b2]!;

    dtcs.push(`${group}${digit}${hex1}${hex2}`);
  }

  return dtcs;
}
