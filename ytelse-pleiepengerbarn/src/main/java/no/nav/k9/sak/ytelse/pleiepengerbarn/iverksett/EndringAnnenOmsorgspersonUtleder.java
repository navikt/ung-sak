package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

public class EndringAnnenOmsorgspersonUtleder {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private BehandlingModellRepository behandlingModellRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    EndringAnnenOmsorgspersonUtleder() {
    }

    @Inject
    public EndringAnnenOmsorgspersonUtleder(BehandlingRepository behandlingRepository,
                                            VilkårResultatRepository vilkårResultatRepository,
                                            MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                            MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                            ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                            EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                            SamtidigUttakTjeneste samtidigUttakTjeneste,
                                            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                            BehandlingModellRepository behandlingModellRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public Endringstidslinjer utledTidslinjerForEndringSomPåvirkerSak(Saksnummer saksnummer,
                                                                      AktørId pleietrengende,
                                                                      Behandling aktuellBehandling,
                                                                      LocalDateTimeline<Boolean> aktuelleVedtaksperioder) {
        var kandidatBehandlingReferanse = BehandlingReferanse.fra(aktuellBehandling);
        var skalRevurderesPgaSykdom = perioderMedRevurderingSykdom(pleietrengende, saksnummer, aktuellBehandling);
        var skalRevurderesPgaEtablertTilsyn = perioderMedRevurderingPgaEtablertTilsyn(kandidatBehandlingReferanse);
        var skalRevurderesPgaNattevåkOgBeredskap = perioderMedRevurderesPgaNattevåkOgBeredskap(kandidatBehandlingReferanse);
        var skalRevurderesPgaEndretUttak = perioderMedRevurderingPgaUttak(kandidatBehandlingReferanse)
            .intersection(aktuelleVedtaksperioder);

        var perioderMedInnvilgetInngangsvilkår = periodermedInnvilgetInngangsvilkår(kandidatBehandlingReferanse);

        var harEndringSomPåvirkerSak = skalRevurderesPgaEtablertTilsyn
            .union(skalRevurderesPgaNattevåkOgBeredskap, StandardCombinators::alwaysTrueForMatch)
            .union(skalRevurderesPgaEndretUttak, StandardCombinators::alwaysTrueForMatch)
            .union(skalRevurderesPgaSykdom, StandardCombinators::alwaysTrueForMatch)
            .intersection(perioderMedInnvilgetInngangsvilkår);
        return new Endringstidslinjer(skalRevurderesPgaSykdom, skalRevurderesPgaEtablertTilsyn, skalRevurderesPgaNattevåkOgBeredskap, skalRevurderesPgaEndretUttak, harEndringSomPåvirkerSak);
    }

    private LocalDateTimeline<Boolean> periodermedInnvilgetInngangsvilkår(BehandlingReferanse kandidatBehandlingReferanse) {
        LocalDateTimeline<Boolean> result = LocalDateTimeline.empty();

        if (!kandidatBehandlingReferanse.getBehandlingStatus().erFerdigbehandletStatus()) {
            // Ved åpen behandling trenger vi ikke begrense hvilke
            result = TidslinjeUtil.tilTidslinjeKomprimert(utledVurderingsperiode(kandidatBehandlingReferanse));
        }
        var vilkårene = vilkårResultatRepository.hent(kandidatBehandlingReferanse.getBehandlingId());
        var vilkårTidslinjer = vilkårene.getVilkårene()
            .stream()
            .filter(vilkår -> !Objects.equals(vilkår.getVilkårType(), VilkårType.SØKNADSFRIST))
            .map(v -> new LocalDateTimeline<>(v.getPerioder()
                .stream()
                .map(vp -> new LocalDateSegment<>(vp.getPeriode().toLocalDateInterval(), Objects.equals(Utfall.OPPFYLT, vp.getGjeldendeUtfall())))
                .toList()))
            .toList();

        LocalDateTimeline<Boolean> vilkårstidslinje = LocalDateTimeline.empty();

        for (LocalDateTimeline<Boolean> tidslinje : vilkårTidslinjer) {
            vilkårstidslinje = vilkårstidslinje.combine(tidslinje, this::mergeVilkårsSegmenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }


        return vilkårstidslinje.combine(result, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private LocalDateSegment<Boolean> mergeVilkårsSegmenter(LocalDateInterval localDateInterval, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        if (lhs == null || lhs.getValue() == null) {
            return rhs;
        }
        if (rhs == null || rhs.getValue() == null) {
            return lhs;
        }
        if (!rhs.getValue()) {
            return new LocalDateSegment<>(localDateInterval, rhs.getValue());
        }
        return new LocalDateSegment<>(localDateInterval, lhs.getValue());
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaUttak(BehandlingReferanse referanse) {
        if (!referanse.getBehandlingStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return LocalDateTimeline.empty();
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(samtidigUttakTjeneste.perioderMedEndringerMedUbesluttedeData(referanse));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        if (referanse.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return LocalDateTimeline.empty();
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId()));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEtablertTilsyn(BehandlingReferanse referanse) {
        if (referanse.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return LocalDateTimeline.empty();
        }
        return erEndringPåEtablertTilsynTjeneste.perioderMedEndringerFraEksisterendeVersjon(referanse)
            .filterValue(Objects::nonNull);
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        if (søknadsperiodeTjeneste.utledFullstendigPeriode(sisteBehandlingPåKandidat.getId()).isEmpty()) {
            return LocalDateTimeline.empty();
        }
        if (erUnderBehandlingOgIkkeKommetTilSykdom(sisteBehandlingPåKandidat)) {
            return LocalDateTimeline.empty();
        }
        var kandidatSykdomBehandling = medisinskGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid()).orElseThrow();
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(kandidatSykdomBehandling.getBehandlingUuid()).orElseThrow();
        var vurderingsperioder = utledVurderingsperiode(BehandlingReferanse.fra(behandling));
        var manglendeOmsorgenForPerioder = medisinskGrunnlagTjeneste.hentManglendeOmsorgenForPerioder(behandling.getId());
        var utledetGrunnlag = medisinskGrunnlagRepository.utledGrunnlag(kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, vurderingsperioder, manglendeOmsorgenForPerioder);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(kandidatSykdomBehandling.getGrunnlagsdata()), utledetGrunnlag).getDiffPerioder();

        return endringerISøktePerioder.compress()
            .filterValue(Objects::nonNull);
    }

    private boolean erUnderBehandlingOgIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        return !sisteBehandlingPåKandidat.erSaksbehandlingAvsluttet() && harIkkeKommetTilSykdom(sisteBehandlingPåKandidat);
    }

    private boolean harIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        final BehandlingStegType steg = sisteBehandlingPåKandidat.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(sisteBehandlingPåKandidat.getType(), sisteBehandlingPåKandidat.getFagsakYtelseType());
        return modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR);
    }

    private NavigableSet<DatoIntervallEntitet> utledVurderingsperiode(BehandlingReferanse referanse) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        return perioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .map(vilkårType -> vilkårene.getVilkår(vilkårType)
                .map(Vilkår::getPerioder))
            .flatMap(Optional::stream)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public record Endringstidslinjer(LocalDateTimeline<Boolean> harEndretSykdomTidslinje,
                                      LocalDateTimeline<Boolean> harEndretEtablertTilsynTidslinje,
                                      LocalDateTimeline<Boolean> harEndretNattevåkOgBeredskapTidslinje,
                                      LocalDateTimeline<Boolean> harEndretUttakTidslinje,
                                      LocalDateTimeline<Boolean> harEndringSomPåvirkerSakTidslinje) {
    }

}
