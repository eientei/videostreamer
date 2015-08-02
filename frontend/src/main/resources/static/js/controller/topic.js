'use strict';

angular.module('video').controller('topic', ['$scope', '$routeParams', 'StreamService', 'TitleService', function ($scope, $routeParams, StreamService, TitleService) {
    function setTopic(topic) {
        if (!topic || topic.length == 0 || topic.trim().length == 0) {
            topic = null;
        }
        $scope.topictext = topic;
        TitleService.add(topic);
    }

    $scope.$watch('stream', function (n) {
        if (n.resolved === true) {
            setTopic(n.topic);
            $scope.topicedit = n.ownstream;
        } else {
            $scope.topictext = null;
            $scope.topicedit = false;
        }
    }, true);

    $scope.editTopic = function () {
        if (!$scope.topicedit) {
            return;
        }
        $scope.editing = true;
    };

    $scope.sendTopic = function (text) {
        if (!text) {
            text = ' ';
        }
        StreamService.topic($routeParams.app, $routeParams.name, text).then(function () {
            setTopic(text);
            $scope.editing = false;
        });
    };
}]);