
# Ver-ID Face Authentication SDK

## Introduction
Ver-ID gives your application the ability to authenticate users by their faces. Ver-ID replaces the need to remember and type passwords. Ver-ID stores all its assets on the client making the entire face authentication process available offline.

Your application will interact with Ver-ID in two tasks:

1. To register the user's faces and add them as templates for future authentication,
2. To authenticate the user on subsequent visits.

## Tasks

### User registration
Before the user can authenticate Ver-ID needs to acquire a number of images of the user's face. These images serve as templates used for comparison at the time of authentication.
Your app instructs Ver-ID to register the user. Ver-ID then attempts to register the user and returns a result of the registration session. After successful registration the user is able to authenticate using her/his face.

Your app may register additional faces for an authenticated user to extend the probability of positive authentication under varying ambient lighting conditions.

### Authentication
Ver-ID can only authenticate users who have previously registered (see previous section).
Your app can ask Ver-ID to either authenticate a specific user or to identify and authenticate any registered user. This means your registered users may not even need to enter their user name to authenticate.

### Liveness Detection
If you need ensure that the user in front of the camera is a live person and not a picture or video impersonation you can use Ver-ID's liveness detection feature. Ver-ID will ask the user to turn in random directions.

## Adding Ver-ID in Your Android Studio Project

Follow these steps to add Ver-ID to your Android Studio project:

1. [Request an API secret](https://dev.ver-id.com/admin/register) for your app.
1. Open your Android Studio project's **build.gradle** file and under `allprojects/repositories` add

	```
	maven {
		url 'https://dev.ver-id.com/artifactory/gradle-release'
	}
	```
1. Open your app module's **build.gradle** file and under `dependencies` add

	```
	compile 'com.appliedrec:shared:2.0.2'
	compile 'com.appliedrec:det-rec-lib:2.0.2'
	compile 'com.appliedrec:verid:2.0.2'
	```
1. Open your app's **AndroidManifest.xml** file and add the following tag in `<application>` replacing `[your API secret]` with the API secret your received in step 1:
    
    ~~~xml
    <meta-data 
       android:name="com.appliedrec.verid.apiSecret" 
       android:value="[your API secret]" />
    ~~~

## Getting Started with the Ver-ID API
The easiest way to integrate Ver-ID to your app is to use Android's intents to launch Ver-ID activities and listen for the activity result to determine the session's outcome.

In your app's activity that needs to authenticate the user:

1. Launch Ver-ID using `startActivityForResult(Intent, int)` passing an intent configured for the particular Ver-ID task.
1. Override `onActivityResult(int, int, Intent)` and use the received intent to determine the outcome of the Ver-ID session.

When you no longer expect to need Ver-ID your app may call `VerID.unload()` to free up the resources associated with Ver-ID.

### <a name="registration"></a>Registration
Following are the exact steps your application should take to register a user.

1. Check that your user is not already registered. The easiest way to do this is to use Ver-ID's Android loader subclass [`VerIDUsersLoader`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.loaders.VerIDUsersLoader.html) with your activity implementing `LoaderManager.LoaderCallbacks<VerIDLoaderResponse>`:

    ~~~java
    public class MyActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<VerIDLoaderResponse> {
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           getSupportLoaderManager().initLoader(0, null, this).forceLoad();
        }
        
        @Override
        public Loader onCreateLoader(int id, Bundle args) {
           return new VerIDUsersLoader();
        }
        
        @Override
        public void onLoadFinished(Loader loader, VerIDLoaderResponse data) {
           if (data != null && data.getException() != null && data.getException() instanceof IllegalStateException) {
               // Unable to load Ver-ID
               return;
           }
           if (data != null && data.getResult() != null) {
               VerIDUser[] users = (VerIDUser[])data.getResult();
               if (users.length > 0) {
                   // Some users are already registered. Iterate over the users array to find out 
                   // whether the user you are trying to register is already registered.
               }
           }
        }
    
        @Override
        public void onLoaderReset(Loader loader) {
           // LoaderCallbacks implementation
        }
    }
    ~~~
1. If the user is already registered authenticate her/him before registering new faces. See <a href="#authentication">Authentication</a>.
1. In your activity configure an intent to launch a Ver-ID registration session: 

	~~~java
	// Select an ID for your user.
	String userId = "myUserId";
	// If set to null or the parameter is excluded in the registration settings 
	// constructor Ver-ID will create a random user ID.
	VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(userId);
	Intent intent = new VerIDRegistrationIntent(this, settings);
	startActivityForResult(intent, 1);
	~~~
