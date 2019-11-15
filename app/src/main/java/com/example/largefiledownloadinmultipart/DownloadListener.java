package com.example.largefiledownloadinmultipart;

import java.io.File;

public interface DownloadListener {
    void onChunkDownloadComplete();

    void onFullDownloadComplete(File file);
}
