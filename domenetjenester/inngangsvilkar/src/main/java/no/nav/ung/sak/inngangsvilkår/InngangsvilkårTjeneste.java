package no.nav.ung.sak.inngangsvilkår;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.VilkårTypeRef.VilkårTypeRefLiteral;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;

/**
 * Denne angir implementasjon som skal brukes for en gitt {@link VilkårType} slik at {@link Vilkår} og
 * {@link Vilkårene} kan fastsettes.
 */
@ApplicationScoped
public class InngangsvilkårTjeneste {

    private static final Logger log = LoggerFactory.getLogger(InngangsvilkårTjeneste.class);

    private Instance<Inngangsvilkår> alleInngangsvilkår;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    InngangsvilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårTjeneste(@Any Instance<Inngangsvilkår> alleInngangsvilkår, BehandlingRepositoryProvider repositoryProvider) {
        this.alleInngangsvilkår = alleInngangsvilkår;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    /**
     * Finn {@link Inngangsvilkår} for angitt {@link VilkårType}. Husk at denne må closes når du er ferdig med den.
     */
    public Inngangsvilkår finnVilkår(VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        Instance<Inngangsvilkår> selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType));
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
     * Overstyr gitt aksjonspunkt på Inngangsvilkår.
     */
    public void overstyrAksjonspunkt(Long behandlingId, VilkårType vilkårType, Utfall utfall, String avslagsårsakKode,
                                     BehandlingskontrollKontekst kontekst, LocalDate fom, LocalDate tom, String begrunnelse,
                                     String innvilgelseMerknadKode) {
        log.info("Overstyrer {} periode :[{}-{}] -> '{}' [{}]", vilkårType, fom, tom, utfall, avslagsårsakKode);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingId);
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        Avslagsårsak avslagsårsak = finnAvslagsårsak(avslagsårsakKode, utfall);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        builder.leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfallOverstyrt(utfall)
            .medAvslagsårsak(Utfall.IKKE_OPPFYLT.equals(utfall) ? avslagsårsak : null)
            .medMerknad(Utfall.OPPFYLT.equals(utfall) ? VilkårUtfallMerknad.fraKode(innvilgelseMerknadKode) : null)
            .medBegrunnelse(begrunnelse)));

        var oppdatertVikårResultat = builder.build();
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
