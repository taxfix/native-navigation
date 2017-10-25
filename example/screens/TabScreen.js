import React from 'react';
import { TabBar, Tab } from '../../index';

const propTypes = {};
const defaultProps = {};

export default class TabScreen extends React.Component {
  render() {
    return (
      <TabBar
        elevation={20}
      >
        <Tab
          route="ScreenOne"
          title="Home"
          image={require('../icons/home.png')}
          props={{}}
        />
        <Tab
          route="ScreenOne"
          title="Chat"
          image={require('../icons/chat.png')}
          props={{}}
        />
        <Tab
          route="ScreenOne"
          title="Data"
          image={require('../icons/backup.png')}
          props={{}}
        />
        <Tab
          route="ScreenOne"
          title="Settings"
          image={require('../icons/settings.png')}
          props={{}}
        />
      </TabBar>
    );
  }
}

TabScreen.defaultProps = defaultProps;
TabScreen.propTypes = propTypes;
