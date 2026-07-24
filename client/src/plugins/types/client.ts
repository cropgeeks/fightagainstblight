import type { Outbreak } from '@/plugins/types/Outbreak'

export interface HighlightOutbreak extends Outbreak {
  highlightValue?: string
  highlightColor?: string
}
