package com.example.demo.util;

import com.alibaba.fastjson.JSONObject;

public class JSONUtil {
    public boolean isjson(String string){
        try {
            JSONObject jsonStr= JSONObject.parseObject(string);
            return  true;
        } catch (Exception e) {
            return false;
        }
    }

}
