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

// Shared scratch buffer for zero-allocation byte parsing in single-threaded JS runtime
const SCRATCH_BYTES = new Uint8Array(512);

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
 * High-performance zero-allocation hex parser. Writes decoded bytes directly
 * into SCRATCH_BYTES and returns byte count.
 * Ignores whitespace (\s, \r, \n, \t) and prompt character (>).
 * Returns -1 if non-hex characters are present, if digit count is odd, or if buffer overflows.
 */
function parseHexBytesScratch(hexString: string): number {
  const len = hexString.length;
  let byteCount = 0;
  let highNibble = -1;

  for (let i = 0; i < len; i++) {
    const code = hexString.charCodeAt(i);
    if (code >= 128) return -1;
    const val = HEX_CHAR_VAL[code]!;

    if (val === -2) continue; // Fast skip whitespace & delimiter
    if (val === -1) return -1; // Invalid character

    if (highNibble === -1) {
      highNibble = val;
    } else {
      if (byteCount >= SCRATCH_BYTES.length) return -1; // Buffer overflow guard
      SCRATCH_BYTES[byteCount++] = (highNibble << 4) | val;
      highNibble = -1;
    }
  }

  if (highNibble !== -1) return -1;
  return byteCount;
}

function decodeAsciiPayloadScratch(start: number, count: number): string {
  let result = '';
  for (let i = start; i < count; i++) {
    const val = SCRATCH_BYTES[i]!;
    if (val !== 0) {
      result += String.fromCharCode(val);
    }
  }
  return result;
}

/**
 * High-performance SAE J1979 Mode 01 response decoder using zero-allocation scratch buffer.
 */
export function decodeMode01Response(hexString: string): DecodedPid | null {
  const count = parseHexBytesScratch(hexString);
  if (count < 3 || SCRATCH_BYTES[0] !== 0x41) return null;

  const pidByte = SCRATCH_BYTES[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const byteA = SCRATCH_BYTES[2]!;
  const byteB = count > 3 ? SCRATCH_BYTES[3]! : 0;

  switch (pid) {
    case '0C': // Engine RPM
      if (count < 4) return null;
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
  const count = parseHexBytesScratch(hexString);
  if (count < 2 || SCRATCH_BYTES[0] !== 0x49) return null;

  const pidByte = SCRATCH_BYTES[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  let start = 2;
  if (start >= count) return null;

  if (SCRATCH_BYTES[start] === 0x00) {
    start++;
  }
  if (start >= count) return null;

  const ascii = decodeAsciiPayloadScratch(start, count);
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
  const count = parseHexBytesScratch(hexMask);
  if (count <= 0) return [];

  const pids: string[] = [];

  for (let byteIndex = 0; byteIndex < count; byteIndex++) {
    const byte = SCRATCH_BYTES[byteIndex]!;
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
  const count = parseHexBytesScratch(hexString);
  if (count <= 0 || SCRATCH_BYTES[0] !== 0x43) return [];

  const payloadLen = count - 1;
  if (payloadLen <= 0 || payloadLen % 2 !== 0) return [];

  const dtcs: string[] = [];
  for (let i = 1; i < count; i += 2) {
    const b1 = SCRATCH_BYTES[i]!;
    const b2 = SCRATCH_BYTES[i + 1]!;
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
