(function( $ ) {
    $.widget( "custom.tooltipX", $.ui.tooltip, {
        options: {
            autoShow: true,
            autoHide: true
        },

        _create: function() {
            this._super();
            if(!this.options.autoShow){
                this._off(this.element, "mouseover focusin");
            }
        },

        _open: function( event, target, content ) {
            this._superApply(arguments);
            if(!this.options.autoHide){
                this._off(target, "mouseleave focusout");
            }

            if ($(target[0]).parents('.preview').length == 0) {
                var el = $('a[data-refid]');
                var idx = jQuery.inArray(target[0], el);
                if (idx >= 0) {
                    el.splice(idx, 1);
                }
                el.tooltipX();
                el.tooltipX('close');
            }

            $(content[0]).parent().parent().mouseleave(function (ev) {
                var els = $(ev.toElement);
                if (els.parents('.ui-tooltip').length == 0 && !els.hasClass('ui-tooltip')) {
                    var ed = $('a[data-refid]');
                    ed.tooltipX();
                    ed.tooltipX('close');
                }
            });
        }
    });

}( jQuery ) );

angular.module('videoApp', [
    'ngRoute',
    'ngSanitize',
    'ngCookies',
    'ngAnimate',
    'videoAppController',
    'videoAppView',
    ['$compileProvider', function ($compileProvider) { $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|rtmp):/); }]
]).constant('USER_TYPE', {
    ANONYMOUS: 1,
    USER: 2
}).constant('CHAT_MESSAGE_TYPE', {
    CONNECT: 1,
    MESSAGE: 2,
    HISTORY: 3,
    TYPING: 4,
    TOPIC: 5,
    ONLINE: 6
}).service('Internal', ['$http', '$q', function ($http, $q) {
    return {
        request: function (path, data) {
            return $q(function (resolve, reject) {
                $http({
                    url: '/internal' + path,
                    method: 'POST',
                    data: data
                }).then(function (data) {
                    resolve(data.data);
                }, function (data) {
                    reject(data.data);
                });
            });
        }
    };
}]).service('Session', function () {
    this.populate = function (data) {
        this.id     = data.id;
        this.name   = data.name;
        this.email  = data.email;
        this.hash   = data.hash;
        this.authed = data.authed;
    };
    this.clear = function () {
        this.id     = null;
        this.name   = null;
        this.email  = null;
        this.hash   = null;
        this.authed = null;
    };
    this.clear();
    return this;
}).service('Stream', function () {
    this.populate = function (data) {
        this.id    = data.id;
        this.topic = data.topic;
    };

    this.setAppName = function (app, name) {
        this.app  = app;
        this.name = name;
    };

    this.clear = function () {
        this.id    = null;
        this.topic = null;
        this.app   = 'video';
        this.name  = 'eientei';
    };

    this.clear();
    return this;
}).service('Config', function () {
    this.initialize = function (data) {
        this.rtmpBase = data.rtmpBase;
        this.captchaPublic = data.captchaPublic;
        this.captchaEnabled = !!(data.captchaPublic);
        this.maxStreams = data.maxStreams;
        this.origin =  window.location.protocol + '//' + window.location.host;
    };
    return this;
}).config(['$routeProvider', '$httpProvider', '$locationProvider', 'USER_TYPE', function ($routeProvider, $httpProvider, $locationProvider, USER_TYPE) {
    $locationProvider.html5Mode(true);

    $routeProvider.when('/', {
        templateUrl: '/view/index.view.html',
        controller: 'IndexController'
    }).when('/signup', {
        templateUrl: '/view/signup.view.html',
        controller: 'SignupController',
        onlyFor: USER_TYPE.ANONYMOUS
    }).when('/profile', {
        templateUrl: '/view/profile.view.html',
        controller: 'ProfileStreamController',
        onlyFor: USER_TYPE.USER
    }).when('/profile/personal', {
        templateUrl: '/view/profile.view.html',
        controller: 'ProfilePersonalController',
        onlyFor: USER_TYPE.USER
    }).when('/profile/credentials', {
        templateUrl: '/view/profile.view.html',
        controller: 'ProfileCredentialsController',
        onlyFor: USER_TYPE.USER
    }).when('/info', {
        templateUrl: '/view/info.view.html'
    }).when('/:app/:stream', {
        templateUrl: '/view/player.view.html',
        controller: 'PlayerController'
    }).otherwise({
        error: 404
    });
}]).run(['$rootScope', '$location', '$route', 'Internal', 'Session', 'Stream', 'Config', 'USER_TYPE', function ($rootScope, $location, $route, Internal, Session, Stream, Config, USER_TYPE) {
    $rootScope.stream = Stream;
    Internal.request('/security/initialize').then(function (data) {
        Session.populate(data);
        $rootScope.session = Session;
        Internal.request('/config/initialize').then(function (data) {
            Config.initialize(data);
            $rootScope.config = Config;
            $rootScope.checkAccess($route.current);
            $rootScope.loaded = true;
        });
    });

    $rootScope.showLogin = function () {
        $rootScope.login = true;
    };

    $rootScope.hideLogin = function () {
        $rootScope.login = false;
    };

    $rootScope.performLogout = function () {
        Internal.request('/user/logout').then(function (data) {
            Session.populate(data);
            $rootScope.checkAccess($route.current, true);
        });
    };

    $rootScope.setPlayback = function (b) {
        $rootScope.playback = b;
    };

    $rootScope.makeError = function(code) {
        $rootScope.error = {};
        $rootScope.error.code = code;
        $rootScope.error.path = $location.path();
        switch (code) {
            case 404:
                $rootScope.error.message = 'Not found';
                break;
            case 401:
                $rootScope.error.message = 'Not authenticated';
                break;
            case 403:
                $rootScope.error.message = 'Not authorized';
                break;
        }
    };

    $rootScope.checkAccess = function (next, redir) {
        $rootScope.error = null;
        $rootScope.hideLogin();
        if (next.error) {
            $rootScope.makeError(next.error);
            return;
        }
        if (next.onlyFor) {
            switch (next.onlyFor) {
                case USER_TYPE.ANONYMOUS:
                    if (Session.authed) {
                        if (redir) {
                            $location.path('/profile');
                        } else {
                            $rootScope.makeError(403);
                        }
                    }
                    break;
                case USER_TYPE.USER:
                    if (!Session.authed) {
                        if (redir) {
                            $location.path('/');
                        } else {
                            $rootScope.makeError(401);
                            $rootScope.showLogin();
                        }
                    }
                    break;
            }
        }
    };

    $rootScope.$on('$routeChangeStart', function (a, next) {
        $rootScope.checkAccess(next);
        $rootScope.setPlayback(false);
        $rootScope.stream.clear();
    });
}]);


