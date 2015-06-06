'use strict';

angular.module('videostreamer').controller('passwordreset', ['$scope', '$routeParams', 'restapi', function ($scope, $routeParams, restapi) {
    $scope.success = false;
    $scope.requestreset = function (username, email) {
        var captcha = $scope.captchael.find('textarea').val();
        restapi.security.resetreq(username, email, captcha).success(function () {
            $scope.success = true;
            $scope.username = '';
            $scope.email = '';
        }).error(function (err) {
            if (grecaptcha) {
                grecaptcha.reset();
            }
            $scope.form.serverErrors = err;
        });
    };
}]);