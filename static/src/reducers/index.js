import {combineReducers} from 'redux';
import {handleActions} from 'redux-actions';

import {history} from '../App';

const navigation = handleActions({
        NAVIGATION: {
            INIT: (state) => ({...state, tab: inferTabId()}),
            HOME: (state) => ({...state, tab: 0, path: '/'}),
            LOGIN: (state) => ({...state, tab: 1, path: '/login'}),
            PROFILE: (state, {payload: {section}}) => ({...state, tab: 1, path: '/profile/' + section}),
            HOWTO: (state) => ({...state, tab: 2, path: '/howto'}),
            SIGNUP: (state) => ({...state, tab: 0, path: '/signup'}),
            PLAY: (state, {payload: {path, stream}}) => ({...state, path: '/live/' + path + '/' + stream}),
            LOGOUT: (state) => ({...state, tab: 0, path: '/logout'}),
            NOTFOUND: (state) => ({...state, tab: 0, path: '/notfound'}),
        }
    },
    {
        path: '/',
        tab: 0,
    }
);

const user = handleActions({
        USER: {
            SET: (state, {payload: {user: {name, email, gravatar, streams, notifications}}}) => ({...state, name, email, gravatar, streams: streams || [], notifications: notifications || []}),
        }
    },
    {
        name: 'anonymous',
        email: '',
        gravatar: '',
        streams: [],
        notifications: [],
    }
);

function validateName(name) {
    if (name.length < 3) {
        return 'Must be longer than three symbols';
    }
    if (name.length > 64) {
        return 'Must be shorter than 64 symbols';
    }
    return '';
}

function validateAlphaName(name) {
    const err = validateName(name);
    if (err.length > 0) {
        return err
    }
    if (!name.match(/^[a-zA-Z0-9]+$/)) {
        return 'Must be alphanumeric';
    }
    return '';
}

function validateAlphaAny(name) {
    if (!name.match(/^[a-zA-Z0-9]*$/)) {
        return 'Must be alphanumeric';
    }
    return '';
}

function validateRepeat(password, passwordrepeat) {
    if (password !== passwordrepeat) {
        return 'Must match password';
    }
    return '';
}

const login = handleActions({
        LOGIN: {
            SET: (state, {payload: {error}}) => ({...state, error, username: {value: state.username.value, error: ''}, password: {value: state.password.value, error: ''}}),
            USERNAME_UPDATE: (state, {payload: {username}}) => ({...state, username: {value: username, error: validateName(username)}}),
            PASSWORD_UPDATE: (state, {payload: {password}}) => ({...state, password: {value: password, error: validateName(password)}}),
            SEND: (state) => ({...state, username: {value: '', error: ''}, password: {value: '', error: ''}}),
        }
    },
    {
        error: '',
        username: {
            value: '',
            error: '',
        },
        password: {
            value: '',
            error: '',
        }
    }
);

const signup = handleActions({
        SIGNUP: {
            SET: (state, {payload: {error}}) => ({...state, error, username: {value: state.username.value, error: ''}, email: {value: state.email.value, error: ''}, password: {value: state.password.value, error: ''}, passwordrepeat: {value: state.passwordrepeat.value, error: ''}}),
            USERNAME_UPDATE: (state, {payload: {username}}) => ({...state, username: {value: username, error: validateAlphaName(username)}}),
            EMAIL_UPDATE: (state, {payload: {email}}) => ({...state, email: {value: email, error: validateName(email)}}),
            PASSWORD_UPDATE: (state, {payload: {password}}) => ({...state, password: {value: password, error: validateName(password)}, passwordrepeat: {value: state.passwordrepeat.value, error: validateRepeat(password, state.passwordrepeat.value)}}),
            PASSWORDREPEAT_UPDATE: (state, {payload: {passwordrepeat}}) => ({...state, passwordrepeat: {value: passwordrepeat, error: validateRepeat(state.password.value, passwordrepeat)}}),
            SEND: (state) => ({...state, username: {value: '', error: ''}, email: {value: '', error: ''}, password: {value: '', error: ''}, passwordrepeat: {value: '', error: ''}}),
        }
    },
    {
        error: '',
        username: {
            value: '',
            error: '',
        },
        email: {
            value: '',
            error: '',
        },
        password: {
            value: '',
            error: '',
        },
        passwordrepeat: {
            value: '',
            error: '',
        },
    }
);