1. In your activity override

    ~~~java
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == 1) { // The same request code you passed to startActivityForResult
	        if (resultCode == RESULT_OK && data != null) {
	            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
	            if (result != null && result.isPositive()) {
	                // The user is now registered
	            } else {
	                // Inspect result.outcome to find out why the registration failed
	            }
	        } else if (resultCode == RESULT_CANCELED) {
	            // The user cancelled the registration
	        }
	    }
	}
	~~~

### <a name="authentication"></a>Authentication
Follow these steps to authenticate any user who previously registered in your app without asking for a user name or password:

1. In your activity configure an intent to launch a Ver-ID authentication session:

	~~~java
	if (userIsRegistered) {
		VerIDAuthenticationSessionSettings settings = new VerIDAuthenticationSessionSettings(userId);
		Intent intent = new VerIDAuthenticationIntent(this, settings);
		startActivityForResult(intent, 2);
	} else {
		// The user will need to register first. See <a href="#registration">Registration</a>
	}
	~~~
1. In your activity override `onActivityResult`:

	~~~java
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 2) { // The same request code you passed to startActivityForResult
		    if (resultCode == RESULT_OK && data != null) {
		    	VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
		    	if (result != null && result.isPositive()) {
		            // The user is authenticated
		        } else {
		            // Inspect result.outcome to find out why the authentication failed
		        }
		    } else if (resultCode == RESULT_CANCELED) {
		        // The user cancelled the authentication
		    }
		}
	}
	~~~

### <a name="liveness_detection"></a>Liveness Detection
Follow these steps to ensure the user holding the device is a live person:

1. In your activity configure an intent to launch a Ver-ID liveness detection session:

	~~~java
	VerIDLivenessDetectionIntent intent = new VerIDLivenessDetectionIntent(this);
	startActivityForResult(intent, 0);
	~~~
1. In your activity override `onActivityResult`:

	~~~java
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) { // The same request code you passed to startActivityForResult
		    if (resultCode == RESULT_OK && data != null) {
		    	VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
		    	if (result != null && result.isPositive()) {
		            // The user holding the device is a live person
		            // Get the images of the user looking straight at the camera
		            Uri[] images = result.getImageUris(VerID.Bearing.STRAIGHT);
		            if (images.length > 0) {
		                // Display the first image in an image view
		                ((ImageView)findViewById(R.id.myImageView)).setImageURI(images[0]);
		            }
		        } else {
		            // Inspect result.outcome to find out why liveness detection failed
		        }
		    } else if (resultCode == RESULT_CANCELED) {
		        // The user cancelled the session
		    }
		}
	}
	~~~

## Documentation

Full API documentation is available on the project's [Github page](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.html).

# Release Notes

## Changes in Version 2.0.2

