package com.gieokham.api.config;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.repository.CategoryRepository;
import com.gieokham.api.member.domain.AuthProvider;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.domain.MemberIdentity;
import com.gieokham.api.member.repository.MemberIdentityRepository;
import com.gieokham.api.member.repository.MemberRepository;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.domain.MemoRepeatType;
import com.gieokham.api.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final MemberIdentityRepository memberIdentityRepository;
    private final CategoryRepository categoryRepository;
    private final MemoRepository memoRepository;

    @Value("${app.local.google.sub-a}")
    private String googleSubA;

    @Value("${app.local.google.sub-b}")
    private String googleSubB;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (googleSubA == null || googleSubA.isBlank() || googleSubB == null || googleSubB.isBlank()) {
            throw new IllegalStateException("테스트용 구글 계정 A,B의 ID를 모두 설정해 주세요.");
        }

        if (googleSubA.equals(googleSubB)) {
            throw new IllegalStateException("A와 B는 서로 다른 구글 계정이어야 합니다.");
        }

        Member memberA = memberRepository.save(new Member("임경민", "테스트 메인계정", null));
        Member memberB = memberRepository.save(new Member("홍길동", "테스트 서브계정", null));
        memberIdentityRepository.save(new MemberIdentity(memberA, AuthProvider.GOOGLE, googleSubA, null));
        memberIdentityRepository.save(new MemberIdentity(memberB, AuthProvider.GOOGLE, googleSubB, null));

        Map<String, Category> categories = createCategories(memberA);

        Category javaCategory = new Category(memberA, "자바", 0);
        javaCategory.changeParent(categories.get("공부"));
        categoryRepository.save(javaCategory);

        Category springCategory = new Category(memberA, "스프링", 0);
        springCategory.changeParent(javaCategory);
        categoryRepository.save(springCategory);

        createMemo(
                memberA, categories.get("단순메모"), "당장 생각나는 것", MemoPriority.HIGH
        );

        createMemo(
                memberA, categories.get("쇼핑"), "클렌징폼", MemoPriority.MEDIUM
        );

        createMemo(
                memberA, categories.get("쇼핑"), "샴푸", MemoPriority.MEDIUM
        );

        createMemo(
                memberA, categories.get("쇼핑"), "쌀", MemoPriority.HIGH
        );

        createMemo(
                memberA, categories.get("일정"), "땡땡이와 점심 약속", MemoPriority.MEDIUM
        );

        createMemo(
                memberA, springCategory, "트랜잭션과 잠금 공부", MemoPriority.HIGH
        );

        createMemo(
                memberA, null, "미분류 메모 테스트", MemoPriority.LOWEST
        );

        Memo paydayMemo = new Memo(memberA, "월급 확인", MemoPriority.MEDIUM);
        paydayMemo.changeCategory(categories.get("일정"));
        paydayMemo.changeRepeat(MemoRepeatType.MONTHLY, Set.of(), 25, null);
        memoRepository.save(paydayMemo);
    }

    private Map<String, Category> createCategories(Member member) {
        String[] names = {
                "단순메모", "회사", "일정", "일상", "쇼핑", "공부", "주식", "운동"
        };

        Map<String, Category> categories = new LinkedHashMap<>();

        for (int i = 0; i < names.length; i++) {
            Category category = categoryRepository.save(
                    new Category(member, names[i], i)
            );

            categories.put(names[i], category);
        }

        return categories;
    }

    private void createMemo(Member member, Category category, String content, MemoPriority priority) {
        Memo memo = new Memo(member, content, priority);
        memo.changeCategory(category);

        memoRepository.save(memo);
    }
}
