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
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class HåndterHåndterePleietrengendeDødsfallTjenestePSB implements HåndterePleietrengendeDødsfallTjeneste {

    private final VilkårForlengingTjeneste vilkårForlengingTjeneste = new VilkårForlengingTjeneste();
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private PleiebehovResultatRepository resultatRepository;

    HåndterHåndterePleietrengendeDødsfallTjenestePSB() {
        // CDI
    }

    @Inject
    public HåndterHåndterePleietrengendeDødsfallTjenestePSB(VilkårResultatRepository vilkårResultatRepository,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                            PersonopplysningTjeneste personopplysningTjeneste,
                                                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                                            PleiebehovResultatRepository resultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.resultatRepository = resultatRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    private static DatoIntervallEntitet utledPeriode(DatoIntervallEntitet periode, TreeSet<DatoIntervallEntitet> last) {
        if (!last.isEmpty() && last.last().getTomDato().isAfter(periode.getTomDato())) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), last.last().getTomDato());
        }
        return periode;
    }

    @Override
    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
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

        if (!harGodkjentSykdomPåDødsdatoen(dødsdato, vilkårene)) {
            return Optional.empty();
        }

        var utvidelseAvPeriode = utledUtvidelse(rettVedDød);
        var sisteDagPgaDødsfall = dødsdato.plus(utvidelseAvPeriode.getAntall(), utvidelseAvPeriode.getEnhet());

        if (!sisteDagPgaDødsfall.isAfter(dødsdato)) {
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
        var perioder = utledPerioder(referanse);
        var perioderSomMåforlenges = TidslinjeUtil.tilTidslinjeKomprimert(perioder)
            .compress()
            .intersection(periode.toLocalDateInterval())
            .stream().map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval())).collect(Collectors.toCollection(TreeSet::new));
        if (perioderSomMåforlenges.size() > 1) {
            throw new IllegalStateException("Fant flere perioder som må forlenges.");
        }
        periode = utledPeriode(periode, perioderSomMåforlenges);

        forlengMedisinskeVilkår(resultatBuilder, vilkårene, periode, pleietrengendePersonopplysninger.getFødselsdato());
        forlengOgVurderAldersvilkåret(resultatBuilder, periode, brukerPersonopplysninger);
        forlengAndreVilkår(periode, vilkårene, resultatBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());

        final var nåværendeResultat = resultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(EtablertPleiebehovBuilder::builder).orElse(EtablertPleiebehovBuilder.builder());
        builder.tilbakeStill(periode);
        resultatRepository.lagreOgFlush(referanse.getBehandlingId(), builder);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse referanse) {
        var perioderUnder = vilkårsPerioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var perioderOver = vilkårsPerioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        var perioder = new TreeSet<>(perioderUnder);
        perioder.addAll(perioderOver);
        return perioder;
    }

    private UtvidelseAvPeriode utledUtvidelse(RettVedDødType rettVedDød) {
        return switch (rettVedDød) {
            case RETT_6_UKER -> new UtvidelseAvPeriode(6, ChronoUnit.WEEKS);
            case RETT_12_UKER -> new UtvidelseAvPeriode(3, ChronoUnit.MONTHS);
        };
    }

    private void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {
        var dødsdato = periode.getFomDato().minusDays(1); //utvidelsesperioden begynner dagen etter dødsdato
        var eksisterendeResultat = finnSykdomVurderingPåDødsdato(dødsdato, vilkårene);
        var set = new TreeSet<>(Set.of(periode));

        var perioderUnder18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
        var perioderOver18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);

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

    private VilkårPeriode finnSykdomVurderingPåDødsdato(LocalDate dødsdato, Vilkårene vilkårene) {
        for (VilkårType vilkårType : Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR)) {
            Optional<VilkårPeriode> vilkårPeriodeForDødsdato = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);
            if (vilkårPeriodeForDødsdato.isPresent()) {
                return vilkårPeriodeForDødsdato.get();
            }
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

    private boolean harGodkjentSykdomPåDødsdatoen(LocalDate dødsdato, Vilkårene vilkårene) {
        for (VilkårType vilkårType : Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR)) {
            Optional<VilkårPeriode> periode = vilkårene.getVilkår(vilkårType).orElseThrow().finnPeriodeSomInneholderDato(dødsdato);
            if (periode.isPresent() && periode.get().getUtfall() == Utfall.OPPFYLT) {
                return true;
            }
        }
        return false;
    }

}
