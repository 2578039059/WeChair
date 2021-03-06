package com.at.wechair.service.impl;

import com.at.wechair.entity.OrdinaryUser;
import com.at.wechair.mapper.LoginDao;
import com.at.wechair.service.LoginService;
import com.at.wechair.util.AesCbcUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author: Annoming
 * @Date: 2020/5/5
 * @Time: 10:42
 * @Description
 */
@Component
@PropertySource(value = "classpath:/sql.properties")
@ConfigurationProperties(prefix = "login")
@Transactional(rollbackFor = Exception.class)
@Service
public class LoginServiceImpl implements LoginService {
    private static final String OPEN_ID = "open_id";
    private OrdinaryUser user = new OrdinaryUser();
    @Resource
    private LoginDao loginDao;
    @Value("${login.updateSql}")
    private String updateSql;
    @Value("${login.insertSessionSql}")
    private String insertSessionSql;
    @Value("${login.updateInfoSql}")
    private String updateInfoSql;
    @Value("${login.updateSessionSql}")
    private String updateSessionSql;
    @Value("${login.updateImageSql}")
    private String updateImageSql;
    @Value("${login.updateAuthSql}")
    private String updateAuthSql;
    private static final String MAN = "1";
    private static final String WOMAN = "0";



    @Override
    public HashMap<String, Object> getUserAuthorities(String openId, HashMap<String, Object> map) {
        OrdinaryUser user = loginDao.getUserInfo(map);
        try {
            map.put("ownAuthority", user.getOwnAuthority());
            if (user.getOpenId() != null) {
                map.put("status", 1);
                map.put("msg", "获取用户权限成功");
            } else {
                map.put("status", 0);
                map.put("msg", "获取用户权限失败");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public HashMap<String, Object> decryptUserInfo(String encryptedData, String sessionKey, String iv, HashMap<String, Object> map) {

        try {
            // 调用工具类方法对encryptedData加密数据进行AES解密
            if (loginDao.getUserInfo(map) == null) {
                storageUserInfo(insertSessionSql, new Object[]{map.get(OPEN_ID), map.get("session_key"),400});
            } else {
                storageUserInfo(updateSessionSql, new Object[]{map.get("session_key"),map.get(OPEN_ID)});
            }
            String string = AesCbcUtil.decrypt(encryptedData, sessionKey, iv, "UTF-8");
            JSONObject result = new JSONObject(string);
            map = getMap(result, map);
            dealWithInfo(map);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 存储解密数据
     *
     * @param result 解密结果
     * @param map    存储解密数据对象
     * @return map
     * @throws JSONException JSON对象异常
     */
    public HashMap<String, Object> getMap(JSONObject result, HashMap<String, Object> map) throws JSONException {
        if (result != null && result.length() > 0) {
            map.put("status", 1);
            map.put("msg", "解密成功");
            map.put("nickName", result.get("nickName"));
            map.put("gender", result.get("gender"));
            map.put("city", result.get("city"));
            map.put("province", result.get("province"));
            map.put("country", result.get("country"));
            map.put("avatarUrl", result.get("avatarUrl"));
        } else {
            map.put("status", 0);
            map.put("msg", "解密失败");
        }
        return map;
    }

    /**
     * 首次登陆用户解密的用户信息插入数据库
     *
     * @param map 存储数据的map容器
     */
    public void dealWithInfo(Map<String, Object> map) {
        user.setOpenId((String) map.get("open_id"));
        user.setSessionKey((String) map.get("session_key"));
        user.setUserName((String) map.get("nickName"));
        String gender = map.get("gender").toString();
        if (MAN.equals(gender)) {
            user.setSex("男");
        } else if (WOMAN.equals(gender)) {
            user.setSex("女");
        } else {

            System.out.println("性别信息获取异常");
        }
        Object[] params = {user.getSex(), user.getUserName(), user.getSessionKey(), user.getOpenId()};
        boolean result = storageUserInfo(updateSql, params);
        if (result) {
            System.out.println("数据存储成功");
        } else {
            System.out.println("数据存储失败");
        }
    }
    @Override
    public boolean storageUserInfo(String sql, Object[] params) {
        return loginDao.dataOperation(sql, params);
    }

//    @Override
//    public boolean updateUserInfo(Object[] params) {
//        return loginDao.dataOperation(updateInfoSql, params);
//
//    }

    @Override
    public boolean updateUserImage(String fileName,String openId) {
        Object[] params = {fileName,openId};
        storageUserInfo(updateAuthSql,new Object[]{500,openId});
        return loginDao.dataOperation(updateImageSql, params);
    }


}

