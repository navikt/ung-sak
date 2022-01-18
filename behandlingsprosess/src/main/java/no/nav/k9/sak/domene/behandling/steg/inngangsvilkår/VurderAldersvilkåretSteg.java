package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.time.LocalDate;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_ALDER")
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class VurderAldersvilkåretSteg implements BehandlingSteg {

    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    VurderAldersvilkåretSteg() {
        // for proxy
    }

    @Inject
    public VurderAldersvilkåretSteg(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    BehandlingRepository behandlingRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);

        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.ALDERSVILKÅR);
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), behandling.getFagsak().getPeriode().getFomDato());
        var fødselsdato = personopplysningerAggregat.getSøker().getFødselsdato();
        var dødsdato = personopplysningerAggregat.getSøker().getDødsdato();
        vurderPerioder(vilkårBuilder, perioderTilVurdering, fødselsdato, dødsdato);
        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato, LocalDate dødsdato) {
        var maksdato = fødselsdato.plusYears(70);
        if (dødsdato != null && maksdato.isAfter(dødsdato)) {
            maksdato = dødsdato;
        }
        var regelInput = "{ 'fødselsdato': '" + fødselsdato + ", 'dødsdato': '" + dødsdato + "', ', 'maksdato': '" + maksdato + "' }";

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            vurderPeriode(vilkårBuilder, maksdato, dødsdato, regelInput, periode);
        }
    }

    private void vurderPeriode(VilkårBuilder vilkårBuilder, LocalDate maksdato, LocalDate dødsdato, String regelInput, DatoIntervallEntitet periode) {
        if (periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato)) && !periode.getFomDato().equals(maksdato)) {
            var builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), maksdato.minusDays(1)));
            builder.medUtfall(Utfall.OPPFYLT)
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

            builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, periode.getTomDato()));
            builder.medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(utledAvslagsårsak(maksdato, dødsdato))
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

        } else {
            var builder = vilkårBuilder.hentBuilderFor(periode);
            if (periode.getFomDato().isAfter(maksdato) || periode.getFomDato().isEqual(maksdato)) {
                builder.medUtfall(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(utledAvslagsårsak(maksdato, dødsdato))
                    .medRegelInput(regelInput);
            } else {
                builder.medUtfall(Utfall.OPPFYLT)
                    .medRegelInput(regelInput);
            }
            vilkårBuilder.leggTil(builder);
        }
    }

    private Avslagsårsak utledAvslagsårsak(LocalDate maksdato, LocalDate dødsdato) {
        if (dødsdato == null) {
            return Avslagsårsak.SØKER_OVER_HØYESTE_ALDER;
        }
        if (maksdato.isBefore(dødsdato)) {
            return Avslagsårsak.SØKER_OVER_HØYESTE_ALDER;
        }
        return Avslagsårsak.SØKER_HAR_AVGÅTT_MED_DØDEN;
    }


    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
