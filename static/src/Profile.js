import React, { Component } from 'react';
import {withStyles} from 'material-ui/styles'
import {
    Button, Divider, Drawer, FormControl, FormHelperText, Grid, List, ListItem, ListItemIcon, ListItemText, Paper,
    SvgIcon, Tab,
    Tabs,
    TextField
} from 'material-ui';

const styles = {
    paper: {
        position: 'absolute',
        right: '100%',
        height: '100%',
        left: 'auto',
    }
};

class Profile extends Component {
    handleUserSelect = () => this.setState({tabSelect: 1});
    handleStreamSelect = (s) => () => this.setState({tabSelect: 0});
    handleStreamAdd = () => null;

    onChange = (n) => (v) => this.setState({values: {...this.state.values, [n]: v.target.value}});

    state = {
        tabSelect: 0,
        sendclicked: false,
        values: {
            email: this.props.user.email,
            password: '',
            passwordrepeat: '',
        },
        error: {
            email: null,
            password: null,
            passwordrepeat: null,
        }
    };

    render() {
        return (
            <Grid container style={{justifyContent: 'center', flexGrow: 1}}>
                <Grid item xs={4} style={{display: 'flex', justifyContent: 'start', flexDirection: 'column', flexGrow: 1, paddingBottom: '64px', paddingTop: '64px'}}>
                    <div style={{position: 'relative', height: '100%'}}>
                        <Drawer variant='permanent' classes={{paper: this.props.classes.paper}} >
                            <List component="nav">
                                <ListItem button onClick={this.handleUserSelect}>
                                    <ListItemText primary="User" />
                                </ListItem>
                            </List>
                            <Divider/>
                            <List component="nav">
                                <ListItem button onClick={this.handleStreamSelect('Abc')}>
                                    <ListItemText primary="yukkuri/Abc" />
                                </ListItem>
                                <ListItem button onClick={this.handleStreamSelect('Efg')}>
                                    <ListItemText primary="yukkuri/Efg" />
                                </ListItem>
                                <ListItem button onClick={this.handleStreamAdd}>
                                    <SvgIcon style={{width: '100%'}}>
                                        <path fill="#000000" d="M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M13,7H11V11H7V13H11V17H13V13H17V11H13V7Z" />
                                    </SvgIcon>
                                </ListItem>
                            </List>
                        </Drawer>
                        <Paper style={{padding: '1em', textAlign: 'center', height: '100%'}}>
                            <form>
                                <FormControl fullWidth error>
                                    <FormHelperText>{this.props.error}</FormHelperText>
                                </FormControl>
                                {this.state.tabSelect === 0 &&
                                <div>
                                    stream
                                </div>
                                }
                                {this.state.tabSelect === 1 &&
                                <div>
                                    <FormControl fullWidth error={this.state.error.email != null}>
                                        <TextField disabled={this.state.sendclicked} id='email' value={this.state.values.email} label='E-Mail' autoComplete='email' style={{marginBottom: '1em'}} onChange={this.onChange('email')}/>
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
                                </div>
                                }
                                <div style={{marginTop: '1em', textAlign: 'center'}}>
                                    <Button style={{display: this.state.sendclicked ? 'none' : 'block', width: '100%'}} onClick={this.send}>Send</Button>
                                    <SvgIcon style={{display: this.state.sendclicked ? 'inline-block' : 'none', marginTop: '8px', height: '24px'}} className='spinner'>
                                        <path fill="#000000" d="M12,4V2A10,10 0 0,0 2,12H4A8,8 0 0,1 12,4Z" />
                                    </SvgIcon>
                                </div>
                            </form>
                        </Paper>
                    </div>
                </Grid>
            </Grid>
        );
    }
}

export default withStyles(styles)(Profile);