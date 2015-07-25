package si.ox.dlnaexplorer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailActivity extends ActionBarActivity {

    String upnpResponse = "";
    String httpResponse = "";

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

        // make details TextView scrollable
        details = (TextView) findViewById(R.id.textView_detailBody);
        details.setMovementMethod(new ScrollingMovementMethod());

        details.setText(upnpResponse + "\n\n" + httpResponse);

        RequestDetails();

    }

    private void RequestDetails() {

        // extract URL
        Pattern p = Pattern.compile("^Location:(.*?)$", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
        Matcher m = p.matcher(upnpResponse);
        String url = "";
        if (m.find())
            url = m.group(1).trim();

        httpResponse = "waiting for http response from:\n" + url;
        details.setText(upnpResponse + "\n\n" + httpResponse);

        new RequestTask().execute(url);
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


    class RequestTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
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

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..

            final String res = result;

            runOnUiThread(new Runnable() {
                public void run() {

                    httpResponse = res;
                    details.setText(upnpResponse + "\n\n" + httpResponse);

                }
            });

        }
    }

}
