package com.gieokham.api.memo.repository;

import com.gieokham.api.category.dto.CategoryMemoCountDto;
import com.gieokham.api.memo.domain.Memo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long>, JpaSpecificationExecutor<Memo> {

    Optional<Memo> findByIdAndMemberId(Long memoId, Long memberId);

    @Modifying
    @Query("""
            delete from Memo m
            where m.member.id = :memberId
             and m.category.id in :categoryIds
            """)
    int deleteByCategoryIds(@Param("memberId") Long memberId, @Param("categoryIds") List<Long> categoryIds);

    @Modifying
    @Query(value = """
            INSERT INTO memo_completion (memo_id, scheduled_date)
            VALUES (:memoId, :scheduledDate)
            ON CONFLICT (memo_id, scheduled_date) DO NOTHING
            """, nativeQuery = true)
    int insertCompletion(@Param("memoId") Long memoId, @Param("scheduledDate")LocalDate scheduledDate);

    @Modifying
    @Query(value = """
            DELETE FROM memo_completion
            WHERE memo_id = :memoId
             AND scheduled_date = :scheduledDate
            """, nativeQuery = true)
    int deleteCompletion(@Param("memoId") Long memoId, @Param("scheduledDate") LocalDate scheduledDate);

    @Query(value = """
            SELECT mc.memo_id
            FROM memo_completion mc
            JOIN memo m ON m.id = mc.memo_id
            WHERE m.member_id = :memberId
             AND m.repeat_type <> 'NONE'
             AND mc.scheduled_date = :scheduledDate
            ORDER BY mc.memo_id
            """, nativeQuery = true)
    List<Long> findCompletedRepeatMemoIds(@Param("memberId") Long memberId, @Param("scheduledDate") LocalDate scheduledDate);

    @Query(value = """
        SELECT m.*
        FROM memo m
        WHERE m.member_id = :memberId
          AND (
              (
                  m.repeat_type = 'NONE'
                  AND m.completed_at IS NULL
                  AND m.start_date <= :today
              )
              OR
              (
                  m.repeat_type <> 'NONE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM memo_completion mc
                      WHERE mc.memo_id = m.id
                        AND mc.scheduled_date = :today
                  )
                  AND (
                      m.repeat_type = 'DAILY'
                      OR (
                          m.repeat_type = 'WEEKLY'
                          AND EXISTS (
                              SELECT 1
                              FROM memo_repeat_day rd
                              WHERE rd.memo_id = m.id
                                AND rd.day_of_week = :dayOfWeek
                          )
                      )
                      OR (
                          m.repeat_type IN ('MONTHLY', 'YEARLY')
                          AND (
                              m.repeat_type = 'MONTHLY'
                              OR m.repeat_month = :month
                          )
                          AND LEAST(m.repeat_day_of_month, :lastDayOfMonth) = :dayOfMonth
                      )
                  )
              )
          )
        ORDER BY CASE m.priority
                     WHEN 'HIGHEST' THEN 1
                     WHEN 'HIGH'    THEN 2
                     WHEN 'MEDIUM'  THEN 3
                     WHEN 'LOW'     THEN 4
                     WHEN 'LOWEST'  THEN 5
                 END,
                 m.created_at DESC,
                 m.id DESC
        """, nativeQuery = true)
    List<Memo> findTodayMemos(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("month") int month,
            @Param("dayOfMonth") int dayOfMonth,
            @Param("lastDayOfMonth") int lastDayOfMonth
    );

    @Query("""
        select new com.gieokham.api.category.dto.CategoryMemoCountDto(
            m.category.id,
            count(m)
        )
        from Memo m
        where m.member.id = :memberId
          and m.category is not null
          and m.completedAt is null
        group by m.category.id
        """)
    List<CategoryMemoCountDto> countActiveMemosByCategory(
            @Param("memberId") Long memberId
    );
}
