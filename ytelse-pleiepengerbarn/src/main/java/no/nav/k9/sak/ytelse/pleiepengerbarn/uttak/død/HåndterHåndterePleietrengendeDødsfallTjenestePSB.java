package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.MAKSÅR;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class HåndterHåndterePleietrengendeDødsfallTjenestePSB implements HåndterePleietrengendeDødsfallTjeneste {

    private VilkårForlengingTjeneste vilkårForlengingTjeneste = new VilkårForlengingTjeneste();
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private boolean utvidVedDødsfall;

    HåndterHåndterePleietrengendeDødsfallTjenestePSB() {
        // CDI
    }

    @Inject
    public HåndterHåndterePleietrengendeDødsfallTjenestePSB(VilkårResultatRepository vilkårResultatRepository,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                            PersonopplysningTjeneste personopplysningTjeneste,
                                                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                                            @KonfigVerdi(value = "PSB_PPN_UTVIDE_VED_DODSFALL", defaultVerdi = "true") boolean utvidVedDødsfall) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.utvidVedDødsfall = utvidVedDødsfall;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        if (!utvidVedDødsfall) {
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

        var utvidelseAvPeriode = utledUtvidelse(rettVedDød);
        var sisteDagPgaDødsfall = dødsdato.plus(utvidelseAvPeriode.getAntall(), utvidelseAvPeriode.getEnhet());

        if (!sisteDagPgaDødsfall.isAfter(dødsdato)){
            return Optional.empty();
        }
        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato.plusDays(1), sisteDagPgaDødsfall));
    }

    @Override
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

    private UtvidelseAvPeriode utledUtvidelse(RettVedDødType rettVedDød) {
        return switch (rettVedDød) {
            case RETT_6_UKER -> new UtvidelseAvPeriode(6, ChronoUnit.WEEKS);
            case RETT_12_UKER -> new UtvidelseAvPeriode(3, ChronoUnit.MONTHS);
        };
    }

    private void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {
        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        var perioderUnder18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
        var perioderOver18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);

        var dødsdato = periode.getFomDato().minusDays(1); //utvidelsesperioden begynner dagen etter dødsdato
        var eksisterendeResultat = finnSykdomVurderingPåDødsdato(dødsdato, perioderUnder18år, perioderOver18år, vilkårene);

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

    private VilkårPeriode finnSykdomVurderingPåDødsdato(LocalDate dødsdato, NavigableSet<DatoIntervallEntitet> perioderUnder18år, NavigableSet<DatoIntervallEntitet> perioderOver18år, Vilkårene vilkårene) {
        if (perioderUnder18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        } else if (perioderOver18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        }
        throw new IllegalStateException("Fant ikke overlapp verken i over eller under 18");
    }

    private void forlengOgVurderAldersvilkåret(VilkårResultatBuilder resultatBuilder, DatoIntervallEntitet periode, PersonopplysningEntitet brukerPersonopplysninger) {
        vilkårForlengingTjeneste.forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
    }

    private void forlengAndreVilkår(DatoIntervallEntitet periode, Vilkårene vilkårene, VilkårResultatBuilder resultatBuilder) {
        Set<VilkårType> vilkår = Set.of(VilkårType.OPPTJENINGSVILKÅRET, VilkårType.OMSORGEN_FOR, VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.MEDLEMSKAPSVILKÅRET, VilkårType.SØKNADSFRIST);
        vilkårForlengingTjeneste.forlengeVilkårMedPeriode(vilkår, resultatBuilder, vilkårene, periode);
    }

    private boolean harIkkeGodkjentSykdomPåDødsdatoen(LocalDate dødsdato, Vilkårene vilkårene) {
        for (VilkårType vilkårType : vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()) {
            Optional<VilkårPeriode> periode = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);
            if (periode.isPresent() && periode.get().getUtfall() == Utfall.IKKE_OPPFYLT) {
                return true;
            }
        }
        return false;
    }

}
