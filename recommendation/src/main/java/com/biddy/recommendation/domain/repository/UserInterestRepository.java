package com.biddy.recommendation.domain.repository;

import com.biddy.recommendation.domain.model.UserInterest;

import java.util.List;

public interface UserInterestRepository {

    UserInterest save(UserInterest userInterest);

    List<UserInterest> findMatchesWithin(float[] queryVector, double maxDistance);
}
