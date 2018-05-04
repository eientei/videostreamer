import React, { Component } from 'react';
import {Button, CssBaseline, Tab, Tabs, Toolbar, Typography, withStyles} from 'material-ui';
import {Route, Switch} from 'react-router-dom';
import {connect} from 'react-redux';
import {actions} from './actions';
import {styles} from './styles/index';

import {history} from './App';

import UserBadge from './UserBadge';
import Login from './Login';
import Profile from './Profile';
import Howto from './Howto';
import Signup from './Signup';
import Player from './Player';
import NotFound from './NotFound';
import StreamList from './StreamList';
import PlayerChat from './PlayerChat';

class Main extends Component {
    render() {
        const {dispatch, user, navigation, ws, classes} = this.props;
        return (
            <div className={classes.root}>
                <CssBaseline/>
                <Toolbar style={{background: '#fff'}}>
                    <Tabs value={navigation.tab}>
                        <Tab label='Home' onClick={() => dispatch(actions.navigation.home())}/>
                        {user.name === 'anonymous' ?
                            <Tab label='Login' onClick={() => dispatch(actions.navigation.login())}/>
                        :
                            <Tab label='Profile' onClick={() => dispatch(actions.navigation.profile('stream'))}/>
                        }
                        <Tab label='Howto' onClick={() => dispatch(actions.navigation.howto('obs'))}/>
                    </Tabs>
                    <Typography className={classes.flexgrow + ' ' + classes.textcenter}>
                        {(history.location.pathname.startsWith('/live/') || history.location.pathname.startsWith('/chat/')) && navigation.path !== '/notfound' &&
                        <span>{ws.info && ws.info.stream && ws.info.stream.title && ws.info.stream.title.length > 0 ? ws.info.stream.title : ''}<em>{!ws.info || !ws.info.stream || !ws.info.stream.title || ws.info.stream.title.length === 0 ? '(No title)' : ''}</em></span>
                        }
                    </Typography>
                    <div className={classes.flexspace}>
                        {user.name === 'anonymous' ?
                            <Button color='inherit' onClick={() => dispatch(actions.navigation.signup())}>
                                Signup
                            </Button>
                            :
                            <Button color='inherit' onClick={() => {dispatch(actions.navigation.logout()); dispatch(actions.ws.userLogout())}}>
                                Logout
                            </Button>
                        }
                        <UserBadge size={48} hash={user.gravatar} text={user.name}/>
                    </div>
                </Toolbar>
                {navigation.path === '/notfound' && <NotFound/>}
                {navigation.path !== '/notfound' &&
                <Switch>
                    {user.name === 'anonymous' ?
                        <Route path='/login' component={Login}/>
                        :
                        <Route path='/profile' component={Profile}/>
                    }
                    <Route path='/howto' component={Howto}/>
                    {user.name === 'anonymous' && <Route path='/signup' component={Signup}/>}
                    <Route path='/live' component={Player}/>
                    <Route path='/chat' component={PlayerChat}/>
                    <Route exact path='/' component={StreamList}/>
                    <Route component={NotFound}/>
                </Switch>
                }
            </div>
        );
    }
}

export default connect(({user, navigation, ws}) => ({user, navigation, ws}))(withStyles(styles)(Main));