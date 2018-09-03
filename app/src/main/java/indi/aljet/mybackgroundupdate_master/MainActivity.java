package indi.aljet.mybackgroundupdate_master;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import indi.aljet.mybackgroundupdate_master.bgupdate.BgUpdate;


public class MainActivity extends AppCompatActivity {

    Button bt_dialog,bt_notification;


//    private String url = "http://183.56.150.169/imtt.dd.qq.com/16891/50172C52EBCCD8F9B0AD2B576DB7BA16.apk?mkey=58fedb8402e16784&f=9602&c=0&fsname=cn.flyexp_2.0.1_6.apk&csr=1bbd&p=.apk";

    private String url = "http://gdown.baidu.com/data/wisegame/1d68c0b23cfdf238/weixin_1340.apk";

//    private String url = "data/wisegame/1d68c0b23cfdf238/weixin_1340.apk";
//    private String url = "a31/rj_sp1/kumanhua.apk";

    private String filePath = Environment
            .getExternalStorageDirectory()
            + "/wechat.apk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_dialog = (Button)findViewById(R.id.bt_dialog);
        bt_notification = findViewById(R.id.bt_notification);
        bt_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BgUpdate.updateForDialog(MainActivity.this,
                        url,filePath);
            }
        });


        bt_notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BgUpdate.updateForNotification(MainActivity.this,
                        url,filePath);
            }
        });

    }

    @Override
    protected void onDestroy() {
        BgUpdate.closeService(this);
        super.onDestroy();
    }
}
