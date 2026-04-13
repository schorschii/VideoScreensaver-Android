package systems.sieber.vscreensaver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class StorageControl {

    public static String FILENAME_VIDEO = "video.mp4";

    private final Context mContext;

    StorageControl(Context c) {
        mContext = c;
    }

    File getStorage(String filename) {
        File exportDir = mContext.getExternalFilesDir(null);
        return new File(exportDir, filename);
    }

    void processFile(String path, Intent data) {
        if(data == null || data.getData() == null) return;
        try {
            File fl = getStorage(path);
            InputStream inputStream = mContext.getContentResolver().openInputStream(data.getData());
            byte[] targetArray = new byte[inputStream.available()];
            inputStream.read(targetArray);
            FileOutputStream stream = new FileOutputStream(fl);
            stream.write(targetArray);
            stream.flush();
            stream.close();
            scanFile(fl);
        } catch(Exception ignored) { }
    }
    void removeFile(String path) {
        try {
            File fl = getStorage(path);
            fl.delete();
            scanFile(fl);
        } catch(Exception ignored) { }
    }
    boolean existsFile(String path) {
        try {
            File fl = getStorage(path);
            return fl.exists();
        } catch(Exception ignored) { }
        return false;
    }

    void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        mContext.sendBroadcast(scanFileIntent);
    }

}
