'use strict';

angular.module('video', [
    'ngAnimate',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngCookies',
    'ui.bootstrap'
]).config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
    var promises = [];

    function resolve(promise, $rootScope, $location) {
        promise.catch(function (details) {
            $rootScope.$broadcast('ResolveFail', {
                path: $location.path(),
                details: details,
                code: 500
            });
        });
        promises.push(promise);
        return promise;
    }

    var profileResolve = {
        hash: ['HashService', function (HashService) {
            return HashService.promise;
        }],
        stream: ['$q', '$rootScope', '$location', 'StreamService', 'SecurityService', function ($q, $rootScope, $location, StreamService, SecurityService) {
            return $q(function (resolve, reject) {
                SecurityService.promise.then(function () {
                    if (SecurityService.user.username == 'anonymous') {
                        $rootScope.$broadcast('ResolveFail', {
                            path: $location.path(),
                            details: 'Forbidden',
                            code: 403
                        });
                        reject();
                    } else {
                        StreamService.mine().then(function () {
                            resolve();
                        });
                    }
                });
            });
        }]
    };

    var resolves = {
        api: ['ApiService', '$rootScope', '$location', function (ApiService, $rootScope, $location) {
            return resolve(ApiService.promise, $rootScope, $location);
        }],
        security: ['SecurityService', '$rootScope', '$location', function (SecurityService, $rootScope, $location) {
            return resolve(SecurityService.promise, $rootScope, $location);
        }],
        config: ['ConfigService', '$rootScope', '$location', function (ConfigService, $rootScope, $location) {
            return resolve(ConfigService.promise, $rootScope, $location);
        }],
        favico: ['TitleService', '$rootScope', '$location', function (TitleService, $rootScope, $location) {
            return resolve(TitleService.promise, $rootScope, $location);
        }],
        loaded: ['$q', '$rootScope', function ($q, $rootScope) {
            var cntr = promises.length;
            return $q(function (resolve, reject) {
                angular.forEach(promises, function (val) {
                    val.then(function () {
                        cntr--;
                        if (cntr == 0) {
                            resolve();
                            $rootScope.$broadcast('ResolveDone');
                        }
                    });
                });
            });
        }]
    };

    var wrappedRouteProvider = angular.extend({}, $routeProvider, {
        when: function (path, route) {
            route.resolve = (route.resolve) ? route.resolve : {};
            angular.extend(route.resolve, resolves);
            route.reloadOnSearch = false;
            $routeProvider.when(path, route);
            return this;
        }
    });

    $locationProvider.html5Mode(true);
    wrappedRouteProvider.when('/', {
        templateUrl: 'html/main.html',
        controller: 'main'
    }).when('/howto', {
        templateUrl: 'html/howto.html',
        controller: 'howto'
    }).when('/passwordreset', {
        templateUrl: 'html/reset.html',
        controller: 'reset',
        resolve: {
            recaptcha: ['RecaptchaService', function (RecaptchaService) {
                return RecaptchaService.promise;
            }]
        }
    }).when('/security/reset/:resetkey', {
        templateUrl: 'html/resetpass.html',
        controller: 'resetpass',
        resolve: {
            resetkey: ['$q', '$rootScope', '$route', '$location', 'SecurityService', 'ApiService', function ($q, $rootScope, $route, $location, SecurityService, ApiService) {
                return $q(function (resolve, reject) {
                    ApiService.promise.then(function () {
                        SecurityService.resettry($route.current.params.resetkey).then(function () {
                            resolve();
                        }, function () {
                            $rootScope.$broadcast('ResolveFail', {
                                path: $location.path(),
                                details: 'Forbidden',
                                code: 403
                            });
                            resolve();
                        });
                    });
                });
            }]
        }
    }).when('/signup', {
        templateUrl: 'html/signup.html',
        controller: 'signup',
        resolve: {
            recaptcha: ['RecaptchaService', function (RecaptchaService) {
                return RecaptchaService.promise;
            }]
        }
    }).when('/profile/:category', {
        templateUrl: 'html/profile.html',
        controller: 'profile',
        resolve: profileResolve
    }).when('/profile', {
        redirectTo: '/profile/streams'
    }).when('/:app/:name', {
        templateUrl: 'html/play.html',
        controller: 'play',
        resolve: {
            stream: ['$route', 'StreamService', function ($route, StreamService) {
                return StreamService.load($route.current.params);
            }]
        }
    }).otherwise({
        resolve: {
            error: ['$rootScope', '$location', function ($rootScope, $location) {
                $rootScope.$broadcast('ResolveFail', {
                    path: $location.path(),
                    details: 'Not found',
                    code: 404
                });
            }]
        }
    });
}]).run(['$rootScope', 'ConfigService', 'SecurityService', 'StreamService', 'TitleService', function ($rootScope, ConfigService, SecurityService, StreamService, TitleService) {
    $rootScope.loaded = false;
    $rootScope.error  = null;

    $rootScope.config = ConfigService.config;
    $rootScope.user = SecurityService.user;
    $rootScope.stream = StreamService.stream;
    $rootScope.title = TitleService.title;

    $rootScope.$on('$routeChangeStart', function () {
        $rootScope.error = null;
    });
    $rootScope.$on('ResolveFail', function (ev, errorData) {
        $rootScope.error = errorData;
        TitleService.error(errorData);
    });
    $rootScope.$on('ResolveDone', function () {
        $rootScope.loaded = true;
    });
}]);