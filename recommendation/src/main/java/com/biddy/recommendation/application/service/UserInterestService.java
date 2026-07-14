package com.biddy.recommendation.application.service;

import com.biddy.recommendation.domain.model.UserInterest;
import com.biddy.recommendation.domain.repository.UserInterestRepository;
import com.biddy.recommendation.infra.acl.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInterestService {

    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final UserInterestRepository userInterestRepository;

    // [2026-07-15] 다른 기능들과 달리 이건 조회가 아니라 쓰기라서, 실패 시 빈 값을 그냥 반환하는 방식의
    // fallback은 맞지 않음(사용자는 등록됐다고 오해하게 됨) - 대신 원인 모를 500 대신 명확한 503으로
    // 실패를 알려주는 방식으로 처리함.
    public void registerInterest(Long memberId, String keyword) {
        try {
            float[] embedding = openAiEmbeddingClient.embed(keyword);
            userInterestRepository.save(UserInterest.create(memberId, keyword, embedding));
        } catch (Exception e) {
            log.error("관심 키워드 등록 처리 중 오류가 발생했습니다. memberId={}, keyword={}", memberId, keyword, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관심 키워드 등록에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
