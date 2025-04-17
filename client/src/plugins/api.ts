// @ts-nocheck

import axios from 'axios'
import emitter from 'tiny-emitter/instance'
import { coreStore } from '@/stores/app'

const acceptableStatusCodes = [400, 401, 403, 404, 409]

/**
 * Sends an axios REST request to the server with the given parameter configuration
 * @param {String} url The remote URL (relative) to send the request to
 * @param {Object} params (Optional) Request payload in the form of a Javascript object
 * @param {String} method (Optional) REST method (default: `'get'`)
 * @returns Promise
 */
const axiosCall = <T> ({ baseUrl = null, url = '', params = null, method = 'get', contentType = 'application/json; charset=utf-8', ignoreErrors = false }: {baseUrl?: string | null, url: string, params?: Object | null, method?: string, contentType?: string, ignoreErrors?: boolean }) => {
  const store = coreStore()
  let requestData = null
  let requestParams = null

  // Stringify the data object for non-GET requests
  if (params !== null || params !== undefined) {
    if (method === 'get') {
      requestParams = params
    } else {
      requestData = params
    }
  }

  const headers = {
    'Content-Type': contentType || 'application/json; charset=utf-8'
  }

  if (store.token && store.token.token) {
    headers['Authorization'] = `Bearer ${store.token.token}`
  }

  return new Promise<T>((resolve, reject) => {
    axios.default({
      baseURL: baseUrl || store.baseUrl,
      url,
      params: requestParams,
      data: requestData,
      method,
      crossDomain: true,
      withCredentials: store.token !== null && store.token.token !== null,
      headers: headers
    }).then(data => {
      if (data && data.data) {
        resolve(data.data)
      } else {
        resolve(null)
      }
    }).catch(error => {
      emitter.emit('show-loading', false)
      if (!ignoreErrors) {
        if (error.response) {
          // The request was made and the server responded with a status code
          // that falls out of the range of 2xx
          if (acceptableStatusCodes.includes(error.response.status)) {
            const err = new Error('API error')
            err.status = error.response.status
            reject(err)
            return
          } else {
            if (error.response.status === 401 || error.response.status === 403) {
              store.setToken(null)
            }
            
            // Handle the error here, then reject
            emitter.emit('api-error', error.response)
          }
        } else if (error.request) {
          // The request was made but no response was received
          // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
          // http.ClientRequest in node.js
          // Handle the error here, then reject
          emitter.emit('api-error', error.request)
        } else {
          // Something happened in setting up the request that triggered an Error
          // Handle the error here, then reject
          emitter.emit('api-error', error.message)
        }
      }

      reject(new Error('API error', { cause: error }))
    })
  })
}

export {
  axiosCall,
}
