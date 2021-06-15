package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.Kilde;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeFraSøknadForPleietrengendeTjeneste.FagsakKravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.delt.UtledetEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
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
        
        final EtablertTilsyn etablertTilsyn = new EtablertTilsyn(tilsynsperioder);
        return etablertTilsyn;
    }
    
    public void opprettGrunnlagForTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final EtablertTilsyn etablertTilsyn = utledGrunnlagForTilsynstidlinje(behandlingRef);
        etablertTilsynRepository.lagre(behandlingRef.getBehandlingId(), etablertTilsyn);
    }
    
    public LocalDateTimeline<Boolean> finnForskjellerSidenForrigeBehandling(BehandlingReferanse behandlingRef) {
        final var behandlingOpt = behandlingRef.getOriginalBehandlingId();
        final EtablertTilsyn forrigeBehandlingEtablertTilsyn = behandlingOpt
                .map(behandlingId -> etablertTilsynRepository.hentHvisEksisterer(behandlingId).orElse(null))
                .map(g -> g.getEtablertTilsyn())
                .orElse(new EtablertTilsyn(List.of()));
        final EtablertTilsyn nyBehandlingtablertTilsyn = utledGrunnlagForTilsynstidlinje(behandlingRef);
        
        final LocalDateTimeline<Duration> forrigeBehandlingEtablertTilsynTidslinje = tilTidslinje(forrigeBehandlingEtablertTilsyn);
        final LocalDateTimeline<Duration> nyBehandlingEtablertTilsynTidslinje = tilTidslinje(nyBehandlingtablertTilsyn);
        
        return forrigeBehandlingEtablertTilsynTidslinje.combine(nyBehandlingEtablertTilsynTidslinje, new LocalDateSegmentCombinator<Duration, Duration, Boolean>() {
            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<Duration> datoSegment, LocalDateSegment<Duration> datoSegment2) {
                if (datoSegment == null || datoSegment2 == null || !datoSegment.getValue().equals(datoSegment2.getValue())) {
                    return new LocalDateSegment<>(datoInterval, Boolean.TRUE);
                }
                return null;
            }
        }, JoinStyle.CROSS_JOIN);
    }

    private LocalDateTimeline<Duration> tilTidslinje(final EtablertTilsyn forrigeBehandlingEtablertTilsyn) {
         return new LocalDateTimeline<>(
            forrigeBehandlingEtablertTilsyn.getPerioder()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getVarighet()))
            .collect(Collectors.toList())
        );
    }

    private LocalDateTimeline<UtledetEtablertTilsyn> byggTidslinje(Saksnummer søkersSaksnummer, List<FagsakKravDokument> fagsakKravDokumenter) {
        var resultatTimeline = new LocalDateTimeline<UtledetEtablertTilsyn>(List.of());
        for (FagsakKravDokument kravDokument : fagsakKravDokumenter) {
            for (var periode : kravDokument.getPerioderFraSøknad().getTilsynsordning().stream().map(Tilsynsordning::getPerioder).flatMap(Collection::stream).collect(Collectors.toList())) {
                final var kilde = søkersSaksnummer.equals(kravDokument.getFagsak().getSaksnummer()) ? Kilde.SØKER : Kilde.ANDRE;
                final var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new UtledetEtablertTilsyn(periode.getVarighet(), kilde, kravDokument.getKravDokument().getJournalpostId()))));
                resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }
        return resultatTimeline.compress();
    }
}
