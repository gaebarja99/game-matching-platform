import type { SVGProps } from 'react';

export type Cs2SupportIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

/** Support: 연막/유틸 지원을 상징하는 보호 크로스 */
export function SupportIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: Cs2SupportIconProps) {
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
      <path d="M12 2.7 20.2 7v10L12 21.3 3.8 17V7L12 2.7Z" opacity="0.35" />
      <path d="M10.8 6.8h2.4v3.9h3.9v2.4h-3.9V17h-2.4v-3.9H6.9v-2.4h3.9V6.8Z" />
    </svg>
  );
}

