'use strict';

angular.module('video').controller('play', ['$q', '$scope', '$routeParams', '$window', '$location', '$anchorScroll', 'TitleService', 'TyperService', function ($q, $scope, $routeParams, $window, $location, $anchorScroll, TitleService, TyperService) {
    $scope.site = 'http://' + location.hostname + ((location.port == 80 || location.port == '') ? '' : ':' + location.port);

    $scope.app = $routeParams.app;
    $scope.name = $routeParams.name;

    $scope.showvideo = !$routeParams.novideo;
    $scope.showchat = !$routeParams.nochat;
    $scope.bufflen = ($routeParams.nobuffer) ? 0.0 : 1.0;
    if ($routeParams.buffer) {
        $scope.bufflen = $routeParams.buffer;
    }

    var timeout;
    var first;
    var sock;
    var previews = {};
    var defers = {};
    var openHandler = function () {
        if (timeout) {
            clearTimeout(timeout);
            timeout = null;
        }
        sock.send(JSON.stringify({
            type: 'CONNECT',
            data: {
                app: $routeParams.app,
                name: $routeParams.name
            }
        }));
        $scope.messages = [];
        first = true;
    };
    var closeHandler = function () {
        if (sock) {
            timeout = setTimeout(initSock, 1000);
        }
    };
    $scope.notifyKeypress = function () {
        sock.send(JSON.stringify({
            type: 'KEYPRESS',
            data: { }
        }));
    };
    $scope.sendMessage = function (text) {
        sock.send(JSON.stringify({
            type: 'MESSAGE',
            data: {
                text: text
            }
        }));
    };
    $scope.preview = function (id) {
        var deferred = $q.defer();

        if (id in previews) {
            deferred.resolve(previews[id]);
        } else {
            defers[id] = deferred;
            sock.send(JSON.stringify({
                type: 'PREVIEWREQUEST',
                data: {
                    id: id
                }
            }));
        }

        return deferred.promise;
    };
    $scope.loadHistory = function (ref) {
        sock.send(JSON.stringify({
            type: 'HISTORYREQUEST',
            data: {
                id: ref
            }
        }));
    };

    $scope.messageref = function (refid) {
        TyperService.insertText('>>' + refid);
    };

    var messageHandler = function (e) {
        var d = JSON.parse(e.data);
        if (d.type == 'ONLINE') {
            handleOnline(d.data);
        } else if (d.type == 'TYPING') {
            handleTypers(d.data);
        } else if (d.type == 'MESSAGE') {
            handleMessage(d.data);
        } else if (d.type == 'HISTORY') {
            handleHistory(d.data);
        } else if (d.type == 'IMAGE') {
            handleImage(d.data);
        } else if (d.type == 'INFO') {
            handleInfo(d.data);
        } else if (d.type == 'TOPIC') {
            handleTopic(d.data);
        } else if (d.type == 'PREVIEW') {
            handlePreview(d.data);
        } else if (d.type == 'MIGRATE') {
            handleMigrate(d.data);
        }
    };
    function handleInfo(data) {
        $scope.stream.topic = data.topic;
        $scope.stream.image = data.image;
        $scope.stream.ownstream = data.owner;
        $scope.$apply();
    }
    function handleTopic(data) {
        $scope.stream.topic = data.topic;
        $scope.$apply();
    }
    function handleOnline(data) {
        $scope.online = data.online;
        $scope.owner = data.owner;
        $scope.$apply();
    }
    function handleImage(data) {
        $scope.stream.image = data.image;
        $scope.$apply();
    }
    function handleHistory(data) {
        $scope.hashistory = data.more;
        var message;
        var offset;
        var scroller = document.getElementById('scroller');

        if (!first) {
            message = $scope.messages[0];
            message.anchor = true;
            offset = document.getElementById('messagefirst').offsetTop - scroller.offsetTop;
        }
        angular.forEach(data.history.reverse(), function (message) {
            $scope.messages.unshift(message);
        });
        $scope.$apply();
        if (first) {
            first = false;
            scroller.scrollTop = scroller.scrollHeight;
        } else {
            $location.hash('anchor');
            $anchorScroll();
            $location.hash(null);
            message.anchor = false;
            scroller.scrollTop -= offset;
        }
    }
    function handleTypers(data) {
        angular.forEach($scope.online, function (online) {
            online.typing = !(data.typers.indexOf(online.hash) < 0);
        });
        if ($scope.owner) {
            $scope.owner.typing = !(data.typers.indexOf($scope.owner.hash) < 0);
        }
        $scope.$apply();
    }
    function handleMessage(data) {
        var scroller = document.getElementById('scroller');

        TitleService.notifymsg();

        $scope.messages.push(data);
        $scope.$apply();
        if (scroller.scrollHeight - scroller.scrollTop <  scroller.clientHeight * 2) {
            scroller.scrollTop = scroller.scrollHeight;
        }
    }
    function handlePreview(data) {
        var deferred = defers[data.id];
        if (deferred) {
            delete deferred[data.id];
            previews[data.id] = data.preview;
            deferred.resolve(previews[data.id]);
        }
    }
    function initSock() {
        sock = new SockJS('http://' + location.hostname + (location.port == 80 ? '' : ':' + location.port) + '/backend/chat');
        sock.onopen = openHandler;
        sock.onmessage = messageHandler;
        sock.onclose = closeHandler;
    }
    function handleMigrate(data) {
        $window.location.href = '/' + $routeParams.app + '/' + data.newname;
    }
    initSock();
    $scope.$on('$routeChangeStart', function () {
        sock.close();
        sock = null;
        if (timeout) {
            clearTimeout(timeout);
        }
    });
}]);