const profile = handleActions({
        PROFILE: {
            SET: (state, {payload: {error}}) => ({...state, error, email: {value: state.email.value, error: ''}, password: {value: state.password.value, error: ''}, passwordrepeat: {value: state.passwordrepeat.value, error: ''}}),
            EMAIL_UPDATE: (state, {payload: {email}}) => ({...state, email: {value: email, error: validateName(email)}}),
            PASSWORD_UPDATE: (state, {payload: {password}}) => ({...state, password: {value: password, error: validateName(password)}, passwordrepeat: {value: state.passwordrepeat.value, error: validateRepeat(password, state.passwordrepeat.value)}}),
            PASSWORDREPEAT_UPDATE: (state, {payload: {passwordrepeat}}) => ({...state, passwordrepeat: {value: passwordrepeat, error: validateRepeat(state.password.value, passwordrepeat)}}),
            SEND: (state) => ({...state}),
        }
    },
    {
        error: '',
        email: {
            value: '',
            error: '',
        },
        password: {
            value: '',
            error: '',
        },
        passwordrepeat: {
            value: '',
            error: '',
        },
    }
);

function inferTabId() {
    if (history.location.pathname.startsWith('/login') || history.location.pathname.startsWith('/profile')) {
        return 1;
    }
    if (history.location.pathname.startsWith('/howto')) {
        return 2;
    }
    return 0;
}

const stream = handleActions({
        STREAM: {
            SET: (state, {payload: {error}}) => ({...state, error}),
            SELECT: (state, {payload: {stream: {id, name, title, logourl, key, privated}}}) => ({...state, selected: id, key, privated, name: {value: name, error: ''}, title: {value: title, error: ''}, logourl: {value: logourl, error: ''}}),
            NAME_UPDATE: (state, {payload: {name}}) => ({...state, name: {value: name, error: validateAlphaAny(name)}}),
            TITLE_UPDATE: (state, {payload: {title}}) => ({...state, title: {value: title, error: ''}}),
            LOGOURL_UPDATE: (state, {payload: {logourl}}) => ({...state, logourl: {value: logourl, error: ''}}),
            PRIVATED_UPDATE: (state, {payload: {privated}}) => ({...state, privated}),
            SEND: (state) => ({...state}),
        }
    },
    {
        selected: 0,
        key: '',
        privated: false,
        error: '',
        name: {
            value: '',
            error: '',
        },
        title: {
            value: '',
            error: '',
        },
        logourl: {
            value: '',
            error: '',
        }
    }
);

const message = handleActions({
        MESSAGE: {
            SET: (state, {payload: {error}}) => ({...state, error}),
            TEXT_UPDATE: (state, {payload: {text}}) => ({...state, text: {value: text}}),
            ADD: (state, {payload: {message: {id, author, gravatar, edited, text, posted}}}) => ({...state, serial: state.serial+1, messages: {...state.messages, [id]: {id, author, gravatar, edited, text, posted}}}),
            CLEAR: (state) => ({...state, messages: {}, serial: 0}),
        }
    },
    {
        serial: 0,
        error: '',
        messages: {},
        text: {
            value: '',
        },
    }
);

const ws = handleActions({
        WS: {
            FLY: (state) => ({...state, inflight: true}),
            LAND: (state) => ({...state, inflight: false}),
            STREAM_LIST: (state, {payload: {streams}}) => ({...state, streams}),
            STREAM_INFO: (state, {payload: {stream}}) => ({...state, info: stream}),
            STREAM_STATUS: (state, {payload: {status}}) => ({...state, status}),
        }
    },
    {
        inflight: false,
        streams: [],
        info: null,
        status: 'offline',
    }
);

export const rootReducer = combineReducers({navigation, user, login, signup, profile, stream, message, ws});