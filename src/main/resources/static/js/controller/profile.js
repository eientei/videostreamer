'use strict';

angular.module('videostreamer').controller('profile', ['$rootScope', '$scope', '$modal', '$location', '$routeParams', 'restapi', function ($rootScope, $scope, $modal, $location, $routeParams, restapi) {
    $scope.tabs = { };

    if ($routeParams.category) {
        $scope.tabs[$routeParams.category] = true;
    }

    $scope.selectTab = function (tabname) {
        for (var v in $scope.tabs) {
            if ($scope.tabs.hasOwnProperty(v)) {
                $scope.tabs[v] = false;
            }
        }
        $location.path('/profile/' + tabname);
        $scope.tabs[tabname] = true;
    };

    $scope.category = $routeParams.category;

    $rootScope.initseq();
    $scope.streams = [];
    $scope.current = null;

    $scope.createStream = function () {
        restapi.streams.allocate().success(function (data) {
            $scope.streams.push(data);
            $scope.select(data);
        });
    };

    var deleteInstance;

    $scope.openDeleteConfirm = function () {
        deleteInstance = $modal.open({
            animation: true,
            templateUrl: 'deleteConfirmation.html',
            scope: $scope
        });
    };

    $scope.deleteStream = function () {
        if (deleteInstance) {
            deleteInstance.dismiss();
        }
        restapi.streams.deallocate($scope.current.app, $scope.current.name).success(function () {
            var idx = $scope.streams.indexOf($scope.current);
            $scope.streams.splice(idx, 1);
            if ($scope.streams.length > 0) {
                $scope.select($scope.streams[0]);
            } else {
                $scope.select(null);
            }
        });
    };

    var renameInstance;

    $scope.openRenameConfirm = function () {
        renameInstance = $modal.open({
            animation: true,
            templateUrl: 'renameConfirmation.html',
            scope: $scope
        });
    };

    $scope.updateStreamName = function () {
        if (renameInstance) {
            renameInstance.dismiss();
        }
        restapi.streams.rename($scope.current.app, $scope.current.name, $scope.current.newname).success(function () {
            $scope.current.name = $scope.current.newname;
        }).error(function () {
            $scope.current.newname = $scope.current.name;
        });
    };

    $scope.updateStreamTopic = function () {
        restapi.streams.topic($scope.current.app, $scope.current.name, $scope.current.newtopic).success(function () {
            $scope.current.topic = $scope.current.newtopic;
        }).error(function () {
            $scope.current.newtopic = $scope.current.topic;
        });
    };

    $scope.updateStreamImage = function () {
        restapi.streams.screensaver($scope.current.app, $scope.current.name, $scope.current.image);
    };

    $scope.updateStreamToken = function () {
        restapi.streams.gentoken($scope.current.app, $scope.current.name).success(function (data) {
            $scope.current.token = data;
        });
    };

    $scope.updateStreamPrivate = function (value) {
        restapi.streams.restricted($scope.current.app, $scope.current.name, !value);
    };

    $scope.select = function (stream) {
        if ($scope.current) {
            $scope.current.active = false;
        }
        $scope.current = stream;
        if (stream) {
            $scope.current.newname = stream.name;
            $scope.current.newtopic = stream.topic;
            stream.active = true;
        }

    };

    $scope.updateEmail = function () {
        restapi.security.email($rootScope.user.email).success(function (data) {
            $rootScope.user.hashicon = data;
        });
    };

    $scope.cred = {};

    $scope.updatePassword = function (oldpassword, newpassword) {
        $scope.passok = false;
        restapi.security.password(oldpassword, newpassword).success(function () {
            $scope.cred = {};
            $scope.passerr = null;
            $scope.passok = true;
        }).error(function (data) {
            $scope.cred.oldpassword = null;
            $scope.passerr = data.form[0];
        });
    };

    restapi.streams.mine().success(function (data) {
        $scope.streams = data;
        $scope.select(data[0]);
    });
}]);