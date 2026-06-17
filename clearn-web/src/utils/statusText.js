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
