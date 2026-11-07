create sequence exercise_id_seq start with 100 increment by 50;
create table exercises (
    id bigint primary key default nextval('exercise_id_seq'),
    owner_id bigint not null references users(id) on delete cascade,
    name varchar(60) not null,
    description varchar(5000) not null,
    goal varchar(2000) not null,
    difficulty varchar(20) not null check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
    type varchar(40) not null check (type in ('TECHNIQUE', 'CUE_BALL_CONTROL', 'POSITIONAL_PLAY', 'TACTICS', 'SAFETY_PLAY', 'MENTAL_PREPARATION')),
    drawing bytea not null,
    drawing_content_type varchar(50) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp,
    version bigint not null default 0
);
create index exercises_owner_created_idx on exercises(owner_id, created_at desc, id desc);
