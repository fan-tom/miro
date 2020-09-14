drop table if exists widgets;
create table widgets (
    id int auto_increment primary key,
    lbx int not null,
    lby int not null,
    rtx int not null,
    rty int not null,
    z_index int not null,
    updated_at timestamp not null default now(),
    constraint z_index_unique unique (z_index)
);