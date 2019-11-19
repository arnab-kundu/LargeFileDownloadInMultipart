package com.example.largefiledownloadinmultipart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements DownloadListener {

    public static final long CHUNK_DOWNLOAD_OFFSET = 3145728 / 3 * 10;//_3MB_IN_BYTES
    public long remaining_download_size = 0;
    public long total_size = 0;
    public long startSize = 0;
    public long endSize = CHUNK_DOWNLOAD_OFFSET;
    DownloadListener downloadListener;
    int totalNumberOfDownloadedFiles;
    int count = 0;
    ImageView mImageView;
    String mFolderName, mFileExtension;
    String fileNameWithExtension;
    long startTime;
String url="https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/asd.mp4?alt=media&token=20c3a6ea-f311-4ec1-a070-0169a2ec310f";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadListener = this;
        startTime = System.currentTimeMillis();
        new DownloadTask(this).execute(
                // "https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/1_li_jiang_guilin_yangshuo_2011.jpg?alt=media&token=651a98d3-8929-4578-b112-095b9055d8b6",
                //"https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/54mb.pdf?alt=media&token=0cf7ee69-4875-4e29-bbdc-44471dc83ea3",
                url,
                "0", "1", "1"
        );

        mImageView = findViewById(R.id.iv);


        //"https://skysite-temp.s3-us-west-1.amazonaws.com/SS-PRD/167157/11694352/TempZIP/3c86a0fa-57cc-4ace-8bcf-b58b280bd9fb.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA3CFE5YOSHYRV7V7X%2F20191114%2Fus-west-1%2Fs3%2Faws4_request&X-Amz-Date=20191114T093101Z&X-Amz-Expires=86399&X-Amz-SignedHeaders=host&X-Amz-Signature=9fceeca27c816115b24dca3d81e2ff118430c0d0057fc3a82a4ddfde235b3196"
        //"https://ars.els-cdn.com/content/image/1-s2.0-S0092867415012702-mmc6.pdf"
        //"https://speed.hetzner.de/100MB.bin"
        //new MergeFileTask().execute();

        //ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,10,5*60*1000, TimeUnit.MILLISECONDS,)



    }


    private void threadInitiator(){
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        do {
            executorService.execute(new DownloaderRunnable(url, startSize, endSize, count,this));
            startSize += CHUNK_DOWNLOAD_OFFSET;
            endSize += CHUNK_DOWNLOAD_OFFSET;
            if (endSize > total_size) {
                endSize = total_size;
            }
            remaining_download_size -= CHUNK_DOWNLOAD_OFFSET;
            count++;
            Log.d("msg", "count: " + count);

        } while (remaining_download_size > 0);
    }

    private void startDownload() {
        do {
            new DownloadTask(this).execute(
                    //"https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/1_li_jiang_guilin_yangshuo_2011.jpg?alt=media&token=651a98d3-8929-4578-b112-095b9055d8b6",
                    "https://firebasestorage.googleapis.com/v0/b/fir-d6ee4.appspot.com/o/54mb.pdf?alt=media&token=0cf7ee69-4875-4e29-bbdc-44471dc83ea3",
                    "" + startSize, "" + endSize, "" + count
            );
            startSize += CHUNK_DOWNLOAD_OFFSET;
            endSize += CHUNK_DOWNLOAD_OFFSET;
            if (endSize > total_size) {
                endSize = total_size;
            }
            remaining_download_size -= CHUNK_DOWNLOAD_OFFSET;
            count++;
            Log.d("msg", "count: " + count);

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
                    total_size = remaining_download_size = Long.valueOf(responseHeaders.get("x-goog-stored-content-length").get(0));
                    fileNameWithExtension = responseHeaders.get("Content-Disposition").get(0);
                    //startDownload();
                    threadInitiator();
                    return null;
                }
                mFolderName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
                mFileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf("."));
                String folder_path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName).getAbsolutePath();
                File folder_file = new File(folder_path);
                if (!folder_file.exists())
                    folder_file.mkdirs();
                String m_path;
                if( sUrl[3].length()>1) {
                    m_path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName + "/Image" + sUrl[3]).getAbsolutePath();
                }else {
                    m_path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName + "/Image0" + sUrl[3]).getAbsolutePath();
                }
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
                    Log.d("msg", "Thread:" + sUrl[3] + " " + total / (1024 * 1024) + "MB");
                    onChunkDownloadComplete();
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
            mFolderName="Arnab";
            mFileExtension=".mp4";
            String folderPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName).getAbsolutePath();
            File folder = new File(folderPath);

            String m_pathFinal = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName + "Final" + mFileExtension).getAbsolutePath();

            try {
                File finalFile = new File(m_pathFinal);
                if (!finalFile.exists()) {
                    finalFile.createNewFile();
                }

                if (folder.exists()) {
                    File[] files = folder.listFiles();
                    ArrayList<File> fileArrayList = new ArrayList<>();
                    for (File file : files) {
                        fileArrayList.add(file);
                    }
                    Collections.sort(fileArrayList);
                    FileOutputStream fileOutputStream = new FileOutputStream(finalFile);
                    for (File file : fileArrayList) {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] xy = new byte[(int) file.length()];
                        fileInputStream.read(xy);
                        fileOutputStream.write(xy);
                        fileInputStream.close();
                        //file.delete();
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    Log.d("msg", "Merge Completed");
                    onFullDownloadComplete(finalFile);
                    /*if (folder.delete()) {
                        Log.d("msg", "deleted");
                    }*/
                }
            } catch (Exception e) {
                Log.e("msg", "Exception: " + e);
            }
            return null;
        }
    }

    @Override
    public void onChunkDownloadComplete() {
        Log.d("msg","callback");
        totalNumberOfDownloadedFiles++;
        if (totalNumberOfDownloadedFiles == count) {
            Log.d("msg", "onChunkDownloadComplete: Finished");
            new MergeFileTask().execute();
        }
    }

    @Override
    public void onFullDownloadComplete(File file) {
        Log.d("msg", "Time taken: " + Math.abs(startTime - System.currentTimeMillis()));
        String filePath = file.getPath();
        final Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bitmap);
            }
        });
    }
}
