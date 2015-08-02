'use strict';

angular.module('video').service('ConfigService', ['$q', 'ApiService', function ($q, ApiService) {
    var config = {};
    var promise = $q(function (resolve, reject) {
        ApiService.promise.then(function () {
            ApiService.client.config.configUsingGET({}, function (reply) {
                angular.extend(config, angular.fromJson(reply.data));
                resolve();
            });
        });
    });

    return {
        promise: promise,
        config: config
    };
}]);