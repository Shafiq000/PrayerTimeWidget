import { StyleSheet, Text, View, Image, Pressable } from 'react-native'
import React, { useEffect } from 'react'
import NetInfo from "@react-native-community/netinfo";
const CheckInternet = ({ isConnected, setIsConnected }) => {
    useEffect(() => {
        const unsubscribe = NetInfo.addEventListener(state => {
            console.log("Connection type", state.type);
            console.log("Is connected?", state.isConnected);
            setIsConnected(state.isConnected);
        });

        return () => {
            unsubscribe();
        }
    }, [isConnected])

    const CheckConnection = () => {
        NetInfo.refresh().then(state => {
            console.log("Connection type", state.type);
            console.log("Is connected?", state.isConnected);
        });
    }
    return (
        <View style={[styles.container]}>
            <Image
                source={require("../assets/images/no-wifi.png")}
                style={[styles.image]}
            ></Image>
            <Text style={[{ fontSize: 15, fontWeight: "700" }]}>No Internet</Text>
            <Text style={[{ fontSize: 13, textAlign: "center", marginTop: 10 }]}>For calculating Prayer Times locations is required.{"\n"}
                To find location internet is required.
            </Text>
            <Text style={[{ fontSize: 13, marginTop: 15 }]}>Please, turn on internet and retry.</Text>
            <View style={styles.viewBtn}>
                <Pressable onPress={() => {
                    alert("Again check for the better connection")
                    CheckConnection()
                }}>
                    <Text style={{ fontSize: 15, fontWeight: "600", textAlign: "center", color: "#fff" }}>Retry</Text>
                </Pressable>
            </View>
        </View>
    )
}

export default CheckInternet

const styles = StyleSheet.create({
    container: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        paddingHorizontal: 10

    },
    image: {
        height: 50,
        width: "15%"
    },
    viewBtn: {
        marginTop: 20,
        height: 40,
        width: 90,
        backgroundColor: "#0a9484",
        alignItems: "center",
        justifyContent: "center",
        borderRadius: 5
    }
})