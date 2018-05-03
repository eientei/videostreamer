import React, { Component } from 'react';
import {styles} from './styles';
import {connect} from 'react-redux';
import {Button, FormControl, FormHelperText, SvgIcon, TextField, withStyles} from 'material-ui';
import {actions} from './actions';

class ABody extends Component {
    render() {
        const {href} = this.props;
        return (<a href={href}>{href}</a>);
    }
}

class StreamProfile extends Component {
    render() {
        const {dispatch, user, stream, ws, classes} = this.props;
        return (
            <form>
                <Button disabled={user.streams.length === 1} className={classes.floatright} onClick={() => dispatch(actions.ws.streamDelete(stream.selected))}>Delete</Button>
                <FormControl fullWidth error>
                    <FormHelperText>{stream.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={stream.name.error !== ''}>
                    <TextField disabled={ws.inflight} value={stream.name.value} id='name' label='Name' onChange={(v) => dispatch(actions.stream.nameUpdate(v.target.value))}/>
                    <FormHelperText>{stream.name.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={stream.title.error !== ''}>
                    <TextField disabled={ws.inflight} value={stream.title.value} id='title' label='Title' onChange={(v) => dispatch(actions.stream.titleUpdate(v.target.value))}/>
                    <FormHelperText>{stream.title.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={stream.logourl.error !== ''}>
                    <TextField disabled={ws.inflight} value={stream.logourl.value} id='logourl' label='Logo URL' onChange={(v) => dispatch(actions.stream.logourlUpdate(v.target.value))}/>
                    <FormHelperText>{stream.logourl.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth className={classes.flexrow}>
                    <TextField disabled={true} className={classes.flexgrow} value={stream.key} id='key' label='Key'/>
                    <Button onClick={() => dispatch(actions.ws.streamKeyUpdate(stream.selected))} disabled={ws.inflight} >
                        Generate new
                    </Button>
                </FormControl>
                <FormControl fullWidth className={classes.flexrow}>
                    {
                        stream.privated && (
                            <Button onClick={() => dispatch(actions.ws.streamPrivatedUpdate(stream.selected, false))} disabled={ws.inflight} >
                                Make public (currently private)
                            </Button>
                        )
                    }
                    {
                        !stream.privated && (
                            <Button onClick={() => dispatch(actions.ws.streamPrivatedUpdate(stream.selected, true))} disabled={ws.inflight} >
                                Make private (currently public)
                            </Button>
                        )
                    }
                </FormControl>
                <div className={classes.sendcontainer}>
                    <Button disabled={
                        stream.name.error !== '' ||
                        stream.title.error !== '' ||
                        stream.logourl.error !== ''
                    } className={ws.inflight ? classes.hidden : ''} onClick={() => {
                        dispatch(actions.ws.streamInfoUpdate(stream.selected, stream.name.value, stream.title.value, stream.logourl.value));
                        dispatch(actions.stream.send());
                    }}>Send</Button>
                    <SvgIcon className={(ws.inflight ? '' : (classes.hidden + ' ')) + classes.spinner}>
                        <path fill="#000000" d="M12,4V2A10,10 0 0,0 2,12H4A8,8 0 0,1 12,4Z" />
                    </SvgIcon>
                </div>
                <p>
                    Useful links:
                </p>
                <table>
                    <tbody>
                    <tr>
                        <td>RTMP publish url:</td>
                        <td><ABody href={'rtmp://' + window.location.hostname + '/' + user.name + '/' + stream.key}/></td>
                    </tr>
                    <tr>
                        <td>Player url:</td>
                        <td><ABody href={window.location.protocol + '//' + window.location.hostname + '/live/' + user.name + (stream.name.value.length === 0 ? '' : ('/' + stream.name.value))}/></td>
                    </tr>
                    <tr>
                        <td>MP4 file url:</td>
                        <td><ABody href={window.location.protocol + '//' + window.location.hostname + '/video/' + user.name + (stream.name.value.length === 0 ? '' : ('/' + stream.name.value)) + '.mp4'}/></td>
                    </tr>
                    <tr>
                        <td>WS file url:</td>
                        <td><ABody href={(window.location.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + window.location.hostname + '/video/' + user.name + (stream.name.value.length === 0 ? '' : ('/' + stream.name.value)) + '.wss'}/></td>
                    </tr>
                    </tbody>
                </table>
            </form>
        );
    }
}

export default connect(({user, stream, ws}) => ({user, stream, ws}))(withStyles(styles)(StreamProfile));