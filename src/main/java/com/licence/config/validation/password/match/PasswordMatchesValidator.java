package com.licence.config.validation.password.match;

import com.licence.web.models.Keyspace;
import com.licence.web.models.User;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if(object instanceof User) {
            User user = (User) object;
            return user.getPassword().equals(user.getMatchingPassword());
        } else if(object instanceof Keyspace) {
            Keyspace keyspace = (Keyspace) object;
            return !(keyspace.getPassword().isEmpty() && keyspace.isPasswordEnabled()) && keyspace.getPassword().equals(keyspace.getMatchingPassword());
        }
        return false;
    }
}
