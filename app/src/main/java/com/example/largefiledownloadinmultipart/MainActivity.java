package com.example.largefiledownloadinmultipart;

import android.content.Context;
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
import java.util.concurrent.Callable;
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
    String url = "https://skysite-temp.s3-us-west-1.amazonaws.com/SS-PRD/167157/11694352/TempZIP/3c86a0fa-57cc-4ace-8bcf-b58b280bd9fb.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA3CFE5YOSHYRV7V7X%2F20191119%2Fus-west-1%2Fs3%2Faws4_request&X-Amz-Date=20191119T112556Z&X-Amz-Expires=518399&X-Amz-SignedHeaders=host&X-Amz-Signature=f808b9ba0b4782d003af40d4ec2f2489dca35c75cdd90daf05ac4870ed7ebb95";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadListener = this;
        startTime = System.currentTimeMillis();
        new DownloadTask(this).execute(url, "0", "1", "1");
        mImageView = findViewById(R.id.iv);
        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };

        try {
            callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void threadInitiator() {
        Log.d("msg", "Available Thread: " + Runtime.getRuntime().availableProcessors());
        ExecutorService executorService = Executors.newFixedThreadPool(50);//Runtime.getRuntime().availableProcessors());
        do {
            executorService.execute(new DownloaderRunnable(url, startSize, endSize, count, this));
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
            new DownloadTask(this).execute(url, "" + startSize, "" + endSize, "" + count);
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
                    //total_size = remaining_download_size = Long.valueOf(responseHeaders.get("x-goog-stored-content-length").get(0));
                    total_size = remaining_download_size = Long.valueOf(responseHeaders.get("Content-Range").get(0).substring(10));
                    fileNameWithExtension = "AWSZipFile"; //responseHeaders.get("Content-Disposition").get(0);
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
                if (sUrl[3].length() > 1) {
                    m_path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName + "/Image" + sUrl[3]).getAbsolutePath();
                } else {
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
            mFolderName = "Arnab";
            mFileExtension = ".zip";
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
        Log.d("msg", "callback");
        totalNumberOfDownloadedFiles++;
        if (totalNumberOfDownloadedFiles == count) {
            Log.d("msg", "onChunkDownloadComplete: Finished");
            new MergeFileTask().execute();
        }
    }

    @Override
    public void onFullDownloadComplete(File file) {
        Log.d("msg", "Time taken: " + Math.abs(startTime - System.currentTimeMillis()));
        /*String filePath = file.getPath();
        final Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bitmap);
            }
        });*/
    }
}
