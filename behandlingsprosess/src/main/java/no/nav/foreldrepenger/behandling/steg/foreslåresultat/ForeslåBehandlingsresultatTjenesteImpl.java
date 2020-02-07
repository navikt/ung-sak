package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;

@ApplicationScoped
@FagsakYtelseTypeRef
class ForeslåBehandlingsresultatTjenesteImpl implements no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste {

    private UttakRepository uttakRepository;

    private AvslagsårsakTjeneste avslagsårsakTjeneste;

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    ForeslåBehandlingsresultatTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåBehandlingsresultatTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                           AvslagsårsakTjeneste avslagsårsakTjeneste,
                                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                           @FagsakYtelseTypeRef RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutlederFelles) {
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.avslagsårsakTjeneste = avslagsårsakTjeneste;
        this.revurderingBehandlingsresultatutlederFelles = revurderingBehandlingsresultatutlederFelles;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }


    protected boolean minstEnGyldigUttaksPeriode(Behandlingsresultat behandlingsresultat) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        return uttakResultat.isPresent() && uttakResultat.get().getGjeldendePerioder().getPerioder().stream().anyMatch(UttakResultatPeriodeEntitet::isInnvilget);
    }


    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (behandlingsresultat.isPresent()) {
            final var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
            if (sjekkVilkårAvslått(vilkårene)) {
                vilkårAvslått(ref, behandlingsresultat.get(), vilkårene);
            } else {
                Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get()).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
                // Må nullstille avslagårsak (for symmetri med setting avslagsårsak ovenfor, hvor avslagårsak kopieres fra et vilkår)
                Optional.ofNullable(behandlingsresultat.get().getAvslagsårsak()).ifPresent(ufjernetÅrsak -> behandlingsresultat.get().setAvslagsårsak(Avslagsårsak.UDEFINERT));
                if (ref.erRevurdering()) {
                    boolean erVarselOmRevurderingSendt = erVarselOmRevurderingSendt(ref);
                    return revurderingBehandlingsresultatutlederFelles.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
                }
            }
        }
        return behandlingsresultat.orElse(null);
    }

    private boolean sjekkVilkårAvslått(Vilkårene vilkårene) {
        return vilkårene.getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .anyMatch(vp -> Utfall.IKKE_OPPFYLT.equals(vp.getGjeldendeUtfall()));
    }


    private void vilkårAvslått(BehandlingReferanse ref, Behandlingsresultat behandlingsresultat, Vilkårene vilkårene) {
        Optional<Vilkår> ikkeOppfyltVilkår = finnAvslåtteVilkår(vilkårene);
        ikkeOppfyltVilkår.ifPresent(vilkår -> {
            Avslagsårsak avslagsårsak = avslagsårsakTjeneste.finnAvslagsårsak(vilkår);
            behandlingsresultat.setAvslagsårsak(avslagsårsak);
        });
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
    }

    private Optional<Vilkår> finnAvslåtteVilkår(Vilkårene vilkårene) {
        return vilkårene.getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getPerioder().stream().anyMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall())))
            .findFirst();
    }

    private boolean skalTilInfoTrygd(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
        return fagsak.getSkalTilInfotrygd();
    }

    private boolean erVarselOmRevurderingSendt(BehandlingReferanse ref) {
        return dokumentBehandlingTjeneste.erDokumentProdusert(ref.getBehandlingId(), DokumentMalType.REVURDERING_DOK);
    }
}
