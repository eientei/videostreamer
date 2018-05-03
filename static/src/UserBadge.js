import React, { Component } from 'react';

export default class UserBadge extends Component {
    render() {
        const {hash, size, text} = this.props;
        return (
            <img {...this.props} src={"https://www.gravatar.com/avatar/" + hash + "?d=identicon&s=" + size} alt={text}/>
        );
    }
}