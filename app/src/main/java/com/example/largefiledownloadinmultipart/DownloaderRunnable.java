package com.example.largefiledownloadinmultipart;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class DownloaderRunnable implements Runnable, DownloadListener {


    public DownloaderRunnable(String url, long startSize, long endSize, int count) {
        this.url = url;
        this.startSize = startSize;
        this.endSize = endSize;
        this.count = count;
    }

    public String url;
    public static final long CHUNK_DOWNLOAD_OFFSET = 3145728 / 3 * 10;//_3MB_IN_BYTES
    public long remaining_download_size = 0;
    public long total_size = 0;
    public long startSize = 0;
    public long endSize = CHUNK_DOWNLOAD_OFFSET;
    DownloadListener downloadListener;
    int totalNumberOfDownloadedFiles;
    int count = 0;
    String mFolderName="Arnab", mFileExtension="pdf";
    String fileNameWithExtension;
    long startTime;


    @Override
    public void run() {


        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(DownloaderRunnable.this.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Range", "bytes=" + startSize + "-" + endSize);
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
            //mFolderName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
            //mFileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf("."));
            String folder_path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName).getAbsolutePath();
            File folder_file = new File(folder_path);
            if (!folder_file.exists())
                folder_file.mkdirs();
            String m_path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + File.separator + "/" + mFolderName + "/Image" + count).getAbsolutePath();
            File file = new File(m_path);
            if (!file.exists())
                file.createNewFile();
            output = new FileOutputStream(file);

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                Log.d("msg", "Thread:" + DownloaderRunnable.this.count + " " + total / (1024 * 1024) + "MB");
                onChunkDownloadComplete();
            }
        } catch (Exception e) {
            Log.e("msg", "" + e);
            return;
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
        return;
    }

    @Override
    public void onChunkDownloadComplete() {
        Log.d("msg", "onChunkDownloadComplete: "+count);
    }

    @Override
    public void onFullDownloadComplete(File file) {

    }
}
