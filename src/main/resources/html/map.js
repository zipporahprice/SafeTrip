let map, baseLayer, heatmapLayer, markerLayer;
var javaScriptBridge; // must be declared as var (will not be correctly assigned in java with let keyword)
let markers = [];
var routes = [];


/**
 * This object can be returned to our java code, where we can call the functions we define inside it
 */
let jsConnector = {
    addMarker: addMarker,
    displayRoute: displayRoute,
    removeRoute: removeRoute,
    initMap: initMap,
    setData: setData
};

/**
 * creates and initialises the map, also defines on click event that calls java code
 */
function initMap() {
    baseLayer= new L.TileLayer('https://tile.csse.canterbury.ac.nz/hot/{z}/{x}/{y}.png', { // UCs tilemap server
        attribution: '© OpenStreetMap contributors<br>Served by University of Canterbury'
    });

    // Setup layers
    var cfg = {
        // radius should be small ONLY if scaleRadius is true (or small radius is intended)
        // if scaleRadius is false it will be the constant radius used in pixels
        "radius": 0.1,
        "maxOpacity": .4,
        // scales the radius based on map zoom
        "scaleRadius": true,
        // if set to false the heatmap uses the global maximum for colorization
        // if activated: uses the data maximum within the current map boundaries
        //   (there will always be a red spot with useLocalExtremas true)
        "useLocalExtrema": false,
        // which field name in your data represents the latitude - default "lat"
        latField: 'lat',
        // which field name in your data represents the longitude - default "lng"
        lngField: 'lng',
        // which field name in your data represents the data value - default "value"
        valueField: 'count'
    };
    heatmapLayer = new HeatmapOverlay(cfg);
    markerLayer = L.markerClusterGroup();

    // Setup map
    let mapOptions = {
        center: [-41.0, 172.0],
        zoom: 5.5,
        layers:[baseLayer, heatmapLayer]
    };
    map = new L.map('map', mapOptions);

    // Initialise layers and setup callbacks
    setFilteringViewport();
    setData();
    updateView();
    map.on('zoomend', updateDataShown);
    map.on('moveend', updateDataShown);
}

function updateDataShown() {
    setFilteringViewport();
    setData();
    updateView();
}

function setFilteringViewport() {
    const bounds = map.getBounds();
    const minLatitude = bounds.getSouth();
    const minLongitude = bounds.getWest();
    const maxLatitude = bounds.getNorth();
    const maxLongitude = bounds.getEast();
    javaScriptBridge.setFilterManagerViewport(minLatitude, minLongitude, maxLatitude, maxLongitude);
}

function automaticViewChange() {
    var zoomLevel = map.getZoom();
    if (zoomLevel >= 12) {
        if (map.hasLayer(heatmapLayer)) {
            map.removeLayer(heatmapLayer);
        }
        if (!map.hasLayer(markerLayer)) {
            map.addLayer(markerLayer);
        }
    }
    else {
        if (map.hasLayer(markerLayer)) {
            map.removeLayer(markerLayer);
        }
        if (!map.hasLayer(heatmapLayer)) {
            map.addLayer(heatmapLayer);
        }
    }
}

/**
 * Updates the view according to the user selection
 * Three views available:
 * Automatic - Changes from heatmap to crash locations at "12" zoomed in and back
 * Heatmap - Shows heatmap at all zooms
 * Crash Locations - Shows crash locations in clusters at all zooms
 */
function updateView() {
    var currentView = javaScriptBridge.currentView();

    if (currentView === "Automatic") {
        automaticViewChange();
        map.on('zoomend', automaticViewChange);
    } else if (currentView === "Heatmap") {
        map.off('zoomend', automaticViewChange);
        if (map.hasLayer(markerLayer)) {
            map.removeLayer(markerLayer);
        }
        map.addLayer(heatmapLayer);
        heatmapLayer.setData(testData);
    } else {
        map.off('zoomend', automaticViewChange);
        if (map.hasLayer(heatmapLayer)) {
            map.removeLayer(heatmapLayer);
        }
        map.addLayer(markerLayer);
    }
}


/**
 * Adds a marker to the map and stores it in the markers array for later use (e.g. removal)
 * @param title tooltip to display on hover
 * @param lat latitude to place marker at
 * @param lng longitude to place marker at
 */
function addMarker(title, lat, lng) {
    var m = new L.Marker([lat, lng])
    m.bindPopup(title).openPopup()
    m.addTo(map)
    markers.push(m)
}

/**
 * Displays a route with two or more waypoints for cars (e.g. roads and ferries) and displays it on the map
 * @param waypointsIn a string representation of an array of lat lng json objects [("lat": -42.0, "lng": 173.0), ...]
 */
