import React, { useState,useEffect } from 'react';
import { View, Text, ScrollView } from 'react-native';
import PrayerTimesForm from './src/PrayerTimesForm';
import notifee, { EventType } from '@notifee/react-native';

const App = () => {
  const [prayerTimes, setPrayerTimes] = useState(null);

  const handlePrayerTimesFetched = (timings) => {
    setPrayerTimes(timings);
  };

  useEffect(() => {
    // Register background event handler
    notifee.onBackgroundEvent(async (event) => {
      switch (event.type) {
        case EventType.ACTION_PRESS:
          console.log('Action pressed:', event.detail);
          break;
        case EventType.DISMISSED:
          console.log('Notification dismissed:', event.detail);
          break;
      }
    });
  }, []);
  return (
    <View style={{flex:1}}>
      <PrayerTimesForm onPrayerTimesFetched={handlePrayerTimesFetched} />
      {prayerTimes && (
        <View style={{ marginTop: 20 }}>
          <Text>Prayer Times:</Text>
          <Text>Fajr: {prayerTimes.Fajr}</Text>
          <Text>Dhuhr: {prayerTimes.Dhuhr}</Text>
          <Text>Asr: {prayerTimes.Asr}</Text>
          <Text>Maghrib: {prayerTimes.Maghrib}</Text>
          <Text>Isha: {prayerTimes.Isha}</Text>
        </View>
      )}
    </View>
  );
};

export default App;
