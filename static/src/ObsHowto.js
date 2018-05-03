import React, { Component } from 'react';

export default class ObsHowto extends Component {
    render() {
        return (
            <div>
                <p>
                    URL like <strong>{'rtmp://' + window.location.hostname + '/yukkuri/11eae5e89644a5f177c655412a49cc13e1f6efdf'}</strong> should be broke up into constant for current user part (<strong>{'rtmp://' + window.location.hostname + '/yukkuri'}</strong>) and specific for desired stream part (<strong>11eae5e89644a5f177c655412a49cc13e1f6efdf</strong>), also known as stream key
                </p>
                <img src="/url-obs.png" alt='OBS URL configuration screenshot'/>
                <p>
                    Then some configurations are to be made for stream to be compatible with chromium-like browsers, namely <strong>baseline</strong> profile must be selected and <strong>no</strong> tuning options must be enabled.
                </p>
                <img src="/reference-obs.png" alt='OBS stream configuration screenshot'/>
            </div>
        );
    }
}