package ren.perry.GetImage;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"ALL"})
public class Main {

    private static String productUrl;

    public static void main(String[] args) throws IOException {
        init();
    }

    private static void init() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("请输入技术部暗号：");
        String in = br.readLine().replaceAll(" ", "");
        if (!in.equals("1024")) {
            System.err.println("你输入的暗号不对哟~  去问问技术部的小哥哥再来吧~  本程序将在3秒后自动退出");
            exitSystem();
        } else {
            while (true) {
                System.out.println("请输入需要爬取图片的网站类型：\n0：退出\t\t1：1688\t\t2：淘宝\t\t3：天猫\t\t4：京东");
                String type = br.readLine().replaceAll(" ", "");
                switch (type) {
                    case "0":
                        System.err.println("感谢使用~ 本程序将在3秒后自动退出");
                        exitSystem();
                        return;
                    case "1":
                        startJsoupEngine(1, br);
                        break;
                    case "2":
                        startJsoupEngine(2, br);
                        break;
                    case "3":
                        startJsoupEngine(3, br);
                        break;
                    case "4":
                        startJsoupEngine(4, br);
                        break;
                }
            }
        }
    }

    /*
     * -----------------------------------------------------------------------------------------------------------------
     */

    /**
     * 启动Jsoup前检查url
     *
     * @param type 1：1688  2：淘宝  3：京东
     * @param br   BufferedReader
     */
    private static void startJsoupEngine(int type, BufferedReader br) throws IOException {
        while (true) {
            String hint = "";
            switch (type) {
                case 1:
                    hint = "----------------------------\n请输入1688产品详情的网址(输入0返回上一步)：";
                    break;
                case 2:
                    hint = "----------------------------\n请输入淘宝产品详情的网址(输入0返回上一步)：";
                    break;
                case 3:
                    hint = "----------------------------\n请输入天猫产品详情的网址(输入0返回上一步)：";
                    break;
                case 4:
                    hint = "----------------------------\n请输入京东产品详情的网址(输入0返回上一步)：";
                    break;
            }
            System.out.println(hint);
            String url = br.readLine().replaceAll(" ", "");
            if (url.equals("0")) return;
            if (isNetUrl(url)) {
                productUrl = url;
                switch (type) {
                    case 1:
                        get1688();
                        break;
                    case 2:
                        getTaobao();
                        break;
                    case 3:
                        getTmall();
                        break;
                    case 4:
                        getJd();
                        break;
                }
            } else {
                System.err.println("格式不对哟~");
            }
        }
    }

    /**
     * 爬取及下载1688
     */
    private static void get1688() throws IOException {
        List<String> bannerImgUrls = new ArrayList<>();
        List<String> detailImgUrls = new ArrayList<>();

        System.out.println("开始获取网页信息...");
        Document doc = Jsoup.connect(productUrl).get();

        //获取Title
        String title = doc.select("h1[class=d-title]").get(0).text();
        title = title.replaceAll("/", "");
        title = title.replaceAll("\\|", " ");
        title = title.replaceAll("\\*", "x");
        System.out.println(title);

        /*
         * 获取Banner图
         */
        //获取第一张
        Elements lisFirst = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger active]");
        if (lisFirst.size() >= 1) {
            JSONObject jo1 = (JSONObject) JSONObject.parse(lisFirst.get(0).attr("data-imgs"));
            String url1 = jo1.getString("original");
            bannerImgUrls.add(imgUrlAddHttp(url1));
        }

        //获取最后一张
        Elements lisLast = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger last-col]");
        if (lisLast.size() >= 1) {
            JSONObject jo2 = (JSONObject) JSONObject.parse(lisLast.get(0).attr("data-imgs"));
            String url2 = jo2.getString("original");
            bannerImgUrls.add(imgUrlAddHttp(url2));
        }

        //获取剩余
        Elements lis = doc.select("ul[class=nav nav-tabs fd-clr]").get(0).select("li[class=tab-trigger]");
        for (Element li : lis) {
            String urlJson = li.attr("data-imgs");
            JSONObject jo = JSONObject.parseObject(urlJson);
            String url = jo.getString("original");
            bannerImgUrls.add(imgUrlAddHttp(url));
        }
        System.out.println("产品图：" + bannerImgUrls.size() + "张");

        /*
         * 获取详情图
         */
        String detailDataUrl = doc.select("div[class=desc-lazyload-container]").get(0).attr("data-tfs-url");
        String detailDataResult = sendGet(imgUrlAddHttp(detailDataUrl), "");
        Document detailContent = Jsoup.parse(detailDataResult);
        Elements detailImg = detailContent.select("img");
        for (Element img : detailImg) {
            String imgUrl = imgUrlAddHttp(img.attr("src"));

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

        //创建文件夹
        String folderPath[] = createPathFolder(title, "1688产品图片下载");

        //下载产品图图片
        System.out.println("开始下载产品图...");
        for (int i = 0; i < bannerImgUrls.size(); i++) {
            downloadImage(bannerImgUrls.get(i), folderPath[0] + (i + 1) + ".jpg");
        }

        //下载详情图
        System.out.println("开始下载详情图...");
        for (int i = 0; i < detailImgUrls.size(); i++) {
            downloadImage(detailImgUrls.get(i), folderPath[1] + (i + 1) + ".jpg");
        }

        System.out.println("下载完成");
        productUrl = "";
    }

    /**
     * 爬取及下载淘宝
     */
    private static void getTaobao() throws IOException {
        List<String> bannerImgUrls = new ArrayList<>();
        List<String> detailImgUrls = new ArrayList<>();

        System.out.println("开始获取网页信息...");
        Document doc = Jsoup.connect(productUrl).get();

        //获取Title
        String title = doc.select("h3[class=tb-main-title]").attr("data-title");
        title = title.replaceAll("/", "");
        title = title.replaceAll("\\|", " ");
        title = title.replaceAll("\\*", "x");
        System.out.println(title);

        /*
         * 获取Banner图
         */
        Element bannerScript = doc.getElementsByTag("script").get(0);
        String auctionImageSplit = bannerScript.data().split("auctionImages")[1];
        String auctionImageData = auctionImageSplit.substring(auctionImageSplit.indexOf("["), auctionImageSplit.indexOf("]") + 1);
        JSONArray bannerArray = JSONArray.parseArray(auctionImageData);
        for (Object o : bannerArray) {
            bannerImgUrls.add(imgUrlAddHttp(o.toString()));
        }
        System.out.println("产品图：" + bannerImgUrls.size() + "张");

        /*
         * 获取详情图
         */
        Element detailScript = doc.getElementsByTag("script").get(0);
        String descUrlSplit = detailScript.data().split("descUrl")[1];
        String descUrlAll = descUrlSplit.substring(descUrlSplit.indexOf("//"), descUrlSplit.indexOf("counterApi"));
        String descUrl_1 = descUrlAll.substring(descUrlAll.indexOf("//"), descUrlAll.indexOf("'"));
        String descUrlSub_2 = descUrlAll.substring(descUrl_1.length(), descUrlAll.lastIndexOf("'"));
        String descUrl_2 = descUrlSub_2.substring(descUrlSub_2.indexOf("//"));

        String descResult_1 = sendGet(imgUrlAddHttp(descUrl_1), "");
        Document descContent_1 = Jsoup.parse(descResult_1);
        Elements detailImg_1 = descContent_1.select("img");
        for (Element img : detailImg_1) {
            String imgUrl = imgUrlAddHttp(img.attr("src"));

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

        if (detailImgUrls.size() < 1) {
            String descResult_2 = sendGet(imgUrlAddHttp(descUrl_2), "");
            Document descContent_2 = Jsoup.parse(descResult_2);
            Elements detailImg_2 = descContent_2.select("img");
            for (Element img : detailImg_2) {
                String imgUrl = imgUrlAddHttp(img.attr("src"));
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
        }
        System.out.println("详情图：" + detailImgUrls.size() + "张");

        //创建文件夹
        String folderPath[] = createPathFolder(title, "淘宝产品图片下载");

        //下载产品图图片
        System.out.println("开始下载产品图...");
        for (int i = 0; i < bannerImgUrls.size(); i++) {
            downloadImage(bannerImgUrls.get(i), folderPath[0] + (i + 1) + ".jpg");
        }

        //下载详情图
        System.out.println("开始下载详情图...");
        for (int i = 0; i < detailImgUrls.size(); i++) {
            downloadImage(detailImgUrls.get(i), folderPath[1] + (i + 1) + ".jpg");
        }

        System.out.println("下载完成");
        productUrl = "";
    }

    /**
     * 爬取及下载天猫
     */
    private static void getTmall() throws IOException {
        List<String> bannerImgUrls = new ArrayList<>();
        List<String> detailImgUrls = new ArrayList<>();

        System.out.println("开始获取网页信息...");
        Document doc = Jsoup.connect(productUrl).get();

        //获取Title
        String title = doc.select("div[class=tb-detail-hd]").select("h1").text();
        title = title.replaceAll("/", "");
        title = title.replaceAll("\\|", " ");
        title = title.replaceAll("\\*", "x");
        System.out.println(title);

        /*
         * 获取Banner图
         */
        Elements bannerScripts = doc.getElementsByTag("script");
        Element bannerScript = null;
        for (Element bs : bannerScripts) {
            if (bs.toString().contains("TShop.Setup")) {
                bannerScript = bs;
            }
        }

        String bannerImageSplit = bannerScript.data().split("TShop.Setup")[1];
        String bannerImageData = bannerImageSplit.substring(bannerImageSplit.indexOf("(") + 1, bannerImageSplit.lastIndexOf(");") - 9);
        String bannerImageDataSub = bannerImageData.substring(bannerImageData.indexOf("{"), bannerImageData.lastIndexOf("}") + 1);
        JSONObject bannerImageDataJO = JSONObject.parseObject(bannerImageDataSub);
        JSONArray bannerImageJA = bannerImageDataJO.getJSONObject("propertyPics").getJSONArray("default");
        for (Object o : bannerImageJA) {
            bannerImgUrls.add(imgUrlAddHttp(o.toString()));
        }
        System.out.println("产品图：" + bannerImgUrls.size() + "张");

        /*
         * 获取详情图
         */
        JSONObject apiJO = bannerImageDataJO.getJSONObject("api");
        String detailUrl_1 = imgUrlAddHttp(apiJO.getString("descUrl"));
        String detailUrl_2 = imgUrlAddHttp(apiJO.getString("httpsDescUrl"));
        String detailUrl_3 = imgUrlAddHttp(apiJO.getString("fetchDcUrl"));
        Document detailContent_1 = Jsoup.parse(sendGet(detailUrl_1, ""));
        Elements detailImg_1 = detailContent_1.select("img");
        for (Element img : detailImg_1) {
            String imgUrl = imgUrlAddHttp(img.attr("src"));
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
        if (detailImgUrls.size() < 1) {
            Document detailContent_2 = Jsoup.parse(sendGet(detailUrl_2, ""));
            Elements detailImg_2 = detailContent_2.select("img");
            for (Element img : detailImg_2) {
                String imgUrl = imgUrlAddHttp(img.attr("src"));
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
        }
        if (detailImgUrls.size() < 1) {
            Document detailContent_3 = Jsoup.parse(sendGet(detailUrl_3, ""));
            Elements detailImg_3 = detailContent_3.select("img");
            for (Element img : detailImg_3) {
                String imgUrl = imgUrlAddHttp(img.attr("src"));
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
        }
        System.out.println("详情图：" + detailImgUrls.size() + "张");

        //创建文件夹
        String folderPath[] = createPathFolder(title, "天猫产品图片下载");

        //下载产品图图片
        System.out.println("开始下载产品图...");
        for (int i = 0; i < bannerImgUrls.size(); i++) {
            downloadImage(bannerImgUrls.get(i), folderPath[0] + (i + 1) + ".jpg");
        }

        //下载详情图
        System.out.println("开始下载详情图...");
        for (int i = 0; i < detailImgUrls.size(); i++) {
            downloadImage(detailImgUrls.get(i), folderPath[1] + (i + 1) + ".jpg");
        }

        System.out.println("下载完成");
        productUrl = "";

    }

    /**
     * 爬取及下载京东
     */
    private static void getJd() throws IOException {
        List<String> bannerImgUrls = new ArrayList<>();
        List<String> detailImgUrls = new ArrayList<>();

        System.out.println("开始获取网页信息...");
        Document doc = Jsoup.connect(productUrl).get();

        //获取Title
        String title = doc.select("div[class=sku-name]").text();
        title = title.replaceAll("/", "");
        title = title.replaceAll("\\|", " ");
        title = title.replaceAll("\\*", "x");
        System.out.println(title);

        /*
         * 获取Banner图
         * http://img13.360buyimg.com/imgzone
         * script
         */
        String jdImageDomain = "https://img13.360buyimg.com/imgzone/";
        Elements bannerScripts = doc.getElementsByTag("script");
        Element bannerScript = null;
        for (Element bs : bannerScripts) {
            if (bs.toString().contains("imageList")) {
                bannerScript = bs;
            }
        }

        String bannerImageSplit = bannerScript.data().split("imageList")[1];
        String bannerImageDataSub = bannerImageSplit.substring(bannerImageSplit.indexOf("["), bannerImageSplit.indexOf("]") + 1);
        JSONArray bannerImageDataJA = JSONArray.parseArray(bannerImageDataSub);
        for (Object o : bannerImageDataJA) {
            bannerImgUrls.add(jdImageDomain + o.toString());
        }
        System.out.println("产品图：" + bannerImgUrls.size() + "张");

        /*
         * 获取详情图
         */
        String detailUrlSplit = bannerScript.data().split("desc:")[1];
        String detailUrlSub = detailUrlSplit.substring(detailUrlSplit.indexOf("//"), detailUrlSplit.indexOf(",") - 1);
        String detailUrl = imgUrlAddHttp(detailUrlSub);
        JSONObject detailContentJo = JSONObject.parseObject(sendGet(detailUrl, ""));
        Document detailContent = Jsoup.parse(detailContentJo.getString("content"));
        Elements detailImg = detailContent.select("img");
        for (Element img : detailImg) {
            String imgUrl = imgUrlAddHttp(img.attr("data-lazyload"));
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

        //创建文件夹
        String folderPath[] = createPathFolder(title, "京东产品图片下载");

        //下载产品图图片
        System.out.println("开始下载产品图...");
        for (int i = 0; i < bannerImgUrls.size(); i++) {
            downloadImage(bannerImgUrls.get(i), folderPath[0] + (i + 1) + ".jpg");
        }

        //下载详情图
        System.out.println("开始下载详情图...");
        for (int i = 0; i < detailImgUrls.size(); i++) {
            downloadImage(detailImgUrls.get(i), folderPath[1] + (i + 1) + ".jpg");
        }

        System.out.println("下载完成");
        productUrl = "";

    }

    /*
     * -----------------------------------------------------------------------------------------------------------------
     *
     */

    /**
     * 创建路劲文件夹
     *
     * @param type 类型
     */
    private static String[] createPathFolder(String title, String type) {
        //路径
        int year, month, day;
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH) + 1;
        day = c.get(Calendar.DATE);

        String date = "/" + year + "-" + month + "-" + day + "/";

        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory().getAbsoluteFile();
        String folderPath = desktopDir + "/" + type + "/";
        String datePath = folderPath + date;
        String titleFolder = datePath + title + "/";
        String productFolder = datePath + title + "/产品图/";
        String detailFolder = datePath + title + "/详情图/";

        createFolder(folderPath);
        createFolder(datePath);
        createFolder(titleFolder);
        createFolder(productFolder);
        createFolder(detailFolder);
        return new String[]{productFolder, detailFolder};
    }

    /**
     * 图片前加http
     *
     * @param url 图片url
     * @return 图片http://url
     */
    private static String imgUrlAddHttp(String url) {
        while (url.indexOf("/") == 0 || url.indexOf(" ") == 0) {
            url = url.substring(1);
        }
        if (!url.substring(0, 5).contains("http")) url = "http://" + url;
        return url;
    }

    /**
     * 判断是否是网址
     *
     * @param url url
     * @return boolean
     */
    private static boolean isNetUrl(String url) {
        boolean reault = false;
        if (url != null) {
            if (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("rtsp") || url.toLowerCase().startsWith("mms")) {
                reault = true;
            }
        }
        return reault;
    }

    /**
     * 创建文件夹
     *
     * @param path 路径
     */
    private static void createFolder(String path) {
        File file = new File(path);
        //如果文件夹不存在
        if (!file.exists()) {
            //创建文件夹
            file.mkdir();
        }
    }

    /**
     * 下载图片到本地
     *
     * @param urlList 图片地址
     * @param path    路径
     */
    private static void downloadImage(String urlList, String path) throws IOException {
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
    }

    /**
     * 3s后退出程序
     */
    private static void exitSystem() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    private static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    private static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}