-- fra dagens fom til infinite
update fagsak set periode=daterange(lower(periode), null, '[]') where ytelse_type='OMP_KS';
