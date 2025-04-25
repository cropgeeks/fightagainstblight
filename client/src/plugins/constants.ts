export interface Status {
  icon: string
  text: string
  color: string
  dbValue: string
}

export interface Host {
  text: string
  color: string
  dbValue: string
}

const outbreakStatus: Map<string, Status> = new Map()
outbreakStatus.set('pending', { icon: 'mdi-clock', color: 'grey-darken-2', text: 'Pending', dbValue: 'pending' })
outbreakStatus.set('confirmed', { icon: 'mdi-alert', color: 'error', text: 'Confirmed', dbValue: 'confirmed' })
outbreakStatus.set('negative', { icon: 'mdi-close-circle', color: 'info', text: 'Negative', dbValue: 'negative' })
outbreakStatus.set('deleted', { icon: 'mdi-delete', color: 'grey', text: 'Deleted', dbValue: 'deleted' })

const outbreakHosts: Map<string, Host> = new Map()
outbreakHosts.set('potato', { color: 'purple-darken-1', text: 'Potato', dbValue: 'potato' })
outbreakHosts.set('tomato', { color: 'red-darken-1', text: 'Tomato', dbValue: 'tomato' })
outbreakHosts.set('other', { color: 'blue-grey-lighten-1', text: 'Other', dbValue: 'other' })

export {
  outbreakStatus,
  outbreakHosts,
}
