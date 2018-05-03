import React, { Component } from 'react';
import {styles} from './styles';
import {connect} from 'react-redux';
import {Button, Typography, withStyles} from 'material-ui';
import {actions} from './actions';
import UserBadge from './UserBadge';

class StreamList extends Component {
    render() {
        const {dispatch, user, ws, classes} = this.props;
        return (
            <div className={classes.padded}>
                {ws.streams.map(stream => (
                    <div key={stream.id} >
                        <div className={classes.flexrow + ' ' + classes.maxwidth}>
                            <UserBadge size={64} hash={stream.gravatar} alt={stream.owner + (stream.name.length === 0 ? '' : ('/' + stream.name))}/>
                            <div className={classes.flexgrow}>
                                <Typography className={classes.textcenter} variant='headline'>{stream.owner + (stream.name.length === 0 ? '' : ('/' + stream.name))} - {stream.title}</Typography>
                                <Button className={classes.maxwidth} onClick={() => dispatch(actions.navigation.play(stream.owner, stream.name))}>Watch</Button>
                            </div>
                            <Typography>Currently watching: {stream.clients}</Typography>
                            {
                                user.notifications.find(n => n === stream.owner + (stream.name.length === 0 ? '' : ('/' + stream.name))) ?
                                    <Button onClick={() => dispatch(actions.ws.streamUnsubscribe(stream.id))}>Unsubscribe</Button>
                                    :
                                    <Button onClick={() => Notification.requestPermission().then(perm => perm === 'granted' && dispatch(actions.ws.streamSubscribe(stream.id)))}>Subscribe</Button>
                            }
                        </div>
                        <div>
                            {stream.users.map(u => <UserBadge key={u} size={16} hash={u} alt={u}/>)}
                        </div>
                    </div>
                ))}
            </div>
        );
    }
}

export default connect(({user, ws}) => ({user, ws}))(withStyles(styles)(StreamList));