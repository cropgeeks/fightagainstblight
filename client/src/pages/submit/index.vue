<template>
  <BackButton />

  <h1>Sample submission</h1>

  <section v-if="store.token && store.token.user">
    <p>Please complete the form below to submit your sample.</p>

    <v-row>
      <v-col :cols=12 :lg=3 :md=6>
        <UseGeolocation v-slot="{ coords: { latitude, longitude } }">
          <v-text-field
            v-model="postcode"
            :append-inner-icon="(isFinite(latitude) && isFinite(longitude)) ? 'mdi-map-marker' : 'mdi-map-marker-off'"
            :error="postcodeValid === false"
            hide-details
            label="Full postcode"
            @blur="validatePostcode"
            @click:append-inner="setGps(latitude, longitude)"
            @keydown.enter.exact="$event.target.blur()"
          />
        </UseGeolocation>

        <v-list v-if="selectedPostcode">
          <v-list-item :title="selectedPostcode.nuts" :subtitle="selectedPostcode.country" />
        </v-list>

        <v-list
          v-if="postcodeOptions"
          class="mt-0 pt-0 mh-30vh"
          :items="postcodeOptions"
          item-props
          @click:select="selectPostcode"
        >
          <template #title="{ item }">
            <div class="v-list-item-title d-flex justify-space-between">
              <span>{{ item.value.postcode }}</span>
              <span
                v-if="item.value.distance"
                class="text-grey"
              >
                {{ item.value.distance.toFixed(0) }}m
              </span>
            </div>
          </template>
        </v-list>
      </v-col>
      <v-col :cols=12 :lg=3 :md=6>
        <v-autocomplete
          v-model="selectedSeverity"
          clearable
          label="Severity"
          :items="severityOptions"
        />
        <v-text-field
          v-model="severityOther"
          label="Other severity (if not listed above)"
        />
      </v-col>
      <v-col :cols=12 :lg=3 :md=6>
        <v-autocomplete
          v-model="selectedSource"
          clearable
          label="Source"
          :items="sourceOptions"
        />
        <v-text-field
          v-model="sourceOther"
          label="Other source (if not listed above)"
        />
      </v-col>
      <v-col :cols=12 :lg=3 :md=6>
        <v-autocomplete
          v-model="selectedVariety"
          clearable
          label="Variety"
          :items="varietyOptions"
        />
        <v-textarea
          v-model="comment"
          label="Comment"
        />
      </v-col>
    </v-row>

    <v-btn
      color="primary"
      :disabled="!canContinue || submitting"
      @click="onSubmit"
    >
      Submit
    </v-btn>

    <p class="mt-3">You can also use this map to locate the outbreak position by either clicking on the map or dragging the marker to the location of the outbreak.</p>

    <div
      id="map"
      ref="mapElement"
    />
  </section>
  <section v-else>
    <router-link to="/login">Please log in to submit an outbreak.</router-link>
  </section>
</template>