- Implicit loading of Ver-ID instance. You no longer need to call [`VerID.shared.load`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.html#load(Context,%20String,%LoadCallback))` before launching Ver-ID sessions. The older method with callback continues to work.
- Added [`VerIDUsersLoader`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.loaders.VerIDUsersLoader.html) and [`VerIDUserPictureUriLoader`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.loaders.VerIDUserPictureUriLoader.html) to facilitate easy asynchronous loading of Ver-ID user data. 

## Changes in Version 2.0.1

- Added ability to save face templates extracted in liveness detection sessions. To get the face templates set [`VerIDLivenessDetectionSessionSettings.includeFaceTemplatesInResult`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDLivenessDetectionSessionSettings.html#includeFaceTemplatesInResult) to `true` when starting a liveness detection session. Then call `getRecognitionFaces()` on the result of the session. You will receive an array of `RecognitionFace` objects. Call `getTemplate()` on each object to load the face template as a `float` array. The templates may be used for face comparison outside your app.
- Various bug fixes.

## Changes in Version 2.0

- New and improved face recognition.
- Simpler on-screen guidance.

## Changes in Version 1.8

- Added 3 [liveness detection levels](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.LivenessDetection.html). Regular (default) liveness detection no longer requires the user to register multiple poses.
- Added the option to guide the user through [registration](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings.html#showGuide) and [authentication](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings.html#showGuide).
- Added the option to display the result of [registration](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings.html#showResult) and [authentication](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings.html#showResult) sessions.
- Simplified retrieval of session results: `VerIDActivity.EXTRA_SESSION_RESULT` [parcelable extra](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html) contains all session information including collected [images](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html#getImageUris()) and [faces](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html#getFaceImages()).
- Deprecated `VerIDIntent` class. Use Intent extra constants from [`VerIDActivity`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.ui.VerIDActivity.html).
- Deprecated `VerIDSession`. The [`outcome`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html#outcome) parameter of `VerIDSessionResult` is now [`VerIDSessionResult.Outcome`](https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.Outcome.html).

## Changes in Version 1.7
- Real-time face pose estimates:
	- Improved guidance and smoother user experience.
	- More accurate anti-spoofing.
- Added liveness detection.

## Changes in Version 1.6.5

- Improved face detection.
- Improved security.

## Changes in Version 1.6.4

- Improved registration UI.
- Increased anti-spoofing reliability.

## Changes in Version 1.6.2

- Added `VerID.shared.findFaceInImage(String filePath)` method.

## Changes in Version 1.6

- Improved face detection and recognition algorithms.
- Simplified constructing of session intents and of retrieving session results.
- Simplified asynchronous loading of the Ver-ID SDK.

## Changes in Version 1.4
We changed how client apps authenticate with Ver-ID. In order to run Ver-ID you will need to register your app and obtain an API secret. You will provide the API secret when loading Ver-ID. Your application's package name will have to match the registered app's API key.
You can register your app by logging in to <a href="https://dev.ver-id.com/" target="_top">https://dev.ver-id.com/</a>.
### API Changes

- Load Ver-ID <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.html#load(Context, String)">with your API secret</a>

## Changes in Version 1.3
Version 1.3 introduces anti-spoofing. Ver-ID asks the user to register her/his face in various bearings. At authentication the user is requested to assume one of the registered bearings.

You need to determine how many of the <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.Bearing.html">9 bearings</a> you want to request during your registration process. We recommend using the default set of 4 bearings. More bearings provide more security at the expense of user experience.

Existing users who don't have different bearings registered will not be asked to meet the anti-spoofing challenge until they register one or more additional bearings.

### API Changes

- We have simplified the way settings are passed to Ver-ID sessions. Instead of multiple intent extras the application will now pass a single parcelable extra settings object. Please refer to <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDAuthenticationSession.Settings.html">VerIDAuthenticationSession.Settings</a> and <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDUserRegistrationSession.Settings.html">VerIDUserRegistrationSession.Settings</a>.
- With the introduction of anti-spoofing, authentication sessions now comprise one or more authentication segments, each of which correspond to an anti-spoofing challenge. As a result, the timeout of an authentication session is not set on the session but on the segment. When a segment times out the session fails. For more information see <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDAuthenticationSession.Settings.html">VerIDAuthenticationSession.Settings</a>.
- Like authentication sessions, the timing of registration sessions changed with the introduction of anti-spoofing. The user controls the timing of the individual bearing registrations and a session timeout no longer makes sense. The individual bearing registrations may still time out and fail. For more information consult <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDUserRegistrationSession.Settings.html">VerIDUserRegistrationSession.Settings</a>.
- Images are now submitted to Ver-ID sessions as byte arrays of image data. This significantly improves performance. The old way of submitting image file names still works but is discouraged and marked as deprecated. If you're implementing your own image provider consider switching to <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSession.html#addImage(VerIDImageRequest, byte[], int, Point, int)">the new addImage method</a>.
- The image provider interface has changed. If you're using your own image provider implementation please refer to <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDImageProvider.html">the documentation</a>.
</ul>

## Changes in Version 1.2
### API Changes

- Added <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDFaceDetectionListener.html">face detection listener</a>, which gives the client the chance to intercept face detection results before they are passed on for processing.
- Changed the `requestImage` method signature on the <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDImageProvider.html">image provider</a>. The request contains a predicate object that describes the conditions for face acceptance.
- Added `onSessionProgress` method on <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDSessionListener.html">VerIDSessionListener</a> to let the listener receive the individual face processing results.
- <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html">VerIDSessionResult</a> now includes a lot more information about the received face.
- Introduced <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.model.VerIDFace.html">VerIDFace</a> object, which contains information about the detected face. The object is returned by the <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerIDFaceDetectionListener.html">VerIDFaceDetectionListener</a> and as part of <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html">VerIDSessionResult</a>.

### Fixes

- Fixed premature timeout on finite sessions.
- Improved user guidance.

## Changes in Version 1.1
### API Changes

- Added the ability to <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSession.html#setImageRequestTimeoutInterval(long)">set the timeout interval</a> for individual image requests.
- Setting of security level has moved from <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSession.html#setSecurityLevel(VerID.SecurityLevel)">VerIDSession</a> to <a href="https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.VerID.html#setSecurityLevel(VerID.SecurityLevel)">VerID</a>.

### Fixes

- Faster image processing.
- Improved face recognition accuracy.
- Continuous authentication sessions are working properly.
- Clearer progress indication in the Ver-ID fragment.
