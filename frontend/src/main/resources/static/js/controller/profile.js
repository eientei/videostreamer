'use strict';

angular.module('video').controller('profile', ['$scope', '$routeParams', '$location', '$modal', 'HashService', 'StreamService', 'SecurityService', function ($scope, $routeParams, $location, $modal, HashService, StreamService, SecurityService) {
    $scope.tabs = { };

    $scope.data = { };

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
        if (tabname != 'streams') {
            $location.hash(null);
        }
        $scope.tabs[tabname] = true;
    };

    $scope.streams = StreamService.ownstreams;
    $scope.current = null;

    $scope.selectStream = function (stream) {
        if ($scope.current) {
            $scope.current.active = false;
        }
        $scope.current = stream;
        if (stream) {
            stream.newname = stream.name;
            stream.newtopic = stream.topic;
            stream.newimage = stream.image;
            stream.active = true;
            for (var i = 0; i < $scope.streams.length; i++) {
                if ($scope.streams[i] == stream) {
                    $location.hash(i);
                    break;
                }
            }
        }
    };

    var hash = $location.hash();
    for (var i = 0; i < $scope.streams.length; i++) {
        if (hash == i) {
            $scope.selectStream($scope.streams[i]);
            break;
        }
    }

    $scope.createStream = function () {
        StreamService.allocate().then(function (stream) {
            $scope.streams.push(stream);
            $scope.selectStream(stream);
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
        StreamService.deallocate($scope.current.app, $scope.current.name).then(function () {
            var idx = $scope.streams.indexOf($scope.current);
            $scope.streams.splice(idx, 1);
            if ($scope.streams.length > 0) {
                $scope.selectStream($scope.streams[0]);
            } else {
                $scope.selectStream(null);
            }
        });
    };

    var renameInstance;
    $scope.openRenameConfirm = function () {
        if (!$scope.data.stream.form.$errvalidate()) {
            $scope.data.stream.errors = {};
            return;
        }
        $scope.data.stream.errors = {};
        renameInstance = $modal.open({
            animation: true,
            templateUrl: 'renameConfirmation.html',
            scope: $scope
        });
    };
    $scope.updateStreamName = function () {
        $scope.data.stream.form.$submitted = true;
        if (renameInstance) {
            renameInstance.dismiss();
        }
        if (!$scope.current.newname) {
            $scope.current.newname = ' ';
        }
        StreamService.rename($scope.current.app, $scope.current.name, $scope.current.newname).then(function () {
            $scope.current.name = $scope.current.newname;
        }, function (errs) {
            $scope.current.newname = $scope.current.name;
            $scope.data.stream.errors = errs;
        });
    };

    $scope.updateStreamTopic = function () {
        StreamService.topic($scope.current.app, $scope.current.name, $scope.current.newtopic).then(function () {
            $scope.current.topic = $scope.current.newtopic;
        }, function () {
            $scope.current.newtopic = $scope.current.topic;
        });
    };

    $scope.updateStreamImage = function () {
        StreamService.image($scope.current.app, $scope.current.name, $scope.current.newimage).then(function () {
            $scope.current.image = $scope.current.newimage;
        }, function () {
            $scope.current.newimage = $scope.current.image;  a
        });
    };

    $scope.updateStreamToken = function () {
        StreamService.gentoken($scope.current.app, $scope.current.name).then(function (newtoken) {
            $scope.current.token = newtoken;
        });
    };

    $scope.updateStreamPrivate = function (value) {
        StreamService.restricted($scope.current.app, $scope.current.name, value).then(function () {
            $scope.current.restricted = value;
        })
    };

    $scope.userdata = {
        email: $scope.user.email,
        hashicon: $scope.user.hashicon
    };
    $scope.$watch('userdata.email', function (n) {
        if (!n || !n.trim()) {
            $scope.userdata.hashicon = $scope.user.hashicon;
        } else {
            $scope.userdata.hashicon = HashService.md5(n);
        }
    }, true);
    $scope.updateEmail = function () {
        SecurityService.email($scope.userdata.email.trim()).then(function (hash) {
            $scope.user.email = $scope.userdata.email.trim();
            $scope.user.hashicon = hash;
        });
    };

    $scope.cred = {
        oldpassword:  '',
        newpassword:  ''
    };
    $scope.updatePassword = function (oldpassword, newpassword) {
        $scope.pwdsuccess = false;
        $scope.errors = null;
        SecurityService.password(oldpassword, newpassword).then(function () {
            $scope.pwdsuccess = true;
            $scope.cred = {};
        }, function (errors) {
            $scope.errors = errors;
        });
    };

}]);