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

// Module-scoped buffer to avoid dynamic heap allocations during high-frequency telemetry decoding
const SHARED_BYTE_BUF = new Uint8Array(1024);

/**
 * Fast zero-allocation lookup table helper to extract raw hex bytes into SHARED_BYTE_BUF.
 * Ignores whitespace (\s, \r, \n, \t) and prompt character (>).
 * Returns decoded byte count, or -1 if non-hex characters are present or digit count is odd.
 */
function parseHexBytesToBuffer(hexString: string): number {
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
      if (byteCount >= 1024) return -1; // Reject oversized frames exceeding fixed buffer size
      SHARED_BYTE_BUF[byteCount++] = (highNibble << 4) | val;
      highNibble = -1;
    }
  }

  if (highNibble !== -1) return -1;
  return byteCount;
}

function decodeAsciiBuffer(start: number, end: number): string {
  let result = '';
  for (let i = start; i < end; i++) {
    const val = SHARED_BYTE_BUF[i]!;
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
  const count = parseHexBytesToBuffer(hexString);
  if (count < 3 || SHARED_BYTE_BUF[0] !== 0x41) return null;

  const pidByte = SHARED_BYTE_BUF[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  const byteA = SHARED_BYTE_BUF[2]!;
  const byteB = count > 3 ? SHARED_BYTE_BUF[3]! : 0;

  switch (pid) {
    case '04': // Calculated Load
      return { pid: '04', name: 'Engine Load', value: (byteA * 100) / 255, unit: '%' };
    case '05': // Coolant Temp
      return { pid: '05', name: 'Coolant Temperature', value: byteA - 40, unit: '°C' };
    case '06': // Short Term Fuel Trim - Bank 1
      return { pid: '06', name: 'Short Term Fuel Trim', value: ((byteA - 128) * 100) / 128, unit: '%' };
    case '07': // Long Term Fuel Trim - Bank 1
      return { pid: '07', name: 'Long Term Fuel Trim', value: ((byteA - 128) * 100) / 128, unit: '%' };
    case '0A': // Fuel Pressure
      return { pid: '0A', name: 'Fuel Pressure', value: byteA * 3, unit: 'kPa' };
    case '0B': // Intake Manifold Absolute Pressure (MAP)
      return { pid: '0B', name: 'Intake Manifold Pressure', value: byteA, unit: 'kPa' };
    case '0C': // Engine RPM
      if (count < 4) return null;
      return { pid: '0C', name: 'Engine RPM', value: ((byteA * 256) + byteB) / 4, unit: 'RPM' };
    case '0D': // Vehicle Speed
      return { pid: '0D', name: 'Vehicle Speed', value: byteA, unit: 'km/h' };
    case '0E': // Timing Advance
      return { pid: '0E', name: 'Timing Advance', value: (byteA / 2) - 64, unit: '°' };
    case '0F': // Intake Air Temp
      return { pid: '0F', name: 'Intake Air Temp', value: byteA - 40, unit: '°C' };
    case '10': // MAF Air Flow Rate
      if (count < 4) return null;
      return { pid: '10', name: 'MAF Air Flow Rate', value: ((byteA * 256) + byteB) / 100, unit: 'g/s' };
    case '11': // Throttle Position
      return { pid: '11', name: 'Throttle Position', value: (byteA * 100) / 255, unit: '%' };
    case '14': // O2 Sensor 1 Voltage
      return { pid: '14', name: 'O2 Sensor 1 Voltage', value: byteA / 200, unit: 'V' };
    case '21': // Distance Traveled with MIL on
      if (count < 4) return null;
      return { pid: '21', name: 'Distance Traveled with MIL', value: (byteA * 256) + byteB, unit: 'km' };
    case '2F': // Fuel Tank Level
      return { pid: '2F', name: 'Fuel Tank Level', value: (byteA * 100) / 255, unit: '%' };
    case '33': // Absolute Barometric Pressure
      return { pid: '33', name: 'Barometric Pressure', value: byteA, unit: 'kPa' };
    case '42': // Control Module Voltage
      if (count < 4) return null;
      return { pid: '42', name: 'Control Module Voltage', value: ((byteA * 256) + byteB) / 1000, unit: 'V' };
    case '5C': // Engine Oil Temp
      return { pid: '5C', name: 'Engine Oil Temp', value: byteA - 40, unit: '°C' };
    default:
      return { pid, name: `PID_${pid}`, value: byteA, unit: 'raw' };
  }
}

export function decodeMode09Response(hexString: string): DecodedPid | null {
  const count = parseHexBytesToBuffer(hexString);
  if (count < 2 || SHARED_BYTE_BUF[0] !== 0x49) return null;

  const pidByte = SHARED_BYTE_BUF[1]!;
  const pid = HEX_BYTE_TABLE[pidByte]!;

  let offset = 2;
  if (offset < count && SHARED_BYTE_BUF[offset] === 0x00) {
    offset++;
  }
  if (offset >= count) return null;

  const ascii = decodeAsciiBuffer(offset, count);
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
  const count = parseHexBytesToBuffer(hexMask);
  if (count <= 0) return [];

  const pids: string[] = [];

  for (let byteIndex = 0; byteIndex < count; byteIndex++) {
    const byte = SHARED_BYTE_BUF[byteIndex]!;
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

function decodeDtcPayload(count: number, expectedHeader: number): string[] {
  if (count <= 0 || SHARED_BYTE_BUF[0] !== expectedHeader) return [];

  const payloadLen = count - 1;
  if (payloadLen <= 0 || payloadLen % 2 !== 0) return [];

  const dtcs: string[] = [];
  for (let i = 1; i < count; i += 2) {
    const b1 = SHARED_BYTE_BUF[i]!;
    const b2 = SHARED_BYTE_BUF[i + 1]!;
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

/**
 * Decodes stored trouble codes from a Mode 03 response (header 0x43).
 */
export function decodeMode03Response(hexString: string): string[] {
  const count = parseHexBytesToBuffer(hexString);
  return decodeDtcPayload(count, 0x43);
}

/**
 * Decodes pending trouble codes from a Mode 07 response (header 0x47).
 */
export function decodeMode07Response(hexString: string): string[] {
  const count = parseHexBytesToBuffer(hexString);
  return decodeDtcPayload(count, 0x47);
}

/**
 * Decodes permanent trouble codes from a Mode 0A response (header 0x4A).
 */
export function decodeMode0AResponse(hexString: string): string[] {
  const count = parseHexBytesToBuffer(hexString);
  return decodeDtcPayload(count, 0x4A);
}
