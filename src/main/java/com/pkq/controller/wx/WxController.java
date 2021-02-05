package com.pkq.controller.wx;

import com.alibaba.fastjson.JSONObject;
import com.pkq.model.*;
import com.pkq.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/wx")
public class WxController {

    private static final Logger logger = LoggerFactory.getLogger(WxController.class);

    private static final String WX_OPEN_URL = "https://open.weixin.qq.com/connect/qrconnect?" +
            "appid={0}&redirect_uri={1}&response_type=code&" +
            "scope=snsapi_login&state={2}#wechat_redirect";

    private static final String WX_API_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
            "appid={0}&secret={1}&code={2}&grant_type=authorization_code";

    private static final String  WX_REFRESH_TOKEN_URL= "https://api.weixin.qq.com/sns/oauth2/refresh_token?" +
            "appid={0}&grant_type=refresh_token&refresh_token={1}";

    @Autowired
    private WxProperties wxProperties;

    @GetMapping("/refreshWxToken/{token}")
    public void refreshToken(@PathVariable("token")String token){
        String url = MessageFormat.format(WX_REFRESH_TOKEN_URL,new Object[]{wxProperties.getAppid()
                ,token});
        RefreshTokenResp refreshTokenResp = HttpClientUtil.getClient().get(url, null, RefreshTokenResp.class);
        logger.info(JSONObject.toJSONString(refreshTokenResp));
    }

    @GetMapping("/url")
    public void wxLoginUrl(HttpServletResponse response) throws IOException {
        String url = MessageFormat.format(WX_OPEN_URL,new Object[]{
                wxProperties.getAppid(),wxProperties.getRedirectUri(),wxProperties.getState()});
        logger.info("wx url:{}",url);
        String wxLoginResp = HttpClientUtil.getClient().get(url, null, String.class);
        PrintWriter writer = response.getWriter();
        writer.write(wxLoginResp);
        writer.flush();
        writer.close();
    }


    @GetMapping("/login")
    public WxLoginResp wxLogin(@RequestParam(value = "code", required = false) String code,
                          @RequestParam(value = "state") String state){
        logger.info("code:{}, state:{}", code, state);
        if(null == code){
            throw new RuntimeException("微信登录失败");
        }
        String wxApiUrl = MessageFormat.format(WX_API_URL, new Object[]{wxProperties.getAppid()
                , wxProperties.getAppSecret(), code});
        WxLoginResp wxLoginResp = HttpClientUtil.getClient().get(wxApiUrl, null, WxLoginResp.class);
        if(Objects.isNull(wxLoginResp)){
            throw new RuntimeException("微信登录失败");
        }
        return wxLoginResp;
    }


    // http://zhikesit.ecpic.com.cn/wx/check
    @GetMapping("/check")
    public String check(WxCheckReq req) throws NoSuchAlgorithmException {
        logger.info(JSONObject.toJSONString(req));

        logger.info("token: {}", wxProperties.getToken());
        String echostr = req.getEchostr();
        String nonce = req.getNonce();
        String timestamp = req.getTimestamp();
        String signature = req.getSignature();
        if(null == echostr || nonce == null || timestamp == null || signature == null){
            return "error";
        }
        List<String> list = new ArrayList<>();
        list.add(wxProperties.getToken());
        list.add(timestamp);
        list.add(nonce);
        list.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder();
        list.stream().forEach(e ->{
            sb.append(e);
        });
        logger.info("sha1 old :{}", sb.toString());
        String hashcode = getSha1(sb.toString().getBytes());
        logger.info("handle/GET func: hashcode:{}, signature:{}", hashcode, signature);
        if(hashcode.equals(signature))
            return echostr;
        return "error";
    }
    @GetMapping("test")
    public TestHttpClientResp test(){
        TestHttpClientResp resp = new TestHttpClientResp();
        resp.setName("helloClient");
        resp.setPwd("您好");
        return resp;
    }
    private static String getSha1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
