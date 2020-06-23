package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public abstract class ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ForeslåBehandlingsresultatTjeneste.class);

    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private VedtakVarselRepository vedtakVarselRepository;

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private BehandlingRepository behandlingRepository;

    protected ForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    public ForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                              VedtakVarselRepository vedtakVarselRepository,
                                              RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.revurderingBehandlingsresultatutleder = revurderingBehandlingsresultatutleder;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public void foreslåVedtakVarsel(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        Long behandlingId = ref.getBehandlingId();
        
        
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        // kun for å sette behandlingsresultattype
        var vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={} (før)", ref.getBehandlingId(), behandling.getBehandlingResultatType());

        VedtakVarsel oppdatertVarsel;
        if (skalBehandlingenSettesTilAvslått(ref, vilkårene)) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            oppdatertVarsel = foreslåVedtakVarselAvslått(ref, behandling, vedtakVarsel);
        } else {
            behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);  // kan bli overstyrt dersom dette er revurdering
            oppdatertVarsel = foreslåVedtakVarselInnvilget(ref, vedtakVarsel);
        }
        log.info("Foreslår Vedtak. Behandling {}. BehandlingResultatType={}", ref.getBehandlingId(), behandling.getBehandlingResultatType());
        vedtakVarselRepository.lagre(behandlingId, oppdatertVarsel);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private VedtakVarsel foreslåVedtakVarselInnvilget(BehandlingReferanse ref, VedtakVarsel vedtakVarsel) {
        if (ref.erRevurdering()) {
            return revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, vedtakVarsel, erVarselOmRevurderingSendt(ref));
        }
        log.info("Behandling {} innvilget", ref.getBehandlingId());
        return vedtakVarsel == null ? new VedtakVarsel() : vedtakVarsel;
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return vedtakVarselRepository.hentHvisEksisterer(ref.getId()).orElse(new VedtakVarsel()).getErVarselOmRevurderingSendt();
    }

    private boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        var behandlingId = ref.getBehandlingId();
        Optional<VilkårType> førsteAvslåttVilkår = sjekkAllePerioderAvslåttForVilkår(vilkårene, behandlingId);
        if (førsteAvslåttVilkår.isPresent()) {
            log.warn("Avslått behandling {} fordi alle perioder med vilkår {} er avslått", behandlingId, førsteAvslåttVilkår.get());
            return true;
        }
        return skalAvslåsBasertPåAndreForhold(ref);
    }

    /** @return første vilkår som alle perioder er avslått for. */
    private Optional<VilkårType> sjekkAllePerioderAvslåttForVilkår(Vilkårene vilkårene, Long behandlingId) {
        var maksPeriode = getMaksPeriode(behandlingId);

        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);

        return vilkårTidslinjer.entrySet().stream()
            .filter(e -> harAvslåtteVilkårsPerioder(e.getValue()) && harIngenOppfylteVilkårsPerioder(e.getValue()))
            .findFirst().map(e -> e.getKey());
    }

    protected boolean skalAvslåsBasertPåAndreForhold(@SuppressWarnings("unused") BehandlingReferanse ref) {
        return false;
    }

    protected abstract DatoIntervallEntitet getMaksPeriode(Long behandlingId);

    private boolean harIngenOppfylteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return timeline.filterValue(vp -> vp.getAvslagsårsak() == null && vp.getGjeldendeUtfall() == Utfall.OPPFYLT).isEmpty();
    }

    private boolean harAvslåtteVilkårsPerioder(LocalDateTimeline<VilkårPeriode> timeline) {
        return !timeline.filterValue(vp -> vp.getAvslagsårsak() != null && vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT).isEmpty();
    }

    private VedtakVarsel foreslåVedtakVarselAvslått(BehandlingReferanse ref, Behandling behandling, VedtakVarsel vedtakVarsel) {
        if (ref.erRevurdering()) {
            return revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, vedtakVarsel, erVarselOmRevurderingSendt(ref));
        } else {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);

            if (skalTilInfoTrygd(ref)) {
                vedtakVarsel.setVedtaksbrev(Vedtaksbrev.INGEN);
            }
            return vedtakVarsel;
        }
    }

    private boolean skalTilInfoTrygd(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
        return fagsak.getSkalTilInfotrygd();
    }

}
