-- FAGSYSTEM-183539 - frisinn hvor uttaksperioder har gått tapt for mange behandlinger siden - må kopieres over til siste revurdering
with siste_komplette_uttak as (
    select f.saksnummer,
           b.id         beh_id,
           grunnlag.id  g_id,
           grunnlag.oppgitt_uttak_id,
           uttak_akt.id akt_id,
           uttak_akt_per.*
    from fagsak f
             inner join behandling b on f.id = b.fagsak_id
             inner join gr_uttak grunnlag on b.id = grunnlag.behandling_id
             inner join UT_UTTAK_AKTIVITET uttak_akt on grunnlag.oppgitt_uttak_id = uttak_akt.id
             inner join UT_UTTAK_AKTIVITET_PERIODE uttak_akt_per on uttak_akt.id = uttak_akt_per.aktivitet_id
    where f.saksnummer = '6ECA8'
      and grunnlag.aktiv = true
      and grunnlag.behandling_id = 1270009
      and b.behandling_status = 'AVSLU'
      and uttak_akt_per.aktivitet_type = 'SN'
),
     siste_ukomplette_uttak as (
         select f.saksnummer,
                b.id         beh_id,
                grunnlag.id  g_id,
                uttak_akt.id akt_id,
                uttak_akt_per.*
         from fagsak f
                  inner join behandling b on f.id = b.fagsak_id
                  inner join gr_uttak grunnlag on b.id = grunnlag.behandling_id
                  inner join UT_UTTAK_AKTIVITET uttak_akt on grunnlag.oppgitt_uttak_id = uttak_akt.id
                  inner join UT_UTTAK_AKTIVITET_PERIODE uttak_akt_per on uttak_akt.id = uttak_akt_per.aktivitet_id
         where f.saksnummer = '6ECA8'
           and grunnlag.aktiv = true
           and grunnlag.behandling_id = 1431288
           and b.behandling_status = 'UTRED'
           and uttak_akt_per.aktivitet_type = 'SN'
           and uttak_akt_per.fom = '2020-11-01'
           and uttak_akt_per.tom = '2020-11-30'
     ), manglende_perioder as (
    select nextval('SEQ_UT_UTTAK_AKTIVITET_PERIODE') ny_periode_id,
           ukompl.akt_id akt_id_ukompl,
           kompl.fom,
           kompl.tom,
           kompl.aktivitet_type,
           kompl.arbeidsgiver_aktor_id,
           kompl.arbeidsgiver_orgnr,
           kompl.arbeidsforhold_intern_id,
           kompl.versjon,
           'SYSTEM-FEILRETTING' opprettet_av,
           CURRENT_TIMESTAMP opprettet_tid,
           'SYSTEM-FEILRETTING' endret_av,
           CURRENT_TIMESTAMP endret_tid, -- ikke tillatt å sette til null
           kompl.skal_jobbe_prosent,
           kompl.jobber_normalt_per_uke
    from siste_komplette_uttak kompl, siste_ukomplette_uttak ukompl
)
insert into ut_uttak_aktivitet_periode
select * from manglende_perioder
;
