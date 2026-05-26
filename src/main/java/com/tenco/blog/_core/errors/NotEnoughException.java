package com.tenco.blog._core.errors;

// 401 NotEnoughException
public class NotEnoughException extends RuntimeException {

    // 예외 메시지를 외부에서 받아서 내 부모클래스 RuntimeException 에게 생성자로 전달
    public NotEnoughException(String msg) {
        super(msg);
    }

}
