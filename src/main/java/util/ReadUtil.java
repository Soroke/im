package util; /**
 * Created by song on 2018/10/22.
 */

import com.dd.plist.*;
import com.google.zxing.WriterException;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.UseFeature;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author soroke
 *
 */
public final class ReadUtil {

    public static String plistContent="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n" +
            "<dict>\n" +
            "\t<key>items</key>\n" +
            "\t<array>\n" +
            "\t\t<dict>\n" +
            "\t\t\t<key>assets</key>\n" +
            "\t\t\t<array>\n" +
            "\t\t\t\t<dict>\n" +
            "\t\t\t\t\t<key>kind</key>\n" +
            "\t\t\t\t\t<string>software-package</string>\n" +
            "\t\t\t\t\t<key>url</key>\n" +
            "\t\t\t\t\t<string>ipaPackageUrl</string>\n" +
            "\t\t\t\t</dict>\n" +
			"\t\t\t\t<dict>\n" +
			"\t\t\t\t\t<key>kind</key>\n" +
			"\t\t\t\t\t<string>display-image</string>\n" +
			"\t\t\t\t\t<key>needs-shine</key>\n" +
			"\t\t\t\t\t<true/>\n" +
			"\t\t\t\t\t<key>url</key>\n" +
			"\t\t\t\t\t<string>iconImageUrl</string>\n" +
			"\t\t\t\t</dict>\n" +
            "\t\t\t</array>\n" +
            "\t\t\t<key>metadata</key>\n" +
            "\t\t\t<dict>\n" +
            "\t\t\t\t<key>bundle-identifier</key>\n" +
            "\t\t\t\t<string>bundleIdentifier</string>\n" +
            "\t\t\t\t<key>bundle-version</key>\n" +
            "\t\t\t\t<string>bundleVersion</string>\n" +
            "\t\t\t\t<key>kind</key>\n" +
            "\t\t\t\t<string>software</string>\n" +
            "\t\t\t\t<key>title</key>\n" +
            "\t\t\t\t<string>ipaPackageName</string>\n" +
            "\t\t\t</dict>\n" +
            "\t\t</dict>\n" +
            "\t</array>\n" +
            "</dict>\n" +
            "</plist>";

    /**
     * 读取ipa
     */
    public static Map<String,Object> readIPA(String ipaPath,String... iconPath){
        Map<String,Object> map = new HashMap<String,Object>();
        try {
            File file = new File(getIpaPath(ipaPath));
            InputStream is = new FileInputStream(file);
            InputStream is2 = new FileInputStream(file);
            ZipInputStream zipIns = new ZipInputStream(is);
            ZipInputStream zipIns2 = new ZipInputStream(is2);
            ZipEntry ze;
            ZipEntry ze2;
            InputStream infoIs = null;
            NSDictionary rootDict = null;
            String icon = null;
            while ((ze = zipIns.getNextEntry()) != null) {
                if (!ze.isDirectory()) {
                    String name = ze.getName();
                    if (null != name && name.toLowerCase().contains(".app/info.plist")) {
                        ByteArrayOutputStream _copy = new ByteArrayOutputStream();
                        int chunk = 0;
                        byte[] data = new byte[1024];
                        while(-1!=(chunk=zipIns.read(data))){
                            _copy.write(data, 0, chunk);
                        }
                        infoIs = new ByteArrayInputStream(_copy.toByteArray());
                        rootDict = (NSDictionary) PropertyListParser.parse(infoIs);

                        //我们可以根据info.plist结构获取任意我们需要的东西
                        //比如下面我获取图标名称，图标的目录结构请下面图片
                        //获取图标名称
                        NSDictionary iconDict = (NSDictionary) rootDict.get("CFBundleIcons");

                        while (null != iconDict) {
                            if(iconDict.containsKey("CFBundlePrimaryIcon")){
                                NSDictionary CFBundlePrimaryIcon = (NSDictionary)iconDict.get("CFBundlePrimaryIcon");
                                if(CFBundlePrimaryIcon.containsKey("CFBundleIconFiles")){
                                    NSArray CFBundleIconFiles =(NSArray)CFBundlePrimaryIcon.get("CFBundleIconFiles");
                                    NSObject[] iconArrayList = CFBundleIconFiles.getArray();
                                    //icon = CFBundleIconFiles.getArray()[0].toString();
                                    if (iconArrayList.length >= 2) {
                                        icon = iconArrayList[iconArrayList.length - 2].toString();
                                    } else {
                                        icon = iconArrayList[iconArrayList.length - 1].toString();
                                    }
                                    if(icon.contains(".png")){
                                        icon = icon.replace(".png", "");
                                    }
//                                    System.out.println("获取icon名称:" + icon);
                                    break;
                                }else {
                                    iconDict = (NSDictionary) rootDict.get("CFBundleIcons~ipad");
                                    continue;
                                }
                            }
                        }
                        break;
                    }
                }
            }
            //获取icon的路径，如果用户传入路径就使用指定路径，否则在ipa包同路径下创建
            String icon_path = "";
            if (iconPath.length == 0) {
                icon_path = ipaPath;
            } else if (iconPath.length == 1) {
                icon_path = iconPath[0];
            }

            //根据图标名称下载图标文件到指定位置
            if (icon_path != "") {
                while ((ze2 = zipIns2.getNextEntry()) != null) {
                    if (!ze2.isDirectory()) {
                        String name = ze2.getName();
//System.out.println(name);
                        if(name.contains(icon.trim())){
//System.out.println(11111);
                            NSString parameters = (NSString) rootDict.get("CFBundleName");
                            File iconImage = new File(icon_path + "/" + parameters.toString() + "_iOS_icon.png");
                            FileOutputStream fos = new FileOutputStream(iconImage);
                            int chunk = 0;
                            byte[] data = new byte[1024];
                            while(-1!=(chunk=zipIns2.read(data))){
                                fos.write(data, 0, chunk);
                            }
                            fos.close();
                            ProcessingPictures(icon_path);
                            break;
                        }
                    }
                }
            }


            // 应用包名
            NSString parameters = (NSString) rootDict.get("CFBundleIdentifier");
            map.put("package", parameters.toString());
            // 应用版本名
            parameters = (NSString) rootDict.objectForKey("CFBundleShortVersionString");
            map.put("versionName", parameters.toString());
            //应用名称
            //parameters = (NSString) rootDict.get("CFBundleDisplayName");
            //map.put("CFBundleDisplayName", parameters.toString());

            //应用名称1
            parameters = (NSString) rootDict.get("CFBundleName");
            map.put("CFBundleName", parameters.toString());

            /////////////////////////////////////////////////
            infoIs.close();
            is.close();
            zipIns.close();

        } catch (Exception e) {
            map.put("code", "fail");
            map.put("error","读取ipa文件失败");
        }
        return map;
    }


