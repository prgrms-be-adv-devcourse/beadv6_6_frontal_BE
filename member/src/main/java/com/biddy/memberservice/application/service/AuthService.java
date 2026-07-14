package com.biddy.memberservice.application.service;

import com.biddy.memberservice.application.dto.request.LoginRequest;
import com.biddy.memberservice.application.dto.request.SignupRequest;
import com.biddy.memberservice.application.dto.response.TokenResponse;
import com.biddy.memberservice.application.event.MemberEventPublisher;
import com.biddy.memberservice.domain.enums.MemberStatus;
import com.biddy.memberservice.domain.model.EmailVerification;
import com.biddy.memberservice.domain.model.Member;
import com.biddy.memberservice.domain.model.RefreshToken;
import com.biddy.memberservice.domain.repository.EmailVerificationRepository;
import com.biddy.memberservice.domain.repository.MemberRepository;
import com.biddy.memberservice.domain.repository.RefreshTokenRepository;
import com.biddy.memberservice.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final MemberEventPublisher eventPublisher;

    // Gmail SMTP는 인증 계정과 From 주소가 다르면 거부/무시할 수 있어서, 하드코딩 대신 인증 계정과 동일한 값을 씀
    @Value("${spring.mail.username}")
    private String mailFromAddress;

    @Transactional
    @SneakyThrows
    public void signup(SignupRequest request) {
        boolean verified = emailVerificationRepository
                .existsByEmailAndVerifiedAtIsNotNull(request.getEmail());
        if (!verified) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }
        memberRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (existing.getStatus() == MemberStatus.SUSPENDED) {
                throw new IllegalArgumentException("정지된 계정의 이메일로는 가입할 수 없습니다.");
            }
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        });
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Member member = Member.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname(),
                request.getPhone()
        );
        Member savedMember = memberRepository.save(member);

        eventPublisher.publishSignup(savedMember.getId());
        log.info("Kafka 이벤트 발행: topic=member-signup, memberId={}", savedMember.getId());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new IllegalArgumentException("탈퇴 처리 중인 계정입니다.");
        }
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new IllegalArgumentException("정지된 계정입니다.");
        }
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        refreshTokenRepository.deleteByMemberId(member.getId());
        refreshTokenRepository.save(RefreshToken.create(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(7)
        ));

        return TokenResponse.of(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (token.isExpired()) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        Member member = token.getMember();
        String newAccessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        refreshTokenRepository.delete(token);
        refreshTokenRepository.save(RefreshToken.create(
                member,
                newRefreshToken,
                LocalDateTime.now().plusDays(7)
        ));

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    @Transactional
    public void sendVerificationEmail(String email) {
        memberRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getStatus() == MemberStatus.SUSPENDED) {
                throw new IllegalArgumentException("정지된 계정의 이메일로는 가입할 수 없습니다.");
            }
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        });
        String token = String.format("%06d", new java.util.Random().nextInt(1000000));
        emailVerificationRepository.save(EmailVerification.create(
                email,
                token,
                LocalDateTime.now().plusMinutes(10)
        ));

        SimpleMailMessage message = new SimpleMailMessage();
        // 원래 코드: message.setFrom("Biddy <tlsdlcl456@gmail.com>"); — 인증 계정이 바뀔 때마다 코드를 고쳐야 했고,
        // Gmail이 인증 계정과 다른 From 주소를 거부할 수 있어서 인증 계정(spring.mail.username)과 항상 일치하도록 변경
        message.setFrom("Biddy <" + mailFromAddress + ">");
        message.setTo(email);
        message.setSubject("[Biddy] 이메일 인증");
        message.setText("아래 인증 코드를 입력해주세요.\n\n인증 코드: " + token + "\n\n10분 후 만료됩니다.");
        // 원래 코드: mailSender.send(message); — 실패 시 원인이 로그에 전혀 안 남아서(GlobalExceptionHandler가
        // 그냥 500만 반환) 진단을 위해 try-catch + 로그만 임시로 둘렀습니다. 실패해도 동작은 기존과 동일하게 예외를 던집니다.
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[진단] 인증 이메일 발송 실패 - to={}, cause={}", email, e.toString(), e);
            throw e;
        }
    }

    @Transactional
    public void verifyEmail(String email, String token) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 정보를 찾을 수 없습니다."));

        if (verification.isExpired()) {
            throw new IllegalArgumentException("만료된 인증 토큰입니다.");
        }
        if (!verification.getToken().equals(token)) {
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
        }

        verification.verify();
        emailVerificationRepository.save(verification);
    }
}
