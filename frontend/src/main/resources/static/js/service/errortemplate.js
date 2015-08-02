'use strict';

angular.module('video').service('ErrortemplateService', ['$templateCache', function ($templateCache) {
    var mappings = {};

    return {
        get: function (name) {
            if (mappings.hasOwnProperty(name)) {
                return $templateCache.get(mappings[name]);
            }
            return $templateCache.get(name);
        },
        map: function (from, to) {
            mappings[from] = to;
        },
        synonyms: function(a, b) {
            return mappings[a] === mappings[b];
        }
    }
}]);