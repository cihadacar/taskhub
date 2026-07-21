create table users (
    id bigserial primary key,
    email varchar(320) not null,
    username varchar(50) not null,
    password_hash varchar(255) not null,
    created_at timestamp with time zone not null default current_timestamp,
    constraint uk_users_email unique (email)
);

create unique index uk_users_username_ci on users (lower(username));

create table user_roles (
    user_id bigint not null,
    role varchar(20) not null,
    primary key (user_id, role),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade,
    constraint ck_user_roles_role check (role in ('USER', 'ADMIN'))
);

create table projects (
    id bigserial primary key,
    name varchar(120) not null,
    description varchar(2000),
    owner_id bigint not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_projects_owner foreign key (owner_id) references users (id) on delete restrict
);

create index idx_projects_owner_id on projects (owner_id);

create table tags (
    id bigserial primary key,
    name varchar(50) not null,
    color varchar(7) not null,
    constraint uk_tags_name unique (name),
    constraint ck_tags_color check (color ~ '^#[0-9A-F]{6}$')
);

create table tasks (
    id bigserial primary key,
    title varchar(200) not null,
    description varchar(4000),
    status varchar(20) not null,
    priority varchar(20) not null,
    due_date date,
    project_id bigint not null,
    assignee_id bigint,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_tasks_project foreign key (project_id) references projects (id) on delete cascade,
    constraint fk_tasks_assignee foreign key (assignee_id) references users (id) on delete set null,
    constraint ck_tasks_status check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
    constraint ck_tasks_priority check (priority in ('LOW', 'MEDIUM', 'HIGH'))
);

create index idx_tasks_project_id on tasks (project_id);
create index idx_tasks_status on tasks (status);
create index idx_tasks_assignee_id on tasks (assignee_id);

create table task_tags (
    task_id bigint not null,
    tag_id bigint not null,
    primary key (task_id, tag_id),
    constraint fk_task_tags_task foreign key (task_id) references tasks (id) on delete cascade,
    constraint fk_task_tags_tag foreign key (tag_id) references tags (id) on delete cascade
);

create index idx_task_tags_tag_id on task_tags (tag_id);
