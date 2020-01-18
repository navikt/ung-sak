package no.nav.foreldrepenger.inngangsvilkaar;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef.VilkårTypeRefLiteral;
import no.nav.vedtak.konfig.Tid;

/**
 * Denne angir implementasjon som skal brukes for en gitt {@link VilkårType} slik at {@link Vilkår} og
 * {@link VilkårResultat} kan fastsettes.
 */
@ApplicationScoped
public class InngangsvilkårTjeneste {

    private Instance<Inngangsvilkår> alleInngangsvilkår;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    InngangsvilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårTjeneste(@Any Instance<Inngangsvilkår> alleInngangsvilkår, BehandlingRepositoryProvider repositoryProvider) {
        this.alleInngangsvilkår = alleInngangsvilkår;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
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

        final var behandlingsresultat = getBehandlingsresultat(behandlingId);
        VilkårResultat vilkårResultat = behandlingsresultat.getVilkårResultat();
        VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        final var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        builder.leggTil(vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE) // FIXME k9 : Benytte virkelige datoer
                .medUtfallOverstyrt(utfall)
                .medAvslagsårsak(Utfall.IKKE_OPPFYLT.equals(utfall) ? avslagsårsak : null)
            )
        );
        final var oppdatertVikårResultat = builder.build();
        behandlingsresultat.medOppdatertVilkårResultat(oppdatertVikårResultat);
        behandlingRepository.lagre(oppdatertVikårResultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    /**
     * Overstyr gitt aksjonspunkt på Inngangsvilkår.
     */
    public void overstyrAksjonspunkt(Long behandlingId, VilkårType vilkårType, Utfall utfall, String avslagsårsakKode,
                                     BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        final var behandlingsresultat = getBehandlingsresultat(behandlingId);
        VilkårResultat vilkårResultat = behandlingsresultat.getVilkårResultat();
        VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        Avslagsårsak avslagsårsak = finnAvslagsårsak(avslagsårsakKode, utfall);
        final var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        builder.leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .medUtfallOverstyrt(utfall)
            .medAvslagsårsak(avslagsårsak)));

        VilkårResultat resultat = builder.build();
        behandlingsresultat.medOppdatertVilkårResultat(resultat);
        behandlingRepository.lagre(resultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    public Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
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
