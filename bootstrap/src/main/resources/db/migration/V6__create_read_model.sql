-- CQRS 읽기 모델: 투자자별 누적 수익(정산 이벤트로 갱신).
create table investor_return_view
(
    investor_id      varchar(64) not null,
    total_returned   bigint      not null default 0,
    settlement_count int         not null default 0,
    constraint pk_investor_return_view primary key (investor_id)
);

-- 읽기측 멱등 처리용: 이미 반영한 정산 이벤트 ID.
create table processed_event
(
    event_id     varchar(128)             not null,
    processed_at timestamp with time zone not null default now(),
    constraint pk_processed_event primary key (event_id)
);
