import React, { Component } from 'react';
import {connect} from 'react-redux';
import {styles} from './styles/index';
import {Button, FormControl, FormHelperText, Grid, Paper, SvgIcon, TextField, withStyles} from 'material-ui';
import Recaptcha from 'react-recaptcha';
import {actions} from './actions';

import sha1 from 'sha1';

class Signup extends Component {
    verify = (captcha) => {
        const {dispatch, signup} = this.props;
        dispatch(actions.ws.userSignup(signup.username.value, sha1(signup.password.value), signup.email.value, captcha));
        dispatch(actions.signup.send());
    };

    recaptcha = null;

    render() {
        const {dispatch, signup, ws, classes} = this.props;
        return (
            <Grid container className={classes.container}>
                <Grid item xs={6} className={classes.item}>
                    <Paper className={classes.paper}>
                        <form>
                            <FormControl fullWidth error>
                                <FormHelperText>{signup.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={signup.username.error !== ''}>
                                <TextField disabled={ws.inflight} value={signup.username.value} id='username' label='Username' autoComplete='username' onChange={(v) => dispatch(actions.signup.usernameUpdate(v.target.value))}/>
                                <FormHelperText>{signup.username.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={signup.email.error !== ''}>
                                <TextField disabled={ws.inflight} value={signup.email.value} id='email' label='Email' autoComplete='email' onChange={(v) => dispatch(actions.signup.emailUpdate(v.target.value))}/>
                                <FormHelperText>{signup.email.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={signup.password.error !== ''}>
                                <TextField disabled={ws.inflight} value={signup.password.value} id='password' label='Password' type='password' autoComplete='new-password' onChange={(v) => dispatch(actions.signup.passwordUpdate(v.target.value))}/>
                                <FormHelperText>{signup.password.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={signup.passwordrepeat.error !== ''}>
                                <TextField disabled={ws.inflight} value={signup.passwordrepeat.value} id='passwordrepeat' label='Repeat password' type='password' autoComplete='new-password' onChange={(v) => dispatch(actions.signup.passwordrepeatUpdate(v.target.value))}/>
                                <FormHelperText>{signup.passwordrepeat.error}</FormHelperText>
                            </FormControl>
                            <Recaptcha ref={e => this.recaptcha = e} sitekey="6LcJhVUUAAAAAIpZUh3yy5h_ZvAFPlpdqKotGLtr" size="invisible" render="explicit" verifyCallback={this.verify}/>
                            <div className={classes.sendcontainer}>
                                <Button disabled={
                                    signup.username.error !== '' ||
                                    signup.email.error !== '' ||
                                    signup.password.error !== '' ||
                                    signup.passwordrepeat.error !== '' ||
                                    signup.username.value === '' ||
                                    signup.email.value === '' ||
                                    signup.password.value === '' ||
                                    signup.passwordrepeat.value === ''
                                } className={ws.inflight ? classes.hidden : ''} onClick={() => this.recaptcha.execute()}>Send</Button>
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
export default connect(({signup, ws}) => ({signup, ws}))(withStyles(styles)(Signup));