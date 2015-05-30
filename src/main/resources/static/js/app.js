'use strict';

angular.module('videostreamer', [
    'ngAnimate',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ui.bootstrap'
]).config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
    $locationProvider.html5Mode(true);
    $routeProvider.when('/', {
        templateUrl: '/static/frag/main.html',
        controller: 'main'
    }).when('/signup', {
        templateUrl: '/static/frag/signup.html',
        controller: 'signup'
    }).when('/profile', {
        templateUrl: '/static/frag/profile.html',
        controller: 'profile'
    }).when('/profile/:category', {
        templateUrl: '/static/frag/profile.html',
        controller: 'profile'
    }).when('/:app/:name', {
        templateUrl: '/static/frag/play.html',
        controller: 'play'
    }).when('/howto', {
        templateUrl: '/static/frag/howto.html'
    }).otherwise({
        template: '',
        controller: 'notfound'
    });
}]).run(['$rootScope', '$route', '$location', '$sce', 'restapi', function ($rootScope, $route, $location, $sce, restapi) {
    var initsequence = 3;
    $rootScope.loaded = false;

    $rootScope.initseq = function () {
        if (--initsequence == 0) {
            $rootScope.loaded = true;
        }
    };

    restapi.security.user().success(function (user) {
        console.log(user);
        $rootScope.user = user;
        $rootScope.initseq();
    });

    restapi.config.rtmp().success(function (rtmpPrefix) {
        $rootScope.rtmpPrefix = rtmpPrefix;
        $rootScope.initseq();
    });

    restapi.config.captcha().success(function (captchaPub) {
        $rootScope.captchaPub = captchaPub;
        $rootScope.initseq();
    });

    $rootScope.errorify = function (code) {
        $rootScope.error = code;
    };

    $rootScope.topic = false;
    $rootScope.topictext = '';

    var editPerm = null;
    var editSend = null;

    $rootScope.topicEditable = function (actual, permFunc, sendFunc) {
        $rootScope.topicedit = false;
        $rootScope.topictext = actual;
        $rootScope.topic = true;
        editPerm = permFunc;
        editSend = sendFunc;
        $rootScope.$apply();
    };

    $rootScope.$on('$routeChangeStart', function() {
        $rootScope.error = null;
        $rootScope.topic = false;
    });

    $rootScope.editTopic = function () {
        if (!editPerm) {
            $rootScope.topicedit = false;
            return;
        }
        editPerm(function (result) {
            $rootScope.topicedit = result;
            if (result) {
                setTimeout(function () {
                    var field = document.getElementById('topicfield');
                    field.focus();
                    field.select();
                }, 0);
            }
        });
    };

    $rootScope.sendTopic = function (topic) {
        $rootScope.topicedit = false;
        if (editSend) {
            editSend(topic);
        }
    };

    $rootScope.updateTopic = function (topic) {
        $rootScope.topictext = topic;
        $rootScope.$apply();
    };

    $rootScope.trust = function (data) {
        return $sce.trustAsHtml(data);
    };

    $rootScope.favicon = new Favico({
        animation:'fade'
    });
}]);