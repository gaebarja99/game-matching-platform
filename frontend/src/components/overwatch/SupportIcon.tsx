import type { SVGProps } from 'react';

/**
 * 오버워치 2 **지원(Support)** 역할 UI용 심볼 (단순화한 실루엣).
 * 블리자드 공식 SVG를 복제한 것이 아니라, 역할 아이콘 형태를 참고한 프로젝트 전용 벡터입니다.
 */
export type OverwatchSupportIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

export function SupportIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: OverwatchSupportIconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox={`0 0 ${VB} ${VB}`}
      width={width}
      height={height}
      className={className}
      fill={fill}
      aria-hidden={title ? undefined : true}
      role={title ? 'img' : undefined}
      aria-label={title}
      {...rest}
    >
      {title ? <title>{title}</title> : null}
      <path d="M10.75 3.5h2.5v6.75H20v2.5h-6.75V20h-2.5v-7.25H4v-2.5h6.75V3.5Z" />
    </svg>
  );
}
