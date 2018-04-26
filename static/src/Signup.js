import React, { Component } from 'react';
import {Button, FormControl, FormHelperText, Grid, Paper, SvgIcon, TextField} from 'material-ui';
import Recaptcha from 'react-recaptcha';

export default class Signup extends Component {
    recaptcha = null;

    verify = (e) => {
        const data = Object.assign({}, this.state.values, {captcha: e});
        fetch('/api/signup', {
            body: JSON.stringify(data),
            method: 'POST',
        })
            .then(response => {
                this.setState({sendclicked: false});
                console.log(response.json());
            })
    };

    send = () => {
        const error = {};
        let err = false;
        for (const v in this.state.values) {
            if (!this.state.values.hasOwnProperty(v)) {
                continue;
            }
            if (this.state.values[v].length < 3) {
                err = true;
                error[v] = 'Must be longer than 3 symbols'
            } else if (this.state.values[v].length > 64) {
                err = true;
                error[v] = 'Must be shorter than 64 symbols';
            } else {
                error[v] = null;
            }
        }

        if (this.state.values.password !== this.state.values.passwordrepeat) {
            err = true;
            error.passwordrepeat = 'Must match the password';
        }
        this.setState({error});
        if (err) {
            return;
        }
        this.recaptcha.execute();
        this.setState({sendclicked: true});
    };

    onChange = (n) => (v) => this.setState({values: {...this.state.values, [n]: v.target.value}});

    state = {
        sendclicked: false,
        values: {
            username: '',
            email: '',
            password: '',
            passwordrepeat: '',
        },
        error: {
            username: null,
            email: null,
            password: null,
            passwordrepeat: null,
        }
    };

    render() {
        return (
            <Grid container style={{justifyContent: 'center', flexGrow: 1}}>
                <Grid item xs={4} style={{display: 'flex', justifyContent: 'center', flexDirection: 'column'}}>
                    <Paper style={{padding: '1em', textAlign: 'center', marginTop: '-64px'}}>
                        <form>
                            <FormControl fullWidth error={this.state.error.username != null}>
                                <TextField disabled={this.state.sendclicked} id='username' label='Username' autoComplete='username' style={{marginBottom: '1em', marginTop: '1em'}} onChange={this.onChange('username')}/>
                                <FormHelperText>{this.state.error.username}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={this.state.error.email != null}>
                                <TextField disabled={this.state.sendclicked} id='email' label='E-Mail' autoComplete='email' style={{marginBottom: '1em'}} onChange={this.onChange('email')}/>
                                <FormHelperText>{this.state.error.email}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={this.state.error.password != null}>
                                <TextField disabled={this.state.sendclicked} id='password' label='Password' type='password' autoComplete='new-password' style={{marginBottom: '1em'}} onChange={this.onChange('password')}/>
                                <FormHelperText>{this.state.error.password}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={this.state.error.passwordrepeat != null}>
                                <TextField disabled={this.state.sendclicked} id='passwordrepeat' label='Repeat password' type='password' autoComplete='new-password' style={{marginBottom: '1em'}} onChange={this.onChange('passwordrepeat')}/>
                                <FormHelperText>{this.state.error.passwordrepeat}</FormHelperText>
                            </FormControl>
                            <Recaptcha ref={e => this.recaptcha = e} sitekey="6LcJhVUUAAAAAIpZUh3yy5h_ZvAFPlpdqKotGLtr" size="invisible" render="explicit" verifyCallback={this.verify}/>
                            <div style={{marginTop: '1em', textAlign: 'center'}}>
                                <Button style={{display: this.state.sendclicked ? 'none' : 'block', width: '100%'}} onClick={this.send}>Send</Button>
                                <SvgIcon style={{display: this.state.sendclicked ? 'inline-block' : 'none', marginTop: '8px', height: '24px'}} className='spinner'>
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