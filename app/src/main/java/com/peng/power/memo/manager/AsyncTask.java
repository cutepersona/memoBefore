package com.peng.power.memo.manager;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AsyncTask {
    //  리액티브 비동기 스레드 객체
    public static Disposable mBackgroundtask;

    // os에 의해 앱의 프로세스가 죽거는 등의 상황에서
    // Single 객체를 가로채기 위함
    public static CompositeDisposable mDisposable = new CompositeDisposable();

    /**
     * 동기화 처리 함수
     * @param asyncTaskITF - 수행 내용 인터페이스
     */
    public static void task(AsyncTaskInterface asyncTaskITF) {
        mBackgroundtask = Observable.fromCallable(() -> {
                    /** doInBackground    **/
                    asyncTaskITF.doInBackground();
                    return false;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> false)
                .subscribe((result) -> {
                    /** onPostExecute    **/
                    asyncTaskITF.onPostExecute();
                });
    }
}
