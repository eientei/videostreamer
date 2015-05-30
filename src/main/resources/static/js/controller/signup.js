'use strict';

angular.module('videostreamer').controller('signup', ['$rootScope', '$scope', '$location', 'restapi', function ($rootScope, $scope, $location, restapi) {

    $scope.signup = function (username, password, email) {
        var captcha = $scope.captchael.find('textarea').val();
        restapi.security.signup(username, password, email, captcha).success(function () {
            restapi.security.login(username, password).success(function () {
                restapi.security.user().success(function (user) {
                    $rootScope.user = user;
                    $location.url('/profile');
                });
            });
        }).error(function (err) {
            if (grecaptcha) {
                grecaptcha.reset();
            }
            angular.forEach(err, function (v, k) {
                if (k == 'form') {
                    return;
                }
                if (!form[k]) {
                    angular.forEach(v, function (r) {
                        if (!err.form) {
                            err.form = [];
                        }
                        err.form.push(r);
                    });
                    delete err[k];
                }
            });
            $scope.form.serverErrors = err;
        });
    };
}]);