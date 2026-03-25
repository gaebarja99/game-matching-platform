import Layout from '../components/Layout';

/**
 * 이스포츠 페이지 — frontend/public/images 의 이스포츠 이미지 표시
 * 이미지: 16bd752dfb349cacf.jpg
 */
const ESPORTS_IMAGE_FILENAME = '16bd752dfb349cacf.jpg';
const ESPORTS_IMAGE = '/images/' + ESPORTS_IMAGE_FILENAME;

export default function Esports() {
  return (
    <Layout>
      <div className="esports-page">
        <h1 className="esports-page-title">이스포츠</h1>
        <div className="esports-page-image-wrap">
          <img
            src={ESPORTS_IMAGE}
            alt="이스포츠"
            className="esports-page-image"
            onError={(e) => {
              const el = e.target as HTMLImageElement;
              el.style.display = 'none';
              const wrap = el.closest('.esports-page-image-wrap');
              if (wrap && !wrap.querySelector('.esports-page-image-fallback')) {
                const fallback = document.createElement('p');
                fallback.className = 'esports-page-image-fallback';
                fallback.textContent = `이미지를 불러올 수 없습니다. public/images/ 에 ${ESPORTS_IMAGE_FILENAME} 파일이 있는지 확인해 주세요.`;
                el.parentNode?.appendChild(fallback);
              }
            }}
          />
        </div>
      </div>
    </Layout>
  );
}
