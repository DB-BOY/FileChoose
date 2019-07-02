package cn.dbboy.filechoose;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String DOC = "application/msword";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLS = "application/vnd.ms-excel";
    private static final String XLS1 = "application/x-excel";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPT = "application/vnd.ms-powerpoint";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String PDF = "application/pdf";
    private static final String MP4 = "video/mp4";
    private static final String M3U8 = "application/x-mpegURL";

    private static final int REQUEST_CODE_FILE = 985 << 2;
    private static final int REQUEST_PHONE_STATE = 211 << 2;
    
    private final String[] fileSuffix = {".pptx", ".ppt", ".xlsx", ".docx", ".xls", ".doc", ".pdf"};
    private final String[] videoSuffix = {".m3u8", ".mp4"};
    TextView tv;
    String type;
    AlertDialog.Builder dialog;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.text);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionRead = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionRead == PackageManager.PERMISSION_GRANTED) {
                grantSuccess();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PHONE_STATE);
            }
        } else {
            grantSuccess();
        }
        findViewById(R.id.btn_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile(true);
            }
        });
        findViewById(R.id.btn_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile(false);
            }
        });
        findViewById(R.id.btn_file_path).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFileWithPath();
            }
        });
        findViewById(R.id.btn_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePic();
            }

        });
    }

    /**
     * 选择图片文件
     */
    private void choosePic() {
        Intent intent;
        intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_CODE_FILE);
    }

    /**
     * 根据类型，加载文件选择器
     */
    private void chooseFileWithPath() {
        //如果使用系统文件选择器，可以实现文件类型过滤，三方的文件选择器不可用。腾讯，es等
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {DOC, DOCX, PDF, PPT, PPTX, XLS, XLS1, XLSX};

        //跳转指定路径，如果路径不存在，切到sdcard
        //需要读权限
        String path = getSDPath();
        if (!TextUtils.isEmpty(path)) {
            path = path + File.separator + "tencent/MicroMsg/Download";
            File file = new File(path);
            if (file.exists()) {
                intent.setDataAndType(FileUtil.getUriFromFile(this, new File(path)), "application/*");
            } else {
                intent.setType("application/*");
            }
        } else {
            intent.setType("application/*");
        }

        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_FILE);
    }

    /**
     * 根据类型，加载文件选择器
     */
    private void chooseFile(boolean isFile) {
        //如果使用系统文件选择器，可以实现文件类型过滤，三方的文件选择器不可用。腾讯，es等
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {DOC, DOCX, PDF, PPT, PPTX, XLS, XLS1, XLSX};
        if (!isFile) {
            mimeTypes = new String[]{MP4, M3U8};
        }
        intent.setType(isFile ? "application/*;*.xls" : "video/mp4;*.m3u8");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_FILE) {
            Uri uri = data.getData();
            tv.setText("");
            String s = uri.toString() + "  \n " + uri.getPath() + " \n " + uri.getAuthority();
            tv.setText(s);
            Log.i("-----", s);
            
            if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                path = uri.getPath();
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                    path = FileUtil.getPath(this, uri);
                } else {//4.4以下下系统调用方法
                    path = FileUtil.getRealPathFromURI(this, uri);
                }
            }

            //uri.getLastPathsegment()不一定能获取到文件名
            //content://com.android.providers.media.documents/document/video:5186
            //必须要通过path去判断
            
            String name = uri.getLastPathSegment().toLowerCase();
            if (!checkFileType(name)) {
                Toast.makeText(this, "暂不支持文件类型", Toast.LENGTH_SHORT).show();
                return;
            }


            doSomething();
        }
        //其他情况自行处理
    }

    /**
     * 检查文件类型
     *
     * @param fileName
     *
     * @return
     */
    private boolean checkFileType(String fileName) {
        if ("video".equals(type)) {
            for (String suffix : videoSuffix) {
                if (fileName.endsWith(suffix)) {
                    return true;
                }
            }
        } else {
            for (String suffix : fileSuffix) {
                if (fileName.endsWith(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 向js返回数据
     */
    private void doSomething() {
        tv.setText(tv.getText() + "\n----dosomething----\n" + path);
    }

    /**
     * @return
     */
    public String getSDPath() {
        String path = "";
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
            path = sdDir.toString();
        }
        return path;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults == null || grantResults.length == 0) {
            return;
        }
        if (requestCode == REQUEST_PHONE_STATE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                grantSuccess();
            } else {
                if (dialog == null) {
                    dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setMessage("我们需要您的允许获取读取存储权限");
                    dialog.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PHONE_STATE);
                        }
                    }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            grantSuccess();
                        }
                    }).show();
                }
            }
        }
    }

    /**
     * 授权成功
     */
    private void grantSuccess() {

    }

}
