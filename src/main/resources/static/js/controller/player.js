'use strict';

angular.module('videostreamer').controller('player', ['$scope', '$routeParams', function ($scope, $routeParams) {
    $scope.streamname = $routeParams.name;
}]);