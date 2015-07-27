package si.ox.dlnaexplorer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class DetailActivity extends ActionBarActivity implements ActionBar.TabListener {

    String upnpResponse = "";
    String httpResponse = "";
    String deviceInfo = "";
    boolean detailsRequested = false;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    /**
     * InfoFragment controls
     */
    TextView info = null;

    /**
     * RawDataFragment controls
     */
    TextView details = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // params from parent activity
        Bundle extras = getIntent().getExtras();
        upnpResponse = extras.getString("upnpDiscoveryStr");

        // restore the activity state
        if (savedInstanceState != null) {
            httpResponse = savedInstanceState.getString("httpResponse");
        }

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        // force create all tabs, so we can reference their widgets from here on
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount());

        // now we have to wait for the pages to be created...
        // continued in mSectionsPagerAdapter.finishUpdate()

    }

    private void RequestDetails() {
        detailsRequested = true;

        // extract URL
        Pattern p = Pattern.compile("^Location:(.*?)$", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
        Matcher m = p.matcher(upnpResponse);
        String url = "";
        if (m.find())
            url = m.group(1).trim();

        // Location URL might not actually be a URL
        if (url.toLowerCase().startsWith("http")) {

            httpResponse = "waiting for http response from:\n" + url;
            if (details != null)
                details.setText(upnpResponse + "\n\n" + httpResponse);

            new RequestTask().execute(url);

        } else {

            httpResponse = "Location is not a URL: " + url;
            if (details != null)
                details.setText(upnpResponse + "\n\n" + httpResponse);

        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString("httpResponse", httpResponse);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {

            // make UP button behave like BACK button instead
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_copy_to_clipboard:

                //String text = (String) details.getText();
                String text = upnpResponse + "\n\n" + httpResponse;

                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("upnpHttpResponse", text);
                clipboard.setPrimaryClip(clip);

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return new InfoFragment();
                case 1:
                    return new RawDataFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.detail_info_title).toUpperCase(l);
                case 1:
                    return getString(R.string.detail_raw_title).toUpperCase(l);
            }
            return null;
        }

        @Override
        public void finishUpdate(ViewGroup container){
            super.finishUpdate(container);

            // tabs changed... update our references to various widgets on tab pages

            details = (TextView) findViewById(R.id.textView_detailBody);
            info = (TextView) findViewById(R.id.textView_detail_info);

            // also, request device info .xml if not done so already
            if (!detailsRequested)
                RequestDetails();

        }
    }


    public static class InfoFragment extends Fragment {

        public InfoFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_detail_info_fragment, container, false);
            return rootView;
        }
    }


    public static class RawDataFragment extends Fragment {

        public RawDataFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_detail_raw_fragment, container, false);

            // make details TextView scrollable
            TextView details = (TextView) rootView.findViewById(R.id.textView_detailBody);
            details.setMovementMethod(new ScrollingMovementMethod());
            //details.setText(upnpResponse + "\n\n" + httpResponse);

            return rootView;
        }
    }


    class RequestTask extends AsyncTask<String, String, String> {

        String xmlUrl = "";

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                xmlUrl = uri[0];  // save for later
                response = httpclient.execute(new HttpGet(xmlUrl));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        protected void ExtractDeviceInfo(String xmlRespose, String xmlUrl) {
            deviceInfo = "";
            try {

                InputStream inputStream = new ByteArrayInputStream(xmlRespose.getBytes("UTF-8"));

                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document document = builder.parse(inputStream);
                XPath xpath = XPathFactory.newInstance().newXPath();
                Node node = null, node2 = null;
                NodeList nodeList = null;

                node = (Node) xpath.evaluate("/root/device/friendlyName", document, XPathConstants.NODE);
                if (node != null)
                    deviceInfo += "\nFriendly name: " + node.getTextContent();

                node = (Node) xpath.evaluate("/root/device/modelName", document, XPathConstants.NODE);
                if (node != null)
                    deviceInfo += "\nModel name: " + node.getTextContent();

                node = (Node) xpath.evaluate("/root/device/modelNumber", document, XPathConstants.NODE);
                if (node != null)
                    deviceInfo += "\nModel number: " + node.getTextContent();

                node = (Node) xpath.evaluate("/root/device/deviceType", document, XPathConstants.NODE);
                if (node != null)
                    deviceInfo += "\nDevice type: " + node.getTextContent();

                node = (Node) xpath.evaluate("/root/device/manufacturer", document, XPathConstants.NODE);
                if (node != null)
                    deviceInfo += "\nManufacturer: " + node.getTextContent();

                nodeList = (NodeList) xpath.evaluate("/root/device/serviceList/service", document, XPathConstants.NODESET);
                deviceInfo += "\n\nServices (" + Integer.toString(nodeList.getLength()) + "): ";
                for (int i = 0; i < nodeList.getLength(); i++) {
                    node = nodeList.item(i);

                    deviceInfo += "\n";

                    node2 = (Node) xpath.evaluate("serviceType", node, XPathConstants.NODE);
                    if (node2 != null)
                        deviceInfo += "\nService type: " + node2.getTextContent();

                    node2 = (Node) xpath.evaluate("SCPDURL", node, XPathConstants.NODE);
                    if (node2 != null) {
                        String scpdurl = node2.getTextContent();
                        if (!scpdurl.toLowerCase().startsWith("http")) {
                            String rootUrl = "";
                            String[] urlParts = xmlUrl.split("/");
                            if (urlParts.length > 3) {
                                rootUrl = urlParts[0] + "//" + urlParts[2];
                                scpdurl = rootUrl + scpdurl;
                            }
                        }
                        deviceInfo += "\nSCPD: " + scpdurl;
                    }

                }



            } catch (Exception e) {
                deviceInfo += "\n\nException: " + e.toString() + " " + e.getMessage();
            }
            if (deviceInfo == "")
                deviceInfo = "no info found.";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..

            httpResponse = result;
            ExtractDeviceInfo(result, xmlUrl);  // updates: deviceInfo

            runOnUiThread(new Runnable() {
                public void run() {

                    details.setText(upnpResponse + "\n\n" + httpResponse);
                    info.setText(deviceInfo);

                }
            });

        }
    }

}
