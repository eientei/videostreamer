import React, { Component } from 'react';
import {Button, Tab, Tabs, Toolbar} from 'material-ui';
import {Link, withRouter} from 'react-router-dom';
import Badge from './Badge';

class Header extends Component {
    handleTabSelect = (_,v) => this.setState({tabSelect: v});

    static getDerivedStateFromProps(nextProps, prevState) {
        switch (nextProps.location.pathname) {
            default:
                return {tabSelect: 0};
            case "/profile":
            case "/login":
                return {tabSelect: 1};
            case "/howto":
                return {tabSelect: 2};
        }
    }

    logout = () => this.props.api.logout();

    state = {
        tabSelect: 0,
    };

    render() {
        const {tabSelect} = this.state;
        return (
            <Toolbar>
                <Tabs value={tabSelect} onChange={this.handleTabSelect} style={{flexGrow: 1}}>
                    <Tab label="Home" component={Link} to='/'/>
                    {this.props.user != null && this.props.user.username !== 'anonymous' ?
                        <Tab label="Profile" component={Link} to='/profile'/>
                        :
                        <Tab label="Login" component={Link} to='/login'/>
                    }
                    <Tab label="Howto" component={Link} to='/howto'/>
                </Tabs>
                {this.props.user.username !== 'anonymous' ?
                    <Button color='inherit' onClick={this.logout}>
                        Logout
                    </Button>
                    :
                    <Button color='inherit' component={Link} to='/signup'>
                        Signup
                    </Button>
                }
                <Badge size={48} hash={this.props.user.gravatar} text={this.props.user.username} style={{marginLeft: '1em'}}/>
            </Toolbar>
        );
    }
}

export default withRouter(Header);