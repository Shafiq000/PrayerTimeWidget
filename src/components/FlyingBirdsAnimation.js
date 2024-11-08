import React, { useEffect } from 'react';
import { View, Image, StyleSheet } from 'react-native';
import Animated, { Easing, useSharedValue, useAnimatedStyle, withTiming, withRepeat } from 'react-native-reanimated';

// Replace with your bird image
const birdImage = require('../assets/images/birdgiff.gif');

const FlyingBirdsAnimation = () => {
  const numberOfBirds = 7; // Number of birds you want to animate

  // Generate random Y positions for birds
  const birds = Array.from({ length: numberOfBirds }).map(() => {
    return {
      translateX: useSharedValue(0), // Starting point on X-axis
      translateY: Math.random() * 100 + 10, // Random Y position for each bird (closer to the top)
      speed: Math.random() * 4000 + 5000, // Random flying speed
    };
  });

  useEffect(() => {
    birds.forEach((bird) => {
      bird.translateX.value = withRepeat(
        withTiming(300, { duration: bird.speed, easing: Easing.linear }), // Fly to the right
        -1, // Infinite loop
        false // Do not reset to 0 automatically
      );
    });
  }, []);

  return (
    <View style={styles.container}>
      {birds.map((bird, index) => {
        const animatedStyle = useAnimatedStyle(() => {
          return {
            transform: [
              { translateX: bird.translateX.value }, // Move along the X-axis
              { translateY: bird.translateY }, // Static Y position per bird
            ],
          };
        });

        return (
          <Animated.Image
            key={index}
            source={birdImage}
            style={[styles.bird, animatedStyle]}
          />
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0, // Ensure the container is positioned at the top
    left: 0,
    right: 0,
    height: '90%',
    alignItems: 'flex-start',
    justifyContent: 'center',
    overflow: 'hidden', // Ensure birds are clipped
    bottom:0
  },
  bird: {
    width: 30, // Adjust size of the bird
    height: 30,
    // margin: 10, // Space between birds
    position: 'absolute',
    top: 0, // Ensure the container is positioned at the top
    left: 0,
    right: 0,
    bottom:0
  },
});

export default FlyingBirdsAnimation;
