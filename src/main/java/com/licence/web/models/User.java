package com.licence.web.models;

import com.datastax.driver.core.DataType;
import com.datastax.driver.mapping.annotations.FrozenValue;
import com.licence.config.validation.email.ValidEmail;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.config.validation.password.pattern.PasswordPattern;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.UDT.UserNotification;
import jnr.ffi.annotations.In;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Data
@Builder
@Table(value = "users")
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatches
public class User {

    @PrimaryKey("USER_ID")
    private String id;

    @NotEmpty
    @ValidEmail
    private String email;

    @Column("USERNAME")
    @Size(min = 3, max = 20)
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9]+")
    private String userName;

    @Column("PASSWORD")
    @NotEmpty
    @PasswordPattern
    private String password;

    @Transient
    @NotEmpty
    private String matchingPassword;

    @Indexed
    @Column(value = "ROLES")
    private List<String> roles;

    @Column(value = "REGISTER_DATE")
    @DateTimeFormat
    private Date registerDate;

    @Column(value = "ENABLED")
    private boolean enabled;

    @Column(value = "REGISTER_TOKEN")
    private String token;

    @Column(value = "REGISTER_TOKEN_EXPIRY_DATE")
    private Date expiryDate;

    @Column(value = "CHANGE_EMAIL_TOKEN")
    private String emailToken;

    @Column(value = "AVATAR_URL")
    private String avatar;

    @CassandraType(type = DataType.Name.UDT, userTypeName = "user_notification")
    private List<UserNotification> notifications;

    @CassandraType(type = DataType.Name.UDT, userTypeName = "user_keyspace")
    private List<UserKeyspace> keyspaces;
}
