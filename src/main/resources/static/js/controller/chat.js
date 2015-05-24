'use strict';

angular.module('videostreamer').controller('chat', ['$rootScope', '$scope', '$routeParams', '$location', '$window', '$anchorScroll', '$q', function ($rootScope, $scope, $routeParams, $location, $window, $anchorScroll, $q) {
    var sock = null;
    var first = true;

    $scope.messages = [];


    var openHandler = function () {
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

    $scope.messageref = function (ref) {
        $scope.insertText('>>' + ref);
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

    var timeout = null;

    var closeHandler = function () {
        console.log('disconnect');
        if (sock) {
            timeout = setTimeout(initSock, 1000);
        }

    };

    window.document.title = $routeParams.app + '/' + $routeParams.name;
    var listening = false;
    if (!listening) {
        listening = true;

        $rootScope.$on('$routeChangeStart', function () {
            window.document.title = 'video.eientei';
            if (sock) {
                sock.close();
                sock = null;
            }
            if (timeout) {
                clearTimeout(timeout);
            }
        });
    }

    function initSock() {
        sock = new SockJS('http://' + location.hostname + (location.port == 80 ? '' : ':' + location.port) + '/chat');
        sock.onopen = openHandler;
        sock.onmessage = messageHandler;
        sock.onclose = closeHandler;
    }

    initSock();

    function handleOnline(data) {
        $scope.online = data.online;
        $scope.owner = data.owner;
        $scope.$apply();
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

    var isActive = true;
    var misscounter = 0;

    window.onfocus = function () {
        $rootScope.favicon.badge("");
        isActive = true;
    };

    window.onblur = function () {
        misscounter = 0;
        isActive = false;
    };

    function handleMessage(data) {
        var scroller = document.getElementById('scroller');

        if (!isActive) {
            $rootScope.favicon.badge(++misscounter);
        }

        $scope.messages.push(data);
        $scope.$apply();
        if (scroller.scrollHeight - scroller.scrollTop <  scroller.clientHeight * 2) {
            scroller.scrollTop = scroller.scrollHeight;
        }
    }

    $scope.hashistory = false;

    $scope.loadHistory = function (ref) {
        sock.send(JSON.stringify({
            type: 'HISTORYREQUEST',
            data: {
                id: ref
            }
        }));
    };

    function handleHistory(data) {;
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

    function handleInfo(data) {
        $rootScope.topicEditable(
            data.topic,

            function (res) {
                res(data.owner);
            },

            function (topic) {
                sock.send(JSON.stringify({
                    type: 'TOPIC',
                    data: {
                        topic: topic
                    }
                }));
            }
        );
    }

    function handleTopic(data) {
        $rootScope.updateTopic(data.topic);
    }

    var previews = {};
    var defers = {};

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

    function handlePreview(data) {
        var deferred = defers[data.id];
        if (deferred) {
            delete deferred[data.id];
            previews[data.id] = data.preview;
            deferred.resolve(previews[data.id]);
        }
    }

    function handleMigrate(data) {
        $window.location.href = '/' + $routeParams.app + '/' + data.newname;
    }
}]);