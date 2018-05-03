import React, { Component } from 'react';

export default class FfmpegHowto extends Component {
    render() {
        return (
            <div>
                <p>
                    Simply direct your h264/264 with <strong>baseline</strong> profile, <strong>no</strong> h264-tuning options and <strong>44100</strong> sound sampling frequency stream in <strong>flv</strong> container to the streaming endpoint, one like <strong>{'rtmp://' + window.location.hostname + '/yukkuri/11eae5e89644a5f177c655412a49cc13e1f6efdf'}</strong>, only for your desired stream.
                </p>
            </div>
        );
    }
}