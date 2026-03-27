import type { SVGProps } from 'react';

/**
 * 오버워치 2 **돌격(Tank)** 역할 UI용 심볼 (단순화한 실루엣).
 * 블리자드 공식 SVG를 복제한 것이 아니라, 역할 아이콘 형태를 참고한 프로젝트 전용 벡터입니다.
 */
export type OverwatchRoleIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

export function TankIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: OverwatchRoleIconProps) {
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
      {/* 방패: 평평한 상단 + 하단 뾰족한 실루엣 */}
      <path d="M12 2l8 6v6.5l-8 7.5-8-7.5V8l8-6z" />
    </svg>
  );
}
