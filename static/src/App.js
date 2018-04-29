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
import Login from './Login';
import './App.css';

const theme = createMuiTheme({

});

const history = createHistory();


const DisconnectMessage  = 1;
const ErrorMessage       = 2;
const StatusMessage      = 3;
const SignupMessage      = 4;
const PublishedMessage   = 5;
const AuthMessage        = 6;
const UserDetailsMessage = 7;
const LogoutMessage      = 8;

export default class App extends Component {
    ws = null;
    t = null;
    apicomm = {
        signup: (username, email, password, passwordrepeat, captcha) => this.ws.send(JSON.stringify({type: SignupMessage, data: {username, email, password, passwordrepeat, captcha}})),
        auth: (username, password) => this.ws.send(JSON.stringify({type: AuthMessage, data: {username, password}})),
        logout: () => this.ws.send(JSON.stringify({type: LogoutMessage, data: {}})),
    };
    constructor(props) {
        super(props);

        this.ws = new WebSocket('wss://' + window.location.host + '/api/event');
        this.t = null;
        const auth = () => {
            const username = localStorage.getItem('username') || 'anonymous';
            const password = localStorage.getItem('password');
            this.apicomm.auth(username, password);
        };
        const mhandler = (event) => {
            const msg = JSON.parse(event.data);
            switch (msg.type) {
                default:
                    break;
                case ErrorMessage:
                    this.setState({error: msg.data.error});
                    break;
                case StatusMessage:
                    this.setState({status: msg.data.status});
                    break;
                case UserDetailsMessage:
                    if (msg.data.username === 'anonymous') {
                        localStorage.removeItem('username');
                        localStorage.removeItem('password');
                    }
                    if (msg.data.username === 'anonymous' && history.location.pathname === '/profile') {
                        history.push('/');
                    }
                    if (msg.data.username !== 'anonymous' && history.location.pathname === '/login') {
                        history.push('/');
                    }
                    this.setState({user: msg.data});
                    break
            }
        };
        const reconnect = () => {
            if (this.t != null) {
                clearTimeout(this.t);
            }
            console.log('reconnecting...');
            this.t = setTimeout(() => {
                this.ws = new WebSocket('wss://' + window.location.host + '/api/event');
                this.ws.error = reconnect;
                this.ws.onclose = reconnect;
                this.ws.onopen = auth;
                this.ws.onmessage = mhandler;
            }, 1000);
        };

        this.ws.error = reconnect;
        this.ws.onclose = reconnect;
        this.ws.onopen = auth;
        this.ws.onmessage = mhandler;
    }

    state = {
        error: null,
        status: null,
        login: null,
        user: null,
    };

    render() {
        if (this.state.user == null) {
            return null;
        }
        return (
            <MuiThemeProvider theme={theme}>
                <Router history={history}>
                    <div style={{display: 'flex', flexDirection: 'column', height: '100%'}}>
                        <CssBaseline/>
                        <Header api={this.apicomm} updater={this.updater} user={this.state.user}/>
                        <Switch>
                            <Route exact path='/' component={props => <Main {...props} api={this.apicomm} error={this.state.error} status={this.state.status}/>}/>
                            {this.state.user.username !== 'anonymous' ?
                                <Route path='/profile' component={props => <Profile {...props} api={this.apicomm} error={this.state.error} status={this.state.status} user={this.state.user}/>}/>
                                :
                                <Route path='/login' component={props => <Login {...props} api={this.apicomm} error={this.state.error} status={this.state.status}/>}/>
                            }
                            <Route path='/howto' component={props => <Howto {...props} api={this.apicomm} error={this.state.error} status={this.state.status}/>}/>
                            <Route path='/signup' component={props => <Signup {...props} api={this.apicomm} error={this.state.error} status={this.state.status}/>}/>
                            <Route path='/:path/:name' component={props => <Player {...props} api={this.apicomm} error={this.state.error} status={this.state.status}/>}/>
                            <Route component={NotFound}/>
                        </Switch>
                    </div>
                </Router>
            </MuiThemeProvider>
        );
    }
}