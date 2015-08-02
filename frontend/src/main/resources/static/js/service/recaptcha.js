'use strict';

var recaptchaResolver;

function recaptchaCallback() {
    recaptchaResolver();
}

angular.module('video').service('RecaptchaService', ['$q', 'ConfigService', function ($q, ConfigService) {
    var promise = $q(function (resolve, reject) {
        recaptchaResolver = resolve;

        var script = document.createElement('script');
        script.src = 'https://www.google.com/recaptcha/api.js?onload=recaptchaCallback&render=explicit';
        document.body.appendChild(script);
    });

    return {
        promise: promise,
        render: function (element) {
            grecaptcha.render(element[0], {
                sitekey: ConfigService.config.captchaPubkey
            });
            return {};
        },
        reset: function (element) {
            grecaptcha.reset(element[0].id);
        },
        destroy: function () {
            var pls = document.getElementsByClassName('pls-container');
            if (pls.length) {
                pls[0].parentNode.parentNode.removeChild(pls[0].parentNode);
            }
            var ins = document.getElementsByTagName('ins');
            if (ins.length) {
                ins[0].parentNode.removeChild(ins[0]);
            }
        }
    };
}]);