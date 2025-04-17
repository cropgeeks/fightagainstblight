export interface Status {
  icon: string
  text: string
  color: string
  dbValue: string
}

const outbreakStatus: Map<string, Status> = new Map()
outbreakStatus.set('pending', { icon: 'mdi-clock', color: 'grey-darken-2', text: 'Pending', dbValue: 'pending' })
outbreakStatus.set('confirmed', { icon: 'mdi-alert', color: 'error', text: 'Confirmed', dbValue: 'confirmed' })
outbreakStatus.set('negative', { icon: 'mdi-close-circle', color: 'info', text: 'Negative', dbValue: 'negative' })
outbreakStatus.set('deleted', { icon: 'mdi-delete', color: 'grey', text: 'Deleted', dbValue: 'deleted' })

export {
  outbreakStatus,
}
