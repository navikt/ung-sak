package no.nav.foreldrepenger.inngangsvilkaar;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef.VilkårTypeRefLiteral;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.konfig.Tid;

/**
 * Denne angir implementasjon som skal brukes for en gitt {@link VilkårType} slik at {@link Vilkår} og
 * {@link Vilkårene} kan fastsettes.
 */
@ApplicationScoped
public class InngangsvilkårTjeneste {

    private Instance<Inngangsvilkår> alleInngangsvilkår;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    InngangsvilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårTjeneste(@Any Instance<Inngangsvilkår> alleInngangsvilkår, BehandlingRepositoryProvider repositoryProvider) {
        this.alleInngangsvilkår = alleInngangsvilkår;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    /**
     * Finn {@link Inngangsvilkår} for angitt {@link VilkårType}. Husk at denne må closes når du er ferdig med den.
     */
    public Inngangsvilkår finnVilkår(VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        Instance<Inngangsvilkår> selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType.getKode()));
        if (selected.isAmbiguous()) {
            return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Har ikke Inngangsvilkår for " + fagsakYtelseType));
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }

        Inngangsvilkår minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    /**
     * Vurder om et angitt {@link VilkårType} er et {@link Inngangsvilkår}
     *
     * @param vilkårType en {@link VilkårType}
     * @return true hvis {@code vilkårType} er et {@link Inngangsvilkår}
     */
    public boolean erInngangsvilkår(VilkårType vilkårType) {
        Instance<Inngangsvilkår> selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType.getKode()));
        return !selected.isUnsatisfied();
    }

    /**
     * Overstyr søkers opplysningsplikt.
     */
    public void overstyrAksjonspunktForSøkersopplysningsplikt(Long behandlingId, Utfall utfall, BehandlingskontrollKontekst kontekst) {
        Avslagsårsak avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON;
        VilkårType vilkårType = VilkårType.SØKERSOPPLYSNINGSPLIKT;

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingId);
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        final var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        builder.leggTil(vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE) // FIXME k9 : Benytte virkelige datoer
                .medUtfallOverstyrt(utfall)
                .medAvslagsårsak(Utfall.IKKE_OPPFYLT.equals(utfall) ? avslagsårsak : null)
            )
        );
        final var oppdatertVikårResultat = builder.build();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        vilkårResultatRepository.lagre(behandlingId, oppdatertVikårResultat);
    }

    /**
     * Overstyr gitt aksjonspunkt på Inngangsvilkår.
     */
    public void overstyrAksjonspunkt(Long behandlingId, VilkårType vilkårType, Utfall utfall, String avslagsårsakKode,
                                     BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingId);
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        Avslagsårsak avslagsårsak = finnAvslagsårsak(avslagsårsakKode, utfall);
        final var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        builder.leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .medUtfallOverstyrt(utfall)
            .medAvslagsårsak(avslagsårsak)));

        Vilkårene oppdatertVikårResultat = builder.build();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        vilkårResultatRepository.lagre(behandlingId, oppdatertVikårResultat);
    }

    private Avslagsårsak finnAvslagsårsak(String avslagsÅrsakKode, Utfall utfall) {
        Avslagsårsak avslagsårsak;
        if (avslagsÅrsakKode == null || utfall.equals(Utfall.OPPFYLT)) {
            avslagsårsak = Avslagsårsak.UDEFINERT;
        } else {
            avslagsårsak = Avslagsårsak.fraKode(avslagsÅrsakKode);
        }
        return avslagsårsak;
    }
}
