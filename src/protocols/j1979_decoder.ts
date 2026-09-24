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

// Pre-computed PID lookup matrix for fast bitmask decoding [byteIndex][bitOffset]
// Eliminates runtime index calculations and string formatting in high-frequency PID bitmask scanning
const PID_LOOKUP_TABLE: string[][] = new Array(32);

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

for (let b = 0; b < 32; b++) {
  PID_LOOKUP_TABLE[b] = new Array(8);
  for (let bit = 7; bit >= 0; bit--) {
    const pidNumber = (b * 8) + (7 - bit) + 1;
    PID_LOOKUP_TABLE[b]![7 - bit] = HEX_BYTE_TABLE[pidNumber]!;
  }
}

for (let i = 0; i <= 9; i++) HEX_CHAR_VAL[48 + i] = i; // '0'-'9'
for (let i = 0; i < 6; i++) {
  HEX_CHAR_VAL[65 + i] = 10 + i; // 'A'-'F'
  HEX_CHAR_VAL[97 + i] = 10 + i; // 'a'-'f'
}

// Static reusable Uint8Array buffer to eliminate GC pressure and array allocation overhead
// during continuous high-frequency OBD-II telematics decoding (~500k ops/sec).
const PARSE_BUFFER = new Uint8Array(256);

/**
 * Fast zero-allocation hex parser populating static Uint8Array buffer.
 * Ignores whitespace (\s, \r, \n, \t) and prompt character (>).
 * Returns byte count populated, or -1 if non-hex characters present, odd hex nibble count, or buffer overflow.
 */
function parseHexBytesToBuffer(hexString: string): number {
  const len = hexString.length;
  let count = 0;
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
      if (count >= 256) return -1; // Overflow protection
      PARSE_BUFFER[count++] = (highNibble << 4) | val;
      highNibble = -1;
    }
  }

  if (highNibble !== -1) return -1;
  return count;
}

function decodeAsciiPayloadFromBuffer(offset: number, length: number): string {
  let result = '';
  const end = offset + length;
  for (let i = offset; i < end; i++) {
    const val = PARSE_BUFFER[i]!;
    if (val !== 0) {
      result += String.fromCharCode(val);
    }
  }
  return result;
}

/**
 * High-performance SAE J1979 Mode 01 response decoder using zero-allocation buffer parsing.
 */
export function decodeMode01Response(hexString: string): DecodedPid | null {
  const len = parseHexBytesToBuffer(hexString);
  if (len < 3 || PARSE_BUFFER[0] !== 0x41) return null;

  const pidByte = PARSE_BUFFER[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const byteA = PARSE_BUFFER[2]!;
  const byteB = len > 3 ? PARSE_BUFFER[3]! : 0;

  switch (pid) {
    case '0C': // Engine RPM
      if (len < 4) return null;
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
  const len = parseHexBytesToBuffer(hexString);
  if (len < 2 || PARSE_BUFFER[0] !== 0x49) return null;

  const pidByte = PARSE_BUFFER[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const rawPayloadLen = len - 2;
  if (rawPayloadLen <= 0) return null;

  let payloadOffset = 2;
  let payloadLen = rawPayloadLen;
  if (PARSE_BUFFER[payloadOffset] === 0x00) {
    payloadOffset += 1;
    payloadLen -= 1;
  }
  if (payloadLen <= 0) return null;

  const ascii = decodeAsciiPayloadFromBuffer(payloadOffset, payloadLen);
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

/**
 * High-performance SAE J1979 PID bitmask decoder using unrolled bit checks and precomputed PID lookup matrix.
 */
export function decodeSupportedPidMask(hexMask: string): string[] {
  const numBytes = parseHexBytesToBuffer(hexMask);
  if (numBytes <= 0) return [];

  const pids: string[] = [];

  for (let byteIndex = 0; byteIndex < numBytes; byteIndex++) {
    const byte = PARSE_BUFFER[byteIndex]!;
    if (byte === 0) continue;

    // Use pre-computed PID lookup array or calculate fallback if byte index > 31
    const lookup = PID_LOOKUP_TABLE[byteIndex] || new Array(8);

    // Unrolled bitwise checks for maximum decoding throughput
    if (byte & 0x80) pids.push(lookup[0] || HEX_BYTE_TABLE[(byteIndex * 8) + 1]!);
    if (byte & 0x40) pids.push(lookup[1] || HEX_BYTE_TABLE[(byteIndex * 8) + 2]!);
    if (byte & 0x20) pids.push(lookup[2] || HEX_BYTE_TABLE[(byteIndex * 8) + 3]!);
    if (byte & 0x10) pids.push(lookup[3] || HEX_BYTE_TABLE[(byteIndex * 8) + 4]!);
    if (byte & 0x08) pids.push(lookup[4] || HEX_BYTE_TABLE[(byteIndex * 8) + 5]!);
    if (byte & 0x04) pids.push(lookup[5] || HEX_BYTE_TABLE[(byteIndex * 8) + 6]!);
    if (byte & 0x02) pids.push(lookup[6] || HEX_BYTE_TABLE[(byteIndex * 8) + 7]!);
    if (byte & 0x01) pids.push(lookup[7] || HEX_BYTE_TABLE[(byteIndex * 8) + 8]!);
  }

  return pids;
}

export function decodeMode03Response(hexString: string): string[] {
  const len = parseHexBytesToBuffer(hexString);
  if (len <= 0 || PARSE_BUFFER[0] !== 0x43) return [];

  const payloadLen = len - 1;
  if (payloadLen <= 0 || payloadLen % 2 !== 0) return [];

  const dtcs: string[] = [];
  for (let i = 1; i < len; i += 2) {
    const b1 = PARSE_BUFFER[i]!;
    const b2 = PARSE_BUFFER[i + 1]!;
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
