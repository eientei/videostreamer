'use strict';

angular.module('videostreamer').service('restapi', ['$http', function($http) {
    var me = this;

    me.post = function (path, data) {
        return $http.post(path, data);
    };

    me.postform = function (path, data) {
        return $http.post(path, data, {
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            transformRequest: function (obj) {
                var str = [];
                for(var p in obj) {
                    if (!obj.hasOwnProperty(p)) {
                        continue;
                    }
                    str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                }
                return str.join("&");
            }
        })
    };

    me.get = function (path, args) {
        return $http.get(path, args);
    };

    me.security = {
        user: function () {
            return me.get('security/user');
        },
        login: function (username, password) {
            return me.postform('security/login', {
                username: username,
                password: password,
                rememberme: true
            });
        },
        logout: function () {
            return me.post('security/logout');
        },
        signup: function (username, password, email, captcha) {
            return me.post('security/signup', {
                username: username,
                password: password,
                email: email,
                captcha: captcha
            });
        },
        email: function (newmail) {
            return me.post('security/email', newmail);
        },
        password: function (oldpass, newpass) {
            return me.post('security/password', {
                current: oldpass,
                desired: newpass
            });
        },
        resetreq: function(name, email, captcha) {
            return me.post('security/resetreq', {
                name: name,
                email: email,
                captcha: captcha
            });
        },
        resettry: function (resetkey) {
            return me.post('security/resettry', resetkey);
        },
        resetsubmit: function (resetkey, password) {
            return me.post('security/resetsubmit', {
                resetkey: resetkey,
                password: password
            });
        }
    };

    me.config = {
        rtmp: function () {
            return me.get('config/rtmp');
        },
        captcha: function () {
            return me.get('config/captcha');
        }
    };

    me.streams = {
        running: function () {
            return me.post('streams/running');
        },
        stream: function (app, name) {
            return me.get('streams/stream/' + app +'/' + name);
        },
        mine: function () {
            return me.get('streams/mine');
        },
        allocate: function () {
            return me.get('streams/allocate');
        },
        deallocate: function (app, name) {
            return me.get('streams/deallocate/' + app + '/' + name);
        },
        rename: function (app, name, newname) {
            return me.post('streams/rename/' + app + '/' + name, newname);
        },
        topic: function (app, name, newtopic) {
            return me.post('streams/topic/' + app + '/' + name, newtopic);
        },
        screensaver: function (app, name, newurl) {
            return me.post('streams/screensaver/' + app + '/' + name, newurl);
        },
        gentoken: function (app, name) {
            return me.post('streams/gentoken/' + app + '/' + name);
        },
        restricted: function (app, name, value) {
            return me.post('streams/restricted/' + app + '/' + name, JSON.stringify(value));
        }
    };

    return me;
}]);