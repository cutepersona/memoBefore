package com.peng.power.memo.util;

import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.peng.power.memo.manager.AsyncCallback;
import com.peng.power.memo.manager.ClovaSpeechClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class AudioUtils {

    //    final static public int RECORDER_SAMPLERATE = 44100;

    final static public int RECORDER_SAMPLERATE = 16000;
    final int RECORDER_BPP = 16;
    private static final int TRANSFER_BUFFER_SIZE = 10 * 1024;


    /**
     * 파일 병합
     * @param filePart1 파일 파티션1
     * @param filePart2 파일 파티션2
     * @param finalFilePath 병합 된 파일 경로
     */
    public void merge(String filePart1, String filePart2, String finalFilePath) {
        FileInputStream in1 = null, in2 = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

//        DEBUG.d("finalFilePath : " + finalFilePath);

//        long byteRate = RECORDER_SAMPLERATE * 2;

//        byte[] data = new byte[bufferSize];
        byte[] data = new byte[TRANSFER_BUFFER_SIZE];

        try {
            in1 = new FileInputStream(filePart1);
            in2 = new FileInputStream(filePart2);

            out = new FileOutputStream(finalFilePath);

            totalAudioLen = in1.getChannel().size() + in2.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    16000, channels, byteRate);

            while (in1.read(data) != -1) {

                out.write(data);

            }
            while (in2.read(data) != -1) {

                out.write(data);
            }

            out.close();
            in1.close();
            in2.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte)(totalDataLen & 0xff);
        header[5] = (byte)((totalDataLen >> 8) & 0xff);
        header[6] = (byte)((totalDataLen >> 16) & 0xff);
        header[7] = (byte)((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte)(longSampleRate & 0xff);
        header[25] = (byte)((longSampleRate >> 8) & 0xff);
        header[26] = (byte)((longSampleRate >> 16) & 0xff);
        header[27] = (byte)((longSampleRate >> 24) & 0xff);
        header[28] = (byte)(byteRate & 0xff);
        header[29] = (byte)((byteRate >> 8) & 0xff);
        header[30] = (byte)((byteRate >> 16) & 0xff);
        header[31] = (byte)((byteRate >> 24) & 0xff);
        header[32] = (byte)(2 * 16 / 8);
        header[33] = 0;
        header[34] = RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(totalAudioLen & 0xff);
        header[41] = (byte)((totalAudioLen >> 8) & 0xff);
        header[42] = (byte)((totalAudioLen >> 16) & 0xff);
        header[43] = (byte)((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }


    public void rawToWave(final File rawFile, final File waveFile) throws IOException {

        Log.d("Noah ", "rawFile / " + rawFile);
        Log.d("Noah ", "waveFile / " + waveFile);

        if (!rawFile.exists()){
            Log.d("Noah ", "pcmFileCheck ==== None");
        }

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        }catch (Exception e) {
            Log.d("Noah ", "rawToWave " + e);
        } finally {
            Log.d("Noah ", "input / " + input);
            if (input != null) {
                input.close();
            }
        }

        Log.d("Noah ", " ==================================================== ");

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // formatmerge files
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
//            writeInt(output, 44100); // sample rate
            writeInt(output, 16000); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));

            Log.d("Noah ", "write " + output);

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    public static void writeToOutput(OutputStream output, String data) throws IOException {
        for (int i = 0; i < data.length(); i++)
            output.write(data.charAt(i));
    }

    public static void writeToOutput(OutputStream output, int data) throws IOException {
        output.write(data >> 0);
        output.write(data >> 8);
        output.write(data >> 16);
        output.write(data >> 24);
    }

    public static void writeToOutput(OutputStream output, short data) throws IOException {
        output.write(data >> 0);
        output.write(data >> 8);
    }

    public static long copy(InputStream source, OutputStream output)
            throws IOException {
        return copy(source, output, TRANSFER_BUFFER_SIZE);
    }

    public static long copy(InputStream source, OutputStream output, int bufferSize) throws IOException {
        long read = 0L;
        byte[] buffer = new byte[bufferSize];
        for (int n; (n = source.read(buffer)) != -1; read += n) {
            output.write(buffer, 0, n);
        }
        return read;
    }

    /**
     * Mp4 to Wav
     * @param inputFilePath - 원본 mp4 파일 경로
     * @param outputFilePath - 추출 할 wav 파일 경로
     */
    public static void convertMp4ToWav(String inputFilePath, String outputFilePath) {
        String[] cmd = new String[] {
                "-i", inputFilePath,
                "-vn",       // 오디오 스트림만 추출
                "-acodec", "pcm_s16le", // 오디오 코덱을 PCM으로 설정
                "-ar", "44100", // 샘플링 레이트 설정
                outputFilePath
        };

        int rc = FFmpeg.execute(cmd);

        if (rc == Config.RETURN_CODE_SUCCESS) {
            Log.d("FFmpeg", "성공적으로 변환 완료");
        } else if (rc == Config.RETURN_CODE_CANCEL) {
            Log.d("FFmpeg", "작업이 취소되었습니다.");
        } else {
            Log.d("FFmpeg", "오류 발생. 코드: " + rc);
        }
    }

    /**
     * Naver STT API 호출 - 음성 파일 to text
     * @param path - 음성 파일 경로
     * @param callback - 응답 받은 결과 return
     */
    public static void callNaverSttApi(String path, AsyncCallback callback) {
//        DEBUG.d("wav file length : " + new File(path).length());
//        DEBUG.d("wav path : " + path);
        final ClovaSpeechClient clovaSpeechClient = new ClovaSpeechClient(new ClovaSpeechClient.Listener() {
            @Override
            public void onResult(String data, String originJson) {
//                DEBUG.d("# originJson data: " + originJson);
                ArrayList<String> responseValue = new ArrayList<>();
                responseValue.add(data);
                responseValue.add(originJson);
                callback.onCallback(responseValue);
            }
        });
        clovaSpeechClient.upload(path);
    }
}
