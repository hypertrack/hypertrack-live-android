# Live Location Sharing
[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://opensource.org/licenses/MIT) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) 

This open source app for Live Location Sharing is built with [HyperTrack](https://www.hypertrack.com). Developers can now build live location sharing into their apps within minutes by using HyperTrack Live source code. For iOS, refer to our open source [iOS repository](https://github.com/hypertrack/live-app-ios).

[HyperTrack Live for Android](https://play.google.com/store/apps/details?id=com.hypertrack.live) is available on the Google Play Store for use by users signed up with HyperTrack. [HyperTrack Live for iOS](https://apps.apple.com/us/app/hypertrack-live/id1076924821) is available on the public App Store.

> 👋 Read our [blog post announcement](https://hypertrack.com/blog/2019/07/19/build-live-location-sharing-in-your-work-app-within-minutes/) to learn more about this sample app.

The app demonstrates a simple flow for users to share their live location with friends through their favorite messaging app when on the way to meet up. The convenient 23-character secure short-URL displays live location with accuracy, speed, bearing and recency. The view highlights the ongoing activity (walk, drive, stop, etc.). Device getting inactive due to permissions or other reasons, or disconnected due to unknown reasons is highlighted in the tracking experience. Battery charing and low battery states are flagged as well.

## Live Location Sharing
The primary feature of Hypertrack Live is Live Location Sharing. Live Location Sharing is useful for:
- Workforce productivity apps with messaging and customer support capabilities
- Peer-to-peer marketplaces for goods and services
- Ridesharing and carpooling apps
- Consumer apps like messengers and social apps

It helps solve the user's anxiety of “where are you⁉️”.

## Architecture

![Architecture](images/ArchitectureLiveApp.png)

## How HyperTrack Live App uses HyperTrack Trips API

HyperTrack Live App uses [HyperTrack Trips API](https://docs.hypertrack.com/#guides-apis-usage-trips) to [create](https://docs.hypertrack.com/#references-apis-trips-post-trips) and [complete](https://docs.hypertrack.com/#references-apis-trips-post-trips-trip_id-complete) trips by using Live App Backend. Live App Backend allows integrate with HyperTrack Trips API via backend server integration.

Once a trip is created in the HyperTrack Live App, Live App Backend calls creates a trip via a call to [Trips API](https://docs.hypertrack.com/#references-apis-trips-post-trips). Once the trip is completed, the app notifies Live App Backend calls HyperTrack Trips API to [complete](https://docs.hypertrack.com/#references-apis-trips-post-trips-trip_id-complete) the trip.

## Usage
#### To use this app

```bash
# Clone this repository
$ git clone https://github.com/hypertrack/live-app-android.git

# Go into the repository
$ cd live-app-android

# Change com.google.android.geo.API_KEY on yours in AndroidManifest.xml
<meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="<you_google_maps_key>" />
```

Get your HyperTrack Publishable Key [here](https://dashboard.hypertrack.com/signup) and type it in the Publishable Key screen in the app.

## Documentation
For detailed documentation of the APIs, customizations and what all you can build using HyperTrack, please visit the official [docs](https://docs.hypertrack.com/).

[HyperTrack Quickstart](https://github.com/hypertrack/quickstart-android),
[HyperTrack Views Sample](https://github.com/hypertrack/views-android),
[HyperTrack Maps SDK](https://github.com/hypertrack/sdk-maps-google-android)

## Contribute
Feel free to clone, use, and contribute back via [pull requests](https://help.github.com/articles/about-pull-requests/). We'd love to see your pull requests - send them in! Please use the [issues tracker](https://github.com/hypertrack/live-app-android/issues) to raise bug reports and feature requests.

We are excited to see what Live Location feature you build in your app using this project. Do ping us at help@hypertrack.com once you build one, and we would love to feature your app on our blog!

## Support
Join our [Slack community](https://join.slack.com/t/hypertracksupport/shared_invite/enQtNDA0MDYxMzY1MDMxLTdmNDQ1ZDA1MTQxOTU2NTgwZTNiMzUyZDk0OThlMmJkNmE0ZGI2NGY2ZGRhYjY0Yzc0NTJlZWY2ZmE5ZTA2NjI) for instant responses. You can also email us at help@hypertrack.com.


