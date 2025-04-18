<template>
  <div
    id="map"
    ref="mapElement"
  />

  <!-- <div ref="popupContent" v-if="selectedOutbreak">
    <span>{{ selectedOutbreak }}</span>
    <v-btn block color="primary" class="marker-button" :to="`/outbreak/${selectedOutbreak.outbreakId}`">View outbreak</v-btn>
  </div> -->

  <!-- Teleport is a Vue 3 feature; it basically appends the component
  to any DOM target (:to). Here, we point it to the content class of the Leaflet popup. Since only one popup is open at a time (presumably) this is safe. Otherwise you'd need to create a unique ID when creating the Leaflet popup. -->
  <Teleport v-if="selectedOutbreak" :to="`.leaflet-popup-content${selectedOutbreak.outbreakId ? '' : ''}`" :key="selectedOutbreak.outbreakId">
    <v-list>
      <v-list-item
        title="Outbreak code"
        :subtitle="selectedOutbreak.outbreakCode"
      />
      <v-list-item
        title="Reported on"
        :subtitle="selectedOutbreak.dateSubmitted ? new Date(selectedOutbreak.dateSubmitted).toLocaleDateString() : 'N/A'"
      />
      <v-list-item
        title="Status">
        <template #subtitle>
          <v-chip
            v-if="selectedOutbreak.status"
            :color="status.get(selectedOutbreak.status)?.color"
            :prepend-icon="status.get(selectedOutbreak.status)?.icon"
          >
            {{ status.get(selectedOutbreak.status)?.text }}
          </v-chip>
        </template>
      </v-list-item>
      <v-list-item
        title="Severity">
        <template #subtitle>
          <v-chip v-if="selectedOutbreak.severityName">
            <v-img
              class="me-3"
              contains
              height="20"
              :src="`/img/severity/${selectedOutbreak.severityName.toLowerCase().replaceAll(/[\s\/]+/g, '-')}.svg`"
              width="20"
            />
            <span>{{ selectedOutbreak.severityName }}</span>
          </v-chip>
        </template>
      </v-list-item>
      <v-list-item
        title="Source">
        <template #subtitle>
          <v-chip v-if="selectedOutbreak.sourceName">
            <v-img
              class="me-3"
              contains
              height="20"
              :src="`/img/source/${selectedOutbreak.sourceName.toLowerCase().replaceAll(/[\s\/]+/g, '-')}.svg`"
              width="20"
            />
            <span>{{ selectedOutbreak.sourceName }}</span>
          </v-chip>
        </template>
      </v-list-item>
    </v-list>
    <v-btn block color="primary" class="marker-button" :to="`/outbreak/${selectedOutbreak?.outbreakId}`">View outbreak</v-btn>
  </Teleport>
</template>

