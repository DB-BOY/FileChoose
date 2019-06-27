# Android浏览文件
>记一次文件上传引发的血案

[测试Demo](http://gitlab.docbook.com.cn:91/db_boy/FileChoose)

------

##### 前情描述：
使用系统文件管理器，选择指定文件类型上传。

##### 踩坑点
* 调起文件管理器
* 指定浏览位置(路径转URI)
* 设置多种文件类型
* MIME
* URI转路径

#### 1. MIME
> MIME (Multipurpose Internet Mail Extensions) 是描述消息内容类型的因特网标准。

#### 2. 调起文件管理器

1. 所有类型文件
````
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    //任意类型文件
    intent.setType("*/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(intent,1);
    
    
    //-------常用类型
        //图片
    //intent.setType(“image/*”);
        //音频
    //intent.setType(“audio/*”);
        //视频
    //intent.setType(“video/*”); 
    //intent.setType(“video/*;image/*”);

````
2. 系统的相冊

````
    Intent intent= new Intent(Intent.ACTION_PICK, null);
    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
    startActivityForResult(intent, REQUEST_CODE_FILE);

````