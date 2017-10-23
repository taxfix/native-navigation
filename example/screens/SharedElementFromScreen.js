import React, {
  Component,
} from 'react';
import Navigator from '../../index';

import ImageRow from '../components/ImageRow';
import Screen from '../components/Screen';

export default class SharedElementFromScreen extends Component {
  render() {
    return (
      <Screen>
        {Array.from({ length: 8 }).map((_, id) => (
          <Navigator.SharedElementGroup
            key={id} // eslint-disable-line react/no-array-index-key
            id={id}
            style={{ overflow: 'hidden' }}
          >
            <ImageRow
              id={id}
              onPress={() => Navigator.push('SharedElementToScreen', { id }, {
                transitionGroup: `${id}`,
              })}
            />
          </Navigator.SharedElementGroup>
        ))}
      </Screen>
    );
  }
}
