import type { SVGProps } from 'react';

export type Cs2IglIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

/** IGL: 전략 콜을 상징하는 보드/체크라인 */
export function IglIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: Cs2IglIconProps) {
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
      <path d="M7.2 3.2h9.6a1.8 1.8 0 0 1 1.8 1.8v14a1.8 1.8 0 0 1-1.8 1.8H7.2A1.8 1.8 0 0 1 5.4 19V5a1.8 1.8 0 0 1 1.8-1.8Z" opacity="0.38" />
      <path d="M8.2 8h7.6v1.8H8.2Zm0 3.1h7.6V13H8.2Zm0 3.1h4.8V16H8.2Z" />
      <path d="m14.3 15.1 1.1 1.1 2.4-2.4 1.2 1.2-3.6 3.6-2.3-2.3 1.2-1.2Z" />
    </svg>
  );
}

