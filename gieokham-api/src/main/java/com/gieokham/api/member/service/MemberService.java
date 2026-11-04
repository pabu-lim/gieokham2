package com.gieokham.api.member.service;

import com.gieokham.api.member.domain.AuthProvider;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.domain.MemberIdentity;
import com.gieokham.api.member.exception.MemberAlreadyRegisteredException;
import com.gieokham.api.member.exception.MemberNotFoundException;
import com.gieokham.api.member.repository.MemberIdentityRepository;
import com.gieokham.api.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberIdentityRepository memberIdentityRepository;

    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    public Optional<Member> findMember(AuthProvider provider, String providerUserId) {
//        Optional<MemberIdentity> identity = memberIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId);
//        if (identity.isPresent()) {
//            Member member = identity.get().getMember();
//            return Optional.of(member);
//        }
//        return Optional.empty();
        return memberIdentityRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .map(MemberIdentity::getMember);
    }

    @Transactional
    public Member registerMember(AuthProvider provider, String providerUserId, String name,
                                 String nickname, String email) {
        boolean alreadyRegistered = memberIdentityRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .isPresent();

        if (alreadyRegistered) {
            throw new MemberAlreadyRegisteredException();
        }

        Member member = memberRepository.save(new Member(name, nickname, email));

        MemberIdentity identity = new MemberIdentity(member, provider, providerUserId, email);

        memberIdentityRepository.save(identity);

        return member;
    }
}
