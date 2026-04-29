/* Leaflet 1.9.4 - Version simplifiée pour JavaFX WebView */
(function(window, document) {
    'use strict';

    var L = window.L = {};

    // Point class
    L.Point = function(x, y, round) {
        this.x = round ? Math.round(x) : x;
        this.y = round ? Math.round(y) : y;
    };

    L.Point.prototype = {
        clone: function() { return new L.Point(this.x, this.y); },
        add: function(point) { return this.clone()._add(point); },
        _add: function(point) {
            this.x += point.x;
            this.y += point.y;
            return this;
        },
        subtract: function(point) { return this.clone()._subtract(point); },
        _subtract: function(point) {
            this.x -= point.x;
            this.y -= point.y;
            return this;
        },
        divideBy: function(num) { return this.clone()._divideBy(num); },
        _divideBy: function(num) {
            this.x /= num;
            this.y /= num;
            return this;
        },
        multiplyBy: function(num) { return this.clone()._multiplyBy(num); },
        _multiplyBy: function(num) {
            this.x *= num;
            this.y *= num;
            return this;
        },
        distanceTo: function(point) {
            var dx = point.x - this.x, dy = point.y - this.y;
            return Math.sqrt(dx * dx + dy * dy);
        }
    };

    // LatLng class
    L.LatLng = function(lat, lng, alt) {
        if (isNaN(lat) || isNaN(lng)) {
            throw new Error('Invalid LatLng object: (' + lat + ', ' + lng + ')');
        }
        this.lat = +lat;
        this.lng = +lng;
        this.alt = alt === undefined ? undefined : +alt;
    };

    L.LatLng.prototype = {
        clone: function() { return new L.LatLng(this.lat, this.lng, this.alt); },
        distanceTo: function(other) {
            return 6371000 * Math.acos(Math.max(-1, Math.min(1,
                Math.sin(this.lat * Math.PI / 180) * Math.sin(other.lat * Math.PI / 180) +
                Math.cos(this.lat * Math.PI / 180) * Math.cos(other.lat * Math.PI / 180) *
                Math.cos((other.lng - this.lng) * Math.PI / 180)
            )));
        }
    };

    // Bounds class
    L.Bounds = function(a, b) {
        if (!a) { return; }
        var points = b ? [a, b] : a;
        for (var i = 0, len = points.length; i < len; i++) {
            this.extend(points[i]);
        }
    };

    L.Bounds.prototype = {
        extend: function(point) {
            if (!this.min && !this.max) {
                this.min = point.clone();
                this.max = point.clone();
            } else {
                this.min.x = Math.min(point.x, this.min.x);
                this.max.x = Math.max(point.x, this.max.x);
                this.min.y = Math.min(point.y, this.min.y);
                this.max.y = Math.max(point.y, this.max.y);
            }
            return this;
        },
        getCenter: function() {
            return new L.Point(
                (this.min.x + this.max.x) / 2,
                (this.min.y + this.max.y) / 2
            );
        },
        contains: function(point) {
            return point.x >= this.min.x && point.x <= this.max.x &&
                   point.y >= this.min.y && point.y <= this.max.y;
        }
    };

    // Map class
    L.Map = function(id, options) {
        options = options || {};
        this._container = typeof id === 'string' ? document.getElementById(id) : id;
        this._container._leaflet_id = this._leaflet_id = Math.floor(Math.random() * 0xFFFFFF);
        
        this._initLayout();
        this._initEvents();
        
        this._zoom = options.zoom || 1;
        this._center = options.center || new L.LatLng(0, 0);
        this._layers = [];
        
        this.fire('load');
    };

    L.Map.prototype = {
        setView: function(center, zoom) {
            this._center = center;
            this._zoom = zoom;
            this._resetView(center, zoom);
            return this;
        },
        setZoom: function(zoom) {
            return this.setView(this.getCenter(), zoom);
        },
        getCenter: function() {
            return this._center;
        },
        getZoom: function() {
            return this._zoom;
        },
        getBounds: function() {
            var bounds = new L.Bounds();
            bounds.extend(this.project(this._center, this._zoom));
            return bounds;
        },
        project: function(latlng, zoom) {
            zoom = zoom === undefined ? this._zoom : zoom;
            var d = 6378137 * Math.PI / 180,
                max = 85.0511287798,
                lat = Math.max(Math.min(max, latlng.lat), -max),
                sin = Math.sin(lat * Math.PI / 180),
                x = latlng.lng * d,
                y = d * Math.log((1 + sin) / (1 - sin)) / 2;
            
            var scale = 256 * Math.pow(2, zoom);
            return new L.Point(x * scale / 6378137, y * scale / 6378137);
        },
        unproject: function(point, zoom) {
            zoom = zoom === undefined ? this._zoom : zoom;
            var d = 6378137 * Math.PI / 180,
                scale = 256 * Math.pow(2, zoom),
                lng = point.x * 6378137 / scale / d,
                lat = (2 * Math.atan(Math.exp(point.y * 6378137 / scale / d)) - Math.PI / 2) * 180 / Math.PI;
            return new L.LatLng(lat, lng);
        },
        _initLayout: function() {
            this._container.innerHTML = '<div id="map" style="width:100%; height:100%;"></div>';
        },
        _initEvents: function() {
            // Simplified event handling
        },
        _resetView: function(center, zoom) {
            this._zoom = zoom;
            this._center = center;
            this.fire('moveend');
        },
        addLayer: function(layer) {
            layer._map = this;
            this._layers.push(layer);
            if (layer.onAdd) {
                layer.onAdd(this);
            }
            return this;
        },
        removeLayer: function(layer) {
            var index = this._layers.indexOf(layer);
            if (index !== -1) {
                this._layers.splice(index, 1);
                if (layer.onRemove) {
                    layer.onRemove(this);
                }
                delete layer._map;
            }
            return this;
        },
        fire: function(type, data) {
            var event = {
                type: type,
                target: this,
                sourceTarget: data && data.sourceTarget || this
            };
            
            for (var i in this._layers) {
                var layer = this._layers[i];
                if (layer.listens && layer.listens(type)) {
                    layer.fire(type, event);
                }
            }
            return this;
        }
    };

    // TileLayer class
    L.TileLayer = function(url, options) {
        options = options || {};
        this.url = url;
        this.options = options;
        this._tiles = {};
    };

    L.TileLayer.prototype = {
        onAdd: function(map) {
            this._map = map;
            this._reset();
        },
        onRemove: function(map) {
            this._removeAllTiles();
        },
        _reset: function() {
            this._removeAllTiles();
            this._update();
        },
        _update: function() {
            var bounds = this._map.getBounds(),
                zoom = this._map.getZoom(),
                tileSize = 256,
                tileBounds = this._pxBoundsToTileBounds(bounds, zoom);
            
            for (var x = tileBounds.min.x; x <= tileBounds.max.x; x++) {
                for (var y = tileBounds.min.y; y <= tileBounds.max.y; y++) {
                    this._addTile(new L.Point(x, y), zoom);
                }
            }
        },
        _addTile: function(tilePoint, zoom) {
            var key = this._tileCoordsToKey(tilePoint, zoom);
            var tile = document.createElement('img');
            tile.style.width = '256px';
            tile.style.height = '256px';
            tile.style.position = 'absolute';
            tile.style.left = (tilePoint.x * 256) + 'px';
            tile.style.top = (tilePoint.y * 256) + 'px';
            
            tile.src = this.getTileUrl(tilePoint, zoom);
            
            this._tiles[key] = {
                el: tile,
                coords: tilePoint,
                current: true
            };
            
            this._map._container.appendChild(tile);
        },
        _removeAllTiles: function() {
            for (var key in this._tiles) {
                var tile = this._tiles[key];
                if (tile.el && tile.el.parentNode) {
                    tile.el.parentNode.removeChild(tile.el);
                }
            }
            this._tiles = {};
        },
        _pxBoundsToTileBounds: function(bounds, zoom) {
            var tileSize = 256;
            return new L.Bounds(
                new L.Point(Math.floor(bounds.min.x / tileSize), Math.floor(bounds.min.y / tileSize)),
                new L.Point(Math.floor(bounds.max.x / tileSize), Math.floor(bounds.max.y / tileSize))
            );
        },
        _tileCoordsToKey: function(coords, zoom) {
            return coords.x + ':' + coords.y + ':' + zoom;
        },
        getTileUrl: function(coords, zoom) {
            var data = {
                s: this._getSubdomain(coords),
                x: coords.x,
                y: coords.y,
                z: zoom
            };
            return this.url.replace(/\{([sxyz])\}/g, function(match, key) {
                return data[key];
            });
        },
        _getSubdomain: function(tilePoint) {
            var index = Math.abs(tilePoint.x + tilePoint.y) % this.options.subdomains.length;
            return this.options.subdomains[index];
        }
    };

    // Marker class
    L.Marker = function(latlng, options) {
        options = options || {};
        this._latlng = latlng;
        this.options = options;
        this._icon = options.icon || new L.Icon.Default();
    };

    L.Marker.prototype = {
        onAdd: function(map) {
            this._map = map;
            this._icon = this._createIcon();
            
            var pos = map.project(this._latlng);
            this._icon.style.position = 'absolute';
            this._icon.style.left = (pos.x - 15) + 'px';
            this._icon.style.top = (pos.y - 40) + 'px';
            this._icon.style.zIndex = '1000';
            
            map._container.appendChild(this._icon);
        },
        onRemove: function(map) {
            if (this._icon && this._icon.parentNode) {
                this._icon.parentNode.removeChild(this._icon);
            }
        },
        _createIcon: function() {
            var icon = document.createElement('div');
            icon.style.width = '30px';
            icon.style.height = '30px';
            icon.style.borderRadius = '50%';
            icon.style.background = '#3498db';
            icon.style.color = 'white';
            icon.style.display = 'flex';
            icon.style.alignItems = 'center';
            icon.style.justifyContent = 'center';
            icon.style.fontWeight = 'bold';
            icon.style.border = '2px solid white';
            icon.style.boxShadow = '0 2px 5px rgba(0,0,0,0.3)';
            icon.innerHTML = 'G';
            return icon;
        },
        bindPopup: function(content) {
            this._popupContent = content;
            return this;
        },
        openPopup: function() {
            if (this._popupContent) {
                var popup = document.createElement('div');
                popup.style.position = 'absolute';
                popup.style.background = 'white';
                popup.style.border = '1px solid #ccc';
                popup.style.padding = '10px';
                popup.style.borderRadius = '4px';
                popup.style.boxShadow = '0 2px 10px rgba(0,0,0,0.2)';
                popup.style.zIndex = '1001';
                popup.innerHTML = this._popupContent;
                
                var pos = this._map.project(this._latlng);
                popup.style.left = (pos.x - 50) + 'px';
                popup.style.top = (pos.y - 80) + 'px';
                
                this._map._container.appendChild(popup);
            }
        }
    };

    // Icon class
    L.Icon = function(options) {
        options = options || {};
        this.options = options;
    };

    L.Icon.Default = L.Icon.extend({
        options: {
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34]
        }
    });

    // Static methods
    L.map = function(id, options) {
        return new L.Map(id, options);
    };

    L.tileLayer = function(url, options) {
        return new L.TileLayer(url, options);
    };

    L.marker = function(latlng, options) {
        return new L.Marker(latlng, options);
    };

    L.latLng = function(lat, lng, alt) {
        return new L.LatLng(lat, lng, alt);
    };

    // Event handling
    L.Evented = function() {};
    L.Evented.prototype = {
        on: function(type, fn) {
            this._events = this._events || {};
            this._events[type] = this._events[type] || [];
            this._events[type].push(fn);
            return this;
        },
        off: function(type, fn) {
            if (!this._events || !this._events[type]) { return this; }
            var index = this._events[type].indexOf(fn);
            if (index !== -1) {
                this._events[type].splice(index, 1);
            }
            return this;
        },
        fire: function(type, data) {
            if (!this._events || !this._events[type]) { return this; }
            for (var i = 0; i < this._events[type].length; i++) {
                this._events[type][i].call(this, data);
            }
            return this;
        },
        listens: function(type) {
            return this._events && this._events[type] && this._events[type].length > 0;
        }
    };

    // Mixin Evented to Map
    L.Map.prototype = L.extend(L.Map.prototype, L.Evented.prototype);
    L.TileLayer.prototype = L.extend(L.TileLayer.prototype, L.Evented.prototype);
    L.Marker.prototype = L.extend(L.Marker.prototype, L.Evented.prototype);

    // Utility function
    L.extend = function(dest) {
        var sources = Array.prototype.slice.call(arguments, 1);
        for (var j = 0, len = sources.length, src; j < len; j++) {
            src = sources[j] || {};
            for (var i in src) {
                if (src.hasOwnProperty(i)) {
                    dest[i] = src[i];
                }
            }
        }
        return dest;
    };

    // Return the Leaflet object
    return L;

})(window, document);
