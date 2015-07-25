package si.ox.dlnaexplorer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    // discoveryReplyList holds a copy of all discovered services
    ArrayList<String> discoveryReplyList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore the activity state
        if (savedInstanceState != null) {
            discoveryReplyList.addAll(savedInstanceState.getStringArrayList("discoveryReplyList"));
        }

        // setup layout
        setContentView(R.layout.activity_main);

        // make details TextView scrollable
        TextView details = (TextView) findViewById(R.id.textView_mainStatus);
        details.setMovementMethod(new ScrollingMovementMethod());

        // setup discoveryReplyList and it's adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, discoveryReplyList);
        adapter.addAll(discoveryReplyList);
        final ListView lvDiscoveryReplies = (ListView) findViewById(R.id.listView_DiscoveryReplies);
        lvDiscoveryReplies.setAdapter(adapter);
        //
        lvDiscoveryReplies.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // ListView Clicked item value
                String itemValue = (String) lvDiscoveryReplies.getItemAtPosition(position);

                Intent i = new Intent(MainActivity.this, DetailActivity.class);
                i.putExtra("upnpDiscoveryStr", itemValue);
                MainActivity.this.startActivity(i);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            }

        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putStringArrayList("discoveryReplyList", discoveryReplyList);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_copy_to_clipboard) {

            String text = "";
            for (String s : discoveryReplyList) {
                text += s + "\r\n";
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("discoveryReplyList", text);
            clipboard.setPrimaryClip(clip);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateStatusInUiThread(String title, String text) {
        final String title2 = title;
        final String text2 = text;
        runOnUiThread(new Runnable() {
            public void run() {
                TextView status = (TextView) findViewById(R.id.textView_mainTitle);
                TextView details = (TextView) findViewById(R.id.textView_mainStatus);

                status.setText(title2);
                details.setText(text2);

            }
        });
    }

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public void refreshButtonClick(View v) {

        // clear old data
        {
            discoveryReplyList.clear();
            ListView lvDiscoveryReplies = (ListView) findViewById(R.id.listView_DiscoveryReplies);
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) lvDiscoveryReplies.getAdapter();
            adapter.clear();
            adapter.notifyDataSetChanged();
        }

        // get new data
        new Thread(new Runnable() {
            public void run() {

                runOnUiThread(new Runnable() {
                    public void run() {
                        Button btn = (Button) findViewById(R.id.buttonRefresh);
                        btn.setEnabled(false);
                    }
                });

                String details = "";
                try {

                    updateStatusInUiThread("Sending discovery packet.", details);

                    WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiManager.MulticastLock lock = wifi.createMulticastLock("si.ox.dlnaexplorer");
                    lock.acquire();
                    try {
                        MulticastSocket serverSocket = new MulticastSocket(null);
                        try {
                            DhcpInfo dhcp = wifi.getDhcpInfo();
                            String myIP = intToInetAddress(dhcp.ipAddress).getHostAddress();

                            serverSocket.setReuseAddress(true);
                            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 1901));
                            serverSocket.setTimeToLive(4);
                            serverSocket.joinGroup(InetAddress.getByName("239.255.255.250"));
                            //serverSocket.joinGroup(
                            //       new InetSocketAddress(InetAddress.getByName("239.255.255.250"), 1900),
                            //        NetworkInterface.getByInetAddress(InetAddress.getByName(myIP)));

                            details += "\r\nMyIP = " + myIP;
                            updateStatusInUiThread("Sending discovery packet.", details);


                            // DLNA step 1: multicast SSDP M-SEARCH on UPnP multicast address 239.255.255.250 port 1900
                            {
                                String srchMsg = "M-SEARCH * HTTP/1.1\r\n" +
                                        //"Host:" + myIP + ":" + Integer.toString(serverSocket.getLocalPort()) + "\r\n" +
                                        "Host:239.255.255.250:1900\r\n" +
                                        "Man:\"ssdp:discover\"\r\n" +
                                        "MX:3\r\n" +
                                        "ST:ssdp:all\r\n" +
                                        "User-agent:si.ox.dlnaexplorer/0.1 UDAP/2.0\r\n" +
                                        "\r\n";
                                DatagramPacket packet = new DatagramPacket(
                                        srchMsg.getBytes("US_ASCII"), srchMsg.length(),
                                        InetAddress.getByName("239.255.255.250"), 1900);
                                serverSocket.send(packet);
                            }


                            details += "\r\nMy port=" + Integer.toString(serverSocket.getLocalPort());
                            updateStatusInUiThread("Waiting for response.", details);

                            serverSocket.setSoTimeout(100);

                            byte[] data = new byte[2048];
                            DatagramPacket packet = new DatagramPacket(data, data.length);

                            long startTime = System.currentTimeMillis();
                            while (System.currentTimeMillis() - startTime < 15000) {

                                try {
                                    packet.setLength(data.length);
                                    serverSocket.receive(packet);

                                    final String packetData = "Response from " +
                                        packet.getAddress().getHostAddress() +
                                        ":" + Integer.toString(packet.getPort()) +
                                        ":\r\n" +
                                        new String(packet.getData(), packet.getOffset(), packet.getLength());
                                    updateStatusInUiThread("Waiting for more responses.", details);

                                    if (!discoveryReplyList.contains(packetData)) {
                                        discoveryReplyList.add(packetData);

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                ListView lvDiscoveryReplies = (ListView) findViewById(R.id.listView_DiscoveryReplies);
                                                ArrayAdapter<String> adapter = (ArrayAdapter<String>) lvDiscoveryReplies.getAdapter();
                                                adapter.add(packetData);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });

                                    }

                                } catch (SocketTimeoutException e) {
                                }

                            }

                        } finally {
                            serverSocket.leaveGroup(InetAddress.getByName("239.255.255.250"));
                            serverSocket.close();
                        }
                    } finally {
                        lock.release();
                    }

                    details += "\r\n\r\nDone.";
                    updateStatusInUiThread("OK", details);

                } catch (IOException e) {

                    // better than: String stackTrace = Log.getStackTraceString(exception);
                    Writer writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    e.printStackTrace(printWriter);
                    details += "\r\n\r\n" + writer.toString();
                    updateStatusInUiThread("ERROR", details);

                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        Button btn = (Button) findViewById(R.id.buttonRefresh);
                        btn.setEnabled(true);
                    }
                });

            }

        }).start();
    };

}
