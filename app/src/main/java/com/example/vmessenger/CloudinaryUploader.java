package com.example.vmessenger;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CloudinaryUploader {
    private static final String CLOUD_NAME = "dfqd4mj03";
    private static final String UPLOAD_PRESET = "memorychat_upload";
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }
    public static void uploadMedia(Context context, Uri fileUri, UploadCallback callback) {

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String mimeType = context.getContentResolver().getType(fileUri);

                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }

                // Cloudinary auto detects image/video/raw file type.
                String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/auto/upload";

                URL url = new URL(uploadUrl);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                String boundary = "----MemoryChatBoundary" + System.currentTimeMillis();

                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                outputStream.writeBytes("--" + boundary + "\r\n");

                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");

                outputStream.writeBytes(UPLOAD_PRESET + "\r\n");

                outputStream.writeBytes("--" + boundary + "\r\n");

                outputStream.writeBytes(
                        "Content-Disposition: form-data; "
                                + "name=\"file\"; filename=\"memorychat_media\"\r\n"
                );

                outputStream.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);

                if (inputStream == null) {
                    throw new Exception("Unable to open selected file");
                }

                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                bufferedInputStream.close();
                outputStream.writeBytes("\r\n");
                outputStream.writeBytes("--" + boundary + "--\r\n");
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();

                InputStream responseStream;
                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = connection.getInputStream();
                } else {
                    responseStream = connection.getErrorStream();
                }

                StringBuilder responseBuilder = new StringBuilder();

                if (responseStream != null) {
                    byte[] responseBuffer = new byte[4096];
                    int read;
                    while ((read = responseStream.read(responseBuffer)) != -1) {
                        responseBuilder.append(new String(responseBuffer, 0, read));
                    }
                    responseStream.close();
                }

                String response = responseBuilder.toString();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(response);

                    String secureUrl = json.getString("secure_url");

                    new Handler(
                            Looper.getMainLooper()
                    ).post(() ->
                            callback.onSuccess(secureUrl)
                    );

                } else {
                    new Handler(
                            Looper.getMainLooper()
                    ).post(() ->
                            callback.onFailure(
                                    "Cloudinary upload failed: "
                                            + response
                            )
                    );
                }

            } catch (Exception e) {
                new Handler(
                        Looper.getMainLooper()
                ).post(() ->
                        callback.onFailure(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Upload failed"
                        )
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}
