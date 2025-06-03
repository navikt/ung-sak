package no.nav.ung.sak.ungdomsprogram;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.VurderAntallDagerResultat;

@Dependent
public class UngdomsprogramPeriodeTjeneste {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private static final KantIKantVurderer KANT_I_KANT_VURDERER = new DefaultKantIKantVurderer();

    @Inject
    public UngdomsprogramPeriodeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
    }

    @WithSpan
    public VurderAntallDagerResultat finnVirkedagerTidslinje(Long behandlingId) {
        var tidslinje = finnPeriodeTidslinje(behandlingId);
        return FinnForbrukteDager.finnForbrukteDager(tidslinje);
    }

    /**
     * Utleder tidslinje for endringer i perioder fra grunnlaget i forrige behandling og det aktive. I førstegangsbehandlinger vil alle perioder returnerers som endring.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for endrede perioder
     */
    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinjeFraOriginal(BehandlingReferanse behandlingReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var originaltGrunnlag = behandlingReferanse.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinje(UUID fraReferanse, UUID tilReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(tilReferanse);
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var originaltGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(fraReferanse);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    public List<EndretDato> finnEndretStartdatoer(UUID tidligereReferanse, UUID sisteReferanse) {
        var sisteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(sisteReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + sisteReferanse));
        var tidligereGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(tidligereReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + tidligereReferanse));
        var sistePerioder = sisteGrunnlag.getUngdomsprogramPerioder().getPerioder();

        if (sistePerioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke å finne endring i startdato for flere perioder");
        }

        var tidligerePerioder = tidligereGrunnlag.getUngdomsprogramPerioder().getPerioder();
        if (sistePerioder.isEmpty() || tidligerePerioder.isEmpty()) {
            return List.of();
        }

        var startdato = sistePerioder.iterator().next().getPeriode().getFomDato();
        var tidligereStartdato = tidligerePerioder.iterator().next().getPeriode().getFomDato();

        if (startdato.equals(tidligereStartdato)) {
            return List.of();
        }

        return List.of(new EndretDato(startdato, tidligereStartdato));
    }

    public List<EndretDato> finnEndretSluttdatoer(UUID tidligereReferanse, UUID sisteReferanse) {
        var sisteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(sisteReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + sisteReferanse));
        var tidligereGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(tidligereReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + tidligereReferanse));
        var sistePerioder = sisteGrunnlag.getUngdomsprogramPerioder().getPerioder();

        if (sistePerioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke å finne endring i startdato for flere perioder");
        }

        var tidligerePerioder = tidligereGrunnlag.getUngdomsprogramPerioder().getPerioder();
        if (sistePerioder.isEmpty() || tidligerePerioder.isEmpty()) {
            return List.of();
        }

        var sluttdato = sistePerioder.iterator().next().getPeriode().getTomDato();
        var tidligereSluttdato = tidligerePerioder.iterator().next().getPeriode().getTomDato();

        if (sluttdato.equals(tidligereSluttdato)) {
            return List.of();
        }

        return List.of(new EndretDato(sluttdato, tidligereSluttdato));
    }


    /**
     * Lager tidslinje for perioder der bruker deltar i ungdomsprogram basert på verdier fra et oppgitt periodegrunnlag
     *
     * @param ungdomsprogramPeriodeGrunnlag Ungdomsprogram-grunnlag med perioder der bruker er i ungdomsprogram
     * @return Tidslinje for perioder der bruker er i ungdomsprogram
     */
    public LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(UngdomsprogramPeriode::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .map(this::komprimer)
            .orElse(LocalDateTimeline.empty());
    }

    public static LocalDateSegment<Boolean> erEndret(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return new LocalDateSegment<>(di, lhs == null || rhs == null || !lhs.getValue().equals(rhs.getValue()));
    }


    private LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t) {
        return t.compress((d1, d2) -> KANT_I_KANT_VURDERER.erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }


    public record EndretDato(LocalDate nyDato, LocalDate forrigeDato) {}


}
