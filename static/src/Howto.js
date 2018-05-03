import React, { Component } from 'react';
import {Route, Switch} from 'react-router-dom';
import ObsHowto from './ObsHowto';
import FfmpegHowto from './FfmpegHowto';
import {Tab, Tabs} from 'material-ui';
import {actions} from './actions';
import {connect} from 'react-redux';

class Howto extends Component {
    state = {
        tab: (this.props.location.pathname.endsWith('/obs')) ? 0 : 1,
    };

    render() {
        const {dispatch} = this.props;
        return (
            <div style={{overflow: 'scroll', padding: '1em'}}>
                <Tabs value={this.state.tab}>
                    <Tab label='OBS' onClick={() => {this.setState({tab: 0}); dispatch(actions.navigation.howto('obs'))}}/>
                    <Tab label='FFMpeg' onClick={() => {this.setState({tab: 1}); dispatch(actions.navigation.howto('ffmpeg'))}}/>
                </Tabs>
                <Switch>
                    <Route path='/howto/obs' component={ObsHowto}/>
                    <Route path='/howto/ffmpeg' component={FfmpegHowto}/>
                </Switch>

                <p>P.S: the source for streaming service available at github: <a href="https://github.com/eientei/videostreamer">https://github.com/eientei/videostreamer</a></p>
            </div>
        );
    }
}

export default connect()(Howto);