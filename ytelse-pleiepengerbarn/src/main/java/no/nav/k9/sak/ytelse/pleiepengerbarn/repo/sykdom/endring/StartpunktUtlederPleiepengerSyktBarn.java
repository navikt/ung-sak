package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;

@ApplicationScoped
@GrunnlagRef("SykdomGrunnlag")
@FagsakYtelseTypeRef("PSB")
class StartpunktUtlederPleiepengerSyktBarn implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederPleiepengerSyktBarn.class);

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    StartpunktUtlederPleiepengerSyktBarn() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPleiepengerSyktBarn(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                         SykdomGrunnlagService sykdomGrunnlagService,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         EtablertTilsynTjeneste etablertTilsynTjeneste,
                                         @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        StartpunktType sykdomStartpunk = utledStartpunktForSykdom(ref);
        log.info("Kjører diff av sykdom, funnet følgende resultat = {}", sykdomStartpunk);
        StartpunktType tilsynStartpunkt = utledStartpunktForEtablertTilsyn(ref);
        log.info("Kjører diff av etablertTilsyn, funnet følgende resultat = {}", tilsynStartpunkt);

        return Set.of(sykdomStartpunk, tilsynStartpunkt)
            .stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType utledStartpunktForEtablertTilsyn(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = etablertTilsynTjeneste.finnForskjellerSidenForrigeBehandling(referanse);
        resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(referanse.getBehandlingId())));
        //resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
        resultat = resultat.intersection(SykdomUtils.toLocalDateTimeline(perioderTilVurderingTjeneste.utledFullstendigePerioder(referanse.getBehandlingId())));

        if (resultat.isEmpty()) {
            return StartpunktType.UDEFINERT;
        } else {
            return StartpunktType.UTTAKSVILKÅR;
        }
    }

    private StartpunktType utledStartpunktForSykdom(BehandlingReferanse ref) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(SykdomGrunnlagBehandling::getGrunnlag);

        List<Periode> nyeVurderingsperioder = utledVurderingsperiode(ref.getBehandlingId());
        var utledGrunnlag = sykdomGrunnlagService.utledGrunnlagMedManglendeOmsorgFjernet(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getBehandlingId(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomGrunnlagService.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        var erIngenEndringIGrunnlaget = sykdomGrunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var startpunktType = erIngenEndringIGrunnlaget ? StartpunktType.UDEFINERT : StartpunktType.INNGANGSVILKÅR_MEDISINSK;
        return startpunktType;
    }

    private List<Periode> utledVurderingsperiode(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isEmpty()) {
            return List.of();
        }
        var vurderingsperioder = vilkårene.get().getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));

        vurderingsperioder.addAll(vilkårene.get().getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toList()));

        return vurderingsperioder;
    }

}
