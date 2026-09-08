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
  value: number;
  unit: string;
}

export function decodeMode01Response(hexString: string): DecodedPid | null {
  const clean = hexString.replace(/[\s\r\n>]/g, '').toUpperCase();
  if (!clean || clean.length % 2 !== 0 || !/^[0-9A-F]+$/.test(clean)) return null;

  const match = clean.match(/(?:41)([0-9A-F]{2})([0-9A-F]*)/);
  if (!match) return null;

  const pid = match[1] || '';
  const rawBytes = match[2] || '';
  if (!pid || !rawBytes || rawBytes.length % 2 !== 0) return null;

  const bytes: number[] = [];
  for (let i = 0; i < rawBytes.length; i += 2) {
    const byte = Number.parseInt(rawBytes.slice(i, i + 2), 16);
    if (Number.isNaN(byte)) return null;
    bytes.push(byte);
  }

  const A = bytes[0];
  const B = bytes[1];

  switch (pid) {
    case '0C':
      if (bytes.length < 2 || A === undefined || B === undefined) return null;
      return { pid: '0C', name: 'Engine RPM', value: ((A * 256) + B) / 4, unit: 'RPM' };
    case '0D':
      if (A === undefined) return null;
      return { pid: '0D', name: 'Vehicle Speed', value: A, unit: 'km/h' };
    case '05':
      if (A === undefined) return null;
      return { pid: '05', name: 'Coolant Temperature', value: A - 40, unit: '°C' };
    case '0F':
      if (A === undefined) return null;
      return { pid: '0F', name: 'Intake Air Temp', value: A - 40, unit: '°C' };
    case '04':
      if (A === undefined) return null;
      return { pid: '04', name: 'Engine Load', value: (A * 100) / 255, unit: '%' };
    case '11':
      if (A === undefined) return null;
      return { pid: '11', name: 'Throttle Position', value: (A * 100) / 255, unit: '%' };
    case '2F':
      if (A === undefined) return null;
      return { pid: '2F', name: 'Fuel Tank Level', value: (A * 100) / 255, unit: '%' };
    default:
      if (A === undefined) return null;
      return { pid, name: `PID_${pid}`, value: A, unit: 'raw' };
  }
}
