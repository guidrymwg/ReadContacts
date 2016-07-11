package com.lightcone.readcontacts;

import android.Manifest; //added
import android.content.Context; //added
import android.content.DialogInterface; //added
import android.content.Intent; //added
import android.content.pm.PackageManager; //added
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat; //added
import android.support.v7.app.AlertDialog; //added
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // To suppress notational clutter and make structure clearer, define some shorthand constants.

    private static final Uri URI = ContactsContract.Contacts.CONTENT_URI;
    private static final Uri PURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    private static final Uri EURI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
    private static final Uri AURI = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI;
    private static final String ID = ContactsContract.Contacts._ID;
    private static final String DNAME = ContactsContract.Contacts.DISPLAY_NAME;
    private static final String HPN = ContactsContract.Contacts.HAS_PHONE_NUMBER;
    private static final String LOOKY = ContactsContract.Contacts.LOOKUP_KEY;
    private static final String CID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
    private static final String EID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
    private static final String AID = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID;
    private static final String PNUM = ContactsContract.CommonDataKinds.Phone.NUMBER;
    private static final String PHONETYPE = ContactsContract.CommonDataKinds.Phone.TYPE;
    private static final String EMAIL = ContactsContract.CommonDataKinds.Email.DATA;
    private static final String EMAILTYPE = ContactsContract.CommonDataKinds.Email.TYPE;
    private static final String STREET = ContactsContract.CommonDataKinds.StructuredPostal.STREET;
    private static final String CITY = ContactsContract.CommonDataKinds.StructuredPostal.CITY;
    private static final String STATE = ContactsContract.CommonDataKinds.StructuredPostal.REGION;
    private static final String POSTCODE = ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE;
    private static final String COUNTRY = ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY;
    private static final int MAX_NUMBER_ENTRIES = 5;

    private String id;
    private String lookupKey;
    private String name;
    private String street;
    private String city;
    private String state;
    private String postcode;
    private String country;
    private String ph[];
    private String phType[];
    private String em[];
    private String emType[];
    private File root;
    private int emcounter;
    private int phcounter;
    private int addcounter;
    private TextView tv;

    private ProgressBar progressBar;

    // User defines value of REQUEST_CONTACTS. It will identify a permission request
    // specifically for ACCESS_FINE_LOCATION.  Define a different integer for each
    // "dangerous" permission that you will request at runtime (in this example there
    // is only one).

    final private int REQUEST_CONTACTS = 1; // User-defined integer  //added
    private static final String TAG = "RCONTACTS"; //added
    private static final int dialogIcon = R.mipmap.ic_launcher; //added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.TextView01);

        // Allow for up to MAX_NUMBER_ENTRIES email and phone entries for a contact
        em = new String[MAX_NUMBER_ENTRIES];
        emType = new String[MAX_NUMBER_ENTRIES];
        ph = new String[MAX_NUMBER_ENTRIES];
        phType = new String[MAX_NUMBER_ENTRIES];

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Call a check for runtime permissions
        checkRuntimePermissions();

        // Check that external media available and writable
        //checkExternalMedia();  //added

        // Read the contacts and output to a file.  Process this on a background
        // thread defined by an instance of AsyncTask because it will typically
        // take several seconds to process a few hundred contacts. We will display
        // an indeterminate progress bar to the user while the contacts are being
        // processed.

        //new BackgroundProcessor().execute(); // added

    }

    // Method executed to run app if permission has been granted

    public void doTheStuff(){  //added

        // Check that external media available and writable
        checkExternalMedia();  //added

        // Read the contacts and output to a file.  Process this on a background
        // thread defined by an instance of AsyncTask because it will typically
        // take several seconds to process a few hundred contacts. We will display
        // an indeterminate progress bar to the user while the contacts are being
        // processed.

         new BackgroundProcessor().execute();
    }

    //added
    /* Method to check runtime permissions.  For Android 6 (API 23) and
     beyond, we must check for the "dangerous" permission READ_CONTACTS at
     runtime (in addition to declaring it in the manifest file).  The following code checks
     for this permission.  If it has already been granted, it proceeds as normal.  If it
     has not yet been granted by the user, the user is presented with an opportunity to grant
     it. In general, the rest of this class will not execute until the user has granted such permission. */

    public void checkRuntimePermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            /* Permission has not been granted by user previously.  Request it now. The system
             will present a dialog to the user requesting the permission, with options "accept",
             "deny", and a box to check "don't ask again". When the user chooses, the system
             will then fire the onRequestPermissionsResult() callback, passing in the user-defined
             integer defining the type of permission request (REQUEST_CONTACTS in this case)
             and the "accept" or "deny" user response.  We deal appropriately
             with the user response in our override of onRequestPermissionsResult() below.*/

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
        } else {
            Log.i(TAG, "Permission has been granted");

            doTheStuff();
        }

    } //added

      /*Following method invoked by the system after the user response to a runtime permission request
     (Android 6, API 23 and beyond implement such runtime permissions). The system passes to this
     method the user's response, which you then should act upon in this method.  This method can respond
     to more than one type permission.  The user-defined integer requestCode (passed in the call to
     ActivityCompat.requestPermissions) distinguishes which permission is being processed. */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        // Since this method may handle more than one type of permission, distinguish which one by a
        // switch on the requestCode passed back to you by the system.

        switch (requestCode) {

            // The permission response was for fine location
            case REQUEST_CONTACTS:
                Log.i(TAG, "Read contacts permission granted: requestCode=" + requestCode);
                // If the request was canceled by user, the results arrays are empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted. Carry on as we would without the permission request

                    doTheStuff();

                } else {
                    Log.i(TAG, "onRequestPermissionsResult - permission denied: requestCode=" + requestCode);

                    // The permission was denied.  Warn the user of the consequences and give
                    // them one last time to enable the permission.

                    showTaskDialog(1, "Warning!",
                            "This app will not function without this permission!",
                            dialogIcon, this, "OK, Do Over", "Refuse Permission");
                }
                return;

        }
    }

    /**
     * Method showTaskDialog() creates a custom alert dialog. This dialog presents text defining
     * a choice to the user and has buttons for a binary choice. Pressing the rightmost button
     * will execute the method positiveTask(id) and pressing the leftmost button will execute the
     * method negativeTask(id). You should define appropriate actions in each. (If the
     * negativeTask(id) method is empty the default action is just to close the dialog window.)
     * The argument id is a user-defined integer distinguishing multiple uses of this method in
     * the same class.  The programmer should switch on id in the response methods
     * positiveTask(id) and negativeTask(id) to decide which alert dialog to respond to.
     * This version of AlertDialog.Builder allows a theme to be specified. Removing the theme
     * argument from the AlertDialog.Builder below will cause the default dialog theme to be used. */

    private void showTaskDialog(int id, String title, String message, int icon, Context context,
                                String positiveButtonText, String negativeButtonText){

        final int fid=id;  // Must be final to access from anonymous inner class below

        AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.MyDialogTheme);
        builder.setMessage(message).setTitle(title).setIcon(icon);

        // Add the right button
        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                positiveTask(fid);
            }
        });
        // Add the left button
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                negativeTask(fid);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to be executed if user chooses negative button. This returns to main
    // activity since there is no permission to execute this class.

    private void negativeTask(int id){

        // Use id to distinguish if more than one usage of the alert dialog
        switch(id) {

            case 1:
                // Warning that this part of app not enabled
                String warn ="Exiting the app. It is installed but not enabled.  To enable this ";
                warn += "app you may manually enable Contacts permission in ";
                warn += " Settings > App > ReadContacts > Permissions.";
                // New single-button dialog
                showTaskDialog(2,"Task not enabled!", warn, dialogIcon, this, "", "OK");
                break;

            case 2:
                // Exit the app since permission was denied
                finish();
                break;
        }

    }

    // Method to execute if user chooses positive button ("OK, I'll Do It"). This starts the check
    // of runtime permissions again.

    private void positiveTask(int id){

        // Use id to distinguish if more than one usage of the alert dialog
        switch(id) {

            case 1:
                // User agreed to enable location
                checkRuntimePermissions();

                break;

            case 2:

                break;

        }

    }

    // Method to process contacts (reads them and writes formatted output to a file on device, in
    // addition to concatenating a string listing the contacts in the list that will be displayed
    // on the phone screen).  This method will be invoked from a background thread since it will
    // take some time to execute.

    private String processContacts() {

        /** Open a PrintWriter wrapping a FileOutputStream so that we can send output from a
         query of the Contacts database to a file on the SD card.  Must wrap the whole thing
         in a try-catch to catch file not found and i/o exceptions. Note that since we are writing
         to external media we must add a WRITE_EXTERNAL_STORAGE permission to the
         manifest file.  Otherwise  a FileNotFoundException will be thrown. */

        // Create a StringBuilder for efficient concatenation of contact list into single string.
        // (We cannot append directly to the views from here because this will be run on background
        // thread and we cannot touch views on main thread from here. We will concatenate the
        // string here and return it, and then update the view from the onPostExecute method of
        // AsyncTask (which can interact with views on the main thread).

        StringBuilder stringBuilder = new StringBuilder();

        // This will set up output to /sdcard/download/phoneData.txt if /sdcard is the root of
        // the external storage.  See the project WriteSDCard for more information about
        // writing to a file on the SD card.

        File dir = new File(root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir, "phoneData.txt");

        stringBuilder.append("Wrote " + file + "\nfor following contacts:\n");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            // Main loop to query the contacts database, extracting the information.  See
            // http://www.higherpass.com/Android/Tutorials/Working-With-Android-Contacts/

            ContentResolver cr = getContentResolver();
            Cursor cu = cr.query(URI, null, null, null, null);

            if (cu.getCount() > 0) {

                // Loop over all contacts

                while (cu.moveToNext()) {

                    // Initialize storage variables for the new contact
                    street = "";
                    city = "";
                    state = "";
                    postcode = "";
                    country = "";

                    // Get ID information (id, name and lookup key) for this contact. id is an identifier
                    // number, name is the name associated with this row in the database, and
                    // lookupKey is an opaque value that contains hints on how to find the contact
                    // if its row id changed as a result of a sync or aggregation.

                    id = cu.getString(cu.getColumnIndex(ID));
                    name = cu.getString(cu.getColumnIndex(DNAME));
                    lookupKey = cu.getString(cu.getColumnIndex(LOOKY));
                    // Append list of contacts to the StringBuilder object
                    stringBuilder.append("\n" + id + " " + name);

                    // Query phone numbers for this contact (may be more than one), so use a
                    // while-loop to move the cursor to the next row until moveToNext() returns
                    // false, indicating no more rows. Store the results in arrays since there may
                    // be more than one phone number stored per contact. The if-statement
                    // enclosing everything ensures that the contact has at least one phone
                    // number stored in the Contacts database.

                    phcounter = 0;
                    if (Integer.parseInt(cu.getString(cu.getColumnIndex(HPN))) > 0) {
                        Cursor pCur = cr.query(PURI, null, CID + " = ?", new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            ph[phcounter] = pCur.getString(pCur.getColumnIndex(PNUM));
                            phType[phcounter] = pCur.getString(pCur.getColumnIndex(PHONETYPE));
                            phcounter++;
                        }
                        pCur.close();
                    }

                    // Query email addresses for this contact (may be more than one), so use a
                    // while-loop to move the cursor to the next row until moveToNext() returns
                    // false, indicating no more rows. Store the results in arrays since there may
                    // be more than one email address stored per contact.

                    emcounter = 0;
                    Cursor emailCur = cr.query(EURI, null, EID + " = ?", new String[]{id}, null);
                    while (emailCur.moveToNext()) {
                        em[emcounter] = emailCur.getString(emailCur.getColumnIndex(EMAIL));
                        emType[emcounter] = emailCur.getString(emailCur.getColumnIndex(EMAILTYPE));
                        emcounter++;
                    }
                    emailCur.close();

                    // Query Address (assume only one address stored for simplicity). If there is
                    // more than one address we loop through all with the while-loop but keep
                    // only the last one.

                    addcounter = 0;
                    Cursor addCur = cr.query(AURI, null, AID + " = ?", new String[]{id}, null);
                    while (addCur.moveToNext()) {
                        street = addCur.getString(addCur.getColumnIndex(STREET));
                        city = addCur.getString(addCur.getColumnIndex(CITY));
                        state = addCur.getString(addCur.getColumnIndex(STATE));
                        postcode = addCur.getString(addCur.getColumnIndex(POSTCODE));
                        country = addCur.getString(addCur.getColumnIndex(COUNTRY));
                        addcounter++;
                    }
                    addCur.close();

                    // Write identifiers for this contact to the SD card file
                    pw.println(name + " ID=" + id + " LOOKUP_KEY=" + lookupKey);
                    // Write list of phone numbers for this contact to SD card file
                    for (int i = 0; i < phcounter; i++) {
                        pw.println("   phone=" + ph[i] + " type=" + phType[i] + " ("
                                + getPhoneType(phType[i]) + ") ");
                    }
                    // Write list of email addresses for this contact to SD card file
                    for (int i = 0; i < emcounter; i++) {
                        pw.println("   email=" + em[i] + " type=" + emType[i] + " ("
                                + getEmailType(emType[i]) + ") ");
                    }
                    // If street address is stored for contact, write it to SD card file
                    if (addcounter > 0) {
                        if (street != null) pw.println("   street=" + street);
                        if (city != null) pw.println("   city=" + city);
                        if (state != null) pw.println("   state/region=" + state);
                        if (postcode != null) pw.println("   postcode=" + postcode);
                        if (country != null) pw.println("   country=" + country);
                    }
                }
            }
            // Flush the PrintWriter to ensure everything pending is output before closing
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("MEDIA", "File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the manifest file? ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return the string built by the StringBuilder

        return stringBuilder.toString();
    }

    /**
     * Method to check whether external media available and writable and to find the
     * root of the external file system.
     */

    private void checkExternalMedia() {
        // Check external media availability.  This is adapted from
        // http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        // Find the root of the external storage and output external storage info to screen
        root = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        tv.append("External storage: Exists=" + mExternalStorageAvailable + ", Writable="
                + mExternalStorageWriteable + "\nRoot=" + root + "\n");
    }


    /**
     * Method to return label corresponding to phone type code. Data for correspondence from
     * http://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Phone.html
     */

    private String getPhoneType(String index) {
        if (index.trim().equals("1")) {
            return "home";
        } else if (index.trim().equals("2")) {
            return "mobile";
        } else if (index.trim().equals("3")) {
            return "work";
        } else if (index.trim().equals("7")) {
            return "other";
        } else {
            return "?";
        }
    }

    /**
     * Method to return label corresponding to email type code. Data for correspondence from
     * http://developer.android.com/reference/android/provider/ContactsContract.
     * CommonDataKinds.Email.html
     */

    private String getEmailType(String index) {
        if (index.trim().equals("1")) {
            return "home";
        } else if (index.trim().equals("2")) {
            return "work";
        } else if (index.trim().equals("3")) {
            return "other";
        } else if (index.trim().equals("4")) {
            return "mobile";
        } else {
            return "?";
        }
    }


    // Subclass AsyncTask to perform the parsing of the contacts list on a background thread.  The three
    // argument types inside the < > are (1) a type for the input parameters (Void in this case),
    // (2) a type for any published progress during the background task (Void in this case,  because
    // we aren't going to publish progress), and (3) a type for the object returned from the background
    // task (in this case it is type String).

    private class BackgroundProcessor extends AsyncTask<Void, Void, String> {

        // This method executes the task on a background thread

        @Override
        protected String doInBackground(Void... params) {

            // Execute the processContacts() method on this background thread and return from
            // it a string containing the list of contacts on the phone. The method processContacts
            // will also write an output file containing a list of information (phone, email, ...)
            // for each contact.  The string returned will be the argument s in the method onPostExecute(s)
            // below, and in that method we shall update the screen view to display the list of
            // contacts.

            return processContacts();
        }

        // This method executed before the thread run by doInBackground.  It runs on the main UI thread,
        // so we can touch the UI views from here.

        @Override
        protected void onPreExecute() {

            // Hide the textview and display the progress bar while thread running
            tv.setVisibility(TextView.INVISIBLE);
            progressBar.setVisibility(ProgressBar.VISIBLE);

        }

        // This method executed after the thread run by doInBackground has returned. The variable s
        // passed is the string value returned by doInBackground.  This method executes on
        // the main UI thread, so we can update the view tv and the ProgressBar progressBar
        // from it.

        @Override
        protected void onPostExecute(String s) {

            // Append the list of contacts to the TextView
            tv.append(s);

            // Stop the progress bar and make the TextView visible
            progressBar.setVisibility(View.GONE);
            tv.setVisibility(TextView.VISIBLE);

        }
    }
}
