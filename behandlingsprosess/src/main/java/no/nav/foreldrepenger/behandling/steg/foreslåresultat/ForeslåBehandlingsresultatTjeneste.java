package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Utfall;

@ApplicationScoped
@FagsakYtelseTypeRef
class ForeslåBehandlingsresultatTjeneste {

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder;
    private VedtakVarselRepository vedtakVarselRepository;

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private FordelingRepository fordelingRepository;

    private BehandlingRepository behandlingRepository;

    ForeslåBehandlingsresultatTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                              VedtakVarselRepository vedtakVarselRepository,
                                              FordelingRepository fordelingRepository,
                                              @FagsakYtelseTypeRef RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder) {
        this.fordelingRepository = fordelingRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.revurderingBehandlingsresultatutleder = revurderingBehandlingsresultatutleder;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public void foreslåVedtakVarsel(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        Long behandlingId = ref.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        // kun for å sette behandlingsresulattype
        var vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        
        VedtakVarsel oppdatertVarsel;
        if (sjekkVilkårAvslått(behandlingId, vilkårene)) {
            oppdatertVarsel = foreslåVedtakVarselAvslått(ref, behandling, vedtakVarsel);
        } else {
            behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
            oppdatertVarsel = foreslåVedtakVarselInnvilget(ref, vedtakVarsel);
        }
        vedtakVarselRepository.lagre(behandlingId, oppdatertVarsel);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private VedtakVarsel foreslåVedtakVarselInnvilget(BehandlingReferanse ref, VedtakVarsel vedtakVarsel) {
        if (ref.erRevurdering()) {
            return revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt(ref));
        }
        return vedtakVarsel == null ? new VedtakVarsel() : vedtakVarsel;
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return vedtakVarselRepository.hentHvisEksisterer(ref.getId()).orElse(new VedtakVarsel()).getErVarselOmRevurderingSendt();
    }

    private boolean sjekkVilkårAvslått(Long behandlingId, Vilkårene vilkårene) {
        var f = fordelingRepository.hent(behandlingId);
        var maksPeriode = f.getMaksPeriode();

        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);

        return vilkårTidslinjer.values().stream()
            .anyMatch(timeline -> {
                return !avslåttVilkårPeriode(timeline).isEmpty() && oppfylteVilkårPeriode(timeline).isEmpty();
            });
    }

    private LocalDateTimeline<VilkårPeriode> oppfylteVilkårPeriode(LocalDateTimeline<VilkårPeriode> timeline) {
        return timeline.filterValue(vp -> vp.getAvslagsårsak() == null && vp.getGjeldendeUtfall() == Utfall.OPPFYLT);
    }

    private LocalDateTimeline<VilkårPeriode> avslåttVilkårPeriode(LocalDateTimeline<VilkårPeriode> timeline) {
        return timeline.filterValue(vp -> vp.getAvslagsårsak() != null && vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT);
    }

    private VedtakVarsel foreslåVedtakVarselAvslått(BehandlingReferanse ref, Behandling behandling, VedtakVarsel vedtakVarsel) {
        if (ref.erRevurdering()) {
            return revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt(ref));
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
