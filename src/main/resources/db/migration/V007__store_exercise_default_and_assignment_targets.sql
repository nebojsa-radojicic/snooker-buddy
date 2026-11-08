alter table exercises
    add column default_attempts integer not null default 10 check (default_attempts between 1 and 1000),
    add column default_target integer not null default 8 check (default_target between 1 and default_attempts);

alter table training_plan_exercises add column attempts integer, add column target integer;
-- Preserve each existing plan's targets before moving them to its exercise entries.
update training_plan_exercises e
set attempts = p.attempts, target = p.target
from training_plans p where p.id = e.plan_id;
alter table training_plan_exercises
    alter column attempts set not null,
    alter column target set not null,
    add constraint assigned_attempts_valid check (attempts between 1 and 1000),
    add constraint assigned_target_valid check (target between 1 and attempts);
alter table training_plans drop column target, drop column attempts;