angular.module('videoAppView', [

]).filter('topic', function() {
    return function(input) {
        if (input) {
            if (input.trim()) {
                return '"' + input + '"'
            }
        }
        return '<em>no topic</em>';
    };
}).filter('gravatar', function () {
    return function(input) {
        if (!input) {
            input = '00000000000000000000000000000000';
        }
        return 'http://gravatar.com/avatar/' + input + '?d=identicon&s=64'
    };
}).filter('timeago', function () {
    return function (input) {
        return moment.unix(input / 1000).fromNow();
    };
}).directive('reCaptcha', function () {
    return function (scope, element) {
        if (!element[0].id) {
            element[0].id = 'reCaptcha';
        }
        Recaptcha.create(scope.config.captchaPublic, element[0].id, { theme: 'clean' });
    };
}).directive('sameAs', function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attr, ngModel) {
            var oModel = scope.$eval(attr.sameAs);

            ngModel.$parsers.unshift(function (val) {
                ngModel.$setValidity('sameAs', val == oModel.$viewValue);
                return val;
            });
            oModel.$parsers.unshift(function (val) {
                ngModel.$setValidity('sameAs', val == ngModel.$viewValue);
                return val;
            });
        }
    };
}).directive('iconPreview', function () {
    return {
        restrict: 'A',
        link: function (scope, el, attrs) {
            $(el[0]).tooltip({
                items: 'img',
                content: function () {
                    return '<img style="display: block; margin-left: auto; margin-right: auto;" src="http://gravatar.com/avatar/' + scope.online.hash + '?d=identicon&s=64"/><div style="text-align: center;">' + scope.online.name + '</div>';
                }
            });
        }
    };
}).directive('postPreview', ['$http', '$compile', '$templateCache', '$sce', function ($http, $compile, $templateCache, $sce) {
    return {
        restrict: 'A',
        link: function (scope, el, attrs) {
            $(el[0]).tooltipX({
                items: 'a[data-refid]',
                autoHide: false,
                content: function (cb) {
                    $http({
                        url: '/internal/chatinfo/getpreview',
                        method: 'POST',
                        data: {
                            id: $(this).data('refid')
                        }
                    }).then(function (resp) {
                        var template = $templateCache.get('/view/player.chat.message.view.html')[1];
                        var s = scope.$new(true);
                        resp.data.text = $sce.trustAsHtml(resp.data.text);
                        s.message = resp.data;
                        s.message.preview = true;
                        var res = $compile(template)(s);
                        cb(res);
                    }, function (resp) {
                        cb("Error " + resp.status);
                    });
                    cb('Loading ...');
                }
            });
        }
    };
}]).directive('appSubmit', function() {
    return {
        require: 'form',
        restrict: 'A',
        link: function (scope, element, attribute, form) {
            element.on('submit', function () {
                form.$setSubmitted(true);
                angular.forEach(form, function (formElement, fieldName) {
                    if (fieldName[0] === '$') return;
                    formElement.$dirty = true;
                    formElement.$commitViewValue();
                });

                if (form.$valid) {
                    scope.$eval(attribute.appSubmit);
                } else {
                    scope.$apply();
                }
            });
        }
    }
}).directive('trimLimit', function() {
    return {
        restrict: 'A',
        link: function (scope, element, attribute) {
            element.bind('keydown', function (event) {
                var val = element.val();
                if (val.length >= attribute.trimLimit) {
                    if (event.keyCode > 46) {
                        event.preventDefault();
                    }
                    element.val(val.substring(0, attribute.trimLimit));
                }
            });
        }
    }
}).directive('topicChange', ['Internal', function(Internal) {
    return {
        restrict: 'A',
        link: function (scope, element, attribute) {
            element.bind('dblclick', function (event) {
                element.addClass('hidden');
                var input = $('<input/>');
                element.parent().append(input);
                input.val(element.text());
                input.focus();
                input.keydown(function (ev) {
                    if (ev.keyCode == 13) {
                        var newtopic = input.val();
                        Internal.request('/stream/updatetopic', {id: scope.stream.id, topic: newtopic});
                        input.remove();
                        element.removeClass('hidden');
                    }
                });

            });
        }
    }
}]).directive('dragVideo', ['$timeout', function($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attribute) {
            function setValue(x) {
                var videowrap = $('.player-wrapper');
                var chatwrap = $('.chat-wrapper');

                if (!videowrap.length || !chatwrap.length) {
                    $timeout(function () {
                        setValue(x);
                    }, 50);
                    return;
                }

                videowrap.width(x);
                chatwrap.width(window.innerWidth - x);
                element[0].style.left= x + 'px';
                $.cookie('player-x', x, { expires: 365, path: '/' });
            }

            $timeout(function () {
                var oldx = $.cookie('player-x');
                if (oldx) {
                    setValue(oldx);
                }
            });

            element[0].draggable = true;
            element.bind('drag', function (ev) {
                var x = ev.x;
                if (x) {
                    if (x < 100) {
                        x = 100;
                    }
                    if ((window.innerWidth - x) < 100) {
                        x = window.innerWidth - 100;
                    }

                    setValue(x);
                }
            });
        }
    }
}]).directive('dragChat', ['$timeout', function($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attribute) {
            function setValue(y) {
                var messages = $('.messages');
                var bars = $('.bars');

                if (!messages.length || !bars.length) {
                    $timeout(function () {
                        setValue(y);
                    }, 50);
                    return;
                }

                messages.height(y);
                bars.height(window.innerHeight - y);

                element[0].style.top= (y - 10) + 'px';
                $.cookie('chat-y', y, { expires: 365, path: '/' });
            }

            $timeout(function () {
                var oldy = $.cookie('chat-y');
                if (oldy) {
                    setValue(oldy);
                }
            });

            element[0].draggable = true;
            element.bind('drag', function (ev) {
                var y = ev.y;
                if (y) {
                    if (y < 100) {
                        y = 100;
                    }
                    if ((window.innerHeight - y) < 100) {
                        y = window.innerHeight - 100;
                    }

                    y -= 10;

                    setValue(y);
                }
            });
        }
    }
}]).directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind('keydown keypress', function (event) {
            if (event.which == 13 && !event.shiftKey) {
                if (element[0].form && element[0].form.name && scope[element[0].form.name]) {
                    var el = scope[element[0].form.name][attrs.name];
                    el.$commitViewValue();
                    el.$validate();
                    if (el.$valid && attrs.ngEnter) {
                        scope.$eval(attrs.ngEnter);
                    }
                    scope.$apply();
                } else {
                    scope[attrs.name] = {
                        text: element.val(),
                        clear: function () {
                            element.val('')
                        }
                    };
                    scope.$eval(attrs.ngEnter);
                }
                event.preventDefault();
            }
        });
    };
});

