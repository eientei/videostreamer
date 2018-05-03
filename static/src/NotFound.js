import React, { Component } from 'react';
import {Typography, withStyles} from 'material-ui';
import {styles} from './styles';

class NotFound extends Component {
    render() {
        const {classes} = this.props;
        return (
            <Typography className={classes.textcenter + ' ' + classes.maxwidth} variant='headline'>Not found</Typography>
        );
    }
}

export default withStyles(styles)(NotFound);