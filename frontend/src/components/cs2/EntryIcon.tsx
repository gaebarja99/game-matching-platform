import type { SVGProps } from 'react';

export type Cs2RoleIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

/** Entry: 전면 돌파를 상징하는 파열 + 화살표 */
export function EntryIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: Cs2RoleIconProps) {
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
      <path d="M12 2.8 14 8l5.3.2-4.2 3.2 1.4 5.2L12 13.8 7.5 16.6l1.4-5.2-4.2-3.2L10 8l2-5.2Z" />
      <path d="M12 10.5 20.2 12 12 13.5 13.6 21 12 13.5 3.8 12 12 10.5Z" opacity="0.85" />
    </svg>
  );
}

