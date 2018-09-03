package indi.aljet.mybackgroundupdate_master.bgupdate;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import indi.aljet.mybackgroundupdate_master.R;
import okhttp3.ResponseBody;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class DownloadService extends Service {


    private static final String TAG = "DownloadService";

    //保存文件的路径
    private String filePath ;

    //订阅器
    private Subscription subscription;
    private static MyBinder myBinder;

    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager notificationManager;

    private ProgressDialog mProgressDialog;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showDialog(msg.arg1, (Boolean) msg.obj);
        }
    };


    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        myBinder = new MyBinder();
        return myBinder;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (myBinder != null) {
            myBinder.cancelDownload();
        }
        super.onTaskRemoved(rootIntent);
        this.stopSelf();
    }


    /**
     * 显示Dialog
     *
     * @param progress
     * @param isDownSuccess
     */
    private void showDialog(int progress, boolean isDownSuccess) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle("正在下载。。。");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(100);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            myBinder.cancelDownload();
                        }
                    });
            //把TYPE_SYSTEM_ALERT改为TYPE_TOAST,是为了绕过检查
            mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
            mProgressDialog.show();//调用show方法显示进度条对话框
        }
        if (isDownSuccess) {
            mProgressDialog.setTitle("等待安装，请稍等。。。");
        }
        mProgressDialog.setProgress(progress);
    }

    /**
     * 显示通知
     *
     * @param progress
     * @param isSuccess
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification(int progress, boolean isSuccess) {
        if (mNotifyBuilder == null) {
            Intent intentClick = new Intent(this,
                    NotificationBroadcastReceiver.class);
            intentClick.setAction("cancelDownload");
            PendingIntent cancelIntent = PendingIntent
                    .getBroadcast(this, 0,
                            intentClick, PendingIntent.FLAG_ONE_SHOT);
            mNotifyBuilder = (NotificationCompat.Builder)
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_file_download_black_24dp)
                            .setContentTitle("正在下载。。。。")
                            .setAutoCancel(false)
                            .setWhen(System.currentTimeMillis())
                            .addAction(R.mipmap.ic_cancel_black_24dp, "取消"
                                    , cancelIntent);
        }
        if (isSuccess) {
            mNotifyBuilder.setContentTitle("等待安装。。。。");
            mNotifyBuilder.setProgress(100, progress, true);

        } else {
            mNotifyBuilder.setProgress(100, progress, false);
        }
        notificationManager.notify(0, mNotifyBuilder.build());

    }


    /**
     * 保存 下载文件
     * @param body
     * @return
     */
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File filepath = new File(filePath);
            if(!filepath.getParentFile().exists()){
                filepath.getParentFile().mkdirs();                                     
            }
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try{
                byte[] fileReader = new byte[4096];
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(filepath);
                while(true){
                    int read = inputStream.read(fileReader);
                    if(read == -1)
                        break;
                    outputStream.write(fileReader,0,read);
                }
                outputStream.flush();
                return true;
            }catch (IOException e){
                return false;
            }finally{
                if(inputStream != null)
                    inputStream.close();
                if(outputStream != null)
                    outputStream.close();
            }

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 下载完毕，安装新的APK
     */
    private void startInstallApk(){
        myBinder.cancelDownload();
        File apkFile = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT >= 24){
            Uri apkUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri,
                    "application/vnd.android.package-archive");
        }else{
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri
            .parse("file://"+ apkFile.toString()),
                    "application/vnd.android.package-archive");
        }
        startActivity(intent);
        android.os.Process.killProcess(android.os
        .Process.myPid());
    }


    /**
     * 更新失败提示
     */
    private void updateFailHint(){
        Toast.makeText(this,"更新失败,请重试",
                Toast.LENGTH_SHORT).show();
        myBinder.cancelDownload();
    }


    /**
     * 接受通知
     */
    public static class NotificationBroadcastReceiver extends
            BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("cancelDownload")) {
                myBinder.cancelDownload();
            }
        }
    }


    public class MyBinder extends Binder {

        //下载进度
        private int progress;
        //用于第一次显示
        private boolean isShow = false;
        //显示方式
        private int SHOW_TYPE = 0;


        /**
         * 开始下载
         *
         * @param url
         * @param filePath
         * @param type
         */
        public void startDownload(String url,
                                  final String filePath,
                                  int type) {
            DownloadService.this.filePath = filePath;
            SHOW_TYPE = type;
            if (subscription == null) {
                subscription = NetWork.getApi(new ProgressListener() {
                    @Override
                    public void onProgress(float section, float total, boolean done) {
                        int percent = (int) (section / total * 100);
                        if (!isShow) {
                            //开始下载有进度即显示通知栏
                            if (SHOW_TYPE == BgUpdate.TYPE_DIALOG) {
                                Message message = new Message();
                                message.arg1 = percent;
                                message.obj = false;
                                handler.sendMessage(message);
                            } else if (SHOW_TYPE == BgUpdate.TYPE_NOTIFICATION) {
                                showNotification(percent, false);
                            }
                            isShow = true;
                        } else if (percent % 5 == 0 && progress != percent) {
                            //每百分之五和旧进度与新进度不相等时,才更新一次通知栏 , 防止更新频繁
                            progress = percent;
                            if (SHOW_TYPE == BgUpdate.TYPE_DIALOG) {
                                Message message = new Message();
                                message.arg1 = percent;
                                message.obj = percent == 100;
                                handler.sendMessage(message);
                            } else if (SHOW_TYPE == BgUpdate.TYPE_NOTIFICATION) {
                                if (percent == 100) {
                                    //下载完毕时 , 存储文件需要时间 , 修改进度条状态
                                    showNotification(percent, true);
                                } else {
                                    showNotification(percent, false);
                                }
                            }
                        }
                    }
                }).downloadFile(url)   // IO 线程，由 subscribeOn() 指定 , 下载文件
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.newThread())
                        .map(new Func1<ResponseBody, Boolean>() { // 新线程，由 observeOn() 指定 , 对象变化
                            @Override
                            public Boolean call(ResponseBody responseBody) {
                                return writeResponseBodyToDisk(responseBody); //下载完毕 , 存储到手机根目录
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Boolean>() {// Android 主线程，由 observeOn() 指定
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: ", e);
                                updateFailHint();
                            }

                            @Override
                            public void onNext(Boolean isSuccess) {
                                //true - 下载完成并存储在手机根目录 , false - 失败
                                if (isSuccess) {    //存储完成 , 开始安装
                                    startInstallApk();
                                } else {        //存储失败 , 提示错误
                                    updateFailHint();
                                }
                            }
                        });
            }
        }


        /**
         * 取消下载
         */
        public void cancelDownload() {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
                subscription = null;
                mNotifyBuilder = null;
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
                isShow = false;
                notificationManager.cancelAll();
            }
        }
    }


}
