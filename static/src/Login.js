import React, { Component } from 'react';
import {Button, FormControl, FormHelperText, Grid, Paper, SvgIcon, TextField} from 'material-ui';
import sha1 from 'sha1';

export default class Login extends Component {
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
        this.setState({error});
        if (err) {
            return;
        }
        this.setState({sendclicked: true});
        localStorage.setItem('username', this.state.values.username);
        localStorage.setItem('password', sha1(this.state.values.password));
        this.props.api.auth(this.state.values.username, sha1(this.state.values.password));
    };

    onChange = (n) => (v) => this.setState({values: {...this.state.values, [n]: v.target.value}});

    state = {
        sendclicked: false,
        values: {
            username: '',
            password: '',
        },
        error: {
            username: null,
            password: null,
        }
    };

    render() {
        return (
            <Grid container style={{justifyContent: 'center', flexGrow: 1}}>
                <Grid item xs={4} style={{display: 'flex', justifyContent: 'start', flexDirection: 'column'}}>
                    <Paper style={{padding: '1em', textAlign: 'center', marginTop: '64px'}}>
                        <form>
                            <FormControl fullWidth error>
                                <FormHelperText>{this.props.error}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={this.state.error.username != null}>
                                <TextField disabled={this.state.sendclicked} id='username' label='Username' autoComplete='username' style={{marginBottom: '1em', marginTop: '1em'}} onChange={this.onChange('username')}/>
                                <FormHelperText>{this.state.error.username}</FormHelperText>
                            </FormControl>
                            <FormControl fullWidth error={this.state.error.password != null}>
                                <TextField disabled={this.state.sendclicked} id='password' label='Password' type='password' autoComplete='current-password' style={{marginBottom: '1em'}} onChange={this.onChange('password')}/>
                                <FormHelperText>{this.state.error.password}</FormHelperText>
                            </FormControl>
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