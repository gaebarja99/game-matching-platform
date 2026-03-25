import { apiFetch, apiUrl } from './client';

export type BoardCategory =
  | 'FREE'
  | 'NOTICE'
  | 'QUESTION'
  | 'LOL'
  | 'TFT'
  | 'VALORANT'
  | 'PUBG'
  | 'OVERWATCH'
  | 'CS2'
  | 'APEX'
  | 'BLIZZARD'
  | 'STEAM';

export const BOARD_CATEGORIES: BoardCategory[] = [
  'FREE',
  'NOTICE',
  'QUESTION',
  'LOL',
  'TFT',
  'VALORANT',
  'PUBG',
  'OVERWATCH',
  'CS2',
  'BLIZZARD',
  'STEAM',
];

export const BOARD_LABELS: Record<BoardCategory, string> = {
  FREE: '자유',
  NOTICE: '공지',
  QUESTION: '질문',
  LOL: '롤',
  TFT: 'TFT',
  VALORANT: '발로란트',
  PUBG: '배그',
  OVERWATCH: '오버워치',
  CS2: 'CS2',
  APEX: '에이펙스',
  BLIZZARD: '블리자드',
  STEAM: '스팀',
};

export interface PostListItem {
  id: number;
  boardCategory: BoardCategory;
  title: string;
  authorId: number;
  authorUsername: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  isNotice: boolean;
  isPopular: boolean;
  hashtags: string[];
  createdAt: string;
}

export interface AttachmentDto {
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  contentType: string;
}

export interface PostDetail {
  id: number;
  boardCategory: BoardCategory;
  title: string;
  content: string;
  authorId: number;
  authorUsername: string;
  viewCount: number;
  likeCount: number;
  recommendCount: number;
  notRecommendCount: number;
  commentCount: number;
  isNotice: boolean;
  hashtags: string[];
  attachments: AttachmentDto[];
  createdAt: string;
  updatedAt: string;
  liked: boolean;
  bookmarked: boolean;
  myRecommend: number | null;
}

export interface CommentNode {
  id: number;
  postId: number;
  authorId: number;
  authorUsername: string;
  parentId: number | null;
  content: string;
  isDeleted: boolean;
  replies: CommentNode[] | null;
  createdAt: string;
  updatedAt: string;
}

export type ReportReason =
  | 'SPAM'
  | 'HARASSMENT'
  | 'INAPPROPRIATE_CONTENT'
  | 'CHEATING'
  | 'IMPERSONATION'
  | 'HATE_SPEECH'
  | 'OTHER';

export const COMMUNITY_REPORT_REASONS: Array<{ value: ReportReason; label: string }> = [
  { value: 'SPAM', label: '스팸' },
  { value: 'HARASSMENT', label: '괴롭힘' },
  { value: 'INAPPROPRIATE_CONTENT', label: '부적절한 콘텐츠' },
  { value: 'CHEATING', label: '부정행위' },
  { value: 'IMPERSONATION', label: '사칭' },
  { value: 'HATE_SPEECH', label: '혐오 발언' },
  { value: 'OTHER', label: '기타' },
];

export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

function qs(params: Record<string, string | number | undefined>) {
  const u = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== '') u.set(k, String(v));
  });
  const s = u.toString();
  return s ? `?${s}` : '';
}

export async function fetchCommunityPostList(args: {
  category: BoardCategory | null;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
}) {
  const { category, keyword, page = 0, size = 20, sortBy = 'latest' } = args;
  const q = qs({ keyword, page, size, sortBy });
  const path = category != null ? `/api/community/boards/${category}/posts${q}` : `/api/community/posts${q}`;
  return apiFetch<PageResult<PostListItem>>(path);
}

export async function fetchPopularPosts(category: BoardCategory | null, limit = 5) {
  const q = qs({ limit });
  const path = category != null ? `/api/community/boards/${category}/posts/popular${q}` : `/api/community/posts/popular${q}`;
  return apiFetch<PostListItem[]>(path);
}

export async function fetchPostDetailPublic(postId: number) {
  return apiFetch<PostDetail>(`/api/community/posts/${postId}`);
}

export async function fetchPostDetailForUser(userId: number, postId: number) {
  return apiFetch<PostDetail>(`/api/users/${userId}/community/posts/${postId}`);
}

export async function fetchComments(postId: number) {
  return apiFetch<CommentNode[]>(`/api/community/posts/${postId}/comments`);
}

export async function createPostJson(
  userId: number,
  body: {
    boardCategory: BoardCategory;
    title: string;
    content: string;
    isNotice?: boolean;
    hashtags?: string[];
  },
) {
  return apiFetch<PostDetail>(`/api/users/${userId}/community/posts/json`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function createPostMultipart(
  userId: number,
  body: {
    boardCategory: BoardCategory;
    title: string;
    content: string;
    isNotice?: boolean;
    hashtags?: string[];
  },
  files: File[],
) {
  const formData = new FormData();
  formData.append(
    'post',
    new Blob([JSON.stringify(body)], {
      type: 'application/json',
    }),
  );
  files.forEach((file) => formData.append('files', file));

  return apiFetch<PostDetail>(`/api/users/${userId}/community/posts`, {
    method: 'POST',
    body: formData,
  });
}

export async function updatePost(
  userId: number,
  postId: number,
  body: { title: string; content: string; hashtags?: string[] },
) {
  return apiFetch<PostDetail>(`/api/users/${userId}/community/posts/${postId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function addPostAttachment(userId: number, postId: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return apiFetch<void>(`/api/users/${userId}/community/posts/${postId}/attachments`, {
    method: 'POST',
    body: formData,
  });
}

export async function deletePost(userId: number, postId: number) {
  return apiFetch<void>(`/api/users/${userId}/community/posts/${postId}`, {
    method: 'DELETE',
  });
}

export async function toggleLike(userId: number, postId: number) {
  return apiFetch<void>(`/api/users/${userId}/community/posts/${postId}/like`, {
    method: 'POST',
    body: '{}',
  });
}

export async function toggleBookmark(userId: number, postId: number) {
  return apiFetch<void>(`/api/users/${userId}/community/posts/${postId}/bookmark`, {
    method: 'POST',
    body: '{}',
  });
}

export async function recommendPost(userId: number, postId: number, type: 'RECOMMEND' | 'NOT_RECOMMEND') {
  return apiFetch<void>(`/api/users/${userId}/community/posts/${postId}/recommend${qs({ type })}`, {
    method: 'POST',
    body: '{}',
  });
}

export async function createComment(userId: number, postId: number, content: string, parentId?: number) {
  return apiFetch<CommentNode>(`/api/users/${userId}/community/posts/${postId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ content, parentId: parentId ?? undefined }),
  });
}

export async function deleteComment(userId: number, commentId: number) {
  return apiFetch<void>(`/api/users/${userId}/community/comments/${commentId}`, {
    method: 'DELETE',
  });
}

export async function submitCommunityReport(
  userId: number,
  body: {
    targetType: 'POST' | 'COMMENT';
    postId: number;
    commentId?: number;
    reason: ReportReason;
    description?: string;
  },
) {
  return apiFetch<void>(`/api/users/${userId}/community/reports`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function attachmentUrl(filePath: string): string {
  return apiUrl(filePath.replace(/^\//, ''));
}

export function parseHashtagInput(raw: string): string[] {
  return raw
    .split(/[\s,]+/)
    .map((t) => t.replace(/^#+/, '').trim())
    .filter(Boolean)
    .slice(0, 10);
}
