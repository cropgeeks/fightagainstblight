interface Color {
  r: number
  g: number
  b: number
}

/**
 * Converts a HEX value into an RGB object
 * @param {String} hex The hex color
 */
function hexToRgb (hex: string) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return (result && result.length === 4) ? { r: Number.parseInt(result[1] || '', 16), g: Number.parseInt(result[2] || '', 16), b: Number.parseInt(result[3] || '', 16) } : { r: 0, g: 0, b: 0 }
}

function rgbToHex (c: Color) {
  return `#${((1 << 24) + (c.r << 16) + (c.g << 8) + c.b).toString(16).slice(1)}`
}

function hexToRgba (hex: string, a: number) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)

  if (a) {
    return `rgba(${r},${g},${b},${a})`
  } else {
    return `rgba(${r},${g},${b})`
  }
}

function createMultiColorGradient (colors: string[], steps: number): string[] {
  const sections = colors.length - 1

  let result: string[] = []

  for (let i = 0; i < sections; i++) {
    result = result.concat(createColorGradient(colors[i], colors[i + 1], Math.floor(steps / sections)))
  }

  while (result.length < steps) {
    result.push(colors[colors.length - 1])
  }

  return result
}

/**
 * Creates a linear gradient between the two given colors with the given number of steps
 * @param {String} one The first color in HEX
 * @param {String} two The second color in HEX
 * @param {Number} steps The number of steps between the two colors
 */
function createColorGradient (one: string, two: string, steps: number) {
  const oneRgb = hexToRgb(one)
  const twoRgb = hexToRgb(two)

  const result = []

  if (oneRgb && twoRgb) {
    for (let i = 0; i < steps; i++) {
      const iNorm = i / (steps - 1)
      result.push(rgbToHex({
        r: Math.floor(oneRgb.r + iNorm * (twoRgb.r - oneRgb.r)),
        g: Math.floor(oneRgb.g + iNorm * (twoRgb.g - oneRgb.g)),
        b: Math.floor(oneRgb.b + iNorm * (twoRgb.b - oneRgb.b)),
      }))
    }
  }

  return result
}

const GRADIENT_VIRIDIS = ['#440154', '#48186a', '#472d7b', '#424086', '#3b528b', '#33638d', '#2c728e', '#26828e', '#21918c', '#1fa088', '#28ae80', '#3fbc73', '#5ec962', '#84d44b', '#addc30', '#d8e219', '#fde725']
const GRADIENT_YL_OR_RED_6 = ['#fed976', '#feb24c', '#fd8d3c', '#fc4e2a', '#e31a1c', '#b10026']

export {
  hexToRgb,
  hexToRgba,
  rgbToHex,
  createColorGradient,
  createMultiColorGradient,
  GRADIENT_VIRIDIS,
  GRADIENT_YL_OR_RED_6,
}
