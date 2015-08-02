'use strict';

angular.module('video').service('SecurityService', ['$q', 'ApiService', function ($q, ApiService) {
    var user = {};
    var promise = $q(function (resolve, reject) {
        ApiService.promise.then(function () {
            ApiService.client.security.userUsingGET({}, function (reply) {
                angular.extend(user, angular.fromJson(reply.data));
                resolve();
            });
        });
    });

    return {
        promise: promise,
        user: user,
        login: function (username, password) {
            return $q(function (resolve, reject) {
                ApiService.client.security.loginUsingPOST({
                    dto: {
                        username: username,
                        password: password
                    }
                }, function (reply) {
                    angular.extend(user, angular.fromJson(reply.data));
                    resolve();
                }, function () {
                    reject();
                });
            });
        },
        logout: function () {
            return $q(function (resolve, reject) {
                ApiService.client.security.logoutUsingPOST({}, function (reply) {
                    angular.extend(user, angular.fromJson(reply.data));
                    resolve();
                });
            });
        },
        reset: function (username, email, captcha) {
            return $q(function (resolve, reject) {
                ApiService.client.security.resetreqUsingPOST({
                   dto: {
                       username: username,
                       email: email,
                       captcha: captcha
                   }
                }, function () {
                    resolve();
                }, function (reply) {
                    reject(angular.fromJson(reply.data));
                });
            });
        },
        resettry: function (resetkey) {
            return $q(function (resolve, reject) {
                ApiService.client.security.resettryUsingPOST({
                    resetkey: resetkey
                }, function (reply) {
                    if (angular.fromJson(reply.data) === true) {
                        resolve();
                    } else {
                        reject();
                    }
                });
            });
        },
        resetpass: function (resetkey, password) {
            return $q(function (resolve, reject) {
                ApiService.client.security.resetsubmitUsingPOST({
                    dto: {
                        resetkey: resetkey,
                        password: password
                    }
                }, function (reply) {
                    angular.extend(user, angular.fromJson(reply.data));
                    resolve();
                }, function (reply) {
                    reject(angular.fromJson(reply.data));
                });
            });
        },
        signup: function (username, password, email, captcha) {
            return $q(function (resolve, reject) {
                ApiService.client.security.signupUsingPOST({
                    dto: {
                        username: username,
                        password: password,
                        email: email,
                        captcha: captcha
                    }
                }, function (reply) {
                    angular.extend(user, angular.fromJson(reply.data));
                    resolve();
                }, function (reply) {
                    reject(angular.fromJson(reply.data));
                });
            });
        },
        email: function (email) {
            return $q(function (resolve, reject) {
                ApiService.client.security.emailUsingPOST({
                    email: email
                }, function (reply) {
                    resolve(angular.fromJson(reply.data));
                }, reject);
            });
        },
        password: function (oldpassword, newpassword) {
            return $q(function (resolve, reject) {
                ApiService.client.security.passwordUsingPOST({
                    dto: {
                        current: oldpassword,
                        desired: newpassword
                    }
                }, function () {
                    resolve();
                }, function (reply) {
                    reject(angular.fromJson(reply.data));
                });
            });
        }
    };
}]);