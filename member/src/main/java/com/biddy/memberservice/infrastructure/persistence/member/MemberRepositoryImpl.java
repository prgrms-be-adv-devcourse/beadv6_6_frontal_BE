package com.biddy.memberservice.infrastructure.persistence.member;

import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(Member member) {
        return jpaRepository.save(MemberJpaEntity.from(member)).toDomain();
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id).map(MemberJpaEntity::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(MemberJpaEntity::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaRepository.existsByNickname(nickname);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Member> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(MemberJpaEntity::toDomain)
                .toList();
    }
}
