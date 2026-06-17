import { describe, expect, it } from 'vitest';
import { caseProgressText, errorSummaryText, submissionResultText } from './statusText';

describe('status text utilities', () => {
  it('formats case progress', () => {
    expect(caseProgressText({ passedTestCases: 4, totalTestCases: 5 })).toBe('4/5');
    expect(caseProgressText({ passedTestCases: 0, totalTestCases: 0 })).toBe('-');
  });

  it('includes compiler stderr in the full result text', () => {
    const text = submissionResultText({
      status: 'CE',
      statusText: '编译错误',
      score: 0,
      passedTestCases: 0,
      totalTestCases: 5,
      timeUsedMs: 12,
      errorMessage: "main.c:1: error: expected ';'"
    });

    expect(text).toContain('状态：编译错误');
    expect(text).toContain('通过用例：0/5');
    expect(text).toContain("报错信息：main.c:1: error: expected ';'");
  });

  it('summarizes long error messages for table display', () => {
    const summary = errorSummaryText({
      errorMessage: 'line 1\nline 2 with a very long compiler diagnostic message that should not stretch the table'
    });

    expect(summary.length).toBe(80);
    expect(summary.startsWith('line 1 line 2 with a very long compiler diagnostic message that should')).toBe(true);
    expect(summary.endsWith('...')).toBe(true);
  });
});
