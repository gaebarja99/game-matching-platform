import { useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import MatchChatPanel from '../components/MatchChatPanel';

export default function MatchChat() {
  const { sessionId: sessionIdParam } = useParams<{ sessionId: string }>();
  const sessionId = sessionIdParam ? Number(sessionIdParam) : NaN;

  if (!sessionIdParam || Number.isNaN(sessionId)) {
    return (
      <Layout>
        <div className="duo-empty">잘못된 주소입니다.</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <MatchChatPanel sessionId={sessionId} />
    </Layout>
  );
}
