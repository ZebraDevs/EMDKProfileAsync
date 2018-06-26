package com.pietromaggi.sample.profileasync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKException;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;

public class MainActivity extends AppCompatActivity implements EMDKManager.EMDKListener, EMDKManager.StatusListener {
    private final String profileName = "ClockProfile"; // profile name used in EMDKConfig.xml

    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.tvStatus);
        if (null == statusTextView) {
            Toast.makeText(this, "Error: TextView reference is null", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Toast.makeText(this, "EMDKManager object creation failed", Toast.LENGTH_SHORT).show();
            this.finish();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Clean up the objects created by EMDK manager
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        statusTextView.setText("EMDK open success.");

        this.emdkManager = emdkManager;

        // Get the ProfileManager object to process the profiles
        try {
            emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.PROFILE, this);
        } catch (EMDKException e) {
            e.printStackTrace();
        }
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

    @Override
    public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
        if ((EMDKResults.STATUS_CODE.SUCCESS != statusData.getResult()) ||
                (EMDKManager.FEATURE_TYPE.PROFILE != statusData.getFeatureType()) ||
                (EMDKManager.FEATURE_TYPE.PROFILE != emdkBase.getType())){

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTextView.setText("Error: The Profile has not been sent for processing...");
                }
            });

            return;
        }

        ProfileManager profileManager = (ProfileManager) emdkBase;

        if (profileManager != null) {
            String[] modifyData = new String[1];

            profileManager.addDataListener(new ProfileManager.DataListener() {
                @Override
                public void onData(ProfileManager.ResultData resultData) {
                    String resultString = "Error Applying profile";
                    EMDKResults results = resultData.getResult();

                    //Check the return status of processProfile
                    if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
                        resultString = "Profile Applied Successfully";
                    } else if (results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {
                        resultString = parseXML(results.getStatusString());
                    }

                    final String finalResultString = resultString;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText(finalResultString);
                        }
                    });
                }
            });

            EMDKResults results = profileManager.processProfileAsync(profileName,
                    ProfileManager.PROFILE_FLAG.SET, modifyData);

            if (results.statusCode != EMDKResults.STATUS_CODE.PROCESSING) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusTextView.setText("Error: The Profile has not been sent for processing...");
                    }
                });
            }
        }
    }
}
