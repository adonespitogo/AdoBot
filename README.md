# AdoBot
AdoBot spyware android client

# Features
 - hidden app icon (stealth mode)
 - get sms in realtime or scheduled
 - get call logs in realtime or scheduled
 - get contacts
 - monitor location
 - update apk remotely
 - data collected are retained in database
 - realtime notifications about device status
 - For android 6 and above:
   - You can view the permissions of the app
   - The app asks for permission when a certain command is sent the there is no permission

# Instructions

Edit `CommonParams.java` and change the `PRODUCTION_SERVER` variable to the address of your [AdoBot-IO](https://github.com/adonespitogo/AdoBot-IO) server. Next, checkout [AdoBot-IO](https://github.com/adonespitogo/AdoBot-IO) on how to setup the NodeJS server.

# Screen Shots

## Main GUI

![Adobot Main GUI](./screenshots/main.png "Adobot Main GUI")

## Location Tab

![Location Tab](./screenshots/location.png "Adobot Location Tab")

## Main SMS Tab

![Main SMS Tab](./screenshots/sms-main.png "Adobot Main SMS Tab")

## Single SMS Thread View

SMS thread is a pop up modal

![SMS Thread Tab](./screenshots/sms-thread.png "Adobot SMS Thread Tab")

## Call Logs Tab

![Call Logs Tab](./screenshots/call-logs.png "Adobot Call Logs Tab")

## Contacts Tab

![Contacts Tab](./screenshots/contacts.png "Adobot Contacts Tab")

## Pending Commands Tab

When you send a command to an offline device, the command is stored in the datase and will be executed once the device connects online.

![Pending commands Tab](./screenshots/pending-commands.png "Adobot Pending Commands Tab")

## Update APK 

![Update APK](./screenshots/update-apk.png "Adobot update APK")


## Notifications

![Notification](./screenshots/notifications/notif2.png "Adobot notification")
![Notification](./screenshots/notifications/notif3.png "Adobot notification")