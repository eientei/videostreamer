import React, { Component } from 'react';

import {styles} from './styles';
import {Button, SvgIcon, Typography, withStyles} from 'material-ui';
import {connect} from 'react-redux';
import {actions} from './actions';
import PlayerChat from './PlayerChat';

class Player extends Component {
    onprogress = () => {
        const wsvid = document.querySelector('#wsvid');
        if (wsvid.buffered.length === 0) {
            return
        }
        const current = wsvid.currentTime;
        const end = wsvid.buffered.end(wsvid.buffered.length - 1);
        const diff = end - current;
        if (diff > 1.000) {
            if (wsvid.paused) {
                wsvid.play();
                console.log('play');
            }
        } else if (diff < 0.500) {
            if (!wsvid.paused) {
                wsvid.pause();
                console.log('pause');
            }
        }
    };

    handleMove = (e) => {
        if (this.state.resizing) {
            const width = this.state.container.clientWidth - (e.clientX - this.state.handle.clientWidth / 2);
            this.setState({resizing: true, width});
        }
    };

    handleDown = (e) => {
        const container = document.querySelector('#playercontainer');
        const handle = document.querySelector('#handle');
        this.setState({resizing: true, container, handle});
        document.addEventListener('mouseup', this.handleUp);
        document.addEventListener('mousemove', this.handleMove);
    };

    handleUp = () => {
        this.setState({resizing: false, container: null});
        document.removeEventListener('mouseup', this.handleUp);
        document.removeEventListener('mousemove', this.handleMove);
    };

    handleHide = () => this.setState({chathidden: !this.state.chathidden});

    state = {
        width: 300,
        resizing: false,
        container: null,
        handle: null,
        chathidden: false,
    };

    render() {
        const {dispatch, user, ws, classes, location} = this.props;
        if (!ws.info || !ws.info.stream) {
            return null;
        }
        const stream = ws.info.stream;
        const container = document.querySelector('#playercontainer');
        const videowidth = (!this.state.chathidden && container) ? (container.clientWidth - this.state.width) + 'px' : '100%';
        return (
            <div className={classes.flexgrow + ' ' + classes.flexcolumn}>
                <div className={classes.relative + ' ' + classes.playerrow + ' ' + classes.flexgrow} id='playercontainer'>
                    <video id='wsvid' onProgress={this.onprogress} className={classes.maxwidth} style={{width: videowidth, backgroundRepeat: 'no-repeat', backgroundPosition: 'center', backgroundImage: ws.status === 'offline' ? stream.logourl : ''}} controls>
                    </video>
                    <div style={{cursor: 'ew-resize', userSelect: 'none', display: this.state.chathidden ? 'none' : 'flex'}} className={classes.flexrownopaddall} onMouseDown={this.handleDown} id='handle'>
                        <SvgIcon>
                            <path fill="#000000" d="M9,3H11V5H9V3M13,3H15V5H13V3M9,7H11V9H9V7M13,7H15V9H13V7M9,11H11V13H9V11M13,11H15V13H13V11M9,15H11V17H9V15M13,15H15V17H13V15M9,19H11V21H9V19M13,19H15V21H13V19Z" />
                        </SvgIcon>
                    </div>
                    <PlayerChat style={{width: this.state.width + 'px', display: this.state.chathidden ? 'none' : 'flex'}} location={location}/>
                </div>
                <div className={classes.flexrownopadd}>
                    {
                        user.notifications.find(n => n === stream.owner + (stream.name.length === 0 ? '' : ('/' + stream.name))) ?
                            <Button onClick={() => dispatch(actions.ws.streamUnsubscribe(stream.id))}>Unsubscribe</Button>
                            :
                            <Button onClick={() => Notification.requestPermission().then(perm => perm === 'granted' && dispatch(actions.ws.streamSubscribe(stream.id)))}>Subscribe</Button>
                    }
                    <div className={classes.flexgrow + ' ' + classes.flexrownopadd}>
                        <Button onClick={this.handleHide}>{this.state.chathidden ? 'Show' : 'Hide'} chat</Button>
                    </div>
                    <Typography className={classes.textright}>Currently watching: {stream.clients}</Typography>
                </div>
            </div>
        );
    }
}

export default connect(({user, ws}) => ({user, ws}))(withStyles(styles)(Player));