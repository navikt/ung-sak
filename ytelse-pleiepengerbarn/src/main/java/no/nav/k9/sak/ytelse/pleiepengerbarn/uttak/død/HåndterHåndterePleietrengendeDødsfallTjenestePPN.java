package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class HåndterHåndterePleietrengendeDødsfallTjenestePPN implements HåndterePleietrengendeDødsfallTjeneste {
    private final VilkårForlengingTjeneste vilkårForlengingTjeneste = new VilkårForlengingTjeneste();
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;

    HåndterHåndterePleietrengendeDødsfallTjenestePPN() {
        // CDI
    }

    @Inject
    public HåndterHåndterePleietrengendeDødsfallTjenestePPN(VilkårResultatRepository vilkårResultatRepository,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                            PersonopplysningTjeneste personopplysningTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }


    @Override
    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(referanse, referanse.getFagsakPeriode().getFomDato());
        if (personopplysningerAggregat.isEmpty()) {
            //kortslutter når personopplysninger ikke er hentet enda
            return Optional.empty();
        }
        var pleietrengendePersonopplysninger = personopplysningerAggregat.get().getPersonopplysning(referanse.getPleietrengendeAktørId());

        var dødsdato = pleietrengendePersonopplysninger.getDødsdato();
        if (dødsdato == null) {
            return Optional.empty();
        }
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (vilkårene.isEmpty()){
            return Optional.empty();
        }

        if (!harGodkjentSykdomPåDødsdatoen(dødsdato, vilkårene.get())) {
            return Optional.empty();
        }
        LocalDate sisteDato = vilkårResultatRepository.hent(referanse.getBehandlingId()).getAlleIntervaller().getMaxLocalDate();
        if (!sisteDato.isAfter(dødsdato)) {
            return Optional.empty();
        }

        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato.plusDays(1), sisteDato));
    }

    @Override
    public void utvidPerioderVedDødsfall(BehandlingReferanse referanse) {
        Optional<DatoIntervallEntitet> utvidelsesperiode = utledUtvidetPeriodeForDødsfall(referanse);
        if (utvidelsesperiode.isEmpty()) {
            return;
        }
        var periode = utvidelsesperiode.get();

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var brukerPersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getAktørId());

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene).medKantIKantVurderer(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer());

        forlengMedisinskeVilkår(resultatBuilder, vilkårene, periode);
        forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());
    }

    private void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode) {
        var dødsdato = periode.getFomDato().minusDays(1); //utvidelsesperioden begynner dagen etter dødsdato
        var eksisterendeResultat = finnSykdomsvurderingPåDødsdato(dødsdato, vilkårene);

        VilkårBuilder vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.I_LIVETS_SLUTTFASE);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisterendeResultat));
        resultatBuilder.leggTil(vilkårBuilder);
    }

    private VilkårPeriode finnSykdomsvurderingPåDødsdato(LocalDate dødsdato, Vilkårene vilkårene) {
        return vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
    }

    private void forlengOgVurderAldersvilkåret(VilkårResultatBuilder resultatBuilder, DatoIntervallEntitet periode, PersonopplysningEntitet brukerPersonopplysninger) {
        vilkårForlengingTjeneste.forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
    }

    private boolean harGodkjentSykdomPåDødsdatoen(LocalDate dødsdato, Vilkårene vilkårene) {
        Optional<VilkårPeriode> periode = vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE).flatMap(it -> it.finnPeriodeSomInneholderDato(dødsdato));
        return periode.isPresent() && periode.get().getUtfall() == Utfall.OPPFYLT;
    }

}