function displayRoute(routesIn) {
    removeRoute();

    var routesArray = JSON.parse(routesIn);

    var currentRouteIndex = 0; // Starting index at 0
    var routeIndexMap = new Map(); // Map to hold routeId as key and currentRouteIndex as value

    routesArray.forEach((waypointsIn, index) => {
        var waypoints = [];
        waypointsIn.forEach(element => waypoints.push(new L.latLng(element.lat, element.lng)));

        var newRoute = L.Routing.control({
            waypoints: waypoints,
            routeWhileDragging: true,
            showAlternatives: true
        }).addTo(map);

        newRoute.on('routeselected', (e) => {
            var route = e.route;
            var coordinates = route.coordinates;

            // Generating or retrieving a unique identifier for the route.
            // You need to replace 'getRouteIdentifier(route)' with your actual logic of getting or generating an identifier.
            var routeId = getRouteIdentifier(route);

            // Check if this routeId has been selected before
            if (!routeIndexMap.has(routeId)) {
                // If not, add it to the map with the current index as its value
                routeIndexMap.set(routeId, currentRouteIndex);
                // Increment the current index for the next new route
                currentRouteIndex += 1;
            }

            // Retrieve the index associated with the routeId from the map
            var indexToSend = routeIndexMap.get(routeId);

            // Prepare and send the coordinates
            var coordinatesJson = JSON.stringify({
                routeId: indexToSend, // Use the index retrieved from the map
                coordinates: coordinates
            });
            javaScriptBridge.sendCoordinates(coordinatesJson);
        });

        routes.push(newRoute);
    });
}

function getRouteIdentifier(route) {
    // Assuming route.coordinates is an array of coordinate objects
    // And each coordinate can be represented as a string
    // You should replace this logic with the actual properties of your route objects
    if (!route || !route.coordinates) return null;

    // Convert each coordinate to a string and concatenate them
    // Ensure this provides a unique and consistent identifier for each route
    return route.coordinates.map(coord => coordToString(coord)).join(',');
}

function coordToString(coord) {
    // Convert coordinate object to a string
    // Replace this with the actual structure of your coordinate objects
    return `${coord.lat},${coord.lng}`; // Assuming a coord object has lat and lng properties
}





/**
 * Removes the current route being displayed (will not do anything if there is no route currently displayed)
 */
function removeRoute() {
    routes.forEach((r) => {
        r.remove();
    });
    routes = [];
}

function getSeverityStringFromValue(severity) {
    switch (severity) {
        case 1: return "Non-Injury";
        case 2: return "Minor Crash";
        case 4: return "Major Crash";
        case 8: return "Death";
        default: return "Invalid";
    }
}

function getMarkerIcon(severity) {
    var iconUrl;
    switch (severity) {
        case 1: // Non-Injury
            iconUrl = 'crash_markers/non_injury.png';
            break;
        case 2: // Minor Crash
            iconUrl = 'crash_markers/minor_crash.png';
            break;
        case 4: // Major Crash
            iconUrl = 'crash_markers/major_crash.png';
            break;
        case 8: // Death
            iconUrl = 'crash_markers/death_crash.png';
            break;
        default: // Default icon
            iconUrl = 'crash_markers/non_injury.png';
            break;
    }

    return L.icon({
        iconUrl: iconUrl,
        iconSize: [24, 32.7], // Adjust the icon size as needed
    });
}

function setData() {
    var crashesJSON = javaScriptBridge.crashes();
    var crashes = JSON.parse(crashesJSON);
    var testData = {
        max: 10,
        data: crashes
    };
    markerLayer.clearLayers();

    for (var i = 0; i < crashes.length; i++) {
        var a = crashes[i];
        var severity = getSeverityStringFromValue(a.severity);
        var markerIcon = getMarkerIcon(a.severity);
        var marker = L.marker(new L.LatLng(a.lat, a.lng), { title: severity, icon: markerIcon });
        marker.bindPopup("<div style='font-size: 16px;' class='popup-content'>" +
            "<p><strong>Latitude:</strong> " + a.lat + "</p>" +
            "<p><strong>Longitude:</strong> " + a.lng + "</p>" +
            "<p><strong>Severity:</strong> " + severity + "</p>" +
            "<p><strong>Year:</strong> " + a.year + "</p>" + // Add year
            "<p><strong>Weather:</strong> " + a.weather + "</p>" + // Add weather
            "</div>"
        );
        markerLayer.addLayer(marker);
    }

    heatmapLayer.setData(testData);
}
