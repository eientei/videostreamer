export const styles = {
    root: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'hidden',
    },
    flexgrow: {
        flexGrow: 1,
    },
    flexspace: {
        display: 'flex',
        justifyContent: 'space-between',
        flexGrow: 0.1,
    },
    container: {
        justifyContent: 'center',
        flexGrow: 1,
    },
    item: {
        display: 'flex',
        justifyContent: 'start',
        flexDirection: 'column',
    },
    paper: {
        padding: '1em',
        marginTop: '64px',
    },
    sendcontainer: {
        position: 'relative',
        width: '100%',
        height: '32px',
        '& > *': {
            width: '100%',
            left: 0,
        }
    },
    spinner: {
        animation: 'spin 1s linear infinite',
        position: 'absolute',
        height: '100%',
    },
    '@keyframes spin': {
        '100%': {
            transform: 'rotate(360deg)',
        },
    },
    hidden: {
        display: 'none',
    },
    relative: {
        position: 'relative',
    },
    absolutepaper: {
        position: 'absolute',
        right: '100%',
        left: 'auto',
        top: '64px',
        height: 'calc(100vh - 128px)',
    },
    flexrow: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        padding: '1em',
    },
    floatright: {
        float: 'right',
    },
    padded: {
        padding: '1em',
    },
    maxwidth: {
        width: '100%',
    },
    maxheight: {
        height: '100%',
    },
    textcenter: {
        textAlign: 'center',
    },
    rootrow: {
        display: 'flex',
        flexDirection: 'row',
        height: '100%',
    },
    flexcolumn: {
        display: 'flex',
        flexDirection: 'column',
    },
    textright: {
        textAlign: 'right',
    },
    flexrownopadd: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        paddingLeft: '1em',
        paddingRight: '1em',
        justifyContent: 'center',
    },
    flexrownopaddall: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
    },
    playerrow: {
        display: 'flex',
        flexDirection: 'row',
        height: 0,
    },
};