package com.peng.power.memo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.peng.power.memo.manager.DEFINE;
//import com.peng.hanil.safetypatrol.app.Global;
//import com.peng.hanil.safetypatrol.app.UserInfo;
//import com.peng.hanil.safetypatrol.constant.CONSTANT;
//import com.peng.hanil.safetypatrol.constant.FD_FILE;
//import com.peng.hanil.safetypatrol.constant.NETWORK;
//import com.peng.hanil.safetypatrol.itf.AsyncTask;
//import com.peng.hanil.safetypatrol.network.RetroficClient;
//import com.peng.hanil.safetypatrol.network.data.FileUploadResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileManager {
    private static final String TAG = "FileManager";

    // 현재 업로드 중인지 검사 변수
    private static boolean mIsUploading = false;

    /**
     * Insert 해야하는 내용 찾아서 File upload
     *
     * @param context - context
     */
    public static void findInsertUploadFile(Context context) {
        String uploadFilePath = Global.getInsertDirPath();
        File uploadFiles = new File(uploadFilePath);
        File[] fileList = uploadFiles.listFiles();

        if (fileList != null) { // 업로드 디렉터리 검사
            for (File f : fileList) {
//                DEBUG.d("# find f name : " + f.getPath());
                if (f.isDirectory()) {
                    // 한번에 하나의 파일만 업로드 하도록 요청
                    uploadInsertFiles(context, f);
                    return;
                }
            }

        }
    }
        /**
         * 디렉터리 아래 모든 파일 업로드
         * API - insertPatrolFiles
         *
         * @param dirs    - 업로드 하려는 파일 폴더
         */
    public static void uploadInsertFiles(Context context, File dirs) {
        if (mIsUploading)   return; // 업로드 중인경우 return

////        DEBUG.d("# findUploadFilePath");
//        ArrayList<MultipartBody.Part> multipartList = new ArrayList<>();
//        // upload 시도 할 파일 리스트
//        ArrayList<File> uploadRequestFiles = new ArrayList<>();
//
//        // upload 해야하는 파일들이 있는 경로
////        DEBUG.d("uploadFilePath : " + dirs.getPath());
//
//        getUploadFileLists(uploadRequestFiles, dirs.getPath());
//
//        // 해당 경로에 파일이 없다면 삭제 후 업로드 파일 검사
//        if (uploadRequestFiles.size() == 0) {
//            allDeleteFiles(dirs.getPath());
//            findInsertUploadFile(context);
//            return;
//        }
//
//        for (File f : uploadRequestFiles) {
////            DEBUG.d("file list : " + f.getName());
////            DEBUG.d("file len : " + f.length());
//            // Origin Wav 파일 삭제 되지 않고 이동 된 경우 해당 파일 삭제 및 제외 후 업로드
//            if (f.getName().equals(DEFINE.SPEECH_ORIGIN_FILE_NAME + DEFINE.WAV_FORMAT))
//                deleteFile(f.getPath());
//            else {
//                // Uri 타입의 파일경로를 가지는 RequestBody 객체 생성
//                RequestBody fileRequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), f);
//                MultipartBody.Part filePart = MultipartBody.Part.createFormData("uploadFiles", f.getName(), fileRequestBody);
//                multipartList.add(filePart);
//            }
//        }
//
//        Completable.create(emitter -> {
////            DEBUG.d("# uploadInsertFiles");
//            mIsUploading = true;
//            // 해당 함수 내에서 멀티파트 전용 업로드 위한 RetroficClient 객체 싱글톤 사용하지 않음
//            AsyncTask.mDisposable.add(RetroficClient.createService(HttpUrl.parse(UserInfo.getUserInfo().server_base_url + DEFINE.URL_PRE_FIX), false).insertPatrolFiles(multipartList)
//                    .subscribeOn(Schedulers.newThread()) // 새로운 스레드에서 통신한다.
//                    .observeOn(AndroidSchedulers.mainThread()) // 응답 값을 가지고 ui update를 하기 위해 필요함, 메인스레드와 소통하기 위
//                    .subscribeWith(new DisposableSingleObserver<ResponseBody>() {
//                        @Override
//                        public void onSuccess(@NonNull ResponseBody response) {
////                            DEBUG.d("# onSuccess");
//                            try {
//                                final String body = response.string();
//
//                                // key - data
//                                final JSONObject json = new JSONObject(body);
//                                if (json.has(DEFINE.RESULT_CODE)) {
//                                    int resultCode = json.getInt("resultCode");
////                                    DEBUG.d("# resultCode : " + resultCode);
//                                    final JSONObject data = json.getJSONObject(DEFINE.RESPONSE_KEY_DATA);
//
//                                    ArrayList<FileUploadResult> successFileList = new Gson().fromJson(
//                                            data.getJSONArray("upload_result_list").toString(),
//                                            new TypeToken<ArrayList<FileUploadResult>>() {
//                                            }.getType());
//                                    for (FileUploadResult uploadResult : successFileList) {
//                                        /**
//                                         * 파일 업로드 성공 한 경우나, 재요청 안해야 하는 경우 - 파일 삭제
//                                         * 그 외 경우 파일 삭제 하지 않고 재 업로드
//                                         */
//                                        if (uploadResult.success == DEFINE.FILE_UPLOAD_VAULE_SUCCESS ||
//                                                uploadResult.request_again == DEFINE.FILE_UPLOAD_VAULE_NON_RETRY)
//                                            deleteFile(dirs.getPath() + "/" + uploadResult.file_name);
//                                    }
//                                    emitter.onComplete();
//                                }
//                            } catch (Exception e) {
//                                emitter.onError(e);
//                            }
//                        }
//
//                        @Override
//                        public void onError(@NonNull Throwable e) {
//                            emitter.onError(e);
//                        }
//                    })
//            );
//        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doFinally(() -> {
//            mIsUploading = false;
//            findInsertUploadFile(context);
//        }).subscribe(() -> {
////            DEBUG.d("# upload success!");
//        }, throwable -> {
////            DEBUG.logFile(throwable.toString());
//        });
    }

    /**
     * 업로드 할 파일 add
     *
     * @param file    - update arraylist
     * @param dirPath search file dir
     */
    public static void getUploadFileLists(ArrayList<File> file, String dirPath) {
        File uploadFilePath = new File(dirPath);
        File[] fileList = uploadFilePath.listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isDirectory()) { // 디렉터리를 제외한 파일 검사
                    getUploadFileLists(file, f.getPath());
                } else {
                    file.add(f);
//                    DEBUG.d("# file path : " + f.getPath());
                }
            }
        }
    }

    /**
     * 전달 받은 Path에 대한 Wav 파일 리스트 조회
     *
     * @param dirPath search file dir
     */
    public static ArrayList<String> getWavFileList(String dirPath) {
//        DEBUG.d("# dirPath : " + dirPath);
        File path = new File(dirPath);
        File[] fileList = path.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        if (fileList != null) {
            for (File f : fileList) {
                if (!f.isDirectory() && f.getPath().contains(".wav")) { // 디렉터리를 제외한 파일 검사
//                    DEBUG.d("# getTypeOfFileList file name : " + f.getName());
                    fileNames.add(f.getName());
                }
            }
        }
        return fileNames;
    }

    /**
     * 해당 경로 하위 디렉터리까지 모든 파일 삭제
     *
     * @param dirPath delete file dir
     */
    public static void allDeleteFiles(String dirPath) {
        File rootFilePath = new File(dirPath);
        File[] fileList = rootFilePath.listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isDirectory()) {
                    // 디렉터리인 경우 해당 dir 아래 파일 삭제
                    f.delete();
                    allDeleteFiles(f.getPath());
                } else  // 파일 인 경우 파일 삭제
                    f.delete();
            }
        }
        if (rootFilePath != null) rootFilePath.delete();
    }

    // 해당 경로 모든 파일 삭제
    public static void deleteFiles(String dir) {
        File path = new File(dir);

        if (!path.exists())
            path.mkdirs();

        for (File f : path.listFiles()) {
//            DEBUG.d("파일 이름 : " + f.getName());
            if (!f.isDirectory())
                if (!f.delete()){

                }
//                    DEBUG.d("삭제 실패");
        }
    }

    // 해당 경로 파일 삭제
    public static void deleteFile(String filePath) {
        File file = new File(filePath);

        if (file != null) {
            if (!file.delete()){

            }
//                DEBUG.d("삭제 실패");
        }
    }

    /**
     * 파일 리스트 getter
     *
     * @param dirPath dir path
     * @return file list path
     */
    public static File[] getFileList(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir.listFiles();
    }

    /**
     * 중복된 파일 있는 경우 기존 파일 지우고 복사
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void copy(File origin, File dest) {
        try {
            if (dest.exists()) {
                fileDelete(dest.getPath());
            }
            Files.copy(origin.toPath(), dest.toPath());
            origin.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 중복된 파일 있는 경우 파일 이름 변경해서 추가
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void copyOverlabAdd(File origin, File dest) {
        try {
            if (dest.exists()) {
                dest = new File(duplicateFileRename(dest.getPath()));
            }
            Files.copy(origin.toPath(), dest.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 파일 이름 생성 - 년월일시분초밀리초_{item_seq}_{user_id}
     *
//     * @param seq         - 항목 시퀀스
//     * @param userId      - user id
     * @param time - 생성 시점 unix time
     * @return 생성 된 파일 이름
     */
