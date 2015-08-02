'use strict';

angular.module('video').controller('resetpass', ['$scope', '$routeParams', '$location', 'SecurityService', function ($scope, $routeParams, $location, SecurityService) {
    $scope.reset = function (password) {
        SecurityService.resetpass($routeParams.resetkey, password).then(function () {
            $scope.success = true;
            $location.url('/profile');
        }, function (data) {
            $scope.errors = data;
        });
    };
}]);