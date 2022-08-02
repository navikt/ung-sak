package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.hendelse.vedtak.SakMedPeriode;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger.FeriepengerAvvikTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomGrunnlagTjeneste sykdomGrunnlagTjeneste;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private BehandlingModellRepository behandlingModellRepository;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private FeriepengerAvvikTjeneste feriepengerAvvikTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private boolean enableFeriepengerPåTversAvSaker;

    VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                               FagsakRepository fagsakRepository,
                                                               VilkårResultatRepository vilkårResultatRepository,
                                                               SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                               SykdomVurderingRepository sykdomVurderingRepository,
                                                               SykdomGrunnlagTjeneste sykdomGrunnlagTjeneste,
                                                               ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                               EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                               SamtidigUttakTjeneste samtidigUttakTjeneste,
                                                               SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                               BehandlingModellRepository behandlingModellRepository,
                                                               UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                               FeriepengerAvvikTjeneste feriepengerAvvikTjeneste, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                               @KonfigVerdi(value = "ENABLE_FERIEPENGER_PAA_TVERS_AV_SAKER_OG_PR_AAR", defaultVerdi = "true") boolean enableFeriepengerPåTversAvSaker) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomGrunnlagTjeneste = sykdomGrunnlagTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.feriepengerAvvikTjeneste = feriepengerAvvikTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.enableFeriepengerPåTversAvSaker = enableFeriepengerPåTversAvSaker;
    }

    @Override
    public List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        List<SakMedPeriode> resultat = new ArrayList<>();

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, sisteBehandlingPåKandidat.getFagsakYtelseType(), sisteBehandlingPåKandidat.getType());
                var referanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var skalRevurderesPgaSykdom = perioderMedRevurderingSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var skalRevurderesPgaEtablertTilsyn = perioderMedRevurderingPgaEtablertTilsyn(referanse);
                var skalRevurderesPgaNattevåkOgBeredskap = perioderMedRevurderesPgaNattevåkOgBeredskap(referanse);
                var skalRevurderesPgaEndretUttak = perioderMedRevurderingPgaUttak(sisteBehandlingPåKandidat, referanse);

                var skalRevurdereYtelsePgaEndringAnnenSak = skalRevurderesPgaEtablertTilsyn
                    .union(skalRevurderesPgaNattevåkOgBeredskap, StandardCombinators::alwaysTrueForMatch)
                    .union(skalRevurderesPgaEndretUttak, StandardCombinators::alwaysTrueForMatch)
                    .union(skalRevurderesPgaSykdom, StandardCombinators::alwaysTrueForMatch);
                var skalReberegneFeriepenger = enableFeriepengerPåTversAvSaker ? perioderMedRevurderingPgaEndringFeriepenger(sisteBehandlingPåKandidat, referanse) : LocalDateTimeline.empty();

                if (!skalRevurdereYtelsePgaEndringAnnenSak.isEmpty() || !skalReberegneFeriepenger.isEmpty()) {
                    if (!skalRevurdereYtelsePgaEndringAnnenSak.isEmpty()) {
                        //Er kun støtte p.t. for å oppgi en årsak pr sak. Må velge 'endring i ytelse'-årsak da denne (i motsetning til feriepenger-årsak) gir opphav til vilkårsperioder i revurderingen. Feriepengene vil uansett blir reberegnet
                        LocalDateTimeline<Boolean> perioderSkalRevurdereYtelse = tettHull(perioderTilVurderingTjeneste, skalRevurdereYtelsePgaEndringAnnenSak);
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, TidslinjeUtil.tilDatoIntervallEntiteter(perioderSkalRevurdereYtelse), BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));
                    } else {
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, TidslinjeUtil.tilDatoIntervallEntiteter(skalReberegneFeriepenger), BehandlingÅrsakType.RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK));
                    }
                    if (enableFeriepengerPåTversAvSaker) {
                        log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}, feriepenger={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty(), skalReberegneFeriepenger);
                    } else {
                        log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty());
                    }
                }
            }
        }
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(vedtattBehandling.getId()).map(UtsattBehandlingAvPeriode::getPerioder).orElse(Set.of());

        if (!utsattBehandlingAvPeriode.isEmpty()) {
            var perioderSomErUtsatt = utsattBehandlingAvPeriode.stream().map(UtsattPeriode::getPeriode).collect(Collectors.toCollection(TreeSet::new));
            resultat.add(new SakMedPeriode(fagsak.getSaksnummer(), perioderSomErUtsatt, BehandlingÅrsakType.RE_GJENOPPTAR_UTSATT_BEHANDLING));
            log.info("Sak='{}' har utsatte perioder som må behandles", fagsak.getSaksnummer());
        }

        return resultat;
    }

    private LocalDateTimeline<Boolean> tettHull(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, LocalDateTimeline<Boolean> skalRevurderes) {
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();
        var tidslinjeHull = Hjelpetidslinjer.utledHullSomMåTettes(skalRevurderes, kantIKantVurderer);
        return skalRevurderes.crossJoin(tidslinjeHull, StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaUttak(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return LocalDateTimeline.empty();
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(samtidigUttakTjeneste.perioderMedEndringerMedUbesluttedeData(referanse));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEndringFeriepenger(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !feriepengerAvvikTjeneste.harKommetTilTilkjentYtelse(referanse)) {
            return LocalDateTimeline.empty();
        }
        Set<Year> opptjeningsårFeriepengerMåReberegnes = feriepengerAvvikTjeneste.opptjeningsårFeirepengerMåReberegnes(referanse);
        return new LocalDateTimeline<>(opptjeningsårFeriepengerMåReberegnes.stream()
            .map(år -> new LocalDateSegment<>(år.atMonth(1).atDay(1), år.atMonth(12).atDay(31), true))
            .toList());
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return TidslinjeUtil.tilTidslinjeKomprimert(endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId()));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEtablertTilsyn(BehandlingReferanse referanse) {
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
        var kandidatSykdomBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid()).orElseThrow();
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(kandidatSykdomBehandling.getBehandlingUuid()).orElseThrow();
        var vurderingsperioder = utledVurderingsperiode(behandling);
        var manglendeOmsorgenForPerioder = sykdomGrunnlagTjeneste.hentManglendeOmsorgenForPerioder(behandling.getId());
        var utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, vurderingsperioder, manglendeOmsorgenForPerioder);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(kandidatSykdomBehandling.getGrunnlagsdata()), utledetGrunnlag).getDiffPerioder();

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

    private List<Periode> utledVurderingsperiode(Behandling behandling) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());

        return perioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .map(vilkårType -> vilkårene.getVilkår(vilkårType)
                .map(Vilkår::getPerioder))
            .flatMap(Optional::stream)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .toList();
    }

}
