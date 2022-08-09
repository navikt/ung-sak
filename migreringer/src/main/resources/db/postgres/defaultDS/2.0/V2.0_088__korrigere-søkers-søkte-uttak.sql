UPDATE up_uttaksperiode u SET u.timer_per_dag = 27000000000000
where u.holder_id IN (select distinct sp.id
                      from fagsak f
                          inner join behandling b on b.fagsak_id = f.id
                          inner join GR_UTTAKSPERIODER gr on gr.behandling_id = b.id
                          inner join UP_UTTAKSPERIODER_HOLDER hr on hr.id = gr.oppgitt_soknadsperiode_id
                          inner join UP_SOEKNAD_PERIODER sp on sp.holder_id = hr.id
                       where f.saksnummer IN ('BSQFK', 'BSQBE', 'BSQW8', 'BTC5i', 'BWH9Q', 'BRTK8', 'BRSZE', 'BRJZS', 'BX6Go', 'BRK5C', 'BRXGi', 'BW7FK', 'BSFMo', 'BRLNi', 'BRQWE', 'BW9D0', 'BRXQ8', 'BTWDA', 'BRZJi', 'BVFQC', 'BSMGi', 'BToB0', 'BU8PQ', 'BSQJQ', 'BToKQ', 'BVKXU', 'BRZT8', 'BW5XE', 'BWJXK', 'BRLUG', 'BSUG0', 'BWKTi', 'BRKAW', 'BWR3W', 'BW674', 'BWRRi', 'BWN90', 'BV9ZE', 'BRWX2', 'BWX8Q', 'BWXA4', 'BVFXA', 'BSPL0', 'BRLRo', 'BRQ7E', 'BRQH4', 'BSQA0', 'BX2HM', 'BVZWQ', 'BVUE4', 'BRQS8', 'BX646', 'BX65K', 'BX66Y', 'BX68C', 'BX6Ci', 'BRK3Y', 'BRL6U', 'BRRiM', 'BTWTY', 'BRXHW', 'BRJX0', 'BRNYU', 'BRZi4', 'BSCi6', 'BSG7i', 'BSGTQ') and f.ytelse_type='PPN');


UPDATE up_uttaksperiode u SET u.timer_per_dag = 27000000000000
where u.holder_id IN (select distinct sp.id
                      from fagsak f
                               inner join behandling b on b.fagsak_id = f.id
                               inner join GR_UTTAKSPERIODER gr on gr.behandling_id = b.id
                               inner join UP_UTTAKSPERIODER_HOLDER hr on hr.id = gr.relevant_soknadsperiode_id
                               inner join UP_SOEKNAD_PERIODER sp on sp.holder_id = hr.id
                      where f.saksnummer IN ('BSQFK', 'BSQBE', 'BSQW8', 'BTC5i', 'BWH9Q', 'BRTK8', 'BRSZE', 'BRJZS', 'BX6Go', 'BRK5C', 'BRXGi', 'BW7FK', 'BSFMo', 'BRLNi', 'BRQWE', 'BW9D0', 'BRXQ8', 'BTWDA', 'BRZJi', 'BVFQC', 'BSMGi', 'BToB0', 'BU8PQ', 'BSQJQ', 'BToKQ', 'BVKXU', 'BRZT8', 'BW5XE', 'BWJXK', 'BRLUG', 'BSUG0', 'BWKTi', 'BRKAW', 'BWR3W', 'BW674', 'BWRRi', 'BWN90', 'BV9ZE', 'BRWX2', 'BWX8Q', 'BWXA4', 'BVFXA', 'BSPL0', 'BRLRo', 'BRQ7E', 'BRQH4', 'BSQA0', 'BX2HM', 'BVZWQ', 'BVUE4', 'BRQS8', 'BX646', 'BX65K', 'BX66Y', 'BX68C', 'BX6Ci', 'BRK3Y', 'BRL6U', 'BRRiM', 'BTWTY', 'BRXHW', 'BRJX0', 'BRNYU', 'BRZi4', 'BSCi6', 'BSG7i', 'BSGTQ') and f.ytelse_type='PPN');
