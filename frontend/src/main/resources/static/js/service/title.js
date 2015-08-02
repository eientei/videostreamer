'use strict';

angular.module('video').service('TitleService', ['$q', '$rootScope', '$location', function ($q, $rootScope, $location) {
    var title = {};
    var isActive = true;
    var misscounter = 0;
    var favico;

    var promise = $q(function (resolve, reject) {
        var scriptFav = document.createElement('script');
        scriptFav.onload = function () {
            favico = new Favico({
                animation:'fade'
            });
            resolve();
        };
        scriptFav.src = '/webjars/favico.js/0.3.9/favico-0.3.9.min.js';
        document.body.appendChild(scriptFav);
    });

    window.onfocus = function () {
        favico.badge("");
        isActive = true;
    };

    window.onblur = function () {
        misscounter = 0;
        isActive = false;
    };


    $rootScope.$on('$routeChangeSuccess', function () {
        if (title.error) {
            title.value = 'Error ' + title.error.code;
        } else {
            title.value = $location.path().substring(1);
        }

        if (!title.value) {
            title.value = 'video.eientei';
        }
        if (title.add) {
            title.value = title.value + ' - ' + title.add;
        }

        title.error = null;
    });

    return {
        promise: promise,
        error: function (error) {
            title.error = error;
        },
        add: function (add) {
            title.add = add;
        },
        notifymsg: function () {
            if (isActive) {
                return;
            }
            misscounter++;
            favico.badge(misscounter);
        },
        title: title
    };
}]);