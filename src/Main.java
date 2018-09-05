import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.HttpRequest;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"Duplicates", "RegExpRedundantEscape"})
public class Main {

    private static String productUrl;

    public static void main(String[] args) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.println("请输入技术部暗号：");
            String in = br.readLine().replaceAll(" ", "");
            if (!in.equals("1024")) {
                System.out.println("你输入的暗号不对哟~  去问问技术部的小哥哥吧~");
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        System.exit(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                while (true) {
                    System.out.println("----------------------------\n请输入需要下载图片的1688产品详情的网址：");
                    String url = br.readLine().replaceAll(" ", "");
                    if (url.equals("0")) {
                        System.out.println("拜拜咯~");
                        System.exit(0);
                    }
                    String regex = "^([hH][tT]{2}[pP]:/*|[hH][tT]{2}[pP][sS]:/*|[fF][tT][pP]:/*)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+(\\?{0,1}(([A-Za-z0-9-~]+\\={0,1})([A-Za-z0-9-~]*)\\&{0,1})*)$";
                    Pattern pattern = Pattern.compile(regex);
                    if (pattern.matcher(url).matches()) {
                        productUrl = url;
                        jsoupHtml();
                    } else {
                        System.out.println("格式不对哟~\n----------------------------");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void jsoupHtml() {
        List<String> bannerImgUrls = new ArrayList<>();
        List<String> detailImgUrls = new ArrayList<>();
        try {
            System.out.println("开始获取网页信息...");
            Document doc = Jsoup.connect(productUrl).get();

            //获取Title
            String title = doc.select("h1[class=d-title]").get(0).text();
            title = title.replaceAll("/", "");
            System.out.println(title);

            /*
             * 获取Banner图
             */
            //获取第一张
            Elements lisFirst = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger active]");
            if (lisFirst.size() >= 1) {
                JSONObject jo1 = (JSONObject) JSONObject.parse(lisFirst.get(0).attr("data-imgs"));
                String url1 = jo1.getString("original");
                while (url1.indexOf("/") == 0) {
                    url1 = url1.substring(1);
                }
                if (!url1.contains("http")) url1 = "http://" + url1;
                bannerImgUrls.add(url1);
            }

            //获取最后一张
            Elements lisLast = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger last-col]");
            if (lisLast.size() >= 1) {
                JSONObject jo2 = (JSONObject) JSONObject.parse(lisLast.get(0).attr("data-imgs"));
                String url2 = jo2.getString("original");
                while (url2.indexOf("/") == 0) {
                    url2 = url2.substring(1);
                }
                if (!url2.contains("http")) url2 = "http://" + url2;
                bannerImgUrls.add(url2);
            }

            //获取剩余
            Elements lis = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger]");
            for (Element li : lis) {
                String urlJson = li.attr("data-imgs");
                JSONObject jo = JSONObject.parseObject(urlJson);
                String url = jo.getString("original");
                while (url.indexOf("/") == 0) {
                    url = url.substring(1);
                }
                if (!url.contains("http")) url = "http://" + url;
                bannerImgUrls.add(url);
            }
            System.out.println("产品图：" + bannerImgUrls.size() + "张");

            /*
             * 获取详情图
             */
            String detailDataUrl = doc.select("div[class=desc-lazyload-container]").get(0).attr("data-tfs-url");
            String detailDataResult = HttpRequest.sendGet(detailDataUrl, "");
            Document detailContent = Jsoup.parse(detailDataResult);
            Elements detailImg = detailContent.select("img");
            for (Element img : detailImg) {
                String imgUrl = img.attr("src");
                while (imgUrl.indexOf("/") == 0) {
                    imgUrl = imgUrl.substring(1);
                }
                if (!imgUrl.contains("http")) imgUrl = "http://" + imgUrl;
                Pattern pattern = Pattern.compile("http://(?!(\\.jpg|\\.png)).+?(\\.jpg|\\.png)");
                Pattern pattern1 = Pattern.compile("https://(?!(\\.jpg|\\.png)).+?(\\.jpg|\\.png)");
                Matcher matcher = pattern.matcher(imgUrl);
                Matcher matcher1 = pattern1.matcher(imgUrl);
                while (matcher.find()) {
                    detailImgUrls.add(matcher.group(0));
                }
                while (matcher1.find()) {
                    detailImgUrls.add(matcher1.group(0));
                }
            }
            System.out.println("详情图：" + detailImgUrls.size() + "张");

            //路径
            File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory().getAbsoluteFile();
//            System.out.println("桌面=" + desktopDir);
//            String desktopPath = new File("").getAbsolutePath();
            String folderPath = desktopDir + "/1688产品图片下载/";
            String titleFolder = folderPath + title + "/";
            String productFolder = folderPath + title + "/产品图/";
            String detailFolder = folderPath + title + "/详情图/";


            //创建文件夹
            createFolder(folderPath);
            createFolder(titleFolder);
            createFolder(productFolder);
            createFolder(detailFolder);


            //下载产品图图片
            System.out.println("开始下载产品图...");
            for (int i = 0; i < bannerImgUrls.size(); i++) {
                downloadImage(bannerImgUrls.get(i), productFolder + (i + 1) + ".jpg");
            }

            //下载详情图
            System.out.println("开始下载详情图...");
            for (int i = 0; i < detailImgUrls.size(); i++) {
                downloadImage(detailImgUrls.get(i), detailFolder + (i + 1) + ".jpg");
            }

            System.out.println("下载完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建文件夹
     *
     * @param path 路径
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void createFolder(String path) {
        File file = new File(path);
        if (!file.exists()) {//如果文件夹不存在
            file.mkdir();//创建文件夹
        }
    }

    /**
     * 下载图片到本地
     *
     * @param urlList 图片地址
     * @param path    路径
     */
    private static void downloadImage(String urlList, String path) {
        try {
            URL url = new URL(urlList);
            DataInputStream dataInputStream = new DataInputStream(url.openStream());

            FileOutputStream fileOutputStream = new FileOutputStream(new File(path));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = dataInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            fileOutputStream.write(output.toByteArray());
            dataInputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}