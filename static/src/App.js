import React, { Component } from 'react';

import CssBaseline from 'material-ui/CssBaseline';
import { MuiThemeProvider, createMuiTheme } from 'material-ui/styles';
import {Router, Route, Switch} from 'react-router-dom';
import createHistory from "history/createBrowserHistory"

import Header from './Header';
import Main from './Main';
import Profile from './Profile';
import Howto from './Howto';
import Signup from './Signup';
import Player from './Player';
import NotFound from './NotFound';

const theme = createMuiTheme({

});

const history = createHistory();

export default class App extends Component {
    render() {
        return (
            <div>
                <CssBaseline />
                <MuiThemeProvider theme={theme}>
                    <Router history={history}>
                        <div>
                            <Header/>
                            <Switch>
                                <Route exact path='/' component={Main}/>
                                <Route path='/profile' component={Profile}/>
                                <Route path='/howto' component={Howto}/>
                                <Route path='/signup' component={Signup}/>
                                <Route path='/live/:name' component={Player}/>
                                <Route component={NotFound}/>
                            </Switch>
                        </div>
                    </Router>
                </MuiThemeProvider>
            </div>
        );
    }
}