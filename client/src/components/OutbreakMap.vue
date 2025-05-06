<template>
  <div
    :class="showOutbreakLink ? 'show-link' : null"
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
    <OutbreakDetails :outbreak="selectedOutbreak" />
    <v-btn v-if="showOutbreakLink" block color="primary" class="marker-button" :to="`/outbreak/${selectedOutbreak?.outbreakId}`">View outbreak</v-btn>
  </Teleport>
</template>

<script lang="ts" setup>
  import OutbreakDetails from '@/components/OutbreakDetails.vue'
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import L, { Control, Marker, type LatLngExpression } from 'leaflet'
  import 'leaflet.markercluster'
  import { useTheme } from 'vuetify'
  import { outbreakStatus } from '@/plugins/constants'
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
    showOutbreakLink?: boolean,
  }

  // PROPS
  const props = withDefaults(defineProps<Props>(), {
    outbreaks: () => [],
    showOutbreakLink: true,
  })

  // COMPOSITION
  const store = coreStore()
  const vTheme = useTheme()

  // REFS
  // HTML Element
  const mapElement = ref('')
  // User selection
  const selectedOutbreak = ref<Outbreak>()

  let map: any
  let markers: Marker[] = []
  let clusterer: any

  // WATCH
  watch(() => props.outbreaks, () => {
    updateMarkers()
  })

  // METHODS
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
        chunkedLoading: true,
        disableClusteringAtZoom: 9,
        polygonOptions: {
          color: 'rgb(var(--v-theme-secondary))'
        }
      })
      clusterer.on('click', (e: any) => {
        if (selectedOutbreak.value && selectedOutbreak.value.outbreakId === e.layer.data.outbreakId) {
          return
        }

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
      const status = o.status ? outbreakStatus.get(o.status) : undefined
      const icon = L.divIcon({
        className: '',
        iconAnchor: isConfirmed ? [0, 24] : [0, 18],
        popupAnchor: isConfirmed ? [0, -36] : [0, -24],
        html: `<span class="marker-style marker-style-${o.status}" style="background-color: ${(o.status && status) ? (vTheme.current.value.colors[status.color] || 'grey') : 'grey'}" />`
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
      // Admins or owners get to see precise location (if available)
      if (isValidLatLng(o.realLatitude, o.realLongitude)) {
        // @ts-ignore
        latLng = [o.realLatitude, o.realLongitude]
      }
    }

    if (!latLng) {
      // Fall-back to view location else
      if (isValidLatLng(o.viewLatitude, o.viewLongitude)) {
        // @ts-ignore
        latLng = [o.viewLatitude, o.viewLongitude]
      }
    }

    return latLng
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

    L.control.layers(baseMaps).addTo(map)

    const legend = new Control({ position: 'bottomleft' })
    legend.onAdd = () => {
      const div = L.DomUtil.create('div', 'info legend')
      div.classList.add('pa-2')
      outbreakStatus.forEach(status => {
        div.innerHTML += `<i style="color: ${vTheme.current.value.colors[status.color] || 'grey'}"><?xml version="1.0" encoding="UTF-8" standalone="no"?><svg viewBox="0 0 32 32" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"><g><path id="path1" style="fill:currentColor;" d="m 25.192388,4.1152238 a 13,13 0 0 0 -18.3847761,0 13,13 0 0 0 0,18.3847762 L 16,31.692388 25.192388,22.5 a 13,13 0 0 0 0,-18.3847762 z"/></g></svg></i><span>${status.text}</span><br>`
      })
      return div
    }
    legend.addTo(map)
    
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
#map.show-link .leaflet-popup-tip {
  background-color: rgb(var(--v-theme-primary));
}

.leaflet-container .leaflet-marker-pane img.marker-image {
  width: inherit;
}

.marker-cluster {
  color: white;
  background-color: rgba(var(--v-theme-primary), 0.6);
}
.marker-cluster div {
  background-color: rgba(var(--v-theme-primary), 0.8);
}

.marker-cluster-small {
  filter: brightness(130%);
}
.marker-cluster-small div {
  filter: brightness(130%);
}

.marker-cluster-medium {
  filter: brightness(115%);
}
.marker-cluster-medium div {
  filter: brightness(115%);
}

.legend {
  background: white;
  background: rgba(255, 255, 255, 0.8);
  line-height: 1.75em;
  color: #555;
}
.legend i {
  width: 1.5em;
  height: 1.5em;
  float: left;
  margin-right: 0.5em;
}
</style>
