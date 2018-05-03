import React, { Component } from 'react';
import {Route, Switch} from 'react-router-dom';
import UserProfile from './UserProfile';
import StreamProfile from './StreamProfile';
import {Divider, Drawer, Grid, List, ListItem, ListItemText, Paper, SvgIcon, withStyles} from 'material-ui';

import {styles} from './styles/index';
import {connect} from 'react-redux';
import {actions} from './actions';

class Profile extends Component {
    render() {
        const {dispatch, user, classes} = this.props;
        return (
            <Grid container className={classes.container}>
                <Grid item xs={8} className={classes.item + ' ' + classes.relative}>
                    <Drawer variant='permanent' classes={{paper: classes.absolutepaper}}>
                        <List component="nav">
                            <ListItem button onClick={() => {dispatch(actions.profile.set('')); dispatch(actions.navigation.profile('user'))}}>
                                <ListItemText primary="User" />
                            </ListItem>
                        </List>
                        <Divider/>
                        <List component="nav">
                            {
                                user.streams.map(stream => (
                                    <ListItem button key={stream.id} onClick={() => {dispatch(actions.stream.set('')); dispatch(actions.stream.select(stream)); dispatch(actions.navigation.profile('stream'))}}>
                                        <ListItemText primary={user.name + (stream.name.length > 0 ? ('/' + stream.name) : '')} />
                                    </ListItem>
                                ))
                            }
                            <ListItem button onClick={() => dispatch(actions.ws.streamAdd())}>
                                <SvgIcon style={{width: '100%'}}>
                                    <path fill="#000000" d="M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M13,7H11V11H7V13H11V17H13V13H17V11H13V7Z" />
                                </SvgIcon>
                            </ListItem>
                        </List>
                    </Drawer>
                    <Paper className={classes.paper}>
                        <Switch>
                            <Route path='/profile/user' component={UserProfile}/>
                            <Route path='/profile/stream' component={StreamProfile}/>
                        </Switch>
                    </Paper>
                </Grid>
            </Grid>
        );
    }
}

export default connect(({user}) => ({user}))(withStyles(styles)(Profile));