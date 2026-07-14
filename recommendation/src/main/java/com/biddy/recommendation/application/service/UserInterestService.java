package com.biddy.recommendation.application.service;

import com.biddy.recommendation.domain.model.UserInterest;
import com.biddy.recommendation.domain.repository.UserInterestRepository;
import com.biddy.recommendation.infra.acl.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInterestService {

    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final UserInterestRepository userInterestRepository;

    public void registerInterest(Long memberId, String keyword) {
        float[] embedding = openAiEmbeddingClient.embed(keyword);
        userInterestRepository.save(UserInterest.create(memberId, keyword, embedding));
    }
}
