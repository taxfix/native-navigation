import React from 'react';
import PropTypes from 'prop-types';
import {
  View,
} from 'react-native';
import SafeModule from 'react-native-safe-module';
import { processConfig } from './utils';

const NativeTab = SafeModule.component({
  viewName: 'NativeNavigationTabView',
  mockComponent: () => <View />,
});

class Tab extends React.Component {
  static defaultProps = {
    props: {},
  }

  render() {
    const { route, props, ...config } = this.props;
    return (
      <NativeTab
        route={route}
        props={props}
        config={processConfig(config)}
      />
    );
  }
}

Tab.propTypes = {
  route: PropTypes.string.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  props: PropTypes.any,
};

module.exports = Tab;
