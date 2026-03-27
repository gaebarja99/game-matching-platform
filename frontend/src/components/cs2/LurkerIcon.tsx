import type { SVGProps } from 'react';

export type Cs2LurkerIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

/** Lurker: 시야/잠입을 상징하는 아이 실루엣 */
export function LurkerIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: Cs2LurkerIconProps) {
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
      <path d="M12 5c-5.9 0-9.6 5.8-9.8 6.1a1.6 1.6 0 0 0 0 1.8C2.4 13.2 6.1 19 12 19s9.6-5.8 9.8-6.1a1.6 1.6 0 0 0 0-1.8C21.6 10.8 17.9 5 12 5Zm0 2.2a4.8 4.8 0 1 1-4.8 4.8A4.81 4.81 0 0 1 12 7.2Z" />
      <circle cx="12" cy="12" r="2.2" />
    </svg>
  );
}

