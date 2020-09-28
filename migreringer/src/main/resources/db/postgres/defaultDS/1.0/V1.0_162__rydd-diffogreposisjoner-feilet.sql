delete from fagsak_prosess_task fs
where prosess_task_id in (
10359357,
10366750,
10421091,
10404497,
10437426,
10359786
) 
 and exists (select 1 from prosess_task p where p.id=fs.prosess_task_id and p.status='FEILET';

delete from prosess_task
 where task_type='grunnlag.diffOgReposisjoner'
  and id in (
10359357,
10366750,
10421091,
10404497,
10437426,
10359786
)
 and status='FEILET';

--rydd fortsett behandling
delete from prosess_task
 where task_type='behandlingskontroll.fortsettBehandling'
 and id in (
9551300,
9829250,
9032300,
9781350,
9026650,
9392000,
8128900,
8183100,
7793200,
7637550,
7966050,
7984400,
8173500,
7998100,
7985450,
8170500,
8196050,
8126350,
8161200,
8306000,
8519300,
8366100,
8208900,
8910700,
8223200,
8035000,
7862950,
7686150,
8802850,
9178100,
7919200,
7731550,
9190350,
8275150,
8280950,
8294600,
9013600
)
 and status='FEILET';