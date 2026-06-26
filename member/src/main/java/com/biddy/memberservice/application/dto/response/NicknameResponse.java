package com.biddy.memberservice.application.dto.response;

public record NicknameResponse(String nickname) {

    public static NicknameResponse of(String nickname) {
        return new NicknameResponse(nickname);
    }
}
