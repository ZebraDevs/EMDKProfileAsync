package com.pietromaggi.sample.profileasync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.TextView;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;

public class MainActivity extends AppCompatActivity implements EMDKManager.EMDKListener {
    private final String profileName = "ClockProfile"; // profile name used in EMDKConfig.xml

    private ProfileManager profileManager = null;
    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.tvStatus);

        //Set default values for time zone, date and time

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        } else {
            //EMDKManager object creation failed
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Clean up the objects created by EMDK manager
        if (profileManager != null) {
            profileManager = null;
        }

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        String[] modifyData = new String[1];
        String resultString = "Error Applying profile";


        statusTextView.setText("EMDK open success.");

        this.emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

        //Call process profile to modify the profile of specified profile name
        EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, modifyData);

        //Check the return status of processProfile
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            resultString = "Profile Applied Successfully";
        } else if (results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {
            resultString = parseXML(results.getStatusString());
        }

        statusTextView.setText(resultString);

    }

    @Override
    public void onClosed() {
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

        statusTextView.setText("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
    }


    // Method to parse the XML response using XML Pull Parser
    public String parseXML(String statusXMLResponse) { // XmlPullParser myParser) {

        // XML handling string
        String errorType = "";
        String parmName = "";
        String errorDescription = "";
        String errorString = "";
        String resultString = "";
        int event;


        try {
            // Create instance of XML Pull Parser to parse the response
            XmlPullParser parser = Xml.newPullParser();
            // Provide the string response to the String Reader that reads
            // for the parser
            parser.setInput(new StringReader(statusXMLResponse));

            // Call method to parse the response
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:

                        if (name.equals("parm-error")) {
                            parmName = parser.getAttributeValue(null, "name");
                            errorDescription = parser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";
                            return "Profile update failed." + errorString;
                        }
                        if (name.equals("characteristic-error")) {
                            errorType = parser.getAttributeValue(null, "type");
                            errorDescription = parser.getAttributeValue(null, "desc");
                            errorString = " (Type: " + errorType + ", Error Description: " + errorDescription + ")";
                            return "Profile update failed." + errorString;
                        }
                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }
                event = parser.next();

                if (TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription)) {
                    resultString = "Profile update success.";
                } else {

                    resultString = "Profile update failed." + errorString;
                }
            }
        } catch (XmlPullParserException e) {
            resultString = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultString;
    }
}
