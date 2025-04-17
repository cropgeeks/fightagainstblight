export interface Outbreak {
  outbreakId?: number
  outbreakCode?: string
  severityId?: number
  severityName?: string
  severityOther?: string
  sourceId?: number
  sourceName?: string
  sourceOther?: string
  dateReceived?: string
  dateSubmitted?: string
  userComment?: string
  adminComment?: string
  realLatitude?: number
  realLongitude?: number
  viewLatitude?: number
  viewLongitude?: number
  postcode?: string
  status?: string,
  userId?: number
  userName?: string
  userEmail?: string
  reportedVarietyId?: number
}
