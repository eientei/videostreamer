import { take, put, takeLatest, all, call } from 'redux-saga/effects';

import {actions} from '../actions';
import {history, store} from '../App.js';

function reconnect() {
    if (t != null) {
        clearTimeout(t);
    }
    console.log('reconnecting...');
    t = setTimeout(() => {
        ws = makeSocket();
    }, 1000);
}

function errorhandler(e) {
    reconnect();
}

function auth() {
    const username = localStorage.getItem('username') || 'anonymous';
    const password = localStorage.getItem('password');
    store.dispatch(actions.ws.ready());
    store.dispatch(actions.ws.userLogin(username, password));
    store.dispatch(actions.ws.streamListreq());

    if (username === 'anonymous') {
        const ss = localStorage.getItem('subscriptions') || "[]";
        const subs = JSON.parse(ss);
        subs.forEach((s) => {
            store.dispatch(actions.ws.streamSubscribe(s));
        })
    }
}

function handler(event) {
    const {type, data} = JSON.parse(event.data);
    switch (type) {
        case 'disconnect':
            store.dispatch(actions.ws.disconnect());
            break;
        case 'error_signup':
            store.dispatch(actions.ws.errorSignup(data.error));
            break;
        case 'error_login':
            store.dispatch(actions.ws.errorLogin(data.error));
            break;
        case 'error_profile':
            store.dispatch(actions.ws.errorProfile(data.error));
            break;
        case 'error_stream':
            store.dispatch(actions.ws.errorStream(data.error));
            break;
        case 'error_subscribe':
            store.dispatch(actions.ws.errorSub(data.error));
            break;
        case 'error_info':
            store.dispatch(actions.ws.errorInfo(data.error));
            break;
        case 'user_details':
            store.dispatch(actions.ws.userDetails(data));
            if (data.name !== 'anonymous') {
                const {stream: {selected}} = store.getState();
                const str = data.streams.find(s => s.id === selected);
                store.dispatch(actions.stream.select(str ? str : data.streams[0]));
                store.dispatch(actions.profile.emailUpdate(data.email));
            }
            break;
        case 'stream_published':
            const n = data.stream.owner + (data.stream.name.length === 0 ? '' : ('/' + data.stream.name));
            if (history.location.pathname !== '/live/' + n && history.location.pathname !== '/chat/' + n) {
                new Notification('Stream ' + n + ' started', {
                    body: data.stream.title,
                    icon: 'https://www.gravatar.com/avatar/' + data.stream.gravatar + '?d=identicon&s=' + 64,
                });
            }
            break;
        case 'stream_list':
            store.dispatch(actions.ws.streamList(data.streams));
            break;
        case 'stream_redirect':
            if (data.owner === '' && data.stream === '') {
                store.dispatch(actions.navigation.notfound());
            } else {
                store.dispatch(actions.navigation.play(data.owner, data.stream));
            }
            break;
        case 'stream_info':
            store.dispatch(actions.ws.streamInfo(data));
            break;
        case 'message_add':
            store.dispatch(actions.message.add(data));
            break;
        default:
            console.log("Unknwon message type of", event.data);
            break;
    }
}

