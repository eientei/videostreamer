'use strict';

angular.module('videostreamer').controller('passwordresetsubmit', ['$scope', '$routeParams', 'restapi', function ($scope, $routeParams, restapi) {
    $scope.success = false;
    $scope.reset = function (password) {
        if (!$routeParams.resetkey) {
            return;
        }
        restapi.security.resetsubmit($routeParams.resetkey, password).success(function () {
            $scope.success = true;
        }).error(function (err) {
            $scope.form.serverErrors = err;
        });
    };
}]);