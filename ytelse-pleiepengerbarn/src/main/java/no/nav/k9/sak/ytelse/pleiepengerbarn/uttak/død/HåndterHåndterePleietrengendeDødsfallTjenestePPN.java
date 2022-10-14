package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class HåndterHåndterePleietrengendeDødsfallTjenestePPN extends HåndterePleietrengendeDødsfallTjeneste {

    private boolean utvidVedDødsfall;

    HåndterHåndterePleietrengendeDødsfallTjenestePPN() {
        // CDI
    }

    @Inject
    public HåndterHåndterePleietrengendeDødsfallTjenestePPN(BehandlingRepository behandlingRepository,
                                                            VilkårResultatRepository vilkårResultatRepository,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) @BehandlingTypeRef VilkårUtleder vilkårUtleder,
                                                            PersonopplysningTjeneste personopplysningTjeneste,
                                                            @KonfigVerdi(value = "PSB_PPN_UTVIDE_VED_DODSFALL", defaultVerdi = "true") boolean utvidVedDødsfall) {
        super(behandlingRepository, vilkårResultatRepository, vilkårUtleder, vilkårsPerioderTilVurderingTjeneste, personopplysningTjeneste);
        this.utvidVedDødsfall = utvidVedDødsfall;
    }


    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        if (!utvidVedDødsfall) {
            return Optional.empty();
        }

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getPleietrengendeAktørId());

        var dødsdato = pleietrengendePersonopplysninger.getDødsdato();
        if (dødsdato == null) {
            return Optional.empty();
        }
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        if (harIkkeGodkjentSykdomPåDødsdatoen(dødsdato, vilkårene)) {
            return Optional.empty();
        }

        LocalDate sisteDato = vilkårResultatRepository.hent(referanse.getBehandlingId()).getAlleIntervaller().getMaxLocalDate();
        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato, sisteDato));
    }

    protected void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {
        var eksisterendeResultat = finnSykdomsvurderingPåDødsdato(periode.getFomDato(), vilkårene);

        VilkårBuilder vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.I_LIVETS_SLUTTFASE);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisterendeResultat));
        resultatBuilder.leggTil(vilkårBuilder);
    }

    private VilkårPeriode finnSykdomsvurderingPåDødsdato(LocalDate dødsdato, Vilkårene vilkårene) {
        return vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();

    }

}
