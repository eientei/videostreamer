'use strict';

angular.module('video').filter('escape', [function () {
    return function (input) {
        return encodeURIComponent(input).replace(/_/g, '_5F').replace(/%/g, '_');
    };
}]).filter('escapelink', [function () {
    return function (input) {
        return input.replace(/%/g, '%25');
    };
}]);