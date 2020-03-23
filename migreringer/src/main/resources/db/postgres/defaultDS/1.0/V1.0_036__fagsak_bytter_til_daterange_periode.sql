
alter table fagsak add column periode daterange;

update fagsak set periode = daterange(gjelder_fom, gjelder_tom, '[]');

alter table fagsak drop column gjelder_fom, drop column gjelder_tom;

