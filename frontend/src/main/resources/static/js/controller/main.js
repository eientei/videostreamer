'use strict';

angular.module('video').controller('main', ['$scope', 'StreamService', function ($scope, StreamService) {
    StreamService.running().then(function (streams) {
        $scope.streams = streams;
    });
}]);