package com.example.emos.wx;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class TestDemo {
    public static void main(String[] args) {
//        DateTime date = DateUtil.date();
//        System.out.println(date);
//        DateTime today = DateUtil.parseDateTime("2022-11-20 10:10:10");
//        DateTime d = DateUtil.parse("2022-11-20 10:10:10");
//        System.out.println(today);
//        System.out.println(d);
//        System.out.println(DateField.DAY_OF_MONTH.getValue());
//        System.out.println(DateField.DAY_OF_YEAR.getValue());

//        Date date = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        String format = sdf.format(date);
//        System.out.println(date);
//        System.out.println(format);

//        System.out.println(DateUtil.today());

        ArrayList<HashMap> list = new ArrayList<>();
        HashMap map = new HashMap();
        map.put("省份","杭州");
        list.add(map);
//        System.out.println(list);
        map.put("国籍","中国");
        System.out.println(list);
    }
}
