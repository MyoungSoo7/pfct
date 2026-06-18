-- 대출. 모집 완료된 펀딩 라운드를 실행하면 생성된다.
create table loan
(
    id                  varchar(64)   not null,
    borrower_id         varchar(64)   not null,
    principal           bigint        not null,
    annual_rate_percent numeric(5, 2) not null,
    months              int           not null,
    status              varchar(16)   not null,
    constraint pk_loan primary key (id),
    constraint ck_loan_principal_positive check (principal > 0),
    constraint ck_loan_months_positive check (months >= 1)
);
