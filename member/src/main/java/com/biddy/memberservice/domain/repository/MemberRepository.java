package com.biddy.memberservice.domain.repository;

import com.biddy.memberservice.domain.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByEmail(String email);
    boolean existsByNickname(String nickname);
    void deleteById(Long id);
    List<Member> findAll();
}
