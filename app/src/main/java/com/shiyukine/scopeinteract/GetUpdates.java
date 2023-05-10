package com.shiyukine.scopeinteract;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class GetUpdates extends AsyncTask<Object, Void, String>
{

    protected void onPreExecute() {
        //display progress dialog.

    }
    protected String doInBackground(Object... params) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Shiyukine/Shiyukine/main/serv.txt");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            int responseCode = urlConnection.getResponseCode();
            Log.e("djqdhquifhdqsdhuf", "GET Response Code :: " + responseCode);
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    return line;
                }
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }



    protected void onPostExecute(String result) {
        // dismiss progress dialog and update ui
    }
}