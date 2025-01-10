package com.example.backend.Entity;

import lombok.Data;

@Data
public class ErrorData {

    private String errormsg;
    private int errorcode;

    public ErrorData(String errormsg, int errorcode) {
        this.errormsg = errormsg;
        this.errorcode = errorcode;
    }

    public String getErrormsg() {
        return errormsg;
    }

    public void setErrormsg(String errormsg) {
        this.errormsg = errormsg;
    }

    public int getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }
}
