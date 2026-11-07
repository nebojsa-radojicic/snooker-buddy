create table coach_players (
    player_id bigint primary key references users(id) on delete cascade,
    coach_id bigint not null references users(id) on delete cascade,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp,
    version bigint not null default 0,
    constraint player_cannot_coach_self check (coach_id <> player_id),
    unique (coach_id, player_id)
);
create index coach_players_coach_idx on coach_players(coach_id);
create sequence training_plan_id_seq start with 100 increment by 50;
create table training_plans (
    id bigint primary key default nextval('training_plan_id_seq'),
    coach_id bigint not null,
    player_id bigint not null,
    name varchar(120) not null,
    format varchar(40) not null check (format in ('TRAINING_SESSION', 'INDIVIDUAL_EXERCISES')),
    kind varchar(20) not null check (kind in ('INDEPENDENT', 'JOINT')),
    attempts integer not null check (attempts between 1 and 1000),
    target integer not null check (target between 1 and attempts),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp,
    version bigint not null default 0,
    foreign key (coach_id, player_id) references coach_players(coach_id, player_id) on delete cascade
);
create index training_plans_coach_idx on training_plans(coach_id, created_at desc);
create index training_plans_player_idx on training_plans(player_id, created_at desc);
create table training_plan_exercises (
    plan_id bigint not null references training_plans(id) on delete cascade,
    position integer not null,
    exercise_id bigint not null references exercises(id) on delete cascade,
    primary key (plan_id, position),
    unique (plan_id, exercise_id)
);
