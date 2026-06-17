export const STATUS_TEXT = {
  PENDING: '等待判题',
  JUDGING: '判题中',
  AC: '通过',
  WA: '答案错误',
  CE: '编译错误',
  TLE: '运行超时',
  MLE: '内存超限',
  RE: '运行错误',
  SE: '系统错误'
};

export function statusText(status) {
  return STATUS_TEXT[status] || status || '';
}

export function caseProgressText(submission) {
  const passed = submission?.passedTestCases;
  const total = submission?.totalTestCases;
  if (passed === null || passed === undefined || total === null || total === undefined || total <= 0) {
    return '-';
  }
  return `${passed}/${total}`;
}

export function submissionResultText(submission) {
  if (!submission) {
    return '提交后在这里查看判题状态';
  }
  return `状态：${submission.statusText || statusText(submission.status)}
分数：${submission.score ?? 0}
通过用例：${caseProgressText(submission)}
耗时：${submission.timeUsedMs ?? '-'} ms
报错信息：${submission.errorMessage || '-'}`;
}

export function errorSummaryText(submission, maxLength = 80) {
  const message = submission?.errorMessage;
  if (!message) {
    return '-';
  }
  const normalized = message.replace(/\s+/g, ' ').trim();
  if (normalized.length <= maxLength) {
    return normalized;
  }
  return `${normalized.slice(0, maxLength - 3)}...`;
}
