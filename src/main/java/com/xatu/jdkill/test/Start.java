package com.xatu.jdkill.test;

import cn.hutool.core.io.resource.ClassPathResource;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: lianghuan
 * @date: 2021/1/8 20:59
 */
public class Start {
  static final String headerAgent = "User-Agent";
  static final String headerAgentArg =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36";
  static final String Referer = "Referer";
  static final String RefererArg = "https://passport.jd.com/new/login.aspx";
  // 商品id
  static String pid = null;
  // eid
  static String eid = "";
  // fp
  static String fp = "";
  // 抢购数量
  static volatile Integer num = 1;

  static CookieManager manager = new CookieManager();

  static Properties config;

  // 读取配置文件
  static {
    ClassPathResource resource = new ClassPathResource("application.properties");
    config = new Properties();
    try {
      config.load(resource.getStream());
      pid = config.getProperty("pid");
      if (pid == null || pid.equals("")) {
        System.out.println("请在配置文件中配置商品id");
        System.exit(0);
      }
      num = Integer.parseInt(config.getProperty("num"));
      eid = config.getProperty("eid");
      fp = config.getProperty("fp");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException, ParseException {
    CookieHandler.setDefault(manager);
    // 获取venderId
    //        String shopDetail = util.get(null, "https://item.jd.com/" + RushToPurchase.pid +
    // ".html");
    //        String venderID = shopDetail.split("isClosePCShow: false,\n" +
    //                "                venderId:")[1].split(",")[0];
    //        RushToPurchase.venderId = venderID;
    // 登录
    Login.login();
    // 判断是否开始抢购
    judgePruchase();
    // 开始抢购
    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(
            5,
            10,
            1000,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
    for (int i = 0; i < 5; i++) {
      threadPoolExecutor.execute(new RushToPurchase());
    }
    new RushToPurchase().run();
  }

  public static void judgePruchase() throws IOException, ParseException, InterruptedException {
    // 获取开始时间
    JSONObject headers = new JSONObject();
    headers.put(Start.headerAgent, Start.headerAgentArg);
    headers.put(Start.Referer, Start.RefererArg);
    JSONObject shopDetail =
        JSONObject.parseObject(
            HttpUrlConnectionUtil.get(
                headers, "https://item-soa.jd.com/getWareBusiness?skuId=" + pid));
    if (shopDetail.get("yuyueInfo") != null) {
      String buyDate =
          JSONObject.parseObject(shopDetail.get("yuyueInfo").toString()).get("buyTime").toString();
      String startDate = buyDate.split("-202")[0] + ":00";
      Long startTime = HttpUrlConnectionUtil.dateToTime(startDate);
      // 开始抢购
      while (true) {
        // 获取京东时间
        JSONObject jdTime =
            JSONObject.parseObject(
                HttpUrlConnectionUtil.get(headers, "https://a.jd.com//ajax/queryServerData.html"));
        Long serverTime = Long.valueOf(jdTime.get("serverTime").toString());
        if (startTime >= serverTime) {
          System.out.println("正在等待抢购时间");
          Thread.sleep(300);
        } else {
          break;
        }
      }
    }
  }
}
