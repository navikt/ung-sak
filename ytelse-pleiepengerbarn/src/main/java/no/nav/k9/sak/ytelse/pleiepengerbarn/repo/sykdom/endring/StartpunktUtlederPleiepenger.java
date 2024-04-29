package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@GrunnlagRef(MedisinskGrunnlagsdata.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
class StartpunktUtlederPleiepenger implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederPleiepenger.class);

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    StartpunktUtlederPleiepenger() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPleiepenger(MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                 MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                 VilkårResultatRepository vilkårResultatRepository,
                                 ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                 EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                 SamtidigUttakTjeneste samtidigUttakTjeneste,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var result = new HashSet<StartpunktType>();
        StartpunktType sykdomStartpunk = utledStartpunktForSykdom(ref);
        result.add(sykdomStartpunk);
        log.info("Kjører diff av sykdom, funnet følgende resultat = {}", sykdomStartpunk);

        if (ref.getFagsakYtelseType() == FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            StartpunktType tilsynStartpunkt = utledStartpunktForEtablertTilsyn(ref);
            result.add(tilsynStartpunkt);
            log.info("Kjører diff av etablertTilsyn, funnet følgende resultat = {}", tilsynStartpunkt);
        }

        StartpunktType uttakStartpunkt = utledStartpunktForUttak(ref);
        log.info("Kjører diff av uttak, funnet følgende resultat = {}", uttakStartpunkt);
        result.add(uttakStartpunkt);

        if (ref.getFagsakYtelseType() == FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            StartpunktType nattevåkBeredskapStartpunkt = utledStartpunktForNattevåkOgBeredskap(ref);
            result.add(nattevåkBeredskapStartpunkt);
            log.info("Kjører diff av nattevåk & beredskap, funnet følgende resultat = {}", nattevåkBeredskapStartpunkt);
        }

        return result.stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType utledStartpunktForNattevåkOgBeredskap(BehandlingReferanse ref) {
        return endringUnntakEtablertTilsynTjeneste.harEndringerSidenBehandling(ref.getBehandlingId(), ref.getPleietrengendeAktørId()) ? StartpunktType.UTTAKSVILKÅR : StartpunktType.UDEFINERT;
    }

    private StartpunktType utledStartpunktForEtablertTilsyn(BehandlingReferanse referanse) {
        var erUhåndterteEndringer = erEndringPåEtablertTilsynTjeneste.erEndringerSidenBehandling(referanse);

        if (erUhåndterteEndringer) {
            return StartpunktType.UTTAKSVILKÅR;
        } else {
            return StartpunktType.UDEFINERT;
        }
    }

    private StartpunktType utledStartpunktForUttak(BehandlingReferanse ref) {
        if (samtidigUttakTjeneste.isSkalHaTilbakehopp(ref)) {
            return StartpunktType.UTTAKSVILKÅR_VURDERING;
        } else {
            return StartpunktType.UDEFINERT;
        }
    }


    private StartpunktType utledStartpunktForSykdom(BehandlingReferanse ref) {
        var sykdomGrunnlag = medisinskGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(MedisinskGrunnlag::getGrunnlagsdata);

        NavigableSet<DatoIntervallEntitet> nyeVurderingsperioder = utledVurderingsperiode(ref);
        var utledGrunnlag = medisinskGrunnlagTjeneste.utledGrunnlagMedManglendeOmsorgFjernet(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getBehandlingId(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        var erIngenEndringIGrunnlaget = sykdomGrunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        var startpunktType = erIngenEndringIGrunnlaget ? StartpunktType.UDEFINERT : StartpunktType.INNGANGSVILKÅR_MEDISINSK;
        return startpunktType;
    }

    private NavigableSet<DatoIntervallEntitet> utledVurderingsperiode(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (vilkårene.isEmpty()) {
            return new TreeSet<>();
        }

        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());

        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();

        for (VilkårType vilkårType : perioderTilVurderingTjeneste.definerendeVilkår()) {
            var vilkår = vilkårene.get().getVilkår(vilkårType);
            if (vilkår.isPresent()) {
                var perioder = vilkår.get().getPerioder().stream().map(VilkårPeriode::getPeriode).toList();
                tidslinje = tidslinje.combine(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
    }

}
