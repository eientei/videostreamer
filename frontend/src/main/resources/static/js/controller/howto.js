'use strict';

angular.module('video').controller('howto', ['$scope', 'ConfigService', function ($scope, ConfigService) {
    $scope.rtmpPrefix = ConfigService.config.rtmpPrefix;
}]);