import React, { Component } from 'react';
import {Button, FormControl, FormHelperText, Grid, Paper, SvgIcon, TextField, withStyles} from 'material-ui';
import {connect} from 'react-redux';
import {actions} from './actions';
import {styles} from './styles/index';

import sha1 from 'sha1';

class Login extends Component {
    render() {
        const {dispatch, login, ws, classes} = this.props;
        return (
            <Grid container className={classes.container}>
                <Grid item xs={6} className={classes.item}>
                    <Paper className={classes.paper}>
                        <form>
                            <FormControl fullWidth error>
                                <FormHelperText>{login.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={login.username.error !== ''}>
                                <TextField disabled={ws.inflight} value={login.username.value} id='username' label='Username' autoComplete='username' onChange={(v) => dispatch(actions.login.usernameUpdate(v.target.value))}/>
                                <FormHelperText>{login.username.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={login.password.error !== ''}>
                                <TextField disabled={ws.inflight} value={login.password.value} id='password' label='Password' type='password' autoComplete='current-password' onChange={(v) => dispatch(actions.login.passwordUpdate(v.target.value))}/>
                                <FormHelperText>{login.password.error}</FormHelperText>
                            </FormControl>
                            <div className={classes.sendcontainer}>
                                <Button disabled={
                                    login.username.error !== '' ||
                                    login.password.error !== '' ||
                                    login.username.value === '' ||
                                    login.password.value === ''
                                } className={ws.inflight ? classes.hidden : ''} onClick={() => {dispatch(actions.ws.userLogin(login.username.value, sha1(login.password.value)));dispatch(actions.login.send())}}>Send</Button>
                                <SvgIcon className={(ws.inflight ? '' : (classes.hidden + ' ')) + classes.spinner}>
                                    <path fill="#000000" d="M12,4V2A10,10 0 0,0 2,12H4A8,8 0 0,1 12,4Z" />
                                </SvgIcon>
                            </div>
                        </form>
                    </Paper>
                </Grid>
            </Grid>
        );
    }
}

export default connect(({login, ws}) => ({login, ws}))(withStyles(styles)(Login));