'use strict';

angular.module('video').service('StreamService', ['$q', '$rootScope', '$location', 'ApiService', function ($q, $rootScope, $location, ApiService) {
    var stream = {};
    var ownstreams = [];

    function makePromise(app, name) {
        return $q(function (resolve, reject) {
            ApiService.promise.then(function () {
                ApiService.client.streams.streamUsingGET({
                    app: app,
                    name: name
                }, function (reply) {
                    angular.extend(stream, angular.fromJson(reply.data));
                    stream.resolved = true;
                    resolve();
                }, function () {
                    stream.resolved = false;
                    reject();
                });
            });
        });
    }

    $rootScope.$on('$routeChangeStart', function () {
        stream.resolved = false;
        stream.expected = false;
    });

    return {
        promise: ApiService.promise,
        load: function (params) {
            if (!params || !params.app || !params.name) {
                return null;
            }
            return makePromise(params.app, params.name).then(function () {
                stream.expected = true;
            }, function () {
                $rootScope.$broadcast('ResolveFail', {
                    path: $location.path(),
                    details: 'Not found',
                    code: 404
                });
            });
        },
        running: function () {
            return $q(function (resolve, reject) {
                ApiService.client.streams.runningUsingGET({}, function (reply) {
                    resolve(angular.fromJson(reply.data));
                });
            });
        },
        mine: function () {
            return $q(function (resolve, reject) {
                ApiService.promise.then(function () {
                    ApiService.client.streams.mineUsingGET({}, function (reply) {
                        angular.extend(ownstreams, angular.fromJson(reply.data));
                        resolve();
                    });
                });
            });
        },
        allocate: function () {
            return $q(function (resolve, reject) {
                ApiService.client.streams.allocateUsingGET({}, function (reply) {
                    resolve(angular.fromJson(reply.data));
                }, function () {
                    reject();
                });
            });
        },
        deallocate: function (app, name) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.deallocateUsingGET({
                    app: app,
                    name: name
                }, resolve, reject);
            });
        },
        rename: function (app, name, newname) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.renameUsingPOST({
                    app: app,
                    name: name,
                    newname: newname
                }, resolve, function (reply) {
                    reject(angular.fromJson(reply.data));
                });
            });
        },
        topic: function (app, name, newtopic) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.topicUsingPOST({
                    app: app,
                    name: name,
                    newtopic: newtopic
                }, resolve, reject);
            });
        },
        image: function (app, name, newimage) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.imageUsingPOST({
                    app: app,
                    name: name,
                    newimage: newimage
                }, resolve, reject);
            });
        },
        gentoken: function (app, name) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.gentokenUsingGET({
                    app: app,
                    name: name
                }, function (result) {
                    resolve(angular.fromJson(result.data));
                }, reject);
            });
        },
        restricted: function (app, name, restricted) {
            return $q(function (resolve, reject) {
                ApiService.client.streams.restrictedUsingPOST({
                    app: app,
                    name: name,
                    restricted: restricted.toString()
                }, resolve, reject);
            });
        },
        stream: stream,
        ownstreams: ownstreams
    };
}]);