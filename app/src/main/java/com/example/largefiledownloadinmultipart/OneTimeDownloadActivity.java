package com.example.largefiledownloadinmultipart;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class OneTimeDownloadActivity extends AppCompatActivity {

    private long startTime;
    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_time_download);
        iv = findViewById(R.id.iv);
        startTime = System.currentTimeMillis();
        new DownloadTask(this).execute(
                "https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/1_li_jiang_guilin_yangshuo_2011.jpg?alt=media&token=651a98d3-8929-4578-b112-095b9055d8b6"
                //"https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/54mb.pdf?alt=media&token=0cf7ee69-4875-4e29-bbdc-44471dc83ea3"
        );

    }


    // usually, subclasses of AsyncTask are declared inside the activity class.
    // that way, you can easily modify the UI thread from here
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private ProgressDialog progressDialog;
        DecimalFormat df = new DecimalFormat("0.00");

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage("Download Speed:");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Image.jpg");
                if (!file.exists()) {
                    file.createNewFile();
                }
                output = new FileOutputStream(file);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                long one_sec_offset = System.currentTimeMillis();
                float file_downloaded_per_sec = 0;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) { // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                        progressDialog.setProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                    //Log.d("msg", "downloading: " + count);
                    file_downloaded_per_sec += data.length;
                    String speed;
                    if (one_sec_offset <= System.currentTimeMillis() - 1000) {
                        one_sec_offset = System.currentTimeMillis();
                        if (file_downloaded_per_sec / (1024 * 1024) > 1) {
                            Log.d("msg", df.format(file_downloaded_per_sec / (1024 * 1024)) + " MB/s");
                            speed = df.format(file_downloaded_per_sec / (1024 * 1024)) + " MB/s";
                        } else if (file_downloaded_per_sec / 1024 > 1) {
                            Log.d("msg", file_downloaded_per_sec / 1024 + " KB/s");
                            speed = file_downloaded_per_sec / 1024 + " KB/s";
                        } else {
                            Log.d("msg", file_downloaded_per_sec + " Byte/s");
                            speed = file_downloaded_per_sec + " Byte/s";
                        }
                        progressDialog.setMessage("Download Speed: " + speed);
                        file_downloaded_per_sec = 0;
                    }
                }
                Log.d("msg", "Time taken" + (System.currentTimeMillis() - startTime));
            } catch (Exception e) {
                Log.e("msg", "" + e);
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Image.jpg";
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            iv.setImageBitmap(bitmap);
        }
    }
}