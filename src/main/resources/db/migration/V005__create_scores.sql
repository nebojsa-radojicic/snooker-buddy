create sequence score_id_seq start with 100 increment by 50;
create table scores (
    id bigint primary key default nextval('score_id_seq'),
    plan_id bigint not null,
    exercise_id bigint not null,
    total integer not null check (total > 0),
    successful integer not null check (successful between 0 and total),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp,
    version bigint not null default 0,
    foreign key (plan_id, exercise_id) references training_plan_exercises(plan_id, exercise_id) on delete cascade
);
create index scores_plan_idx on scores(plan_id, created_at desc, id desc);
