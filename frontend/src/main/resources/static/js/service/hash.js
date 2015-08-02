'use strict';

angular.module('video').service('HashService', ['$q', function ($q) {
    var promise = $q(function (resolve, reject) {
        var scriptMd5 = document.createElement('script');
        scriptMd5.onload = resolve;
        scriptMd5.src = '/webjars/md5/0.3.0/build/md5.min.js';
        document.body.appendChild(scriptMd5);
    });

    return {
        promise: promise,
        md5: function (n) {
            return md5(n);
        }
    };
}]);