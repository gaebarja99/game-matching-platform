import StudioLayout from '../components/StudioLayout';

export default function StudioAnalysisVideo() {
  const dateRange = '2026.02.08.-2026.03.10.';
  const kpiCards = [
    { label: '업로드한 동영상', value: '0개' },
    { label: '재생수', value: '0회' },
    { label: '시청자수', value: '0명' },
    { label: '시청 시간', value: '00:00:00' },
    { label: '평균 시청 지속률', value: '0%' },
  ];

  return (
    <StudioLayout>
      <h1 className="page-title">동영상 분석</h1>
      <div className="toolbar">
        <input type="text" readOnly value={dateRange} />
        <button type="button" className="btn-query">조회</button>
        <button type="button" className="btn-download">다운로드</button>
      </div>
      <div className="kpi-grid">
        {kpiCards.map((card) => (
          <div key={card.label} className="kpi-card">
            <div className="kpi-label">{card.label}</div>
            <div className="kpi-value">{card.value}</div>
          </div>
        ))}
      </div>
      <table className="data-table">
        <thead>
          <tr>
            <th>영상</th>
            <th>재생수</th>
            <th>총 시청자</th>
            <th>총 시청 시간</th>
            <th>평균 시청 지속 시간</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colSpan={5} className="empty-msg">
              동영상이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </StudioLayout>
  );
}
