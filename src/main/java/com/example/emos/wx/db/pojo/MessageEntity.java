package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "message")
public class MessageEntity implements Serializable {
    @Id
    private String _id;

    @Indexed
    private String uuid;

    @Indexed
    private Integer senderId;

    private String senderPhoto = "https://static-1258386385.cos.apbeijing.myqcloud.com/img/System.jpg";

    private String senderName;

    @Indexed
    private Date sendTime;

    private String msg;
}
