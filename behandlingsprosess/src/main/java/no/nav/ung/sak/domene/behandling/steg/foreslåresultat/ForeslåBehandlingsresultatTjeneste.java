package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.SamleVilkårResultat;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ForeslåBehandlingsresultatTjeneste.class);

    private VilkårResultatRepository vilkårResultatRepository;

    private BehandlingRepository behandlingRepository;

    protected ForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public ForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public void foreslåBehandlingsresultatType(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        Long behandlingId = ref.getBehandlingId();

        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={} (før)", ref.getBehandlingId(), behandling.getBehandlingResultatType());

        var resultatType = finnBehandlingresultatType(ref, vilkårene);
        behandling.setBehandlingResultatType(resultatType);

        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={}", ref.getBehandlingId(), behandling.getBehandlingResultatType());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }


    private BehandlingResultatType finnBehandlingresultatType(BehandlingReferanse ref, Vilkårene vilkårene) {
        BehandlingResultatType resultatType;
        if (skalBehandlingenSettesTilAvslått(vilkårene)) {
            resultatType = BehandlingResultatType.AVSLÅTT;
        } else if (skalBehandlingenSettesTilDelvisInnvilget(ref, vilkårene)) {
            resultatType = BehandlingResultatType.DELVIS_INNVILGET;
        } else if (skalBehandlingenSettesTilOpphørt(ref, vilkårene)){
            resultatType = BehandlingResultatType.OPPHØR;
        } else {
            resultatType = BehandlingResultatType.INNVILGET;
        }
        return resultatType;
    }

    private boolean skalBehandlingenSettesTilOpphørt(BehandlingReferanse ref, Vilkårene vilkårene) {
        var originalVilkårResultat = ref.getOriginalBehandlingId().flatMap(vilkårResultatRepository::hentHvisEksisterer);
        return OpphørUtleder.erOpphør(vilkårene, originalVilkårResultat);
    }



    private boolean skalBehandlingenSettesTilDelvisInnvilget(BehandlingReferanse ref, Vilkårene vilkårene) {
        return false;
    }

    private boolean skalBehandlingenSettesTilAvslått(Vilkårene vilkårene) {
        Optional<VilkårType> førsteAvslåttVilkår = sjekkAllePerioderAvslåttForVilkår(vilkårene);
        return førsteAvslåttVilkår.isPresent();
    }

    /**
     * @return første vilkår som alle perioder er avslått for.
     */
    private Optional<VilkårType> sjekkAllePerioderAvslåttForVilkår(Vilkårene vilkårene) {
        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer();
        return vilkårTidslinjer.entrySet().stream()
            .filter(e -> harAvslåtteVilkårsPerioder(e.getValue()) && harIngenOppfylteVilkårsPerioder(e.getValue()))
            .findFirst().map(Map.Entry::getKey);
    }

    private boolean harIngenOppfylteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return timeline.filterValue(vp -> vp.getAvslagsårsak() == null && vp.getGjeldendeUtfall() == Utfall.OPPFYLT).isEmpty();
    }

    private boolean harAvslåtteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return !timeline.filterValue(vp -> vp.getAvslagsårsak() != null && vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT).isEmpty();
    }

}
