package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.task.EmailTack;
import com.example.emos.wx.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {
    @Autowired
    private SystemConstants constants;

    @Autowired(required = false)
    private TbHolidaysDao holidaysDao;

    @Autowired(required = false)
    private TbWorkdayDao workdayDao;

    @Autowired(required = false)
    private TbCheckinDao checkinDao;

    @Autowired(required = false)
    private TbFaceModelDao faceModelDao;

    @Autowired(required = false)
    private TbCityDao cityDao;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Value("${emos.code}")
    private String code;

    @Autowired
    private EmailTack emailTack;

    @Autowired(required = false)
    private TbUserDao userDao;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;
        String type = "?????????";
        if (DateUtil.date().isWeekend()) {
            type = "?????????";
        }
        if (bool_1) {
            type = "?????????";
        } else if (bool_2) {
            type = "?????????";
        }
        // ===================================
        type = "?????????";
        if (type.equals("?????????")) {
            return "????????????????????????";
        } else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "??????????????????????????????";
            } else if (now.isAfter(attendanceEnd)) {
                return "?????????????????????????????????";
            } else {
                HashMap map = new HashMap();
                map.put("userId",userId);
                map.put("date",date);
                map.put("start",start);
                map.put("end",end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "???????????????????????????????????????" : "????????????";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
        Date d1 = DateUtil.date();
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime);
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
        int status = 1;
        if (d1.compareTo(d2) <= 0) {
            status = 1;
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) <= 0) {
            status = 2;
        }
        int userId = (Integer)param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel == null) {
            throw new EmosException("?????????????????????");
        } else {
            String path = (String)param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path),"targetModel", faceModel);
            //request.form("code", code);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                log.error("????????????????????????");
                throw new EmosException("????????????????????????");
            }
            String body = response.body();
            // ============>
            body = "True";
            if ("?????????????????????".equals(body) || "???????????????????????????".equals(body)) {
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("??????????????????????????????");
            } else if ("True".equals(body)) {
                // ????????????????????????
                int risk = 1;
                String address = (String)param.get("address");
                String country = (String)param.get("country");
                String province = (String)param.get("province");
                String city = (String)param.get("city");
                String district = (String)param.get("district");
                if (!StrUtil.isBlank(city) && !StrUtil.isBlank(district)) {
                    String code = cityDao.searchCode(city);
                    try {
//                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
//                        Document document = Jsoup.connect(url).get();
//                        Elements elements = document.getElementsByClass("list-content");
                        // ===========>
                        String result = "?????????";
//                        if (elements.size() > 0) {
//                            Element element = elements.get(0);
//                            String result = element.select("p:last-child").text();
                        if (false) {
                            //if ("?????????".equals(result)) {
                            if ("?????????".equals(result)) {
                                risk = 3;
                                // ??????????????????
                                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = district != null ? deptName : "";
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("??????" + name + "?????????????????????????????????");
                                message.setText(deptName + "??????" + name + "," + DateUtil.format(new Date(),"yyyy???MM???dd???" + "??????" + address + "????????????????????????????????????????????????????????????????????????????????????"));
                                emailTack.sendAsync(message);
                            } else if ("?????????".equals(result)) {
                                risk = 2;
                            }
                        }
                    } catch (Exception e) {
                        log.error("????????????",e);
                        throw new EmosException("????????????????????????");
                    }
                }
                // ??????????????????
                TbCheckin entiry = new TbCheckin();
                entiry.setUserId(userId);
                entiry.setAddress(address);
                entiry.setCountry(country);
                entiry.setProvince(province);
                entiry.setCity(city);
                entiry.setDistrict(district);
                entiry.setStatus((byte)status);
                entiry.setRisk(risk);
                entiry.setDate(DateUtil.today());
                entiry.setCreateTime(d1);
                checkinDao.insert(entiry);
            } else {
                throw new EmosException("????????????");
            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        request.form("photo", FileUtil.file(path));
        // ================>
        request.form("code", code);
        HttpResponse response = request.execute();
        String body = response.body();
        // ==================>
        body = "linger";
        if ("?????????????????????".equals(body) || "???????????????????????????".equals(body)) {
            throw new EmosException(body);
        } else {
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param);
        ArrayList<String> holidaysList = holidaysDao.searchHolidaysInRange(param);
        ArrayList<String> workdayList = workdayDao.searchWorkdayInRange(param);
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate,endDate, DateField.DAY_OF_MONTH);
        ArrayList<HashMap> list = new ArrayList();
//        while (range.hasNext()) {
//            DateTime one = range.next();
//            String date = one.toString("yyyy-MM-dd");
//            // ??????????????????????????????????????????
//            String type = "?????????";
//            if (one.isWeekend()) {
//                type = "?????????";
//            }
//            if (holidaysList != null && holidaysList.contains(date)) {
//                type = "?????????";
//            } else if (workdayList != null && workdayList.contains(date)) {
//                type = "?????????";
//            }
//            String status = "";
//            if (type.equals("?????????") && DateUtil.compare(one,DateUtil.date()) <= 0) {
//                status = "??????";
//                boolean flag = false;
//                for (HashMap<String, String> map:checkinList) {
//                    if (map.containsValue(date)) {
//                        status = map.get("status");
//                        flag = true;
//                        break;
//                    }
//                }
//                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
//                String today = DateUtil.today();
//                if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag==false) {
//                    status = "";
//                }
//            }
//            HashMap map = new HashMap();
//            map.put("date",date);
//            map.put("status",status);
//            map.put("type",type);
//            map.put("day",one.dayOfWeekEnum().toChinese("???"));
//            list.add(map);
//        }
        range.forEach(one -> {
            String date = one.toString("yyyy-MM-dd");
            // ??????????????????????????????????????????
            String type = "?????????";
            if (one.isWeekend()) {
                type = "?????????";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "?????????";
            } else if (workdayList != null && workdayList.contains(date)) {
                type = "?????????";
            }
            String status = "";
            if (type.equals("?????????") && DateUtil.compare(one,DateUtil.date()) <= 0) {
                status = "??????";
                boolean flag = false;
                for (HashMap<String, String> map:checkinList) {
                    if (map.containsValue(date)) {
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }
                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
                String today = DateUtil.today();
                if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag==false) {
                    status = "";
                }
            }
            HashMap map = new HashMap();
            map.put("date",date);
            map.put("status",status);
            map.put("type",type);
            map.put("day",one.dayOfWeekEnum().toChinese("???"));
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }

}
