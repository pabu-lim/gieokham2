package com.gieokham.api.member.repository;

import com.gieokham.api.member.domain.AuthProvider;
import com.gieokham.api.member.domain.MemberIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberIdentityRepository extends JpaRepository<MemberIdentity, Long> {

    Optional<MemberIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
