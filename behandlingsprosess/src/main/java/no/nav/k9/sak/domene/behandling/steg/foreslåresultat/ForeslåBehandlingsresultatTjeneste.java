package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public abstract class ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ForeslåBehandlingsresultatTjeneste.class);

    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private VedtakVarselRepository vedtakVarselRepository;

    private VilkårResultatRepository vilkårResultatRepository;

    private BehandlingRepository behandlingRepository;

    protected ForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    public ForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                              VedtakVarselRepository vedtakVarselRepository,
                                              RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        this.revurderingBehandlingsresultatutleder = revurderingBehandlingsresultatutleder;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public void foreslåBehandlingsresultatType(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        Long behandlingId = ref.getBehandlingId();

        var vilkårene = vilkårResultatRepository.hent(behandlingId);


        if (ref.getFagsakYtelseType() == FagsakYtelseType.FRISINN) {
            // kun for å sette vedtaksbrev og redusertUtbetalingÅrsak for FRISINN. Ellers er det flyttet til formidling
            var vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());
            vedtakVarselRepository.lagre(behandlingId, vedtakVarsel);

        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={} (før)", ref.getBehandlingId(), behandling.getBehandlingResultatType());

        if (skalBehandlingenSettesTilAvslått(ref, vilkårene)) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        } else if (skalBehandlingenSettesTilDelvisInnvilget(ref, vilkårene)) {
            behandling.setBehandlingResultatType(BehandlingResultatType.DELVIS_INNVILGET);
            log.info("Behandling {} delvis innvilget", ref.getBehandlingId());
        } else {
            behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
            log.info("Behandling {} innvilget", ref.getBehandlingId());
        }

        if (ref.erRevurdering()) {
            revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref);
        }

        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={}", ref.getBehandlingId(), behandling.getBehandlingResultatType());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    protected boolean skalBehandlingenSettesTilDelvisInnvilget(BehandlingReferanse ref, Vilkårene vilkårene) {
        return false;
    }

    protected boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        var behandlingId = ref.getBehandlingId();
        Optional<VilkårType> førsteAvslåttVilkår = sjekkAllePerioderAvslåttForVilkår(vilkårene, behandlingId);
        if (førsteAvslåttVilkår.isPresent()) {
            log.info("Avslått behandling {} fordi alle perioder med vilkår {} er avslått", behandlingId, førsteAvslåttVilkår.get());
            return true;
        }
        return skalAvslåsBasertPåAndreForhold(ref);
    }

    /**
     * @return første vilkår som alle perioder er avslått for.
     */
    private Optional<VilkårType> sjekkAllePerioderAvslåttForVilkår(Vilkårene vilkårene, Long behandlingId) {
        var maksPeriode = getMaksPeriode(behandlingId);

        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);

        return vilkårTidslinjer.entrySet().stream()
            .filter(e -> harAvslåtteVilkårsPerioder(e.getValue()) && harIngenOppfylteVilkårsPerioder(e.getValue()))
            .findFirst().map(Map.Entry::getKey);
    }

    protected boolean skalAvslåsBasertPåAndreForhold(@SuppressWarnings("unused") BehandlingReferanse ref) {
        return false;
    }

    protected abstract DatoIntervallEntitet getMaksPeriode(Long behandlingId);

    protected boolean harIngenOppfylteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return timeline.filterValue(vp -> vp.getAvslagsårsak() == null && vp.getGjeldendeUtfall() == Utfall.OPPFYLT).isEmpty();
    }

    protected boolean harAvslåtteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return !timeline.filterValue(vp -> vp.getAvslagsårsak() != null && vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT).isEmpty();
    }
}
