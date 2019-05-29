package com.licence.web.models.UDT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.Calendar;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@UserDefinedType("user_notification")
public class UserNotification {
    public UserNotification(String author, String content) {
        this.author = author;
        this.content = content;
        this.date = Calendar.getInstance().getTime();
        this.read = false;
    }
    private String author;
    private Date date;
    private boolean read;
    private String content;
}
