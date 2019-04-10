package com.licence.web.repository;

import com.licence.web.models.User;
import com.licence.scratches.UserPrimaryKey;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository  extends CrudRepository<User, String> {

}
