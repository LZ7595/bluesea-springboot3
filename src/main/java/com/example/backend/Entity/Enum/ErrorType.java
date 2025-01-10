package com.example.backend.Entity.Enum;

import com.example.backend.Entity.ErrorData;

// 枚举定义错误类型
public enum ErrorType {

    //注册
    USERNAME_REGISTERED("用户名已被使用",1001),
    EMAIL_REGISTERED("邮箱已注册", 1002),
    REGISTER_FAILED("注册失败",1009),


    //登录
    EMAIL_NOT_REGISTERED("邮箱未注册,请先注册", 1011),
    USERNAME_ERROR("用户名错误",1012),

    PASSWORD_ERROR("密码错误", 1014),
    LOGIN_FAILED("登录失败", 1019),


    //验证码
    CODE_SENDING_FAILED("验证码发送失败", 1021),
    CODE_INSERT_FAILED("验证码存储失败", 1022),
    CODE_INVALID_FAILED("验证码错误", 1023);

    private  int errorcode;
    private  String errormsg;

    ErrorType(String errormsg,int errorcode ) {
        this.errorcode = errorcode;
        this.errormsg = errormsg;
    }

    public int getErrorcode() {
        return errorcode;
    }

    public String getErrormsg() {
        return errormsg;
    }

    public ErrorData toErrorResponse() {
        return new ErrorData(this.errormsg, this.errorcode);
    }
}
