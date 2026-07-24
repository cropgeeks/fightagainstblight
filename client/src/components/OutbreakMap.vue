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

  <v-btn-group variant="tonal" color="info" v-if="isSingleMarker === false">
    <v-btn icon="mdi-gradient-horizontal" @click="showDateGradient = !showDateGradient" :active="showDateGradient" v-tooltip:top="`Colour by 'Sample received on' date`" />
    <v-btn icon="mdi-map-marker-distance" @click="clusterMapMarkers = !clusterMapMarkers" :active="clusterMapMarkers" v-tooltip:top="`Cluster map markers`" />
  </v-btn-group>
</template>

<script lang="ts" setup>
  import OutbreakDetails from '@/components/OutbreakDetails.vue'
  import L, { Control, LatLng, Marker } from 'leaflet'
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
  import type { HighlightOutbreak } from '@/plugins/types/client'

  // Set the leaflet marker icon
  // @ts-ignore
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: iconRetinaUrl,
    iconUrl: iconUrl,
    shadowUrl: shadowUrl
  })

  interface Props {
    outbreaks: HighlightOutbreak[],
    showOutbreakLink?: boolean,
    legendConfig?: LegendConfig,
  }

  export interface LegendConfig {
    minLabel?: string
    minColor?: string
    maxLabel?: string
    maxColor?: string
  }

  // PROPS
  const props = withDefaults(defineProps<Props>(), {
    outbreaks: () => [],
    showOutbreakLink: true,
    legendConfig: undefined,
  })

  // COMPOSITION
  const store = coreStore()
  const vTheme = useTheme()

  // REFS
  const showDateGradient = ref(false)
  const clusterMapMarkers = ref(true)
  // HTML Element
  const mapElement = ref('')
  // User selection
  const selectedOutbreak = ref<HighlightOutbreak>()

  let map: any
  let legend: any
  let markers: Marker[] = []
  let clusterer: any

  const isSingleMarker = computed(() => props.outbreaks.length === 1)

  // WATCH
  watch(() => props.outbreaks, () => {
    selectedOutbreak.value = undefined
    updateMarkers()
  })

  // METHODS
  function updateMarkers (recreateClusterer = false) {
    if (legend) {
      map.removeControl(legend)
    }

    legend = new Control({ position: 'bottomleft' })
    legend.onAdd = () => {
      const div = L.DomUtil.create('div', 'info legend')
      div.classList.add('pa-2')

      if (props.legendConfig && showDateGradient.value) {
        div.innerHTML += `<i style="color: ${props.legendConfig.minColor}"><?xml version="1.0" encoding="UTF-8" standalone="no"?><svg viewBox="0 0 32 32" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"><g><path id="path1" style="fill:currentColor;" d="m 25.192388,4.1152238 a 13,13 0 0 0 -18.3847761,0 13,13 0 0 0 0,18.3847762 L 16,31.692388 25.192388,22.5 a 13,13 0 0 0 0,-18.3847762 z"/></g></svg></i><span>${props.legendConfig.minLabel}</span><br />`
        div.innerHTML += `<i style="color: ${props.legendConfig.maxColor}"><?xml version="1.0" encoding="UTF-8" standalone="no"?><svg viewBox="0 0 32 32" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"><g><path id="path1" style="fill:currentColor;" d="m 25.192388,4.1152238 a 13,13 0 0 0 -18.3847761,0 13,13 0 0 0 0,18.3847762 L 16,31.692388 25.192388,22.5 a 13,13 0 0 0 0,-18.3847762 z"/></g></svg></i><span>${props.legendConfig.maxLabel}</span><br />`

        outbreakStatus.forEach(status => {
          if (status.dbValue !== 'confirmed') {
            div.innerHTML += `<i style="color: ${vTheme.current.value.colors[status.color] || 'grey'}"><?xml version="1.0" encoding="UTF-8" standalone="no"?><svg viewBox="0 0 32 32" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"><g><path id="path1" style="fill:currentColor;" d="m 25.192388,4.1152238 a 13,13 0 0 0 -18.3847761,0 13,13 0 0 0 0,18.3847762 L 16,31.692388 25.192388,22.5 a 13,13 0 0 0 0,-18.3847762 z"/></g></svg></i><span>${status.text}</span><br />`
          }
        })
      } else {
        outbreakStatus.forEach(status => {
          div.innerHTML += `<i style="color: ${vTheme.current.value.colors[status.color] || 'grey'}"><?xml version="1.0" encoding="UTF-8" standalone="no"?><svg viewBox="0 0 32 32" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg"><g><path id="path1" style="fill:currentColor;" d="m 25.192388,4.1152238 a 13,13 0 0 0 -18.3847761,0 13,13 0 0 0 0,18.3847762 L 16,31.692388 25.192388,22.5 a 13,13 0 0 0 0,-18.3847762 z"/></g></svg></i><span>${status.text}</span><br />`
        })
      }
      return div
    }
    legend.addTo(map)

    if (markers && markers.length > 0) {
      markers.forEach(m => m.removeFrom(map))
    }
    // Remove the old geojson layer if required
    if (clusterer) {
      clusterer.clearLayers()

      if (recreateClusterer) {
        map.removeLayer(clusterer)
        clusterer = undefined
      }
    }
    
    if (!clusterer) {
      // @ts-ignore
      clusterer = L.markerClusterGroup({
        chunkedLoading: true,
        disableClusteringAtZoom: clusterMapMarkers.value ? 9 : 1,
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

    const bounds = L.latLngBounds([])

    const locations = new Set<string>()
    props.outbreaks.forEach(o => {
      const isConfirmed = o.status === 'confirmed'
      const status = o.status ? outbreakStatus.get(o.status) : undefined
      const icon = L.divIcon({
        className: '',
        iconAnchor: isConfirmed ? [0, 24] : [0, 18],
        popupAnchor: isConfirmed ? [0, -36] : [0, -24],
        tooltipAnchor: isConfirmed ? [0, -36] : [0, -24],
        html: `<span class="marker-style marker-style-${o.status}" style="background-color: ${(showDateGradient.value && o.status === 'confirmed') ? o.highlightColor : ((o.status && status) ? (vTheme.current.value.colors[status.color] || 'grey') : 'grey')}" />`
      })

      const latLng = getLatLng(o)

      if (latLng) {
        if (locations.has(`${latLng.lat}|${latLng.lng}`)) {
          latLng.lat += (Math.random() - 0.5) * 0.0008
          latLng.lng += (Math.random() - 0.5) * 0.0008
        }

        locations.add(`${latLng.lat}|${latLng.lng}`)

        const marker = new DataMarker<HighlightOutbreak>(latLng, o, {
          icon: icon,
        })
        marker.bindPopup('')
        marker.bindTooltip(`${status?.text} · ${new Date(o.dateSubmitted || '').toLocaleDateString()}`, { direction: 'top' })
        bounds.extend(marker.getLatLng())
        if (isSingleMarker.value) {
          marker.addTo(map)
          markers.push(marker)
        } else {
          clusterer.addLayer(marker)
        }
      }
    })

    if (isSingleMarker.value) {
      const latLng = getLatLng(props.outbreaks[0])
      if (latLng) {
        map.setView(latLng, 12)
      }

      setTimeout(() => {
        markers[0].openPopup()
        nextTick(() => {
          selectedOutbreak.value = props.outbreaks[0]
        })
      }, 300)
    } else {
      if (bounds && bounds.isValid()) {
        map.fitBounds(bounds, { padding: [25, 25] })
      }
    }
  }

  function getLatLng (o: HighlightOutbreak): LatLng | undefined {
    let latLng: LatLng | undefined = undefined
    if (store.token && store.token.user && (store.token.user.isAdmin || store.token.user.userId === o.userId)) {
      // Admins or owners get to see precise location (if available)
      if (isValidLatLng(o.realLatitude, o.realLongitude)) {
        // @ts-ignore
        latLng = L.latLng(o.realLatitude, o.realLongitude)
      }
    }

    if (!latLng) {
      // Fall-back to view location else
      if (isValidLatLng(o.viewLatitude, o.viewLongitude)) {
        // @ts-ignore
        latLng = L.latLng(o.viewLatitude, o.viewLongitude)
      }
    }

    return latLng
  }

  function initMap () {
    map = L.map(mapElement.value)

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
    map.scrollWheelZoom.disable()
    map.on('focus', () => map.scrollWheelZoom.enable())
    map.on('blur', () => map.scrollWheelZoom.disable())

    map.setView([53.971790, -2.323987], 5)

    nextTick(() => updateMarkers())
  }

  watch(() => props.legendConfig, async () => {
    updateMarkers(true)
  }, { deep: true })

  watch(showDateGradient, async () => updateMarkers())

  watch(clusterMapMarkers, async () => updateMarkers(true))

  onMounted(() => initMap())
</script>

<style>
#map {
  height: 65vh;
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