<script lang="ts" setup>
  import axios from 'axios'
  import L, { Map, Marker } from 'leaflet'
  import 'leaflet/dist/leaflet.css'

  import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
  import iconUrl from 'leaflet/dist/images/marker-icon.png'
  import shadowUrl from 'leaflet/dist/images/marker-shadow.png'
  import { UseGeolocation } from '@vueuse/components'

  // Set the leaflet marker icon
  // @ts-ignore
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: iconRetinaUrl,
    iconUrl: iconUrl,
    shadowUrl: shadowUrl
  })

  interface PostcodeOption {
    value: Postcode
    title: string
    subtitle: string
  }

  interface Postcode {
    country: string
    outcode: string
    postcode: string
    distance?: number
    admin_county?: string
    admin_ward?: string
    admin_district?: string
    latitude?: number
    longitude?: number,
    nuts?: string
  }

  import { coreStore } from '@/stores/app'
  import type { Severity } from '@/plugins/types/Severity'
  import type { Source } from '@/plugins/types/Source'
  import { axiosCall } from '@/plugins/api'
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import router from '@/router'
  import type { Variety } from '@/plugins/types/Variety'
  import type { SelectOption } from '@/plugins/types/SelectOption'
  const store = coreStore()

  // Refs
  const mapElement = ref('')
  const gpsLatitude = ref<number | undefined>()
  const gpsLongitude = ref<number | undefined>()
  const postcode = ref<string | undefined>()
  const selectedPostcode = ref<Postcode | undefined>()
  const postcodeValid = ref<boolean | undefined>()
  const varieties = ref<Variety[]>([])
  const postcodeOptions = ref<PostcodeOption[]>([])
  const severities = ref<Severity[]>([])
  const sourceOther = ref<string>()
  const selectedSource = ref<number>()
  const selectedSeverity = ref<number>()
  const selectedVariety = ref<number>()
  const severityOther = ref<string>()
  const sources = ref<Source[]>([])
  const comment = ref<string>()
  const submitting = ref<boolean>(false)

  let map: Map
  let marker: Marker

  // Get all the filtering options
  axiosCall<Severity[]>({ url: 'severities' })
    .then((result: Severity[]) => {
      severities.value = result
    })
  axiosCall<Source[]>({ url: 'sources' })
    .then((result: Source[]) => {
      sources.value = result
    })

  function validatePostcode () {
    postcodeValid.value = undefined
    selectedPostcode.value = undefined
    if (postcode.value) {
      axios.get(`https://api.postcodes.io/postcodes/${postcode.value}`).then(r => {
        selectedPostcode.value = r.data.result
        gpsLatitude.value = r.data.result.latitude
        gpsLongitude.value = r.data.result.longitude
        if (gpsLatitude.value && gpsLongitude.value) {
          marker.setLatLng([gpsLatitude.value, gpsLongitude.value])
          map.setView([gpsLatitude.value, gpsLongitude.value], 14)
        }
        postcodeValid.value = true
      }).catch(() => {
        postcodeValid.value = false
      })
    }
  }

  function selectPostcode (props: any) {
    selectedPostcode.value = props.id
    postcodeOptions.value = []
    postcode.value = props.id.postcode
    gpsLatitude.value = props.id.latitude
    gpsLongitude.value = props.id.longitude
    postcodeValid.value = true
  }

  function getPostCodeFromGps (lat: number, lng: number, isExtended: boolean = false, isWide: boolean = false) {
    return new Promise<Postcode[]>((resolve, reject) => {
      axios.get(`https://api.postcodes.io/postcodes?lon=${lng}&lat=${lat}&radius=${isExtended ? 2000 : 100}&widesearch=${isWide}`).then(response  => {
        if (response && response.data && response.data.result) {
          resolve(response.data.result)
        } else if (!isExtended) {
          getPostCodeFromGps(lat, lng, true)
            .then(result => resolve(result))
            .catch(e => reject(e))
        } else if (!isWide) {
          getPostCodeFromGps(lat, lng, true, true)
            .then(result => resolve(result))
            .catch(e => reject(e))
        } else {
          return []
        }
      }).catch(e => reject(e))
    })
  }

  function setGps (lat: number, lng: number) {
    if (isFinite(lat) && isFinite(lng)) {
      gpsLatitude.value = lat
      gpsLongitude.value = lng
      postcode.value = undefined
      postcodeOptions.value = []
      postcodeValid.value = false
      selectedPostcode.value = undefined

      marker.setLatLng([lat, lng])
      map.setView([lat, lng], 14)

      getPostCodeFromGps(lat, lng, false)
        .then((result: Postcode[]) => {
          postcodeOptions.value = result
            .sort((a: Postcode, b: Postcode) => Math.sign((a.distance || 0) - (b.distance || 0)))
            .map((r: Postcode) => {
              return {
                value: r,
                title: '',
                // title: `${r.postcode}${r.distance ? ` (${r.distance.toFixed(1)}m)` : ''}`,
                subtitle: [r.admin_county, r.admin_district, r.admin_ward].filter(a => a).join(' - ')
              }
            })
        })
    }
  }

  function initMap () {
    map = L.map(mapElement.value)
    map.setView([53.971790, -2.323987], 5)

    const openstreetmap = L.tileLayer('//tile.openstreetmap.org/{z}/{x}/{y}.png', {
      id: 'OpenStreetMap',
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    })
    // Add an additional satellite layer
    const satellite = L.tileLayer('//server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
      id: 'Esri WorldImagery',
      attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
    })

    const baseMaps = {
      OpenStreetMap: openstreetmap,
      'Esri WorldImagery': satellite
    }

    map.addLayer(openstreetmap)
    map.on('click', e => {
      setGps(e.latlng.lat, e.latlng.lng)
    })

    marker = L.marker([53.971790, -2.323987], {
      draggable: true,
      autoPan: true,
    })
    marker.on('moveend', () => {
      setGps(marker.getLatLng().lat, marker.getLatLng().lng)
    })
    marker.addTo(map)

    L.control.layers(baseMaps).addTo(map)
    
    // Disable zoom until focus gained, disable when blur
    // map.scrollWheelZoom.disable()
    // map.on('focus', () => map.scrollWheelZoom.enable())
    // map.on('blur', () => map.scrollWheelZoom.disable())
  }

  const canContinue: ComputedRef<boolean> = computed(() => {
    if (!selectedSeverity.value && (!severityOther.value || severityOther.value.trim().length < 1)) {
      return false
    }
    if (!selectedSource.value && (!sourceOther.value || sourceOther.value.trim().length < 1)) {
      return false
    }
    if (!postcode.value) {
      return false
    }
    if (!gpsLatitude.value || !gpsLongitude.value) {
      return false
    }

    return true
  })

  const severityOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (severities.value) {
      return severities.value.map(s => {
        return {
          value: s.severityId,
          title: s.severityName
        }
      })
    } else {
      return []
    }
  })

  const sourceOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (sources.value) {
      return sources.value.map(s => {
        return {
          value: s.sourceId,
          title: s.sourceName
        }
      })
    } else {
      return []
    }
  })

  const varietyOptions: ComputedRef<SelectOption<number>[]> = computed(() => {
    if (varieties.value) {
      return varieties.value.sort((a: Variety, b: Variety) => a.varietyName.localeCompare(b.varietyName)).map(s => {
        return {
          value: s.varietyId,
          title: s.varietyName
        }
      })
    } else {
      return []
    }
  })

  function onSubmit () {
    submitting.value = true
    const outbreak: Outbreak = {
      severityId: selectedSeverity.value,
      severityOther: severityOther.value,
      sourceId: selectedSource.value,
      sourceOther: sourceOther.value,
      userComment: comment.value,
      realLatitude: gpsLatitude.value,
      realLongitude: gpsLongitude.value,
      postcode: postcode.value ? postcode.value.replace(/\s+/g, '').toUpperCase() : undefined,
    }

    axiosCall<Outbreak>({ url: 'outbreaks', method: 'POST', params: outbreak })
      .then((result: Outbreak) => {
        submitting.value = false
        router.push(`/outbreak/${result.outbreakId}`)
      })
      .catch(e => {
        submitting.value = false
        console.error(e)
      })
  }

  if (store.token && store.token.user) {
    axiosCall<Variety[]>({ url: 'varieties' })
      .then((result: Variety[]) => {
        varieties.value = result
      })

    onMounted(() => initMap())
  }
</script>

<style>
#map {
  height: 50vh;
}
.mh-30vh {
  max-height: 30vh;
}
</style>
