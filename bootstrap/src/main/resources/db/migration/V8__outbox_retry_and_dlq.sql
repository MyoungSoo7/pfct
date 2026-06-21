-- 아웃박스 재시도/백오프 + DLQ(데드레터) 격리 컬럼.
-- 발행이 반복 실패하는 이벤트가 폴링을 영원히 막지 않도록, 시도 횟수·다음 시도 시각·사인(死因)을 남기고
-- 한도를 넘기면 dead 로 격리한다.
alter table outbox_event
    add column attempts        integer                  not null default 0,
    add column next_attempt_at timestamp with time zone,
    add column last_error      text,
    add column dead            boolean                  not null default false;

-- 기존 미발행 인덱스를 "재시도 대기까지 고려한" 발행 후보 인덱스로 교체한다.
drop index if exists ix_outbox_event_unpublished;
create index ix_outbox_event_publishable
    on outbox_event (id)
    where published_at is null and dead = false;

-- DLQ 운영 조회(격리된 이벤트 목록)를 빠르게.
create index ix_outbox_event_dead
    on outbox_event (id)
    where dead = true;
