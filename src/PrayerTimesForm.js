import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, PermissionsAndroid, Pressable, Alert } from 'react-native';
import axios from 'axios';
import LinearGradient from 'react-native-linear-gradient';
import DateTimePicker from '@react-native-community/datetimepicker';
import Geolocation from '@react-native-community/geolocation';
import { NativeModules } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Icon from 'react-native-vector-icons/MaterialIcons'; // Import Material Icons or any other icon set
import BackTime from 'react-native-vector-icons/Entypo';
import { Image } from 'react-native';
import Animated, { BounceInDown, BounceInUp, FadeInDown, FadeInRight, FadeInUp, FadeOutDown } from 'react-native-reanimated';
import { Platform } from 'react-native'; // Import Platform
import FlyingBirdsAnimation from './components/FlyingBirdsAnimation';
import { Switch } from 'react-native-paper';
import notifee, { AndroidImportance, TriggerType } from '@notifee/react-native';
import ToastAndroid from "react-native-root-toast";
import CheckInternet from './components/CheckInternet';

const { PrayerTimesModule, BroadcastSenderModule } = NativeModules;

const PrayerTimesForm = () => {
  const [currentLocation, setCurrentLocation] = useState('');
  const [timings, setTimings] = useState({
    Fajr: '',
    Dhuhr: '',
    Asr: '',
    Maghrib: '',
    Isha: '',
  });
  const [showPicker, setShowPicker] = useState(false);
  const [selectedPrayer, setSelectedPrayer] = useState(null);
  const [countries, setCountries] = useState([]);
  const [isSwitchOn, setIsSwitchOn] = React.useState(false);
  const [isConnected, setIsConnected] = useState(false)

  useEffect(() => {
    fetchCountries();
    requestLocationPermission();
    loadPrayerTimes(); // Load stored prayer times
    loadSwitchStates();
  }, []);

  const fetchCountries = async () => {
    const response = await axios.get('https://restcountries.com/v3.1/all');
    const countryList = response.data.map(country => ({
      name: country.name.common,
      code: country.cca2,
    }));
    setCountries(countryList);
  };

  const requestLocationPermission = async () => {
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        {
          title: 'Location Permission',
          message: 'This app needs access to your location to show prayer times.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        },
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        getCurrentPosition();
      } else {
        console.log('Location permission denied');
      }
    } else {
      getCurrentPosition();
    }
  };

  const getCurrentPosition = () => {
    Geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        getLocationName(latitude, longitude);
      },
      (error) => {
        console.error(error);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
      }
    );
  };

  const getLocationName = async (latitude, longitude) => {
    try {
      const apiKey = 'AIzaSyDZy9lBieXFt2KDcxhLub2QG-2XicbmSM0'; // Replace with your Google API Key
      const response = await axios.get('https://maps.googleapis.com/maps/api/geocode/json', {
        params: {
          latlng: `${latitude},${longitude}`,
          key: apiKey,
        },
      });

      const addressComponents = response.data.results[0]?.address_components;
      const city = addressComponents.find(component => component.types.includes('locality'))?.long_name || 'Unknown City';
      const country = addressComponents.find(component => component.types.includes('country'))?.long_name || 'Unknown Country';

      const location = `${city}, ${country}`;
      setCurrentLocation(location);

      // Fetch prayer times based on the detected city and country
      fetchPrayerTimes(city, country);

    } catch (error) {
      console.error('Error fetching location name:', error);
      setCurrentLocation('Location not found');
    }
  };

  const fetchPrayerTimes = async (city, country) => {
    try {
      const response = await axios.get('https://api.aladhan.com/v1/timingsByCity', {
        params: {
          city,
          country,
          method: 2,
        },
      });

      const apiTimings = response.data.data.timings;

      // Filter the unwanted prayer times before setting the state
      const filteredTimings = {
        Fajr: apiTimings.Fajr,
        Dhuhr: apiTimings.Dhuhr,
        Asr: apiTimings.Asr,
        Maghrib: apiTimings.Maghrib,
        Isha: apiTimings.Isha,
      };

      // Directly set only the filtered prayers into the state
      setTimings(filteredTimings);

      // Send updated prayer times to the native module
      sendPrayerTimes(filteredTimings);
    } catch (error) {
      console.error('Error fetching prayer times:', error);
    }
  };


  const storePrayerTimes = async (updatedTimings) => {
    try {
      await AsyncStorage.setItem('prayerTimes', JSON.stringify(updatedTimings));
    } catch (error) {
      console.error('Error saving prayer times', error);
    }
  };

  const loadPrayerTimes = async () => {
    try {
      const storedTimings = await AsyncStorage.getItem('prayerTimes');
      if (storedTimings) {
        setTimings(JSON.parse(storedTimings));
      }
    } catch (error) {
      console.error('Error loading prayer times', error);
    }
  };

  const sendPrayerTimes = (updatedPrayerTimes) => {
    console.log("Sending Updated Prayer Times to Native Module:", updatedPrayerTimes);

    if (!updatedPrayerTimes || Object.keys(updatedPrayerTimes).length === 0) {
      console.warn("No updated prayer times provided!");
      return;
    }

    PrayerTimesModule.sendPrayerTimes({
      Fajr: updatedPrayerTimes.Fajr,
      Dhuhr: updatedPrayerTimes.Dhuhr,
      Asr: updatedPrayerTimes.Asr,
      Maghrib: updatedPrayerTimes.Maghrib,
      Isha: updatedPrayerTimes.Isha,
    });

    // Store updated prayer times
    storePrayerTimes(updatedPrayerTimes);

    // Trigger the widget update
    triggerWidgetUpdate(updatedPrayerTimes);
  };

  // Function to trigger the widget update with new prayer times
  const triggerWidgetUpdate = (prayerTimes) => {
    BroadcastSenderModule.sendUpdatePrayerTimes(
      prayerTimes.Fajr,
      prayerTimes.Dhuhr,
      prayerTimes.Asr,
      prayerTimes.Maghrib,
      prayerTimes.Isha
    );
  };
  const loadSwitchStates = async () => {
    try {
      const storedSwitchStates = await AsyncStorage.getItem('switchStates');
      if (storedSwitchStates) {
        setIsSwitchOn(JSON.parse(storedSwitchStates));
      }
    } catch (error) {
      console.error('Error loading switch states', error);
    }
  };

  const storeSwitchStates = async (updatedSwitchStates) => {
    try {
      await AsyncStorage.setItem('switchStates', JSON.stringify(updatedSwitchStates));
    } catch (error) {
      console.error('Error saving switch states', error);
    }
  };

  const handleTimePickerChange = (event, selectedTime) => {
    if (event.type === 'dismissed') {
      setShowPicker(false);
      return;
    }

    if (selectedTime) {
      const hours = selectedTime.getHours().toString().padStart(2, '0');
      const minutes = selectedTime.getMinutes().toString().padStart(2, '0');
      const updatedTime = `${hours}:${minutes}`;

      const updatedTimings = {
        ...timings,
        [selectedPrayer]: updatedTime,
      };


      setTimings(updatedTimings);

      // After updating, send the updated timings to the native module
      sendPrayerTimes(updatedTimings);
    }
    setShowPicker(false);
  };

  const handleUpdatePress = (prayer) => {
    setSelectedPrayer(prayer);
    setShowPicker(true);
  };

  // New function to reset prayer times
  const resetPrayerTimes = () => {
    const [city, country] = currentLocation.split(', ');
    fetchPrayerTimes(city, country); // Fetch prayer times again
  };

  const formatDate = (date) => {
    return date.toLocaleString("en-US", {
      weekday: "short",
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "numeric",
      hour12: false, // Use 24-hour format; set to true for 12-hour format
    });
  };

  // const scheduleNotification = async (notificationTime, prayer) => {
  //   try {
  //     await notifee.requestPermission();
  //     const channelId = await notifee.createChannel({
  //       id: "default1",
  //       name: "Default Channel1",
  //       importance: AndroidImportance.HIGH,
  //       sound: "default1",
  //     });

  //     if (!channelId) {
  //       console.error("Failed to create notification channel.");
  //       return;
  //     }

  //     // Convert the notification time string to a Date object
  //     const date = new Date(notificationTime);

  //     // Check if the date is valid
  //     if (isNaN(date.getTime())) {
  //       console.error('Invalid date for notification:', notificationTime);
  //       return;
  //     }

  //     // Format the date without the GMT offset
  //     const formattedDate = formatDate(date);

  //     // Schedule the notification using the timestamp
  //     await notifee.createTriggerNotification(
  //       {
  //         title: `${prayer}`,
  //         body: `${prayer} prayer time has started at ${formattedDate}`, // Use the formatted date here
  //         android: { channelId },
  //       },
  //       {
  //         type: TriggerType.TIMESTAMP,
  //         timestamp: date.getTime(), // Pass the timestamp in milliseconds
  //       }
  //     );

  //     console.log(`Notification scheduled for ${prayer} at ${formattedDate}`);
  //   } catch (error) {
  //     console.error('Error sending notification:', error);
  //   }
  // };


  // const toggleSwitch = async (prayer) => {
  //   const updatedSwitchStates = {
  //     ...isSwitchOn,
  //     [prayer]: !isSwitchOn[prayer],
  //   };

  //   setIsSwitchOn(updatedSwitchStates);
  //   await storeSwitchStates(updatedSwitchStates);

  //   const prayerTime = timings[prayer];
  //   console.log('Prayer time for', prayer, ':', prayerTime);

  //   if (updatedSwitchStates[prayer]) {
  //     const notificationTime = new Date();
  //     const [hours, minutes] = prayerTime.split(':').map(Number);
  //     notificationTime.setHours(hours);
  //     notificationTime.setMinutes(minutes);
  //     notificationTime.setSeconds(0);
  //     notificationTime.setMilliseconds(0);

  //     // Schedule the notification
  //     await scheduleNotification(notificationTime, prayer);
  //   } else {
  //     // Cancel the notification if the switch is turned off
  //     await notifee.cancelNotification(prayer);
  //   }
  // };

  const scheduleDailyNotification = async (notificationTime, prayer) => {
    try {
      await notifee.requestPermission();

      const channelId = await notifee.createChannel({
        id: "default1",
        name: "Default Channel1",
        importance: AndroidImportance.HIGH,
        sound: "default1",
      });

      if (!channelId) {
        console.error("Failed to create notification channel.");
        return;
      }

      const now = new Date();

      // Loop through and schedule notifications for the next 5 days
      for (let i = 0; i < 7; i++) {
        const dayNotificationTime = new Date(notificationTime);
        dayNotificationTime.setDate(notificationTime.getDate() + i); // Increment day by i for each iteration

        // Check if the notification time is valid and in the future
        if (isNaN(dayNotificationTime.getTime())) {
          console.error('Invalid notification date:', dayNotificationTime);
          return;
        }
        if (dayNotificationTime <= now) {
          console.log(`Skipping notification for ${prayer} on day ${i + 1} because the time has already passed.`);
          continue;
        }

        const formattedDate = formatDate(dayNotificationTime);

        // Schedule the notification for each day
        await notifee.createTriggerNotification(
          {
            title: `${prayer}`,
            body: `${prayer} prayer time has started at ${formattedDate}`,
            android: { channelId },
          },
          {
            type: TriggerType.TIMESTAMP,
            timestamp: dayNotificationTime.getTime(),
          }
        );

        console.log(`Notification scheduled for ${prayer} on day ${i + 1} at ${formattedDate}`);
      }

    } catch (error) {
      console.error('Error sending notification:', error);
    }
  };

  const toggleSwitch = async (prayer) => {
    const updatedSwitchStates = {
      ...isSwitchOn,
      [prayer]: !isSwitchOn[prayer],
    };

    setIsSwitchOn(updatedSwitchStates);
    await storeSwitchStates(updatedSwitchStates);
    showToast(`Notifications scheduled for ${prayer}`);

    const prayerTime = timings[prayer];
    console.log('Prayer time for', prayer, ':', prayerTime);

    const now = new Date();
    const notificationTime = new Date();
    const [hours, minutes] = prayerTime.split(':').map(Number);
    notificationTime.setHours(hours);
    notificationTime.setMinutes(minutes);
    notificationTime.setSeconds(0);
    notificationTime.setMilliseconds(0);

    if (updatedSwitchStates[prayer]) {
      // Check if the current time is before the prayer time
      if (notificationTime > now) {
        // Schedule notifications for the next 5 days
        await scheduleDailyNotification(notificationTime, prayer);
      } else {
        // console.log(`Skipping scheduling for ${prayer}, the time has passed for today.`);
        Alert.alert(`Skipping scheduling for ${prayer}, the time has passed for today.`)
      }
    } else {
      // Cancel the notification if the switch is turned off
      await notifee.cancelNotification(prayer);
    }
  };


  // // Function to cancel notifications for a specific prayer
  // const cancelNotification = async (prayer) => {
  //   // Use the prayer name as the ID
  //   await notifee.cancelNotification(prayer);
  //   console.log(`Notification for ${prayer} canceled.`);
  // };

  const showToast = (message) => {
    ToastAndroid.show(message, {
      position: -120,
      duration: ToastAndroid.durations.LONG,
      opacity: 0.9,
      delay: 0,

    });
  };


  return (
    <View style={styles.container}>
      {/* <Header/> */}
      <View style={styles.imageContainer}>
        <Image
          source={require('../src/assets/images/mosque.jpg')}
          style={styles.image}
        />
        <FlyingBirdsAnimation />
        <LinearGradient
          colors={['rgba(255,255,255,0)', 'rgba(255,255,255,0.5)', 'white', 'white']}
          style={styles.gradient}
          start={{ x: 0.9, y: 0 }}
          end={{ x: 0.9, y: 1.6 }}
        />

        <Text style={{ fontSize: 15, fontWeight: '800', color: '#4EC9B0', top: 10 }}>
          Current Location is: {currentLocation}
        </Text>

      </View>

      {/* Prayer Times Content Below Image */}
       {isConnected ? (
         <View style={styles.bodyContainer}>

         {Object.keys(timings).map((prayer, index) => (
           <Animated.View
             key={prayer}
             style={styles.prayerRow}
             entering={FadeInRight.delay(index * 200).springify()} // Apply delay for each prayer row
           >
             <Text style={styles.prayerName}>{prayer}</Text>
             <Text style={styles.prayerTime}>{timings[prayer] || 'Not set'}</Text>
 
             <View style={styles.iconContainer}>
               <TouchableOpacity onPress={() => handleUpdatePress(prayer)}>
                 <Icon name="update" size={24} color="#4EC9B0" />
               </TouchableOpacity>
 
               <TouchableOpacity onPress={resetPrayerTimes}>
                 <BackTime name="back-in-time" size={23} color="#726B53" />
               </TouchableOpacity>
 
               <Switch
                 value={isSwitchOn[prayer]} // Set individual switch state
                 onValueChange={() => toggleSwitch(prayer)} // Handle toggle switch
                 trackColor={{ false: "#ccc", true: "#8FDCCD" }} // Color for track (background)
                 thumbColor={isSwitchOn[prayer] ? "#4EC9B0" : "#f4f3f4"}  // Adjust thumb color based on the individual switch state
               />
             </View>
           </Animated.View>
         ))}
 
         {showPicker && (
           <DateTimePicker
             value={new Date()}
             mode="time"
             is24Hour={false}
             display="spinner"
             onChange={handleTimePickerChange}
           />
         )}
 
       </View>
       ) :  (<CheckInternet isConnected={isConnected} setIsConnected={setIsConnected} />)}

      <Text style={styles.widgetTxt}>You can use Widget of this App</Text>
    </View>
  );

};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  imageContainer: {
    position: 'relative', // To overlay gradient on image
    width: '100%',
    height: 300, // Adjust as per your requirement
    alignItems: 'center'

  },
  image: {
    width: '110%',
    height: '100%',
    resizeMode: 'cover',
    borderRadius: 20, // To round the corners of the image
  },
  gradient: {
    position: 'absolute',
    width: '105%',
    height: '100%',
  },
  bodyContainer: {
    top: 40,
    paddingHorizontal: 10,
    // paddingTop: 10, // To add some space below the image
  },
  prayerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 10,
    padding: 10,
    // Platform-specific shadows and elevation
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
      },
      android: {
        elevation: 5,
      },
    }),
    backgroundColor: '#fff', // Ensure background color to make shadow visible
    borderRadius: 8, // Optional: Add a border radius for a clean look
  },

  prayerName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#4EC9B0',
    width: "22%",
  },
  prayerTime: {
    fontSize: 18,
    fontWeight: '400',
    color: '#726B53',
    width: "15%",
    textAlign: 'center' // To align text to right side
  },
  iconContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 5,
    gap: 15
  },
  widgetTxt: {
    fontSize: 15,
    color: '#ccc',
    marginVertical: 50,
    textAlign: 'center'
  },

});


export default PrayerTimesForm;
