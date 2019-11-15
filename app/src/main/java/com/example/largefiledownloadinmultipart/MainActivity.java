package com.example.largefiledownloadinmultipart;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final long CHUNK_DOWNLOAD_OFFSET = 3145728;//_3MB_IN_BYTES
    public long remaining_download_size = 0;
    public long startSize = 0;
    public long endSize = CHUNK_DOWNLOAD_OFFSET;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*new DownloadTask(this).execute(
                "https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/1_li_jiang_guilin_yangshuo_2011.jpg?alt=media&token=651a98d3-8929-4578-b112-095b9055d8b6",
                "0", "1","1"
        );*/
        //"https://skysite-temp.s3-us-west-1.amazonaws.com/SS-PRD/167157/11694352/TempZIP/3c86a0fa-57cc-4ace-8bcf-b58b280bd9fb.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA3CFE5YOSHYRV7V7X%2F20191114%2Fus-west-1%2Fs3%2Faws4_request&X-Amz-Date=20191114T093101Z&X-Amz-Expires=86399&X-Amz-SignedHeaders=host&X-Amz-Signature=9fceeca27c816115b24dca3d81e2ff118430c0d0057fc3a82a4ddfde235b3196"
        //"https://ars.els-cdn.com/content/image/1-s2.0-S0092867415012702-mmc6.pdf"
        //"https://speed.hetzner.de/100MB.bin"
        new MergeFileTask().execute();
    }

    private void startDownload(){
        int count = 1;
        do {
            new DownloadTask(this).execute(
                    "https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/1_li_jiang_guilin_yangshuo_2011.jpg?alt=media&token=651a98d3-8929-4578-b112-095b9055d8b6",
                    "" + startSize, "" + endSize, "" + count
            );
            startSize += CHUNK_DOWNLOAD_OFFSET;
            endSize += CHUNK_DOWNLOAD_OFFSET;
            remaining_download_size -= CHUNK_DOWNLOAD_OFFSET;
            count++;
            Log.d("msg", "count: "+count);

        } while (remaining_download_size > 0);

    }

    // usually, subclasses of AsyncTask are declared inside the activity class.
    // that way, you can easily modify the UI thread from here
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.addRequestProperty("Range", "bytes=" + sUrl[1] + "-" + sUrl[2]);
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
               /* if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }*/

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                Log.d("msg", "fileLength" + fileLength);
                // download the file
                input = connection.getInputStream();
                Map<String, List<String>> responseHeaders = connection.getHeaderFields();
                if (remaining_download_size == 0) {
                    remaining_download_size = Long.valueOf(responseHeaders.get("x-goog-stored-content-length").get(0));
                    startDownload();
                    return null;
                }
                String folder_path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM /*+ File.separator*/ + "/Arnab").getAbsolutePath();
                File folder_file = new File(folder_path);
                if (!folder_file.exists())
                    folder_file.mkdirs();
                String m_path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM + "/Arnab/Image" + sUrl[3] + ".jpg").getAbsolutePath();
                File file = new File(m_path);
                if (!file.exists())
                    file.createNewFile();
                output = new FileOutputStream(file);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                    Log.d("msg", "" + total / (1024 * 1024) + "MB");
                }
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
    }

    class MergeFileTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String m_path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + "/Arnab.jpg").getAbsolutePath();
            String m_path1 = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + "/Arnab1.jpg").getAbsolutePath();
            String m_pathFinal = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + "/ArnabFinal.jpg").getAbsolutePath();
            try {
                File finalFile = new File(m_pathFinal);
                if (!finalFile.exists()) {
                    finalFile.createNewFile();
                }

                File file1 = new File(m_path);
                if (!file1.exists()) {
                    file1.createNewFile();
                }
                File file2 = new File(m_path1);
                if (!file2.exists()) {
                    file2.createNewFile();
                }



                FileOutputStream fileOutputStream = new FileOutputStream(finalFile);


                FileInputStream fileInputStream = new FileInputStream(file1);
                byte[] xy = new byte[(int) file1.length()];
                fileInputStream.read(xy);
                fileOutputStream.write(xy);
                fileInputStream.close();

                FileInputStream fileInputStream2 = new FileInputStream(file2);
                byte[] xy2 = new byte[(int) file2.length()];
                fileInputStream2.read(xy2);
                fileOutputStream.write(xy2);
                fileInputStream2.close();

                fileOutputStream.close();
            } catch (Exception e) {
                Log.e("msg", "" + e);
            }

            return null;
        }
    }


}
