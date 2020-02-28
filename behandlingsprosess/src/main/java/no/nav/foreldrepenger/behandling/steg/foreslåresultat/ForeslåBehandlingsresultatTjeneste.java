package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Utfall;

@ApplicationScoped
@FagsakYtelseTypeRef
class ForeslåBehandlingsresultatTjeneste {

    private UttakRepository uttakRepository;

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private FordelingRepository fordelingRepository;

    ForeslåBehandlingsresultatTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                       DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                       FordelingRepository fordelingRepository,
                                       @FagsakYtelseTypeRef RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles) {
        this.fordelingRepository = fordelingRepository;
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.revurderingBehandlingsresultatutlederFelles = revurderingBehandlingsresultatutlederFelles;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    protected boolean minstEnGyldigUttaksPeriode(Behandlingsresultat behandlingsresultat) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        return uttakResultat.isPresent()
            && uttakResultat.get().getGjeldendePerioder().getPerioder().stream().anyMatch(UttakResultatPeriodeEntitet::isInnvilget);
    }

    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultat.isPresent()) {
            var vilkårene = vilkårResultatRepository.hent(behandlingId);
            if (sjekkVilkårAvslått(behandlingId, vilkårene)) {
                return foreslåBehandlingresultatAvslått(ref, behandlingsresultat.get());
            } else {
                return foreslåBehandlingsresultatInnvilget(ref, behandlingsresultat);
            }
        } else {
            return null;
        }
    }

    private Behandlingsresultat foreslåBehandlingsresultatInnvilget(BehandlingReferanse ref, Optional<Behandlingsresultat> behandlingsresultat) {
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get()).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        if (ref.erRevurdering()) {
            boolean erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
            return revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
        }
        return behandlingsresultat.orElse(null);
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

    private Behandlingsresultat foreslåBehandlingresultatAvslått(BehandlingReferanse ref, Behandlingsresultat behandlingsresultat) {
        if (ref.erRevurdering()) {
            boolean erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
            revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
        } else {
            Behandlingsresultat.Builder resultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            if (skalTilInfoTrygd(ref)) {
                resultatBuilder.medVedtaksbrev(Vedtaksbrev.INGEN);
            }
        }
        return behandlingsresultat;
    }

    private boolean skalTilInfoTrygd(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
        return fagsak.getSkalTilInfotrygd();
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return dokumentBehandlingTjeneste.erDokumentProdusert(ref.getBehandlingId(), DokumentMalType.REVURDERING_DOK);
    }
}
