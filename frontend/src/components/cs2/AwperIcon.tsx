import type { SVGProps } from 'react';

export type Cs2AwperIconProps = Omit<SVGProps<SVGSVGElement>, 'children'> & {
  title?: string;
};

const VB = 24;

/** AWPer: 스코프 조준선 */
export function AwperIcon({
  title,
  width = VB,
  height = VB,
  className,
  fill = 'currentColor',
  ...rest
}: Cs2AwperIconProps) {
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
      <path d="M12 4.2A7.8 7.8 0 1 0 19.8 12 7.81 7.81 0 0 0 12 4.2Zm0 2.1A5.7 5.7 0 1 1 6.3 12 5.71 5.71 0 0 1 12 6.3Z" />
      <path d="M11.1 2h1.8v4h-1.8Zm0 16h1.8v4h-1.8ZM2 11.1h4v1.8H2Zm16 0h4v1.8h-4Z" opacity="0.9" />
      <circle cx="12" cy="12" r="1.8" />
    </svg>
  );
}

