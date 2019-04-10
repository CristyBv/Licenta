package com.licence.scratches;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

//    @ExceptionHandler({ApplicationException.class})
//    public void notFount(){
//        System.out.println("----------CaughtApplicationException-----------");
//    }
//
//    @ExceptionHandler({Exception.class})
//    public void notFountGlobal(){
//        System.out.println("----------CaughtApplicationException-----------");
//    }
}
