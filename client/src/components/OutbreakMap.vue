<template>
  <div
    id="map"
    ref="mapElement"
  />
</template>

<script lang="ts" setup>
  import type { Outbreak } from '@/plugins/types/Outbreak'
  import L, { Marker, type LatLngExpression } from 'leaflet'
  import { useTheme } from 'vuetify'
  import { outbreakStatus } from '@/plugins/constants'

  import 'leaflet/dist/leaflet.css'

  import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
  import iconUrl from 'leaflet/dist/images/marker-icon.png'
  import shadowUrl from 'leaflet/dist/images/marker-shadow.png'
  import { isValidLatLng } from '@/plugins/misc'
  import { coreStore } from '@/stores/app'

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

  const mapElement = ref('')

  const store = coreStore()
  const vTheme = useTheme()

  let map
  let markers: Marker[] = []

  function updateMarkers () {
    if (markers && markers.length > 0) {
      markers.forEach(m => m.removeFrom(map))
    }

    markers = []

    props.outbreaks.forEach(o => {
      const isConfirmed = o.status === 'confirmed'
      const icon = L.divIcon({
        className: '',
        iconAnchor: isConfirmed ? [0, 24] : [0, 18],
        popupAnchor: isConfirmed ? [0, -36] : [0, -24],
        html: `<span class="marker-style marker-style-${o.status}" style="background-color: ${vTheme.current.value.colors[outbreakStatus.get(o.status).color] || 'grey'}" />`
      })

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

      if (latLng) {
        const marker = L.marker(latLng, {
          icon: icon,
        })
        marker.addTo(map)
        markers.push(marker)
      }
    })
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