angular.module('videoAppController', [

]).controller('IndexController', ['$scope', 'Internal', function($scope, Internal) {
    Internal.request('/index/list').then(function (data) {
        $scope.streamIndex = data;
    });
}]).controller('SignupController', ['$scope', '$location', 'Internal', 'Session', function ($scope, $location, Internal, Session) {
    $scope.signup = function (credentials) {
        var signupData = {
            name: credentials.username,
            password: credentials.password,
            passwordRepeat: credentials.passwordconfirm,
            email: credentials.email,
            captchaChallenge: document.getElementById('recaptcha_challenge_field').value,
            captchaResponse: document.getElementById('recaptcha_response_field').value
        };
        Internal.request('/user/signup', signupData).then(function (data) {
            Session.populate(data);
            $location.path('/profile');
        }, function (data) {
            $scope.error = data.text;
            Recaptcha.reload();
        });
    };
}]).controller('LoginController', ['$scope', '$route', 'Internal', 'Session', function ($scope, $route, Internal, Session) {
    $scope.performLogin = function (credentials) {
        var loginData = {
            name: credentials.username,
            password: credentials.password
        };

        Internal.request('/user/login', loginData).then(function (data) {
            Session.populate(data);
            $scope.hideLogin();
            $scope.loginForm.$setPristine(true);
            $scope.loginForm.$serverErrors = null;
            $scope.credentials = null;
            $scope.checkAccess($route.current, true);
        }, function (data) {
            $scope.login = {
                error: data.text
            };
        });
    };
}]).controller('ProfileStreamController', ['$scope', '$location', 'Internal', function ($scope, $location, Internal) {
    $scope.path = $location.path();

    function setStreams(data) {
        $scope.streams = data.items;
        for (var i = 0; i < $scope.streams.length; i++) {
            $scope.streams[i].error = {};
            $scope.streams[i].copy = {
                name: $scope.streams[i].name,
                topic: $scope.streams[i].topic,
                image: $scope.streams[i].imageUrl
            };
        }
    }

    $scope.updateKey = function (stream) {
        Internal.request('/stream/updatekey', {id: stream.id}).then(function (data) {
            setStreams(data);
        });
    };

    $scope.updateName = function (stream, newName) {
        Internal.request('/stream/updatename', {id: stream.id, name: newName}).then(function (data) {
            setStreams(data);
        }, function (data) {
            stream.error.name = data.text;
        });
    };

    $scope.updateTopic = function (stream, newTopic) {
        Internal.request('/stream/updatetopic', {id: stream.id, topic: newTopic}).then(function (data) {
            setStreams(data);
        });
    };

    $scope.updateImage = function (stream, newImage) {
        Internal.request('/stream/updateimage', {id: stream.id, image: newImage}).then(function (data) {
            setStreams(data);
        });
    };

    $scope.updatePrivate = function (stream) {
        Internal.request('/stream/updateprivate', {id: stream.id}).then(function (data) {
            setStreams(data);
        });
    };

    $scope.deleteStream = function (stream) {
        Internal.request('/stream/delete', { id: stream.id }).then(function (data) {
            setStreams(data);
        });
    };

    $scope.addStream = function () {
        Internal.request('/stream/add').then(function (data) {
            setStreams(data);
        });
    };

    Internal.request('/stream/own').then(function (data) {
        setStreams(data);
    });
}]).controller('ProfilePersonalController', ['$scope', '$location', 'Session', 'Internal', function ($scope, $location, Session, Internal) {
    $scope.path = $location.path();
    $scope.credentials = {
        email: Session.email
    };

    $scope.updatePersonal = function (credentials) {
        Internal.request('/user/updatepersonal', { email: credentials.email }).then(function (data) {
            Session.populate(data);
        });
    };
}]).controller('ProfileCredentialsController', ['$scope', '$location', 'Internal', 'Session', function ($scope, $location, Internal, Session) {
    $scope.path = $location.path();

    $scope.updateCredentials = function (credentials) {
        Internal.request('/user/updatecredentials', {
            originalPassword: credentials.currentPassword,
            password: credentials.password,
            passwordRepeat: credentials.passwordconfirm
        }).then(function (data) {
            Session.populate(data);
            $scope.profileFormCredentials.$setPristine(true);
            $scope.credentials = null;
        }, function (data) {
            $scope.error = data.text;
        });
    };
}]).controller('PlayerController', ['$scope', '$routeParams', '$sce', '$timeout', 'CHAT_MESSAGE_TYPE', 'Internal', function ($scope, $routeParams, $sce, $timeout, CHAT_MESSAGE_TYPE, Internal) {
    $scope.setPlayback(true);

    $scope.streamApp = $routeParams.app;
    $scope.streamName = $routeParams.stream;
    $scope.videoEnabled = true;
    $scope.chatEnabled = true;
    $scope.buffer = '1.0';
    $scope.messages = [];
    $scope.typers = [];
    $scope.onlines = [];

    if ($routeParams.buffer) {
        $scope.buffer = $routeParams.buffer;
    }

    $scope.onlyvideo = $routeParams.onlyvideo;
    $scope.onlychat = $routeParams.onlychat;

    var active = true;
    var first = true;
    var imm = false;
    var refpoint = 0;

    Internal.request('/stream/bootstrap', { app: $routeParams.app, name: $routeParams.stream }).then(function (data) {
        if (!data.ok) {
            $scope.makeError(404);
        }
        $scope.imageUrl = data.idleImage;
        $scope.swfurl = '/swf/yukkuplayer.swf';
        $scope.stream.populate(data);
        $scope.stream.setAppName($routeParams.app, $routeParams.stream);
    });

    $scope.insertOrdinal = function (id) {
        var textarea = $('.inputText');
        var text = '>>' + id;
        var txtval = textarea.val();
        if ((txtval.length + text.length) < 256) {
            var start = textarea.get(0).selectionStart;
            textarea.val(txtval.substring(0, start) + text + txtval.substring(start));
            textarea.keyup();
        }
    };

    $scope.sendTyping = function () {
        $scope.ws.send(JSON.stringify({
            type: CHAT_MESSAGE_TYPE.TYPING
        }));
    };

    $scope.sendMessage = function (el) {
        $scope.ws.send(JSON.stringify({
            type: CHAT_MESSAGE_TYPE.MESSAGE,
            data: {
                text: el.inputText.text
            }
        }));
        el.inputText.clear();
    };

    $scope.loadMore = function () {
        refpoint = $scope.messages[0].id;
        $scope.ws.send(JSON.stringify({
            type: CHAT_MESSAGE_TYPE.HISTORY,
            data: {
                refpoint: $scope.messages[0].id
            }
        }));
    };

    $scope.scrollToBottom = function () {
        var messages = $('.messages');
        var scrollHeight = messages[0].scrollHeight;
        var scrollTop = messages[0].scrollTop;

        var diff = scrollHeight - scrollTop;

        if (diff < messages.height() * 2) {
            messages[0].scrollTop = scrollHeight;
        }
    };

    $scope.scrollHistory = function (ref) {
        var messages = $('.messages');
        if (!messages.length) {
            return;
        }
        var scrollHeight = messages[0].scrollHeight;
        if (ref == 0) {
            messages[0].scrollTop = scrollHeight;
            return;
        }
        var children = messages.children('.message');
        var top = 0;
        for (var i = 0; i < children.length; i++) {
            var ch = $(children[i]);
            if (ch.find('.ordinal').text() == ref) {
                break;
            }
            top += ch.outerHeight(true);
        }
        messages[0].scrollTop = top;
    };

    $scope.connect = function () {
        $scope.ws = new SockJS('/internal/chat');

        $scope.ws.onopen = function () {
            $scope.ws.send(JSON.stringify({
                type: CHAT_MESSAGE_TYPE.CONNECT,
                data: {
                    app: $routeParams.app,
                    name: $routeParams.stream
                }
            }));
        };
        $scope.ws.onclose = function () {
            if (active) {
                if (imm) {
                    imm = false;
                    $scope.connect();
                } else {
                    setTimeout($scope.connect, 1000);
                }
            }
        };

        $scope.ws.onmessage = function(event) {
            var msg = JSON.parse(event.data);
            switch(msg.type) {
                case CHAT_MESSAGE_TYPE.MESSAGE:
                    msg.data.text = $sce.trustAsHtml(msg.data.text);
                    $scope.messages.push(msg.data);
                    $scope.$apply();
                    $scope.scrollToBottom();
                    break;
                case CHAT_MESSAGE_TYPE.ONLINE:

                    $scope.onlines = msg.data.items;

                    $scope.hasAuthor = false;
                    for (var n = 0; n < msg.data.items.length; n++) {
                        if (msg.data.items[n].owner) {
                            var tmp = msg.data.items[0];
                            msg.data.items[0] = msg.data.items[n];
                            msg.data.items[n] = tmp;
                            $scope.hasAuthor = true;
                            break
                        }
                    }

                    if (first) {
                        first = false;
                        $scope.$on('$routeChangeStart', function () {
                            imm = true;
                            active = false;
                            $scope.ws.close();
                            var el = $('a[data-refid]');
                            el.tooltipX();
                            el.tooltipX('close');
                        });
                        $scope.ws.send(JSON.stringify({
                            type: CHAT_MESSAGE_TYPE.HISTORY,
                            data: {
                                refpoint: 0
                            }
                        }));
                    }
                    break;
                case CHAT_MESSAGE_TYPE.TYPING:
                    $scope.typers = msg.data.items;
                    $scope.$apply();
                    break;
                case CHAT_MESSAGE_TYPE.HISTORY:
                    for (var i = 0; i < msg.data.items.length; i++) {
                        msg.data.items[i].text = $sce.trustAsHtml(msg.data.items[i].text);
                        $scope.messages.unshift(msg.data.items[i]);
                    }
                    $scope.hasMore = msg.data.hasMore;
                    $scope.$apply();
                    $timeout(function () {
                        $scope.scrollHistory(refpoint);
                    }, 50);
                    break;
                case CHAT_MESSAGE_TYPE.TOPIC:
                    $scope.stream.topic = msg.data.topic;
                    $scope.$apply();
                    break;
            }
        };
    };

    $scope.connect();
}]);