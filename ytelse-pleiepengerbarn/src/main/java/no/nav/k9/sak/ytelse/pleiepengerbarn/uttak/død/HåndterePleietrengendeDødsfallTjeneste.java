package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder.VurderAldersVilkårTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

public abstract class HåndterePleietrengendeDødsfallTjeneste {

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();

    protected VilkårResultatRepository vilkårResultatRepository;
    protected VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    protected PersonopplysningTjeneste personopplysningTjeneste;

    public static HåndterePleietrengendeDødsfallTjeneste velgTjeneste(Instance<HåndterePleietrengendeDødsfallTjeneste> instanser, BehandlingReferanse referanse) {
        return FagsakYtelseTypeRef.Lookup.find(instanser, referanse.getFagsakYtelseType()).orElseThrow(() -> new IllegalStateException("Fant ikke " + HåndterePleietrengendeDødsfallTjeneste.class.getName() + " for " + referanse.getFagsakYtelseType()));
    }

    HåndterePleietrengendeDødsfallTjeneste() {
        // CDI
    }

    public HåndterePleietrengendeDødsfallTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                                  VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                  PersonopplysningTjeneste personopplysningTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public abstract Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse);

    protected abstract void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato);

    abstract protected Set<VilkårType> vilkårTyperSomForlengesUtoverAldersvilkårOgMedisinskVilkår();

    public void utvidPerioderVedDødsfall(BehandlingReferanse referanse) {
        Optional<DatoIntervallEntitet> utvidelsesperiode = utledUtvidetPeriodeForDødsfall(referanse);
        if (utvidelsesperiode.isEmpty()) {
            return;
        }
        var periode = utvidelsesperiode.get();

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getPleietrengendeAktørId());
        var brukerPersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getAktørId());

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene).medKantIKantVurderer(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer());

        forlengMedisinskeVilkår(resultatBuilder, vilkårene, periode, pleietrengendePersonopplysninger.getFødselsdato());
        forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
        forlengAndreVilkår(periode, vilkårene, resultatBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());
    }

    private void forlengAndreVilkår(DatoIntervallEntitet periode, Vilkårene vilkårene, VilkårResultatBuilder resultatBuilder) {
        Set<VilkårType> vilkår = vilkårTyperSomForlengesUtoverAldersvilkårOgMedisinskVilkår();
        forlengeVilkårMedPeriode(vilkår, resultatBuilder, vilkårene, periode);
    }

    private void forlengOgVurderAldersvilkåret(VilkårResultatBuilder resultatBuilder, DatoIntervallEntitet periode, PersonopplysningEntitet brukerPersonopplysninger) {
        var aldersvilkår = VilkårType.ALDERSVILKÅR;
        var vilkårBuilder = resultatBuilder.hentBuilderFor(aldersvilkår);
        var fødselsdato = brukerPersonopplysninger.getFødselsdato();
        var dødsdato = brukerPersonopplysninger.getDødsdato();

        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        vurderAldersVilkårTjeneste.vurderPerioder(vilkårBuilder, set, fødselsdato, dødsdato);
    }

    private void forlengeVilkårMedPeriode(Set<VilkårType> vilkår, VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode) {
        for (VilkårType vilkårType : vilkår) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            var eksisterendeResultat = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(periode.getFomDato()).orElseThrow();

            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisterendeResultat));
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    protected boolean harIkkeGodkjentSykdomPåDødsdatoen(LocalDate dødsdato, Vilkårene vilkårene) {
        for (VilkårType vilkårType : vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()) {
            Optional<VilkårPeriode> periode = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);
            if (periode.isPresent() && periode.get().getUtfall() == Utfall.IKKE_OPPFYLT) {
                return true;
            }
        }
        return false;
    }

}
