package systems.sieber.vscreensaver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import androidx.appcompat.content.res.AppCompatResources;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Download {

    interface DownloadFinishedListener {
        void finished(boolean success);
    }

    DownloadFinishedListener mFinishedListener;
    Context mContext;
    Download(Context c, DownloadFinishedListener dfl) {
        this.mContext = c;
        this.mFinishedListener = dfl;
    }

    void download(String url, String dest, boolean informSuccess) {
        ProgressDialog mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.downloading));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        final DownloadTask downloadTask = new DownloadTask(mContext, mProgressDialog, informSuccess);
        downloadTask.execute(url, dest);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }

    void infoDialog(String title, String text, boolean success) {
        final AlertDialog.Builder dlg = new AlertDialog.Builder(mContext);
        if(title != null) dlg.setTitle(title);
        if(text != null) dlg.setMessage(text);
        if(success)
            dlg.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_tick_green_24dp));
        else
            dlg.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fail_red_36dp));
        dlg.setPositiveButton(mContext.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dlg.setCancelable(true);
        dlg.create().show();
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        final private ProgressDialog mProgressDialog;
        final private Context mContext;
        final private boolean mInformSuccess;
        //private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context, ProgressDialog pd, boolean informSuccess) {
            this.mContext = context;
            this.mProgressDialog = pd;
            this.mInformSuccess = informSuccess;
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength() / 1024;

                // download the file
                input = connection.getInputStream();
                StorageControl sc = new StorageControl(mContext);
                output = new FileOutputStream(sc.getStorage(params[1]));

                byte[] data = new byte[4096];
                int total = 0;
                int count;
                while((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if(isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if(fileLength > 0) // only if total length is known
                        publishProgress(total / 1024, fileLength);
                    output.write(data, 0, count);
                }
            } catch(Exception e) {
                return e.toString();
            } finally {
                try {
                    if(output != null)
                        output.close();
                    if(input != null)
                        input.close();
                } catch(IOException ignored) {}

                if(connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            /*PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();*/
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(progress[1]);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            //mWakeLock.release();
            mProgressDialog.dismiss();
            if(result != null) {
                infoDialog(mContext.getString(R.string.error), result, false);
                if(mFinishedListener != null) mFinishedListener.finished(false);
            } else {
                if(mInformSuccess) infoDialog(mContext.getString(R.string.finished), mContext.getString(R.string.download_success), true);
                if(mFinishedListener != null) mFinishedListener.finished(true);
            }
        }
    }
}
