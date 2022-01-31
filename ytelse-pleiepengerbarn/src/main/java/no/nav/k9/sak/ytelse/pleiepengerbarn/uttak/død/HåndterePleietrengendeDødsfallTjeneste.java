package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.MAKSÅR;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.RettVedDødType;
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
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder.VurderAldersVilkårTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PSBVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;

@ApplicationScoped
public class HåndterePleietrengendeDødsfallTjeneste {

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();

    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PSBVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private Boolean utvidVedDødsfall;

    HåndterePleietrengendeDødsfallTjeneste() {
        // CDI
    }

    @Inject
    public HåndterePleietrengendeDødsfallTjeneste(RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                                  PersonopplysningTjeneste personopplysningTjeneste,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef PSBVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                  @KonfigVerdi(value = "PSB_UTVIDE_VED_DODSFALL", defaultVerdi = "false") Boolean utvidVedDødsfall) {

        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.utvidVedDødsfall = utvidVedDødsfall;
    }

    public void utvidPerioderVedDødsfall(BehandlingReferanse referanse) {
        if (!utvidVedDødsfall) {
            return;
        }
        if (!Objects.equals(FagsakYtelseType.PSB, referanse.getFagsakYtelseType())) {
            return;
        }

        var rettVedDødGrunnlagOpt = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (rettVedDødGrunnlagOpt.isEmpty()) {
            return;
        }

        var rettVedDød = rettVedDødGrunnlagOpt.orElseThrow().getRettVedPleietrengendeDød().getRettVedDødType();

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getPleietrengendeAktørId());
        var brukerPersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getAktørId());

        var dødsdato = pleietrengendePersonopplysninger.getDødsdato();
        if (dødsdato == null) {
            throw new IllegalStateException("Forventer en dødsdato når det er tatt stilling til rett etter død");
        }
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        if (harIkkeGodkjentSykdomPåDødsdatoen(dødsdato, vilkårene)) {
            return;
        }

        int antallUker = utledAntallUker(rettVedDød);
        var sisteDagPgaDødsfall = dødsdato.plusDays(1).plusWeeks(antallUker);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato, sisteDagPgaDødsfall);
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene).medKantIKantVurderer(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer());

        forlengMedisinskeVilkår(resultatBuilder, vilkårene, periode, pleietrengendePersonopplysninger.getFødselsdato());

        var vilkår = Set.of(VilkårType.OPPTJENINGSVILKÅRET, VilkårType.OMSORGEN_FOR, VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.MEDLEMSKAPSVILKÅRET, VilkårType.ALDERSVILKÅR, VilkårType.SØKNADSFRIST);
        forlengeVilkårMedPeriode(vilkår, resultatBuilder, vilkårene, periode);
        forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());
    }

    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        if (!utvidVedDødsfall) {
            return Optional.empty();
        }
        if (!Objects.equals(FagsakYtelseType.PSB, referanse.getFagsakYtelseType())) {
            return Optional.empty();
        }

        var rettVedDødGrunnlagOpt = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (rettVedDødGrunnlagOpt.isEmpty()) {
            return Optional.empty();
        }

        var rettVedDød = rettVedDødGrunnlagOpt.orElseThrow().getRettVedPleietrengendeDød().getRettVedDødType();

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

        int antallUker = utledAntallUker(rettVedDød);
        var sisteDagPgaDødsfall = dødsdato.plusDays(1).plusWeeks(antallUker);

        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato, sisteDagPgaDødsfall));
    }

    private void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {
        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        var perioderUnder18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
        var perioderOver18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);

        // finn eksisterende resultat på dødsdato
        var eksisterendeResultat = utledEksisterendeResultatSykdom(periode.getFomDato(), perioderUnder18år, perioderOver18år, vilkårene);

        if (!perioderUnder18år.isEmpty()) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);

            leggTilPerioder(resultatBuilder, perioderUnder18år, eksisterendeResultat, vilkårBuilder);
        }
        if (!perioderOver18år.isEmpty()) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);

            leggTilPerioder(resultatBuilder, perioderOver18år, eksisterendeResultat, vilkårBuilder);
        }
    }

    private void leggTilPerioder(VilkårResultatBuilder resultatBuilder, NavigableSet<DatoIntervallEntitet> perioder, VilkårPeriode eksisterendeResultat, VilkårBuilder vilkårBuilder) {
        for (DatoIntervallEntitet intervall : perioder) {
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(intervall).forlengelseAv(eksisterendeResultat));
        }

        resultatBuilder.leggTil(vilkårBuilder);
    }

    private VilkårPeriode utledEksisterendeResultatSykdom(LocalDate dødsdato, NavigableSet<DatoIntervallEntitet> perioderUnder18år, NavigableSet<DatoIntervallEntitet> perioderOver18år, Vilkårene vilkårene) {
        if (perioderUnder18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        } else if (perioderOver18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        }
        throw new IllegalStateException("Fant ikke overlapp verken i over eller under 18");
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

    private boolean harIkkeGodkjentSykdomPåDødsdatoen(LocalDate dødsdato, Vilkårene vilkårene) {
        var under18Periode = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);

        if (under18Periode.isPresent()) {
            return Objects.equals(under18Periode.get().getUtfall(), Utfall.IKKE_OPPFYLT);
        }
        var over18Periode = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);

        return over18Periode.map(it -> Objects.equals(it.getUtfall(), Utfall.IKKE_OPPFYLT)).orElse(false);
    }

    private int utledAntallUker(RettVedDødType rettVedDød) {
        return switch (rettVedDød) {
            case RETT_6_UKER -> 6;
            case RETT_12_UKER -> 12;
        };
    }
}