    /**
     * 有些icon图片获取后是黑色  或者颜色不正确
     * 通过处理还原图片
     * @param iconPath icon图片所在文件夹
     */
    public static void ProcessingPictures(String iconPath) {
        /**
         * 防止获取icon图片有问题执行python脚本进行
         */

        File file = new File(iconPath);
        try {
            String filePath=System.getProperty("user.dir") + "\\src\\main\\resources\\ipin.py";
            Process p = Runtime.getRuntime().exec("python " + filePath, new String[0],file );
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,"GBK"));
            //需要输出内容时放开下面的注释
//            String line;
//            while((line = reader.readLine())!= null){
//                System.out.println(line);
//            }
            p.waitFor();
            is.close();
            reader.close();
            p.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }  catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 创建plist文件
     * @param plistFilePath  文件路径
     * @param ipaPackageUrl     IPA包的URL路径
     * @param bundleIdentifier      应用程序包名
     * @param bundleVersion         应用版本号
     * @param ipaPackageName        应用程序名称
     */
    public static void createPlistFile(String plistFilePath,String ipaPackageUrl,String bundleIdentifier,String bundleVersion,String ipaPackageName,String iconImageUrl) {
        /**
         * 替换ipa包路径、包名、应用名称、应用版本号等信息
         */
        plistContent = plistContent.replaceAll("ipaPackageUrl",ipaPackageUrl);
        plistContent = plistContent.replaceAll("bundleIdentifier",bundleIdentifier);
        plistContent = plistContent.replaceAll("bundleVersion",bundleVersion);
        plistContent = plistContent.replaceAll("ipaPackageName",ipaPackageName);
		plistContent = plistContent.replaceAll("iconImageUrl",iconImageUrl);
        File file = new File(plistFilePath+ "/manifest.plist");
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }else {
                file.createNewFile();
            }

            /**
             * 讲内容写入文件
             */
            byte[] contentInBytes = plistContent.getBytes();
            FileOutputStream fop = new FileOutputStream(file);
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
            System.out.println("plist file create Done.");
        } catch (IOException e) {
            System.out.println("文件创建失败！！！");
            e.printStackTrace();
        }

    }

    /**
     * 获取指定文件夹下的ipa文件的名称
     * @param path 路径
     * @return
     */
    public static String getIpaPath(String path) {
        if (path.equals("") || path == "" || path == null) {
            System.err.println("指定路径为空，无法识别IPA包的path");
            return null;
        }
        String ipaPath ="";
        File file = new File(path);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
//                System.out.println("文件名称：" + tempList[i].getName());
                try{
                    String[] fileNames = tempList[i].getName().split("\\.");
                    if (fileNames[fileNames.length-1].equals("ipa")) {
                        ipaPath=tempList[i].getPath();
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }

        if (ipaPath.equals("") || ipaPath == "") {
            System.err.println("当前路径下不存在ipa包");
            return null;
        }
        return ipaPath;
    }

    /**
     * 获取指定文件夹下的apk文件的名称
     * @param path 路径
     * @return
     */
    public static String getApkPath(String path) {
        if (path.equals("") || path == "" || path == null) {
            System.err.println("指定路径为空，无法识别IPA包的path");
            return null;
        }
        String apkPath ="";
        File file = new File(path);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
//                System.out.println("文件名称：" + tempList[i].getName());
                try{
                    String[] fileNames = tempList[i].getName().split("\\.");
                    if (fileNames[fileNames.length-1].equals("apk")) {
                        apkPath=tempList[i].getPath();
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }

        if (apkPath.equals("") || apkPath == "") {
            System.err.println("当前路径下不存在apk包");
            return null;
        }
        return apkPath;
    }


    /**
     * 读取APK包 获取指定信息 获取icon文件
     * @param path 包所在路径
     * @param iconPath icon文件存放路径
     * @return MAP 项目名称、包名、版本号等信息
     */
    public Map readApk(String path,String... iconPath) {
        Map<String,String> map = new HashMap<String,String>();
        String apkPath = getApkPath(path);
        String icon_path = "";
        try {
            ApkFile apkParser = new ApkFile(new File(apkPath));
            //String xml = apkParser.getManifestXml();
            //System.out.println(xml);
            ApkMeta apkMeta = apkParser.getApkMeta();
            //获取项目名称、包名、版本号等信息
            map.put("name",apkMeta.getLabel());
            map.put("packageName",apkMeta.getPackageName());
            map.put("version",apkMeta.getVersionName().toString());

            //获取icon的路径，如果用户传入路径就使用指定路径，否则在ipa包同路径下创建
            if (iconPath.length == 0) {
                icon_path = path;
            } else if (iconPath.length == 1) {
                icon_path = iconPath[0];
            }
            //将apk包中的icon文件拿出来

            ApkIconUtil.extractFileFromApk(apkPath,apkMeta.getIcon(),icon_path + apkMeta.getLabel() + "_Android_icon.png");

//            System.out.println(apkMeta);
            apkParser.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Test
    public void apk() {
        Map<String,Object> mapIpa = readApk("D:/1111/");
        for (String key : mapIpa.keySet()) {
            System.out.println(key + ":" + mapIpa.get(key));
        }
    }

    @Test
    public void ipa() {
        Map<String,Object> mapIpa = ReadUtil.readIPA("D:/1111/");
        for (String key : mapIpa.keySet()) {
            System.out.println(key + ":" + mapIpa.get(key));
        }
        createPlistFile("D:/1111/","https://appdown.faxuanyun.com:1443/upload/5e847cb055b257001dc14a67/ios/com.faxuan.LawAssess_2.3.5_17.ipa",mapIpa.get("package").toString(),mapIpa.get("versionName").toString(),mapIpa.get("CFBundleName").toString(),"https://appdown.faxuanyun.com:1443/upload/5e847cb055b257001dc14a67/icon/com.faxuan.LawAssess_1.2.3_16_i.png");
        /**
         * 生成二维码
         */
        try {
            QrCodeUtil.createQrCode( "D:/1111//ipa.jpg","itms-services://?action=download-manifest&url=https://appdown.faxuanyun.com:1443/api/plist/5e8e8ed5d59e7a001d935440/5e8e91c3d59e7a001d935444",1100,"JPEG");
            System.out.println("二维码文件生成成功");
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("二维码文件生成失败");
            e.printStackTrace();
        }
    }



    public static void main(String[] ages) {
        /**
         * 获取当前路径下ipa包的名称
         */
        String ipaPath ="";
        boolean flag = false;
        File file = new File(ages[0]);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
//                System.out.println("文件名称：" + tempList[i].getName());
                try{
                    if (tempList[i].getName().split("\\.")[1].equals("ipa")) {
                        ipaPath=tempList[i].getPath();
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

            }
        }

        /**
         * 读取ipa包的信息
         */
        Map<String,Object> mapIpa = ReadUtil.readIPA(ipaPath,ages[0]);
//        for (String key : mapIpa.keySet()) {
//            System.out.println(key + ":" + mapIpa.get(key));
//        }

        /**
         * 根据ipa包获取的信息创建plist文件
         */
        createPlistFile(ages[0],ages[1],mapIpa.get("package").toString(),mapIpa.get("versionName").toString(),mapIpa.get("CFBundleName").toString(),"https://fzbd.t.faxuan.net/ipa/" + ages[2] + "/icon.png");
        /**
         * 生成二维码
         */
        try {
            QrCodeUtil.createQrCode( ages[0] + "/ipa.jpg","itms-services:///?action=download-manifest&url=https://fzbd.t.faxuan.net/ipa/" + ages[2] + "/manifest.plist",1100,"JPEG");
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("二维码文件生成失败");
            e.printStackTrace();
        }
    }
}