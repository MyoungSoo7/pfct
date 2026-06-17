-- 펀딩 라운드 테이블.
-- raised_amount <= target_amount CHECK 제약은 도메인 불변식(오버펀딩 금지)을
-- DB 계층에서도 한 번 더 보장하는 '심층 방어(defense in depth)' 다.
create table funding_round
(
    id            varchar(64) not null,
    target_amount bigint      not null,
    raised_amount bigint      not null default 0,
    status        varchar(16) not null,
    constraint pk_funding_round primary key (id),
    constraint ck_funding_target_positive check (target_amount > 0),
    constraint ck_funding_raised_bounds check (raised_amount >= 0 and raised_amount <= target_amount)
);
