'use strict';

angular.module('videostreamer').controller('main', ['$rootScope', '$scope', 'restapi', function ($rootScope, $scope, restapi) {
    $scope.streamsloaded = false;

    $scope.stream = [];

    restapi.streams.running().success(function (streams) {
        $scope.streams = streams;
        $scope.streamsloaded = true;
    });
}]);