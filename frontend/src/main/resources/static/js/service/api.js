'use strict';

angular.module('video').service('ApiService', ['$q', '$location', '$cookies', function ($q, $location, $cookies) {
    var client = {};
    var promise = $q(function (resolve, reject) {
        new SwaggerClient({
            url: $location.protocol() + '://' + $location.host() + '/backend/v2/api-docs',
            success: function () {
                this.clientAuthorizations.add("xcsrf", {
                    apply: function (spec) {
                        if (spec.method == 'POST') {
                            spec.headers['X-XSRF-TOKEN'] = $cookies.get('XSRF-TOKEN');
                        }
                    }
                });
                angular.extend(client, this);
                resolve();
            }
        });
    });


    return {
        promise: promise,
        client: client
    };
}]);