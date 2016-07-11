Running the AndroidSimpleTracker Sample
============================================
This sample demonstrates how to use Trackrr.IO SDK on Android. It supports Google Play Service Location Trackking And Publish.

## Requirements

* AndroidStudio
* Android API 17 or greater

## Steps

1. Import the AndroidSimpleTracker project into your IDE.
   - using Android Studio:
      * From the Welcome screen, click on "Import project".
      * Browse to the AndroidSimpleTracker directory and press OK.
	  * Accept the messages about adding Gradle to the project.
	  * If the SDK reports some missing Android SDK packages (like Build Tools or the Android API package), follow the instructions to install them.
	  
2. Import the libraries :
   - Gradle will take care of downloading these dependencies for you.

3. Import the Google Play Service SDK into the project.
   - again Gradle will do everything for you.

4. Get a Configuration(google-services.json) File.
   - Access To https://developers.google.com/mobile/add .
   - Provide some additional information.
       - ex) 
       - Appname :SimpleTracker
       - Android packege name: io.trackrr.demo.androidsimpletracker
       - Your country/region: Select Your Region
   - After you complete the registration, download the google-service.json file to add to your project(app dir).

5. Run Application:
   * At this point you can run the sample.
     + Go to Project ->  Clean.
     + Go to Project ->  Build All.
     + Go to Run -> Run.

