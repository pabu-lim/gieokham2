package com.gieokham.api.member.exception;

public class MemberAlreadyRegisteredException extends RuntimeException {

    public MemberAlreadyRegisteredException() {
        super("이미 가입된 계정입니다.");
    }
}
