/*
 * Copyright (c) 2015, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

'use strict';

var React = require('react-native');
var {
    AppRegistry,
    StyleSheet,
    Text,
    View,
    ListView,
    PixelRatio,
    Navigator
} = React;
var forceClient = require('./react.force.net.js');

var App = React.createClass({
    render: function() {
        return (<Navigator
                  style={styles.container}
                  initialRoute={{name: 'Mobile SDK Sample App', index: 0}}
                  renderScene={(route, navigator) => (<UserList/>)}
                  navigationBar={
                          <Navigator.NavigationBar
                            routeMapper={NavigationBarRouteMapper}
                          />
                  }
                />);
    }
});

var NavigationBarRouteMapper = {

  LeftButton: function(route, navigator, index, navState) {
      return null;
  },

  RightButton: function(route, navigator, index, navState) {
      return null;
  },

  Title: function(route, navigator, index, navState) {
      return (
              <View style={styles.navBar}>
                <Text style={styles.navBarText}>
                  {route.name}
                </Text>
              </View>
      );
  },
};


var UserList = React.createClass({
    getInitialState: function() {
      var ds = new ListView.DataSource({rowHasChanged: (r1, r2) => r1 !== r2});
      return {
          dataSource: ds.cloneWithRows([]),
      };
    },
    
    componentDidMount: function() {
        var that = this;
        var soql = 'SELECT Id, Name FROM User LIMIT 10';
        forceClient.query(soql,
                          function(response) {
                              var users = response.records;
                              var data = [];
                              for (var i in users) {
                                  data.push(users[i]["Name"]);
                              }

                              that.setState({
                                  dataSource: that.getDataSource(data),
                              });

                          });
    },

    getDataSource: function(users: Array<any>): ListViewDataSource {
        return this.state.dataSource.cloneWithRows(users);
    },

    render: function() {
        return (
            <ListView style={styles.scene}
              dataSource={this.state.dataSource}
              renderRow={this.renderRow} />
      );
    },

    renderRow: function(rowData: Object) {
        return (
                <View>
                    <View style={styles.row}>
                      <Text numberOfLines={1}>
                       {rowData}
                      </Text>
                    </View>
                    <View style={styles.cellBorder} />
                </View>
        );
    }
});

var styles = StyleSheet.create({
    container: {
        backgroundColor: 'white',
    },
    navBar: {
        height: 50,
        flexDirection: 'row',
        alignItems: 'center',
    },
    navBarText: {
        fontSize: 18,
    },
    scene: {
        flex: 1,
        paddingTop: 50,
    },
    row: {
        flex: 1,
        alignItems: 'center',
        backgroundColor: 'white',
        flexDirection: 'row',
        padding: 12,
    },
    cellBorder: {
        backgroundColor: 'rgba(0, 0, 0, 0.1)',
        // Trick to get the thinest line the device can display
        height: 1 / PixelRatio.get(),
        marginLeft: 4,
    },
});


AppRegistry.registerComponent('ReactNativeTemplateApp', () => App);

