export interface DownloadRequest {
  blob: Blob
  filename: string
  extension: string
}

const isValidLatLng = (lat: number | undefined, lng: number | undefined) => {
  return lat !== undefined && lng !== undefined && isFinite(lat) && isFinite(lng)
}

/**
 * Downloads the data file given in the parameter using the blow, filename and extension.
 * @param {Object} object Object of type `{ filename: '', blob: '', extension: '' }`
 */
const downloadBlob = (object: DownloadRequest) => {
  if (!object || !object.blob) {
    return
  }

  const filename = object.filename
  const extension = object.extension

  const url = window.URL.createObjectURL(object.blob)

  const downloadLink = document.createElement('a')
  downloadLink.href = url
  downloadLink.target = '_blank'
  downloadLink.rel = 'noopener noreferrer'
  if (filename) {
    downloadLink.download = filename

    if (extension) {
      downloadLink.download += '.' + extension
    }
  }
  document.body.appendChild(downloadLink)
  downloadLink.click()
  document.body.removeChild(downloadLink)
}

export {
  isValidLatLng,
  downloadBlob
}