//    public static String getInsertFileName(String userId, int seq, String time) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(time); // 유닉스 타임
//        sb.append("_");
//        sb.append(seq); // 시퀀스
//        sb.append("_");
//        sb.append(userId);    // 카테고리 인덱스
////        DEBUG.d("# file name : " + sb.toString());
//        return sb.toString();
//    }

    public static String getInsertFileName(String time) {
        StringBuilder sb = new StringBuilder();
        sb.append(time); // 유닉스 타임
//        sb.append("_");
//        sb.append(seq); // 시퀀스
//        sb.append("_");
//        sb.append(userId);    // 카테고리 인덱스
//        DEBUG.d("# file name : " + sb.toString());
        return sb.toString();
    }


    public static boolean fileDelete(String filePath) {
        //filePath : 파일경로 및 파일명이 포함된 경로입니다.
        try {
//            DEBUG.d("# delete filePath : " + filePath);
            File file = new File(filePath);
            // 파일이 존재 하는지 체크
            if (file.exists()) {
                file.delete();
                return true;  // 파일 삭제 성공여부를 리턴값으로 반환해줄 수 도 있습니다.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 중복된 파일 Rename
     * 파일명에 (1), (2) 항목 검사해서 제일 마지막 인덱스 추가
     *
     * @return index
     */
    public static String duplicateFileRename(String filePath) {
        int dotIndex = filePath.lastIndexOf("_");
        for (int i = 0; i < 9999; i++) {
            StringBuilder sb = new StringBuilder(filePath);
            sb.insert(dotIndex, "(" + i + ")");
            if (!new File(sb.toString()).exists())
                return sb.toString();
        }
        return "err";
    }

    /**
     * 원본 bitmap 전달받아 섬네일으로 변경 후 파일 저장
     *
     * @param originFile - 원본 사진 파일
     */
    public static String createThumbnail(File originFile) {
        StringBuilder thumbPath = new StringBuilder(originFile.getParent()).append("/thumb_").append(originFile.getName());
        File fileCacheItem = new File(thumbPath.toString());
        OutputStream out = null;

        Bitmap bitmap = null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(originFile), null, options);

            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            // 15 is down scale size
            bitmap = Bitmap.createScaledBitmap(bitmap, width / DEFINE.THUMBNAIL_DOWN_SCALE_SIZE,
                    height / DEFINE.THUMBNAIL_DOWN_SCALE_SIZE, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return thumbPath.toString();
    }

    /**
     * 원본 mp4 File 경로 섬네일 사진 추출
     * @param originFile - 원본 mp4 파일
     */
    public static String createVideoThumbnail(File originFile) {
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        m.setDataSource(originFile.getPath());
        Bitmap bitmap = m.getFrameAtTime(1000000); // 시간은 uSec단위

        StringBuilder thumbPath = new StringBuilder(originFile.getParent()).append("/thumb_");
        thumbPath.append(removeExtension(originFile.getName()));
        thumbPath.append(DEFINE.JPG_FORMAT);
        File fileCacheItem = new File(thumbPath.toString());
        OutputStream out = null;

        try {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            // 15 is down scale size
            bitmap = Bitmap.createScaledBitmap(bitmap, width / DEFINE.THUMBNAIL_DOWN_SCALE_SIZE,
                    height / DEFINE.THUMBNAIL_DOWN_SCALE_SIZE, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return thumbPath.toString();
    }

    /**
     * 전달 받은 압축 파일 zip
     * @param files - 압축 할 파일 리스트
     * @return 압축 파일 zip
     */
    public static File FilesToZipCompress(File[] files, String fileName) {
        File zipFile = new File(Global.getLogFileDir(), fileName);

        if (zipFile.getParentFile().exists())
            zipFile.getParentFile().mkdirs();

        if (zipFile.exists())
            FileManager.deleteFile(zipFile.getPath());

        byte[] buf = new byte[4096];

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File file : files) {
                try (FileInputStream in = new FileInputStream(file)) {
                    ZipEntry ze = new ZipEntry(file.getName());
                    out.putNextEntry(ze);
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return zipFile;
        }
    }

    /**
     * 확장자 제외한 파일명 가져오기 함수
     * @param fileName - origin
     * @return 확장자 제외 파일명
     */
    private static String removeExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1) {
            fileName = fileName.substring(0, lastIndex);
        }
        return fileName;
    }

    /**
     * String 내용 File에 기록
     */
    public static void StringWriteFile(String content, String path) {
        File file = new File(path);

        try {
            // 파일 생성
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(content.getBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
