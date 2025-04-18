import * as L from 'leaflet'

export class DataMarker<T> extends L.Marker {
  data: T

  constructor(latLng: L.LatLngExpression, data: T, options?: L.MarkerOptions) {
    super(latLng, options)
    this.data = data
  }

  getData() {
    return this.data
  }

  setData(data: T) {
    this.data = data
  }
}