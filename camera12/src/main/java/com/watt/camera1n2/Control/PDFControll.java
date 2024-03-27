package com.watt.camera1n2.Control;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.watt.camera1n2.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PDFControll extends AppCompatActivity implements IShowPages {

    private static final String TAG = "Deangjun";
    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    public int currentPage; /* 현재페이지 */
    public int fullPage; /* 전체페이지 */
    public Context pdfContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pdfview);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }


    public void openPdf(Context context, String filePath) {
        try {
            openRenderer(context, filePath);

        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void openRenderer(Context context, String filePath) throws IOException {
        pdfContext = context;

        File file = new File(filePath);
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        if (mFileDescriptor != null) {
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        }

    }

    private void closeRenderer() throws IOException {
        if (null != mCurrentPage)
            mCurrentPage.close();

        if (null != mPdfRenderer)
            mPdfRenderer.close();

        if (null != mFileDescriptor)
            mFileDescriptor.close();
    }


    public Bitmap showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return null;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).

        DisplayMetrics dm = pdfContext.getResources().getDisplayMetrics();

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        Bitmap bitmap = Bitmap.createBitmap(width * 2, height * 2,
                Bitmap.Config.ARGB_8888);
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        fullPage = mPdfRenderer.getPageCount();

        Log.d(TAG, "deangjun fullPage : " + fullPage);

        currentPage = mCurrentPage.getIndex();
        Log.d(TAG, "deangjun currentPage : " + currentPage);
        return bitmap;
    }

}
