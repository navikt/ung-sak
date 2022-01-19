package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.Kilde;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForPleietrengendeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForPleietrengendeTjeneste.FagsakKravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.delt.UtledetEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;

@Dependent
public class EtablertTilsynTjeneste {

    private EtablertTilsynRepository etablertTilsynRepository;
    private PeriodeFraSøknadForPleietrengendeTjeneste periodeFraSøknadForPleietrengendeTjeneste;

    @Inject
    public EtablertTilsynTjeneste(EtablertTilsynRepository etablertTilsynRepository,
                                  PeriodeFraSøknadForPleietrengendeTjeneste periodeFraSøknadForPleietrengendeTjeneste) {
        this.etablertTilsynRepository = etablertTilsynRepository;
        this.periodeFraSøknadForPleietrengendeTjeneste = periodeFraSøknadForPleietrengendeTjeneste;
    }


    public LocalDateTimeline<UtledetEtablertTilsyn> beregnTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final var tilsynsgrunnlagPåTversAvFagsaker = periodeFraSøknadForPleietrengendeTjeneste.hentAllePerioderTilVurdering(behandlingRef.getPleietrengendeAktørId(), behandlingRef.getFagsakPeriode());
        return byggTidslinje(behandlingRef.getSaksnummer(), tilsynsgrunnlagPåTversAvFagsaker);
    }

    public EtablertTilsyn utledGrunnlagForTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final LocalDateTimeline<UtledetEtablertTilsyn> tilsynstidslinje = beregnTilsynstidlinje(behandlingRef);

        final List<EtablertTilsynPeriode> tilsynsperioder = tilsynstidslinje.stream()
            .map(s -> new EtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet(), s.getValue().getJournalpostId()))
            .collect(Collectors.toList());

        return new EtablertTilsyn(tilsynsperioder);
    }

    public void opprettGrunnlagForTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final EtablertTilsyn etablertTilsyn = utledGrunnlagForTilsynstidlinje(behandlingRef);
        etablertTilsynRepository.lagre(behandlingRef.getBehandlingId(), etablertTilsyn);
    }

    public LocalDateTimeline<Boolean> finnForskjellerSidenForrigeBehandling(BehandlingReferanse behandlingRef) {
        final var behandlingOpt = behandlingRef.getOriginalBehandlingId();
        final EtablertTilsyn forrigeBehandlingEtablertTilsyn = behandlingOpt.flatMap(behandlingId -> etablertTilsynRepository.hentHvisEksisterer(behandlingId))
            .map(EtablertTilsynGrunnlag::getEtablertTilsyn)
            .orElse(new EtablertTilsyn(List.of()));
        return uhåndterteEndringerFraForrige(behandlingRef, forrigeBehandlingEtablertTilsyn);
    }

    public LocalDateTimeline<Boolean> finnForskjellerFraEksisterendeVersjon(BehandlingReferanse behandlingRef) {
        var etablertTilsynGrunnlag = etablertTilsynRepository.hentHvisEksisterer(behandlingRef.getBehandlingId());
        if (etablertTilsynGrunnlag.isEmpty()) {
            return new LocalDateTimeline<>(List.of());
        }

        var forrigeEtablertTilsyn = etablertTilsynGrunnlag
            .map(EtablertTilsynGrunnlag::getEtablertTilsyn)
            .orElse(new EtablertTilsyn(List.of()));

        return uhåndterteEndringerFraForrige(behandlingRef, forrigeEtablertTilsyn);
    }

    private LocalDateTimeline<Boolean> uhåndterteEndringerFraForrige(BehandlingReferanse behandlingRef, EtablertTilsyn forrigeBehandlingEtablertTilsyn) {
        final EtablertTilsyn nyBehandlingtablertTilsyn = utledGrunnlagForTilsynstidlinje(behandlingRef);

        final LocalDateTimeline<Duration> forrigeBehandlingEtablertTilsynTidslinje = tilTidslinje(forrigeBehandlingEtablertTilsyn);
        final LocalDateTimeline<Duration> nyBehandlingEtablertTilsynTidslinje = tilTidslinje(nyBehandlingtablertTilsyn);

        return forrigeBehandlingEtablertTilsynTidslinje.combine(nyBehandlingEtablertTilsynTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null || datoSegment2 == null || !datoSegment.getValue().equals(datoSegment2.getValue())) {
                return new LocalDateSegment<>(datoInterval, Boolean.TRUE);
            }
            return null;
        }, JoinStyle.CROSS_JOIN);
    }

    private LocalDateTimeline<Duration> tilTidslinje(final EtablertTilsyn etablertTilsyn) {
        return new LocalDateTimeline<>(
            etablertTilsyn.getPerioder()
                .stream()
                .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getVarighet()))
                .collect(Collectors.toList())
        );
    }

    private LocalDateTimeline<UtledetEtablertTilsyn> byggTidslinje(Saksnummer søkersSaksnummer, List<FagsakKravDokument> fagsakKravDokumenter) {
        var resultatTimeline = new LocalDateTimeline<UtledetEtablertTilsyn>(List.of());
        for (FagsakKravDokument kravDokument : fagsakKravDokumenter) {
            var tilsynsordningPerioder = kravDokument.getPerioderFraSøknad()
                .getTilsynsordning()
                .stream()
                .map(Tilsynsordning::getPerioder)
                .flatMap(Collection::stream)
                .toList();

            for (var periode : tilsynsordningPerioder) {
                final var kilde = søkersSaksnummer.equals(kravDokument.getFagsak().getSaksnummer()) ? Kilde.SØKER : Kilde.ANDRE;
                final var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new UtledetEtablertTilsyn(periode.getVarighet(), kilde, kravDokument.getKravDokument().getJournalpostId()))));
                resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }
        return resultatTimeline.compress();
    }
}
