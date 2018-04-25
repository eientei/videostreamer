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
                return {tabSelect: 1};
            case "/howto":
                return {tabSelect: 2};
        }
    }

    state = {
        tabSelect: 0,
    };

    render() {
        const {tabSelect} = this.state;
        return (
            <Toolbar>
                <Tabs value={tabSelect} onChange={this.handleTabSelect} style={{flexGrow: 1}}>
                    <Tab label="Home" component={Link} to='/'/>
                    <Tab label="Profile" component={Link} to='/profile'/>
                    <Tab label="Howto" component={Link} to='/howto'/>
                </Tabs>
                <Badge size={32}/>
                <Button color='inherit' component={Link} to='/signup'>
                    Signup
                </Button>
            </Toolbar>
        );
    }
}

export default withRouter(Header);