function makeSocket() {
    const ws = new WebSocket((window.location.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + window.location.host + '/api/event');;
    ws.error = errorhandler;
    ws.onclose = reconnect;
    ws.onopen = auth;
    ws.onmessage = handler;
    return ws;
}

let ws = makeSocket();
let t = null;

function send(data) {
    if (!('data' in data)) {
        data['data'] = {};
    }
    ws.send(JSON.stringify(data));
}

function* wsDisconnect() {
    yield call(console.log, 'Disconnect by server');
}

function* wsUserSignup({payload: {username, password, email, captcha}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'user_signup', data: {username, password, email, captcha}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_SIGNUP']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            if (response.payload.details.username !== 'anonymous') {
                localStorage.setItem('username', username);
                localStorage.setItem('password', password);
            }
            yield put(actions.user.set(response.payload.details));
            history.push('/profile/stream');
            break;
        case 'WS/ERROR_SIGNUP':
            yield put(actions.signup.set(response.payload.error));
            break;
        default:
            yield put(actions.signup.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsUserLogin({payload: {username, password}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'user_login', data: {username, password}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_LOGIN']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            if (response.payload.details.username !== 'anonymous') {
                localStorage.setItem('username', username);
                localStorage.setItem('password', password);
            }

            yield put(actions.user.set(response.payload.details));
            if (history.location.pathname === '/login') {
                history.push('/profile/stream');
            }
            break;
        case 'WS/ERROR_LOGIN':
            yield put(actions.login.set(response.payload.error));
            break;
        default:
            yield put(actions.login.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsUserLogout() {
    yield call(send, {type: 'user_logout'});
    const {payload: {details}} = yield take('WS/USER_DETAILS');
    localStorage.removeItem('username');
    localStorage.removeItem('password');
    yield put(actions.user.set(details));
}

function* wsUserInfoUpdate({payload: {email, password}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'user_info_update', data: {email, password}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_PROFILE']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_PROFILE':
            yield put(actions.profile.set(response.payload.error));
            break;
        default:
            yield put(actions.profile.set('Unknown reply'));
    }
    yield put(actions.ws.land());
    if (password !== '') {
        localStorage.setItem('password', password);
    }
}

function* wsStreamInfoUpdate({payload: {id, name, title, logourl}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'stream_info_update', data: {id, name, title, logourl}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_STREAM']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_STREAM':
            yield put(actions.stream.set(response.payload.error));
            break;
        default:
            yield put(actions.stream.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsStreamKeyUpdate({payload: {id}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'stream_key_update', data: {id}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_STREAM']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_STREAM':
            yield put(actions.stream.set(response.payload.error));
            break;
        default:
            yield put(actions.stream.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsStreamPrivatedUpdate({payload: {id, privated}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'stream_privated_update', data: {id, privated}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_STREAM']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_STREAM':
            yield put(actions.stream.set(response.payload.error));
            break;
        default:
            yield put(actions.stream.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsStreamDelete({payload: {id}}) {
    yield put(actions.ws.fly());
    yield call(send, {type: 'stream_delete', data: {id}});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_STREAM']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_STREAM':
            yield put(actions.stream.set(response.payload.error));
            break;
        default:
            yield put(actions.stream.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsStreamAdd() {
    yield put(actions.ws.fly());
    yield call(send, {type: 'stream_add'});
    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_STREAM']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            yield put(actions.user.set(response.payload.details));
            break;
        case 'WS/ERROR_STREAM':
            yield put(actions.stream.set(response.payload.error));
            break;
        default:
            yield put(actions.stream.set('Unknown reply'));
    }
    yield put(actions.ws.land());
}

function* wsStreamSubscribe({payload: {id}}) {
    yield call(send, {type: 'stream_subscribe', data: {id}});

    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_SUB']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            const {payload: {details}} = response;
            yield put(actions.user.set(details));
            const username = localStorage.getItem('username') || 'anonymous';
            if (username === 'anonymous') {
                const ss = localStorage.getItem('subscriptions') || "[]";
                const subs = JSON.parse(ss);
                subs.push(id);
                localStorage.setItem('subscriptions', JSON.stringify(subs));
            }
            break;
        case 'WS/ERROR_SUB':
            break;
        default:
    }
}

function* wsStreamUnsubscribe({payload: {id}}) {
    yield call(send, {type: 'stream_unsubscribe', data: {id}});

    const response = yield take(['WS/USER_DETAILS', 'WS/ERROR_SUB']);
    switch (response.type) {
        case 'WS/USER_DETAILS':
            const {payload: {details}} = response;
            yield put(actions.user.set(details));

            const username = localStorage.getItem('username') || 'anonymous';
            if (username === 'anonymous') {
                const ss = localStorage.getItem('subscriptions') || "[]";
                const subs = JSON.parse(ss);
                const index = subs.indexOf(id);
                if (index !== -1) {
                    subs.splice(index, 1);
                }
                localStorage.setItem('subscriptions', JSON.stringify(subs));
            }
            break;
        case 'WS/ERROR_SUB':
            break;
        default:
    }
}

function* wsStreamListreq() {
    yield call(send, {type: 'stream_list', data: {streams: []}});
}

function* wsStreamInforeq({payload: {owner, stream}}) {
    yield call(send, {type: 'stream_info_req', data: {owner, stream}});
    const response = yield take(['WS/STREAM_INFO', 'WS/ERROR_INFO']);
    switch (response.type) {
        case 'WS/STREAM_INFO':
            vidrespawn();
            break;
        case 'WS/ERROR_INFO':
            if (vidsocket != null) {
                vidsocket.close();
            }
            vidsocket = null;
            vidtimeout = null;
            vidsocket = null;
            vidsourceBuffer = null;
            vidqueue = [];
            yield put(actions.navigation.notfound());
            break;
        default:
    }
}

function* wsMessageSend({payload: {streamid, text}}) {
    yield call(send, {type: 'message_send', data: {streamid, text}});
}

function* wsMessageEdit({payload: {id, text}}) {
    yield call(send, {type: 'message_edit', data: {id, text}});
}

function* wsMessageDelete({payload: {id}}) {
    yield call(send, {type: 'message_delete', data: {id}});
}

function* wsMessageHistory({payload: {streamid, before}}) {
    yield call(send, {type: 'message_history', data: {streamid, before}});
}

let vidsocket = null;
let vidqueue = [];
let vidsourceBuffer = null;
let vidtimeout = null;

function viddrain() {
    if (vidqueue.length === 0) {
        return;
    }
    if (vidsourceBuffer.updating) {
        return;
    }
    const next = vidqueue.shift();
    try {
        vidsourceBuffer.appendBuffer(next);
    } catch (e) {
        vidrespawn();
    }
}

function vidonmessage(event) {
    if (!history.location.pathname.startsWith('/live/')) {
        return;
    }
    const fileReader = new FileReader();
    fileReader.onload = () => {
        const uint8ArrayNew = new Uint8Array(fileReader.result);
        console.log('recv bytes', uint8ArrayNew.length);
        vidqueue.push(uint8ArrayNew);
        if (vidsourceBuffer != null) {
            viddrain();
        }
    };
    fileReader.readAsArrayBuffer(event.data);
}

function vidrespawn(e) {
    console.log(e);
    store.dispatch(actions.ws.streamStatus('offline'));
    if (vidsocket != null) {
        vidsocket.close();
    }
    if (vidtimeout != null) {
        clearTimeout(vidtimeout);
    }
    vidtimeout = setTimeout(vidreconnect, 1000);
}

function vidreconnect() {
    if (!history.location.pathname.startsWith('/live/')) {
        return;
    }

    const path = history.location.pathname.replace('/live/', '');
    const wsvid = document.querySelector('#wsvid');
    if (wsvid == null) {
        return
    }

    vidsocket = new WebSocket((window.location.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + window.location.host + '/video/' + path + '.wss');
    vidsocket.onopen = () => {
        store.dispatch(actions.ws.streamStatus('online'));
        const ms = new MediaSource();
        ms.onsourceopen = () => {
            vidsourceBuffer = ms.addSourceBuffer('video/mp4; codecs="avc1.42E01E,mp4a.40.2"');
            vidsourceBuffer.onupdate = viddrain;
            viddrain();
        };
        wsvid.src = window.URL.createObjectURL(ms);
    };
    vidsocket.onerror = vidrespawn;
    vidsocket.onclose = vidrespawn;
    vidsocket.onmessage = vidonmessage;
}

function* locationHandler({payload: e}) {
    if (e.pathname.startsWith('/live/') || e.pathname.startsWith('/chat/')) {
        const path = e.pathname.replace(/^\/(live|chat)\//, '');
        const arr = path.split('/');
        const owner = arr[0];
        const stream = arr.length > 0 ? arr[1] : '';
        yield put(actions.ws.streamInforeq(owner, stream));
    } else {
        yield put(actions.ws.streamInforeq('', ''));
        yield put(actions.ws.streamInfo(null));
        yield put(actions.message.clear());
    }
}

export function* rootSaga() {
    yield take('WS/READY');
    yield put(actions.navigation.init());
    yield all([
        takeLatest('WS/DISCONNECT', wsDisconnect),
        takeLatest('WS/USER_SIGNUP', wsUserSignup),
        takeLatest('WS/USER_LOGIN', wsUserLogin),
        takeLatest('WS/USER_LOGOUT', wsUserLogout),
        takeLatest('WS/USER_INFO_UPDATE', wsUserInfoUpdate),
        takeLatest('WS/STREAM_INFO_UPDATE', wsStreamInfoUpdate),
        takeLatest('WS/STREAM_KEY_UPDATE', wsStreamKeyUpdate),
        takeLatest('WS/STREAM_PRIVATED_UPDATE', wsStreamPrivatedUpdate),
        takeLatest('WS/STREAM_DELETE', wsStreamDelete),
        takeLatest('WS/STREAM_ADD', wsStreamAdd),
        takeLatest('WS/STREAM_SUBSCRIBE', wsStreamSubscribe),
        takeLatest('WS/STREAM_UNSUBSCRIBE', wsStreamUnsubscribe),
        takeLatest('WS/STREAM_LISTREQ', wsStreamListreq),
        takeLatest('WS/STREAM_INFOREQ', wsStreamInforeq),
        takeLatest('WS/MESSAGE_SEND', wsMessageSend),
        takeLatest('WS/MESSAGE_EDIT', wsMessageEdit),
        takeLatest('WS/MESSAGE_DELETE', wsMessageDelete),
        takeLatest('WS/MESSAGE_HISTORY', wsMessageHistory),
        takeLatest('LOCATION', locationHandler),
        takeLatest('NAVIGATION/HOME', () => { history.push('/'); return wsStreamListreq()}),
        takeLatest('NAVIGATION/LOGIN', () => history.push('/login')),
        takeLatest('NAVIGATION/PROFILE', ({payload: {section}}) => history.push('/profile/' + section)),
        takeLatest('NAVIGATION/HOWTO', ({payload: {section}}) => history.push('/howto/' + section)),
        takeLatest('NAVIGATION/SIGNUP', () => history.push('/signup')),
        takeLatest('NAVIGATION/PLAY', ({payload: {path, stream}}) => history.push('/live/' + path + (stream.length === 0 ? '' : ('/' + stream)))),
    ]);
    yield put({type: 'LOCATION', payload: history.location});
}