package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt.EtablertTilsynUnntaksutnuller;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt.SykdomsperiodeEtablertTilsynSmører;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;

@Dependent
public class HentEtablertTilsynTjeneste {

    private final LocalDate ukesmøringOmsorgstilbudFomDato;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;
    private EtablertTilsynRepository etablertTilsynRepository;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public HentEtablertTilsynTjeneste(SykdomVurderingTjeneste sykdomVurderingTjeneste,
                                      PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository,
                                      EtablertTilsynRepository etablertTilsynRepository,
                                      EtablertTilsynTjeneste etablertTilsynTjeneste,
                                      PersonopplysningTjeneste personopplysningTjeneste,
                                      @KonfigVerdi(value = "UKESMOERING_OMSORGSTILBUD_FOM_DATO", defaultVerdi = "", required = false) String ukesmøringOmsorgstilbudFomDatoString) {

        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
        this.etablertTilsynRepository = etablertTilsynRepository;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;

        if (ukesmøringOmsorgstilbudFomDatoString == null || ukesmøringOmsorgstilbudFomDatoString.trim().isEmpty()) {
            this.ukesmøringOmsorgstilbudFomDato = null;
        } else {
            this.ukesmøringOmsorgstilbudFomDato = LocalDate.parse(ukesmøringOmsorgstilbudFomDatoString);
        }
    }

    private static LocalDateTimeline<Duration> gammelLøsningForEtablertTilsyn(LocalDateTimeline<Duration> etablertTilsynForPleietrengende,
                                                                              List<PleietrengendeSykdomInnleggelsePeriode> innleggelser) {

        return EtablertTilsynUnntaksutnuller.ignorerEtablertTilsynVedInnleggelser(etablertTilsynForPleietrengende, innleggelser);
    }

    private static List<PeriodeMedVarighet> toPeriodeMedVarighetList(LocalDateTimeline<Duration> etablertTilsynPerioderTidslinje, LocalDate pleietrengendeDødsdato) {
        var relevantPeriodeForTilsyn = new LocalDateTimeline<>(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, true);
        if (pleietrengendeDødsdato != null) {
            relevantPeriodeForTilsyn = new LocalDateTimeline<>(Tid.TIDENES_BEGYNNELSE, pleietrengendeDødsdato, true);
        }
        return etablertTilsynPerioderTidslinje.intersection(relevantPeriodeForTilsyn).stream().map(s -> new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue())).collect(Collectors.toList());
    }

    private static LocalDateTimeline<Duration> toVarighettidslinjeFraPerioderMedVarighet(List<PeriodeMedVarighet> perioderMedVarighetliste) {
        return new LocalDateTimeline<>(perioderMedVarighetliste.stream()
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getVarighet()))
            .collect(Collectors.toList())
        );
    }

    private static LocalDateTimeline<Duration> toVarighettidslinje(List<EtablertTilsynPeriode> etablertTilsynPerioder) {
        LocalDateTimeline<Duration> etablertTilsynTidslinje = new LocalDateTimeline<>(etablertTilsynPerioder.stream()
            .map(e -> new LocalDateSegment<>(e.getPeriode().getFomDato(), e.getPeriode().getTomDato(), e.getVarighet()))
            .collect(Collectors.toList()));
        return etablertTilsynTidslinje;
    }

    public List<PeriodeMedVarighet> hentOgSmørEtablertTilsynPerioder(BehandlingReferanse referanse,
                                                                     Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende,
                                                                     boolean brukUbesluttedeData) {

        if (referanse.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return List.of();
        }

        final LocalDateTimeline<Duration> etablertTilsynForPleietrengende = hentEtablertTilsynForPleietrengende(referanse, brukUbesluttedeData);
        final List<PleietrengendeSykdomInnleggelsePeriode> innleggelser;
        if (brukUbesluttedeData) {
            innleggelser = pleietrengendeSykdomDokumentRepository.hentInnleggelse(referanse.getPleietrengendeAktørId()).getPerioder();
        } else {
            innleggelser = pleietrengendeSykdomDokumentRepository.hentInnleggelse(referanse.getBehandlingUuid()).getPerioder();
        }
        final LocalDateTimeline<Duration> gammelLøsningEtablertTilsynTidslinje = gammelLøsningForEtablertTilsyn(etablertTilsynForPleietrengende, innleggelser);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, null);
        var pleietrengendeDødsdato = personopplysningerAggregat.getPersonopplysning(referanse.getPleietrengendeAktørId()).getDødsdato();

        if (ukesmøringOmsorgstilbudFomDato == null) {
            return toPeriodeMedVarighetList(gammelLøsningEtablertTilsynTidslinje, pleietrengendeDødsdato);
        }

        final LocalDateTimeline<Boolean> sykdomsperioderPåPleietrengende;
        if (brukUbesluttedeData) {
            sykdomsperioderPåPleietrengende = sykdomVurderingTjeneste.hentPsbOppfyltePerioderPåPleietrengende(referanse.getPleietrengendeAktørId());
        } else {
            sykdomsperioderPåPleietrengende = sykdomVurderingTjeneste.hentPsbOppfyltePerioderPåBehandling(referanse.getBehandlingUuid());
        }

        final LocalDateTimeline<Duration> etablertTilsynForSmøring = EtablertTilsynUnntaksutnuller.ignorerEtablertTilsynVedInnleggelserOgUnntak(etablertTilsynForPleietrengende, unntakEtablertTilsynForPleietrengende, innleggelser);
        final List<PeriodeMedVarighet> smurteEtablertTilsynPerioder = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(sykdomsperioderPåPleietrengende, etablertTilsynForSmøring);
        final LocalDateTimeline<Duration> etablertTilsynPerioderTidslinje = håndterSmøringFraFeatureToggleDato(gammelLøsningEtablertTilsynTidslinje, smurteEtablertTilsynPerioder);

        return toPeriodeMedVarighetList(etablertTilsynPerioderTidslinje, pleietrengendeDødsdato);
    }

    private LocalDateTimeline<Duration> hentEtablertTilsynForPleietrengende(BehandlingReferanse referanse, boolean brukUbesluttedeData) {
        final List<EtablertTilsynPeriode> etablertTilsynPerioder;
        if (brukUbesluttedeData) {
            etablertTilsynPerioder = etablertTilsynTjeneste.utledGrunnlagForTilsynstidlinje(referanse).getPerioder();
        } else {
            etablertTilsynPerioder = etablertTilsynRepository.hentHvisEksisterer(referanse.getBehandlingId())
                .map(EtablertTilsynGrunnlag::getEtablertTilsyn)
                .map(EtablertTilsyn::getPerioder)
                .orElse(List.of());
        }
        final LocalDateTimeline<Duration> etablertTilsynTidslinje = toVarighettidslinje(etablertTilsynPerioder);
        return etablertTilsynTidslinje;
    }

    private LocalDateTimeline<Duration> håndterSmøringFraFeatureToggleDato(LocalDateTimeline<Duration> ikkeSmurtTidslinje,
                                                                           List<PeriodeMedVarighet> smurteEtablertTilsynPerioder) {

        LocalDateTimeline<Duration> smurtTidslinje = toVarighettidslinjeFraPerioderMedVarighet(smurteEtablertTilsynPerioder);
        smurtTidslinje = smurtTidslinje.intersection(DatoIntervallEntitet.fraOgMed(ukesmøringOmsorgstilbudFomDato).toLocalDateInterval());

        return ikkeSmurtTidslinje.union(smurtTidslinje, StandardCombinators::coalesceRightHandSide);
    }
}
