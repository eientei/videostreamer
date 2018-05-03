import React, { Component } from 'react';
import { createStore, applyMiddleware } from 'redux'
import createSagaMiddleware from 'redux-saga';
import {Provider} from 'react-redux';

import {rootReducer} from './reducers';
import {rootSaga} from './sagas';
import {Router} from 'react-router-dom';
import createHistory from 'history/createBrowserHistory';

import Main from './Main';

export const history = createHistory();

const sagaMiddleware = createSagaMiddleware();
export const store = createStore(
    rootReducer,
    applyMiddleware(sagaMiddleware)
);
sagaMiddleware.run(rootSaga);

history.listen((e) => store.dispatch({type: 'LOCATION', payload: e}));

export default class App extends Component {
    render() {
        return (
            <Provider store={store}>
                <Router history={history}>
                    <Main/>
                </Router>
            </Provider>
        );
    }
}