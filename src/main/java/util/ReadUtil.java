package util; /**
 * Created by song on 2018/10/22.
 */

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.google.zxing.WriterException;
import org.testng.annotations.Test;

import java.io.*;
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
            File file = new File(ipaPath);
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
                    if (null != name &&
                            name.toLowerCase().contains(".app/info.plist")) {
                        ByteArrayOutputStream _copy = new
                                ByteArrayOutputStream();
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
                                    icon = CFBundleIconFiles.getArray()[0].toString();
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

            //根据图标名称下载图标文件到指定位置
            if (iconPath.length == 1) {
                while ((ze2 = zipIns2.getNextEntry()) != null) {
                    if (!ze2.isDirectory()) {
                        String name = ze2.getName();
//System.out.println(name);
                        if(name.contains(icon.trim())){
//System.out.println(11111);
                            NSString parameters = (NSString) rootDict.get("CFBundleDisplayName");
                            File iconImage = new File(iconPath[0] + "/icon.png");
                            FileOutputStream fos = new FileOutputStream(iconImage);
                            int chunk = 0;
                            byte[] data = new byte[1024];
                            while(-1!=(chunk=zipIns2.read(data))){
                                fos.write(data, 0, chunk);
                            }
                            fos.close();
                            ProcessingPictures(iconPath[0]);
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
            parameters = (NSString) rootDict.get("CFBundleDisplayName");
            map.put("CFBundleDisplayName", parameters.toString());

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
            System.out.println("Done");
        } catch (IOException e) {
            System.out.println("文件创建失败！！！");
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
        createPlistFile(ages[0],ages[1],mapIpa.get("package").toString(),mapIpa.get("versionName").toString(),mapIpa.get("CFBundleDisplayName").toString(),"https://fzbd.t.faxuan.net/ipa/" + ages[2] + "/icon.png");
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