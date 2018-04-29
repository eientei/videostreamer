import React, { Component } from 'react';

export default class Badge extends Component {
    render() {
        return (
            <img src={"https://www.gravatar.com/avatar/" + this.props.hash + "?d=identicon&s=" + this.props.size} alt={this.props.text} style={this.props.style}/>
        );
    }
}