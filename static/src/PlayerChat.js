import React, { Component } from 'react';
import {Button, Divider, TextField, withStyles} from 'material-ui';
import {styles} from './styles';
import {Link} from 'react-router-dom';
import {connect} from 'react-redux';
import {actions} from './actions';
import UserBadge from './UserBadge';

class PlayerChat extends Component {
    toTime = (time) => {
        const t = new Date(0);
        t.setUTCSeconds(time);
        return t.toString();
    };

    state = {
        text: '',
        serial: 0,
        style: {},
    };

    was = -1;

    shouldComponentUpdate(nextProps, nextState) {
        if (nextProps.message.serial !== this.state.serial) {
            const messages = document.querySelector('#messages');
            setTimeout(() => {
                let height = 0;
                messages.childNodes.forEach(c => height += c.clientHeight);
                console.log(messages.scrollTop, height - messages.clientHeight - 100);
                if (messages.scrollTop >= height - messages.clientHeight - 100 || this.was === -1) {
                    this.was = 0;
                    messages.scrollTop = height;
                }
            }, 100);
        }
        return nextProps.message.serial !== this.state.serial || nextProps.message.text.value !== this.state.text || nextProps.style !== this.state.style;
    }

    componentDidUpdate() {
        this.setState({serial: this.props.message.serial, text: this.props.message.text.value, style: this.props.style});
    }

    render() {
        const {dispatch, ws, message, classes, location} = this.props;
        if (!ws.info || !ws.info.stream) {
            return null;
        }

        const keys = Object.keys(message.messages).map((e) => parseInt(e, 10)).sort((a,b) => a - b);
        const first = keys.length > 0 ? keys[0] : 0;
        const stream = ws.info.stream;
        return (
            <div style={this.props.style || {}} className={classes.flexcolumn + ' ' + classes.flexgrow}>
                <div className={classes.flexgrow + ' ' + classes.flexcolumn} style={{overflowY: 'auto', height: 0, paddingBottom: '1em'}} id='messages'>
                    <Button disabled={first <= stream.oldest} onClick={() => dispatch(actions.ws.messageHistory(stream.id, first))}>Before</Button>
                    {
                        keys.map(k =>
                            <div key={k} style={{clear: 'both'}}>
                                <div className={classes.textright} style={{fontSize: 'small', paddingRight: '1em'}}>{this.toTime(message.messages[k].posted)} <em>{message.messages[k].edited ? '(Edited)' : ''}</em> >>{message.messages[k].id}</div>
                                <div>
                                    <UserBadge style={{float: 'left'}} hash={message.messages[k].gravatar} size={32} text={message.messages[k].hash}/>
                                    <span style={{margin: '0.5em', wordWrap: 'break-word'}}>{message.messages[k].text}</span>
                                </div>
                            </div>
                        )
                    }
                </div>
                <Divider/>
                <TextField multiline={true} value={message.text.value} fullWidth={true} rows={3} onChange={(v) => dispatch(actions.message.textUpdate(v.target.value))} onKeyPress={(e) => {if (e.key === 'Enter'){ this.was = -1; e.preventDefault(); dispatch(actions.ws.messageSend(stream.id, message.text.value)); dispatch(actions.message.textUpdate(''))}}}/>
                <div>
                    <Link to={(location.pathname.startsWith('/live/') ? '/chat/' : '/live/') + stream.owner + (stream.name.length === 0 ? '' : ('/' + stream.name))}>Direct link</Link>
                </div>
            </div>
        );
    }
}

export default connect(({user, ws, message}) => ({user, ws, message}))(withStyles(styles)(PlayerChat));