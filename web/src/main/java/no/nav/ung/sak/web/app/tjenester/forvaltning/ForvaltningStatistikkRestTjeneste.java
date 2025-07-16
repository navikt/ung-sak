package no.nav.ung.sak.web.app.tjenester.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static no.nav.ung.abac.BeskyttetRessursKoder.DRIFT;

@Path("/statistikk/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningStatistikkRestTjeneste {

    private EntityManager entityManager;

    @Inject
    public ForvaltningStatistikkRestTjeneste(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ForvaltningStatistikkRestTjeneste() {
        // For Rest-CDI
    }


    @GET
    @Path("antall-deltakere")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Lister ut antall deltakere med ulike sats-typer for en dato", summary = ("Brukes for statistikkformål"), tags = "statistikk")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public AntallDeltakereStatistikk antallDeltakere(@NotNull @QueryParam("dato") @Parameter(description = "dato i format YYYY-MM-DD", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) String datoParameter) {
        LocalDate dato = LocalDate.parse(datoParameter, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Object[] resultat = (Object[]) entityManager.createNativeQuery("""
                with
                    --hent programdeltakelse for siste vedtatte avsluttede behandling
                    -- tar med denne for å ikke få med perioder som har blitt fjernet
                    programdeltakelse_pr_sak as (
                        select distinct on (f.saksnummer) saksnummer, fom, tom
                        from ung_gr_ungdomsprogramperiode gr_ungprog
                        join behandling b on gr_ungprog.behandling_id = b.id
                        join fagsak f on b.fagsak_id = f.id
                        join ung_ungdomsprogramperiode ungprog_periode on gr_ungprog.ung_ungdomsprogramperioder_id = ungprog_periode.ung_ungdomsprogramperioder_id
                        where gr_ungprog.aktiv and b.behandling_status = 'AVSLU'
                        order by f.saksnummer, b.opprettet_tid desc
                    ),
                    sats_pr_sak as (
                        select distinct on (f.saksnummer) sats_type, antall_barn
                        from ung_gr ung_gr
                        join ung_sats_perioder uspp on ung_gr.ung_sats_perioder_id = uspp.id
                        join ung_sats_periode usp on uspp.id = usp.ung_sats_perioder_id
                        join behandling b on ung_gr.behandling_id = b.id
                        join fagsak f on b.fagsak_id = f.id
                        join programdeltakelse_pr_sak programdeltakelse on programdeltakelse.saksnummer = f.saksnummer
                        where ung_gr.aktiv
                        and b.behandling_status = 'AVSLU'
                        and usp.periode && daterange(:dato, :dato, '[]')
                        and programdeltakelse.fom <= :dato
                        and programdeltakelse.tom >= :dato
                        order by f.saksnummer, b.opprettet_tid desc) --sorterer på behandlingens opprettettid for å velge den nyeste behandlingen som treffer perioden (pr sak)
                 select
                    count(*) as antall_totalt,
                    sum(case when sats_type = 'LAV' then 1 else 0 end) as antall_lav,
                    sum(case when sats_type = 'HØY' then 1 else 0 end) as antall_høy,
                    sum(case when antall_barn > 0 then 1 else 0 end) as antall_med_barnetillegg,
                    sum(antall_barn) as antall_barn
                 from sats_pr_sak
                """)
            .setParameter("dato", dato)
            .getSingleResult();

        return new AntallDeltakereStatistikk(
            dato,
            resultat[0] != null ? (Long) resultat[0] : 0L,
            resultat[1] != null ? (Long) resultat[1] : 0L,
            resultat[2] != null ? (Long) resultat[2] : 0L,
            resultat[3] != null ? (Long) resultat[3] : 0L,
            resultat[4] != null ? (Long) resultat[4] : 0L
        );
    }

    public record AntallDeltakereStatistikk(
        LocalDate dato,
        long antallTotalt,
        long antallLavSats,
        long antallHøySats,
        long antallMedBarnetillegg,
        long antallBarn) {
    }

    @GET
    @Path("utmeldt-prosentandel")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Lister ut prosentandel av deltaker som meldes ut før det har gått ett år", summary = ("Brukes for statistikkformål"), tags = "statistikk")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public BigDecimal utmeldtProsentandel() {
        //hardkodet 260 representerer ett år (ca antall virkedager)
        return (BigDecimal) entityManager.createNativeQuery("""
                with ungdomsprogramperiode_siste_behandling as (
                     select distinct on (f.saksnummer)
                         saksnummer, fom, tom, case when tom <> '9999-12-31' then tom - fom else 260 end as varighet
                     from ung_gr_ungdomsprogramperiode gr_ungprog
                     join behandling b on gr_ungprog.behandling_id = b.id
                     join fagsak f on b.fagsak_id = f.id
                     join ung_ungdomsprogramperiode ungprog_periode on gr_ungprog.ung_ungdomsprogramperioder_id = ungprog_periode.ung_ungdomsprogramperioder_id
                     where gr_ungprog.aktiv
                     and b.behandling_status = 'AVSLU'
                     order by f.saksnummer, b.opprettet_tid desc)
                 select 100 * round(sum(case when varighet < 260 then 1 else 0 end) / sum(1.0), 4) as prosentandel_utmeldt_før_ett_år
                 from ungdomsprogramperiode_siste_behandling
                """)
            .getSingleResult();
    }

    @GET
    @Path("vedtak")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Lister ut antall vedtak for en måned", summary = ("Brukes for statistikkformål"), tags = "statistikk")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public VedtakStatistikkMåned vedtak(@NotNull @QueryParam("måned") @Parameter(description = "måned i format YYYY-MM", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) String månedParameter) {
        YearMonth måned = YearMonth.parse(månedParameter, DateTimeFormatter.ofPattern("yyyy-MM"));
        //hardkodet 260 representerer ett år (ca antall virkedager)
        Object[] resultat = (Object[]) entityManager.createNativeQuery("""
                with aktuelle_vedtak as (
                    select bv.id, b.ansvarlig_saksbehandler
                    from behandling_vedtak bv
                    join behandling b on bv.behandling_id = b.id
                    where vedtak_dato >= :fom and vedtak_dato < :til),
                summerte_vedtak as (
                    select sum(1.0) as                                                      antall,
                    sum(case when ansvarlig_saksbehandler is null then 1 else 0 end) antall_automatiske
                    from aktuelle_vedtak)
                select antall, antall_automatiske, 100 * round(antall_automatiske / antall, 4) as prosentandel_automatiske
                from summerte_vedtak
                """)
            .setParameter("fom", måned.atDay(1))
            .setParameter("til", måned.plusMonths(1).atDay(1))
            .getSingleResult();

        return new VedtakStatistikkMåned(
            måned,
            resultat[0] != null ? ((BigDecimal) resultat[0]).longValue() : 0L,
            resultat[1] != null ? (Long) resultat[1] : 0L,
            resultat[2] != null ? (BigDecimal) resultat[2] : null
        );
    }

    public record VedtakStatistikkMåned(
        YearMonth måned,
        long antallVedtak,
        long antallAutomatiskeVedtak,
        BigDecimal prosentandelAutomatiskeVedtak) {
    }

}
