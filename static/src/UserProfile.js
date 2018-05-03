import React, { Component } from 'react';
import {Button, FormControl, FormHelperText, SvgIcon, TextField, withStyles} from 'material-ui';
import {actions} from './actions';

import sha1 from 'sha1';
import {styles} from './styles';
import {connect} from 'react-redux';

class UserProfile extends Component {
    render() {
        const {dispatch, profile, ws, classes} = this.props;
        return (
            <form>
                <FormControl fullWidth error>
                    <FormHelperText>{profile.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={profile.email.error !== ''}>
                    <TextField disabled={ws.inflight} value={profile.email.value} id='email' label='Email' onChange={(v) => dispatch(actions.profile.emailUpdate(v.target.value))}/>
                    <FormHelperText>{profile.email.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={profile.password.error !== ''}>
                    <TextField disabled={ws.inflight} value={profile.password.value} id='password' label='Password' type='password' autoComplete='new-password' onChange={(v) => dispatch(actions.profile.passwordUpdate(v.target.value))}/>
                    <FormHelperText>{profile.password.error}</FormHelperText>
                </FormControl>
                <FormControl fullWidth error={profile.passwordrepeat.error !== ''}>
                    <TextField disabled={ws.inflight} value={profile.passwordrepeat.value} id='passwordrepeat' label='Repeat password' type='password' autoComplete='new-password' onChange={(v) => dispatch(actions.profile.passwordrepeatUpdate(v.target.value))}/>
                    <FormHelperText>{profile.passwordrepeat.error}</FormHelperText>
                </FormControl>
                <div className={classes.sendcontainer}>
                    <Button disabled={
                        profile.email.error !== '' ||
                        profile.password.error !== '' ||
                        profile.passwordrepeat.error !== ''
                    } className={ws.inflight ? classes.hidden : ''} onClick={() => {
                        dispatch(actions.ws.userInfoUpdate(profile.email.value, profile.password.value.length > 0 ? sha1(profile.password.value) : ''));
                        dispatch(actions.profile.send());
                    }}>Send</Button>
                    <SvgIcon className={(ws.inflight ? '' : (classes.hidden + ' ')) + classes.spinner}>
                        <path fill="#000000" d="M12,4V2A10,10 0 0,0 2,12H4A8,8 0 0,1 12,4Z" />
                    </SvgIcon>
                </div>
            </form>
        );
    }
}

export default connect(({profile, ws}) => ({profile, ws}))(withStyles(styles)(UserProfile));