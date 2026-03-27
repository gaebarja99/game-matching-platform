import type { SVGProps } from 'react';

/**
 * 오버워치 2 **공격(Damage)** 역할 UI용 심볼 (단순화한 실루엣).
 * 블리자드 공식 SVG를 복제한 것이 아니라, 역할 아이콘 형태를 참고한 프로젝트 전용 벡터입니다.
 */
export type OverwatchDamageIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

export function DamageIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: OverwatchDamageIconProps) {
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
      {/* 상·하강 삼각이 중앙에서 맞닿는 형태 (교차/타격 느낌) */}
      <path d="M12 3l7.5 9H4.5L12 3zm0 18L4.5 12h15L12 21z" />
    </svg>
  );
}
