const isValidLatLng = (lat: number | undefined, lng: number | undefined) => {
  return lat !== undefined && lng !== undefined && isFinite(lat) && isFinite(lng)
}

export {
  isValidLatLng
}