<script lang="ts" setup>
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import L, { Marker, type LatLngExpression } from 'leaflet'
  import 'leaflet.markercluster'
  import { useTheme } from 'vuetify'
  import { outbreakStatus, type Status } from '@/plugins/constants'

  import 'leaflet/dist/leaflet.css'
  import 'leaflet.markercluster/dist/MarkerCluster.css'
  import 'leaflet.markercluster/dist/MarkerCluster.Default.css'

  import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
  import iconUrl from 'leaflet/dist/images/marker-icon.png'
  import shadowUrl from 'leaflet/dist/images/marker-shadow.png'
  import { isValidLatLng } from '@/plugins/misc'
  import { coreStore } from '@/stores/app'
  import { DataMarker } from '@/plugins/types/DataMarker'

  // Set the leaflet marker icon
  // @ts-ignore
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: iconRetinaUrl,
    iconUrl: iconUrl,
    shadowUrl: shadowUrl
  })

  interface Props {
    outbreaks: Outbreak[],
  }

  const props = withDefaults(defineProps<Props>(), {
    outbreaks: () => [],
  })

  const selectedOutbreak = ref<Outbreak>()
  const mapElement = ref('')
  const status = ref<Map<string, Status>>(outbreakStatus)

  const store = coreStore()
  const vTheme = useTheme()

  let map: any
  let markers: Marker[] = []
  let clusterer: any

  function updateMarkers () {
    if (markers && markers.length > 0) {
      markers.forEach(m => m.removeFrom(map))
    }
    // Remove the old geojson layer if required
    if (clusterer) {
      clusterer.clearLayers()
    } else {
      // @ts-ignore
      clusterer = L.markerClusterGroup({
        chunkedLoading: true
      })
      clusterer.on('click', (e: any) => {
        selectedOutbreak.value = undefined
        setTimeout(() => {
          nextTick(() => {
            selectedOutbreak.value = e.layer.data
          })
        }, 200)
      })
      map.addLayer(clusterer)
    }

    markers = []

    const isSingleMarker = props.outbreaks.length === 1
    const bounds = L.latLngBounds([])

    props.outbreaks.forEach(o => {
      const isConfirmed = o.status === 'confirmed'
      const icon = L.divIcon({
        className: '',
        iconAnchor: isConfirmed ? [0, 24] : [0, 18],
        popupAnchor: isConfirmed ? [0, -36] : [0, -24],
        html: `<span class="marker-style marker-style-${o.status}" style="background-color: ${(o.status && outbreakStatus.get(o.status)) ? (vTheme.current.value.colors[outbreakStatus.get(o.status).color] || 'grey') : 'grey'}" />`
      })

      const latLng = getLatLng(o)

      if (latLng) {
        const marker = new DataMarker<Outbreak>(latLng, o, {
          icon: icon,
        })
        marker.bindPopup('')
        bounds.extend(marker.getLatLng())
        if (isSingleMarker) {
          marker.addTo(map)
          markers.push(marker)
        } else {
          clusterer.addLayer(marker)
        }
      }
    })

    if (isSingleMarker) {
      const latLng = getLatLng(props.outbreaks[0])
      if (latLng) {
        map.setView(latLng, 12)
      }

      markers[0].openPopup()
      nextTick(() => {
        selectedOutbreak.value = props.outbreaks[0]
      })
    } else {
      if (bounds && bounds.isValid()) {
        map.fitBounds(bounds, { padding: [25, 25] })
      }
    }
  }

  function getLatLng (o: Outbreak): LatLngExpression | undefined {
    let latLng: LatLngExpression | undefined = undefined
    if (store.token && store.token.user && (store.token.user.isAdmin || store.token.user.userId === o.userId)) {
      if (isValidLatLng(o.realLatitude, o.realLongitude)) {
        // @ts-ignore
        latLng = [o.realLatitude, o.realLongitude]
      } else if (isValidLatLng(o.viewLatitude, o.viewLongitude)) {
        // @ts-ignore
        latLng = [o.viewLatitude, o.viewLongitude]
      }
    } else {
      if (isValidLatLng(o.viewLatitude, o.viewLongitude)) {
        // @ts-ignore
        latLng = [o.viewLatitude, o.viewLongitude]
      }
    }
    return latLng
  }

  watch(() => props.outbreaks, () => {
    updateMarkers()
  })

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

    L.control.layers(baseMaps).addTo(map)
    
    // Disable zoom until focus gained, disable when blur
    // map.scrollWheelZoom.disable()
    // map.on('focus', () => map.scrollWheelZoom.enable())
    // map.on('blur', () => map.scrollWheelZoom.disable())

    updateMarkers()
  }

  onMounted(() => initMap())
</script>

<style>
#map {
  height: 50vh;
}

.marker-style {
  width: 1.5rem;
  height: 1.5rem;
  left: -0.75rem;
  top: -0.75rem;
  border-radius: 1.5rem 1.5rem 0;
  display: block;
  position: relative;
  transform: rotate(45deg);
  border: 1px solid #FFFFFF;
}

.marker-style-confirmed {
  width: 2rem;
  height: 2rem;
  left: -1rem;
  top: -1rem;
  border-radius: 2rem 2rem 0;
}
</style>

<style>
#map .leaflet-popup-content-wrapper {
  border-radius: 0;
}
#map .leaflet-popup-content {
  margin: 0;
  width: 300px !important;
}
#map .leaflet-popup-content .v-btn {
  border-radius: 0;
}
#map .leaflet-popup-tip {
  background-color: rgb(var(--v-theme-primary));
}

.leaflet-container .leaflet-marker-pane img.marker-image {
  width: inherit;
}
</style>
