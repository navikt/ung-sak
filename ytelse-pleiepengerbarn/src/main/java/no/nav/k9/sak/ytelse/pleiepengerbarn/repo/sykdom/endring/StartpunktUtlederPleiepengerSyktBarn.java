package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@GrunnlagRef("SykdomGrunnlag")
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
class StartpunktUtlederPleiepengerSyktBarn implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederPleiepengerSyktBarn.class);

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårResultatRepository vilkårResultatRepository;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;

    StartpunktUtlederPleiepengerSyktBarn() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPleiepengerSyktBarn(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                         SykdomGrunnlagService sykdomGrunnlagService,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                         EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                         SamtidigUttakTjeneste samtidigUttakTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var result = new HashSet<StartpunktType>();
        StartpunktType sykdomStartpunk = utledStartpunktForSykdom(ref);
        result.add(sykdomStartpunk);
        log.info("Kjører diff av sykdom, funnet følgende resultat = {}", sykdomStartpunk);

        StartpunktType tilsynStartpunkt = utledStartpunktForEtablertTilsyn(ref);
        result.add(tilsynStartpunkt);
        log.info("Kjører diff av etablertTilsyn, funnet følgende resultat = {}", tilsynStartpunkt);

        StartpunktType uttakStartpunkt = utledStartpunktForUttak(ref);
        log.info("Kjører diff av uttak, funnet følgende resultat = {}", uttakStartpunkt);
        result.add(uttakStartpunkt);

        StartpunktType nattevåkBeredskapStartpunkt = utledStartpunktForNattevåkOgBeredskap(ref);
        result.add(nattevåkBeredskapStartpunkt);
        log.info("Kjører diff av nattevåk & beredskap, funnet følgende resultat = {}", nattevåkBeredskapStartpunkt);

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
