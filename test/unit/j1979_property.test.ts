import { describe, expect, it } from 'vitest';
import { decodeMode01Response } from '../../src/protocols/j1979_decoder';

describe('SAE J1979 randomized accuracy and corruption coverage', () => {
  it('decodes every single-byte speed value exactly', () => {
    for (let speed = 0; speed <= 255; speed += 1) {
      const response = `41 0D ${speed.toString(16).padStart(2, '0')}`;
      expect(decodeMode01Response(response)?.value).toBe(speed);
    }
  });

  it('decodes randomized RPM values without quantization drift', () => {
    let seed = 0x5eed1234;
    for (let i = 0; i < 2000; i += 1) {
      seed = (seed * 1664525 + 1013904223) >>> 0;
      const rpm = seed % 16384;
      const encoded = rpm * 4;
      const a = (encoded >> 8) & 0xff;
      const b = encoded & 0xff;
      const response = `41 0C ${a.toString(16).padStart(2, '0')} ${b.toString(16).padStart(2, '0')}`;
      expect(decodeMode01Response(response)?.value).toBe(rpm);
    }
  });

  it('rejects corrupted response frames across representative mutation classes', () => {
    const invalidFrames = [
      '41 0D',
      '41 0D 0',
      '41 0D GG',
      '41 0D 100',
      '40 0D 37',
      'NO DATA 41 0D 37',
      '41 0C 0F',
      '41 10 09',
      '41 0D 37🙂',
      '\u0000\u0001',
    ];

    for (const frame of invalidFrames) {
      expect(decodeMode01Response(frame), `frame should be rejected: ${frame}`).toBeNull();
    }
  });
});
