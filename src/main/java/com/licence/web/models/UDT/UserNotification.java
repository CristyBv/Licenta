package com.licence.web.models.UDT;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.Date;

@Data
@UserDefinedType("user_notification")
public class UserNotification {
    private String content;
    private Date date;
    private boolean read;
}
