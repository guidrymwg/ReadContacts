package com.lightcone.readcontacts;

import android.os.AsyncTask;
import android.os.Bundle;
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

        // Call a check for runtime permissions here

        // Check that external media available and writable
        checkExternalMedia();

        // Read the contacts and output to a file.  Process this on a background
        // thread defined by an instance of AsyncTask because it will typically
        // take several seconds to process a few hundred contacts. We will display
        // an indeterminate progress bar to the user while the contacts are being
        // processed.

        new BackgroundProcessor().execute();

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
