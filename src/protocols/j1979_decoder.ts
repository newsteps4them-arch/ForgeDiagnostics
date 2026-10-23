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
  const clean = hexString.replace(/\s+/g, '');
  const match = clean.match(/(?:41)([0-9A-F]{2})([0-9A-F]+)/i);
  if (!match) return null;

  const pid = match[1]?.toUpperCase() || '';
  const rawBytes = match[2] || '';
  const bytes: number[] = [];
  for (let i = 0; i < rawBytes.length; i += 2) {
    bytes.push(parseInt(rawBytes.substr(i, 2), 16));
  }

  const A = bytes[0] || 0;
  const B = bytes[1] || 0;

  switch (pid) {
    case '0C': // Engine RPM
      return { pid: '0C', name: 'Engine RPM', value: ((A * 256) + B) / 4, unit: 'RPM' };
    case '0D': // Vehicle Speed
      return { pid: '0D', name: 'Vehicle Speed', value: A, unit: 'km/h' };
    case '05': // Coolant Temp
      return { pid: '05', name: 'Coolant Temperature', value: A - 40, unit: '°C' };
    case '0F': // Intake Air Temp
      return { pid: '0F', name: 'Intake Air Temp', value: A - 40, unit: '°C' };
    case '04': // Calculated Load
      return { pid: '04', name: 'Engine Load', value: (A * 100) / 255, unit: '%' };
    default:
      return { pid, name: `PID_${pid}`, value: A || 0, unit: 'raw' };
  }
}
