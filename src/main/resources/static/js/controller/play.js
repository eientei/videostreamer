'use strict';

var onlineCb = null;
var offlineCb = null;

function streamOnline() {
    if (onlineCb) {
        onlineCb();
    }
}

function streamOffline() {
    if (offlineCb) {
        offlineCb();
    }
}

angular.module('videostreamer').controller('play', ['$rootScope', '$scope', '$routeParams', 'restapi', function ($rootScope, $scope, $routeParams, restapi) {
    $scope.flashinited = false;

    onlineCb = function () {
        $scope.flashinited = true;
        $scope.$apply();
        $scope.$broadcast('flash-online');
    };

    offlineCb = function () {
        $scope.flashinited = true;
        $scope.$apply();
        $scope.$broadcast('flash-offline');
    };

    $scope.showvideo = !$routeParams.novideo;
    $scope.showchat = !$routeParams.nochat;
    $scope.bufflen = ($routeParams.nobuffer) ? 0.0 : 1.0;
    if ($routeParams.buffer) {
        $scope.bufflen = $routeParams.buffer;
    }
    restapi.streams.stream($routeParams.app, $routeParams.name).success(function (stream) {
        $scope.stream = stream;
    }).error(function (e) {
        $rootScope.errorify(e.status);
    });
}]);