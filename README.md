# ffa
Floor Finder Application

## Summary
This code dump is the combined work for an Android app that is supposed to make it a little bit easier for users to navigate the world.

The purpose of the app is ultimately to show its users where a premise (a lecture room, a conference room, etc) is located, by using a combination of Google Maps and custom made views that displays images (e.g markers and floor plans).

## Why this?
I spent quite some time dealing with how to implement a view that
* Made it possible to drag it around
* Made it possible to scale/zoom
* Made use of bounds when dragging and/or scaling/zooming
* Would accept 1-N markers to be added to it
* Didn't scale the marker when the view was scaled/zoomed

After a lot of StackOverflowing, tutorial finding, experimenting and headbashing, I managed to scrounge together a decent implementation that wasn't overly obese but still did the things already mentioned.
There are so many more complete/extended projects of what the Floor Finder App does, but I'd at least like to add my implementation to that family, because it may suit someone else's similar needs.

## Features (or functions or stuff in that category)
* The stuff mentioned in "Why this?"
* Two different ways of handling the resources
    * Local: This is assets-based in that you place a json-formatted file in the assets folder, as well as your floor plans
    * Remote: You would place the source(-image)ry on e.g. a web server and let an AsyncTask do all the syncie-work for you. The source files will be placed on external storage

## Anything you need to know apart from what's already been said?
Yes.
* You need to add your own Google Maps key to this project in order for the MapsView to function properly. There's a link in the source code that should get you sorted out on how to acquire the key.
* You need to host your own remote (web) solution in order for the AsyncTask to be able to do its job. I, unfortunately, can't afford to host everyone else's graphical assets.
