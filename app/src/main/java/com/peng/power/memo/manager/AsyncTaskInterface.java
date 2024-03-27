package com.peng.power.memo.manager;

public interface AsyncTaskInterface {
    // 백 그라운드 작업
    public void doInBackground();
    // 백 그라운드 작업 끝나고 실행
    public void onPostExecute();
}
