-- 로컬 개발용: 기존 기록을 전부 삭제한다.
-- 외래 키로 참조하는 테이블부터 삭제한다.

DROP TABLE IF EXISTS memo_completion;
DROP TABLE IF EXISTS memo_repeat_day;
DROP TABLE IF EXISTS memo;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS member_identity;
DROP TABLE IF EXISTS member;