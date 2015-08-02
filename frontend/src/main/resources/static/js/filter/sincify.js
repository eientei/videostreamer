'use strict';

angular.module('video').filter('sincify', [function () {
    return function (input) {
        return moment(input).format('YYYY-MM-DD HH:mm:ss ZZ');
    };
}]);