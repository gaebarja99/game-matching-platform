import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';
import { AlertProvider } from './contexts/AlertContext';
import Home from './pages/Home';
import Login from './pages/Login';
import FindLoginId from './pages/FindLoginId';
import FindPassword from './pages/FindPassword';
import Register from './pages/Register';
import Streams from './pages/Streams';
import Watch from './pages/Watch';
import Profile from './pages/Profile';
import ProfileMyInfo from './pages/ProfileMyInfo';
import ProfilePang from './pages/ProfilePang';
import ProfileMileageShop from './pages/ProfileMileageShop';
import ProfileAdFree from './pages/ProfileAdFree';
import ProfileSubscriptions from './pages/ProfileSubscriptions';
import ProfileMyPosts from './pages/ProfileMyPosts';
import ProfileSavedPosts from './pages/ProfileSavedPosts';
import ProfileEsportsPredictions from './pages/ProfileEsportsPredictions';
import ProfileEsportsRewards from './pages/ProfileEsportsRewards';
import Studio from './pages/Studio';
import StudioLive from './pages/StudioLive';
import StudioSettings from './pages/StudioSettings';
import StudioAlerts from './pages/StudioAlerts';
import StudioChatSettings from './pages/StudioChatSettings';
import StudioAnalysisLive from './pages/StudioAnalysisLive';
import StudioAnalysisVideo from './pages/StudioAnalysisVideo';
import StudioViewersFollowers from './pages/StudioViewersFollowers';
import StudioViewersSubscribers from './pages/StudioViewersSubscribers';
import StudioViewersBlocklist from './pages/StudioViewersBlocklist';
import StudioRevenue from './pages/StudioRevenue';
import Admin from './pages/Admin';
import AdminReports from './pages/AdminReports';
import AdminMembers from './pages/AdminMembers';
import AdminSettlements from './pages/AdminSettlements';
import AdminRevenue from './pages/AdminRevenue';
import AdminCommunity from './pages/AdminCommunity';
import AdminMatchRooms from './pages/AdminMatchRooms';
import AdminBroadcasts from './pages/AdminBroadcasts';
import StudioAdminStreamers from './pages/StudioAdminStreamers';
import Following from './pages/Following';
import History from './pages/History';
import Channel from './pages/Channel';
import GameRooms from './pages/GameRooms';
import GroupChat from './pages/GroupChat';
import GroupChatRoom from './pages/GroupChatRoom';
import MatchChat from './pages/MatchChat';
import MatchHistory from './pages/MatchHistory';
import Records from './pages/Records';
import Esports from './pages/Esports';
import Chatbot from './pages/Chatbot';
import DirectMessages from './pages/DirectMessages';
import Community from './pages/Community';
import CommunityPost from './pages/CommunityPost';
import CommunityWrite from './pages/CommunityWrite';
import ProfileLayout from './components/ProfileLayout';
import './App.css';

function ThemeWrapper({ children }: { children: React.ReactNode }) {
  const { theme } = useTheme();
  const { user } = useAuth();
  return (
    <div className={`theme-${theme} ${user ? 'logged-in' : ''}`}>
      {children}
    </div>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AlertProvider>
        <AuthProvider>
          <ThemeWrapper>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/records" element={<Records />} />
              <Route path="/esports" element={<Esports />} />
              <Route path="/login" element={<Login />} />
              <Route path="/find-login-id" element={<FindLoginId />} />
              <Route path="/find-password" element={<FindPassword />} />
              <Route path="/register" element={<Register />} />
              <Route path="/streams" element={<Streams />} />
              <Route path="/watch/:streamId" element={<Watch />} />
              <Route path="/profile" element={<ProfileLayout />}>
                <Route index element={<Profile />} />
                <Route path="my-info" element={<ProfileMyInfo />} />
                <Route path="pang" element={<ProfilePang />} />
                <Route path="mileage-shop" element={<ProfileMileageShop />} />
                <Route path="adfree" element={<ProfileAdFree />} />
                <Route path="subscriptions" element={<ProfileSubscriptions />} />
                <Route path="my-posts" element={<ProfileMyPosts />} />
                <Route path="saved-posts" element={<ProfileSavedPosts />} />
                <Route path="esports-predictions" element={<ProfileEsportsPredictions />} />
                <Route path="esports-rewards" element={<ProfileEsportsRewards />} />
              </Route>
              <Route path="/studio" element={<Studio />} />
              <Route path="/studio/live" element={<StudioLive />} />
              <Route path="/studio/settings" element={<StudioSettings />} />
              <Route path="/studio/alerts" element={<StudioAlerts />} />
              <Route path="/studio/chat" element={<StudioChatSettings />} />
              <Route path="/studio/analysis/live" element={<StudioAnalysisLive />} />
              <Route path="/studio/analysis/video" element={<StudioAnalysisVideo />} />
              <Route path="/studio/viewers/followers" element={<StudioViewersFollowers />} />
              <Route path="/studio/viewers/subscribers" element={<StudioViewersSubscribers />} />
              <Route path="/studio/viewers/blocklist" element={<StudioViewersBlocklist />} />
              <Route path="/studio/revenue" element={<StudioRevenue />} />
              <Route path="/studio/admin/streamers" element={<Navigate to="/admin/streamers" replace />} />
              <Route path="/admin" element={<Admin />} />
              <Route path="/admin/streamers" element={<StudioAdminStreamers />} />
              <Route path="/admin/broadcasts" element={<AdminBroadcasts />} />
              <Route path="/admin/community" element={<AdminCommunity />} />
              <Route path="/admin/match-rooms" element={<AdminMatchRooms />} />
              <Route path="/admin/reports" element={<AdminReports />} />
              <Route path="/admin/members" element={<AdminMembers />} />
              <Route path="/admin/settlements" element={<AdminSettlements />} />
              <Route path="/admin/revenue" element={<AdminRevenue />} />
              <Route path="/following" element={<Following />} />
              <Route path="/history" element={<History />} />
              <Route path="/channel" element={<Channel />} />
              <Route path="/game-rooms" element={<GameRooms />} />
              <Route path="/group-chat" element={<GroupChat />} />
              <Route path="/group-chat/room/:roomId" element={<GroupChatRoom />} />
              <Route path="/match-chat/:sessionId" element={<MatchChat />} />
              <Route path="/match-history" element={<MatchHistory />} />
              <Route path="/community" element={<Community />} />
              <Route path="/community/posts/:postId" element={<CommunityPost />} />
              <Route path="/community/write/:postId" element={<CommunityWrite />} />
              <Route path="/community/write" element={<CommunityWrite />} />
              <Route path="/chatbot" element={<Chatbot />} />
              <Route path="/dm" element={<DirectMessages />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
          </ThemeWrapper>
        </AuthProvider>
      </AlertProvider>
    </ThemeProvider>
  );
}

export default App;
