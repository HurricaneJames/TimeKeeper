# Project Status #
Releasing Beta on Google Play store as soon as this checklist is complete.

Main Project List
 - [x] Timer Button starts/stops timer
 - [x] Timer Button correctly reflects summation of time records (removing any overlapping times)
 - [x] Note Counts
 - [x] Note Buttons (call note capture views)
 - [x] Selecting project opens project details view

Project Details View
 - [x] Display notes (see below)
 - [x] Display all time records
 - [x] Each time record displays duration, start time, and number of notes
 - [x] Tap on time record displays notes recorded during that session
 - [x] Tapping on project name allows user to change project name
 - [x] Project name auto-saves while typing
 - [x] Project name has imeOption for done action so user can easily close the soft keyboard (if open)

Note Capture
 - [x] Text
 - [x] Camera (Video)
 - [x] Camera (Image)
 - [x] Audio

Note View/Playback
 - [x] Text
 - [x] Camera (Video)
 - [x] Camera (Image)
 - [x] Audio

Features/Enhancements
 - [ ] Replace image/video capture apps with custom implementation that works from a single button for both image/video (user choice when capturing)
 - [ ] Add back lists (low priority as they did not work very well in the initial implementation)
 - [ ] Add settings/preferences dialog

Known Bugs
 - [ ] Correct project selection in dual-pane view. (Select first project, start timer for second project, second project becomes first project in list and takes selection, but details are not updated to reflect selected item)
 - [ ] Clicking '+ New' changes the project list as expected. However, the selected item changes to a different project while the details are still set to the last selected item.
 - [ ] Note Count in project details (time record parent view) does not update when new notes are added.
 - [ ] Video capture cuts off last 1/2 second (MPEG/AAC encoder error in Android)
 - [ ] DevTools still in options menu.

# Description #
Back in 2011 I wanted to create an Android application to help me keep track of time spent on various projects. It should have basic stopwatch functionality tied to an optional "project" name. It should also support adding "Notes" to the time recordings. Notes are text blobs, lists, images/videos, and/or sound recordings. The general use case was to click start and then type or say what project was being done.

Shortly after starting the uphill trudge that is learning Android development, a new job happened. Then life got even more complicated. Ultimately TimeKeeper, and Android development in general, was put on the back burner.

Luckly, my design was pretty good, because Google released Google Keep a year or so later. Google Keep was almost everything in the original design, minus the time tracking. It was "good enough" for most of what wanted.

Skip forward a couple years and there is some time and a desire to develop Android apps again. This is really a "MyFirstAndroid" type app, so it is probably best not to expect it to do anything useful for a while.

## 2014-06-14 Update ##
After implementing the list functionality, it because obvious that lists were one feature that worked much better on paper than in practice. Lists were replaced with a video button. Lists may come back at some point in the future (when replacing the stock video/image capture apps with custom implementations built for this project).

# Compatibility #
This app is designed to work with KitKat and newer (API 19+). At some point it may be extended to older devices, but for now, this is just easier.

# Icons #
Added some icons from the Google's current (2014-05-04) Android Action Bar Icon Pack.
https://developer.android.com/design/downloads/index.html
https://developer.android.com/design/style/iconography.html
https://developer.android.com/downloads/design/Android_Design_Icons_20131106.zip