-- fiks fagsak periode for 2017 saker
update fagsak set periode='[2017-01-01, 2018-01-01)'
where saksnummer in ('7Yo28');
                
-- fiks fagsak periode for 2018 saker
update fagsak set periode='[2018-01-01, 2019-01-01)'
where saksnummer in ('7T784');