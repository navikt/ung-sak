package no.nav.k9.sak.inngangsvilkår.omsorg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForGrunnlagMapper;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForKnekkpunkter;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkår;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class OmsorgenForTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private Instance<OmsorgenForVilkår> omsorgenForVilkårene;
    private Instance<OmsorgenForGrunnlagMapper> omsorgenForGrunnlagMappere;

    OmsorgenForTjeneste() {
        // CDI
    }

    @Inject
    public OmsorgenForTjeneste(OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                               BehandlingRepository behandlingRepository,
                               BasisPersonopplysningTjeneste personopplysningTjeneste,
                               @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                               @Any Instance<OmsorgenForVilkår> omsorgenForVilkårene,
                               @Any Instance<OmsorgenForGrunnlagMapper> omsorgenForGrunnlagMappere) {
        this.omsorgenForVilkårene = omsorgenForVilkårene;
        this.omsorgenForGrunnlagMappere = omsorgenForGrunnlagMappere;
        this.utfallOversetter = new VilkårUtfallOversetter();
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    public List<VilkårData> vurderPerioder(BehandlingReferanse referanse, LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje) {
        final List<VilkårData> resultat = new ArrayList<>();
        var omsorgenForVilkår = OmsorgenForVilkår.finnTjeneste(omsorgenForVilkårene, referanse.getFagsakYtelseType());
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            var periode = DatoIntervallEntitet.fra(s.getLocalDateInterval());
            var knekkpunkter = new OmsorgenForKnekkpunkter(periode);
            final var evaluation = omsorgenForVilkår.evaluer(s.getValue(), knekkpunkter);
            final var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), periode);
            resultat.add(vilkårData);
        }

        return resultat;
    }

    public LocalDateTimeline<OmsorgenForVilkårGrunnlag> mapGrunnlag(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioder) {
        var behandlingId = referanse.getBehandlingId();
        var grunnlagMapper = OmsorgenForGrunnlagMapper.finnTjeneste(omsorgenForGrunnlagMappere, referanse.getFagsakYtelseType());
        var grunnlagPerPeriode = grunnlagMapper.map(referanse, perioder);
        var systemdatatidslinje = new LocalDateTimeline<>(grunnlagPerPeriode.entrySet()
            .stream().map(entry -> new LocalDateSegment<>(entry.getKey().toLocalDateInterval(), entry.getValue()))
            .collect(Collectors.toList()));
        final var vurdertOmsorgenForTidslinje = oversettTilRegelModellOmsorgenForVurderinger(behandlingId);

        return slåSammenGrunnlagFraSystemOgVurdering(vurdertOmsorgenForTidslinje, systemdatatidslinje);
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> slåSammenGrunnlagFraSystemOgVurdering(final LocalDateTimeline<OmsorgenForVilkårGrunnlag> vurdertOmsorgenForTidslinje, LocalDateTimeline<OmsorgenForVilkårGrunnlag> systemdatatidslinje) {
        return vurdertOmsorgenForTidslinje.combine(systemdatatidslinje, (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null) {
                return new LocalDateSegment<>(datoInterval, datoSegment2.getValue());
            }

            final OmsorgenForVilkårGrunnlag sg = datoSegment2.getValue();
            final OmsorgenForVilkårGrunnlag sammensatt = new OmsorgenForVilkårGrunnlag(DatoIntervallEntitet.fra(datoInterval), sg.getRelasjonMellomSøkerOgPleietrengende(), sg.getSøkersAdresser(),
                sg.getPleietrengendeAdresser(), datoSegment.getValue().getHarBlittVurdertSomOmsorgsPerson(), sg.getFosterbarn(), sg.getDeltBostedsAdresser());
            return new LocalDateSegment<>(datoInterval, sammensatt);
        }, JoinStyle.RIGHT_JOIN);
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> oversettTilRegelModellOmsorgenForVurderinger(long behandlingId) {
        final Optional<OmsorgenForGrunnlag> omsorgenForGrunnlag = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        return new LocalDateTimeline<>(
            omsorgenForGrunnlag.map(og -> og.getOmsorgenFor().getPerioder()).orElse(List.of())
                .stream()
                .map(ofp -> new LocalDateSegment<>(ofp.getPeriode().getFomDato(), ofp.getPeriode().getTomDato(), new OmsorgenForVilkårGrunnlag(mapToErOmsorgsperson(ofp.getResultat()))))
                .collect(Collectors.toList())
        );
    }

    private Boolean mapToErOmsorgsperson(Resultat resultat) {
        return switch (resultat) {
            case OPPFYLT -> true;
            case IKKE_OPPFYLT -> false;
            case IKKE_VURDERT -> null;
        };
    }

    public Systemdata hentSystemdata(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var fullstendigePerioder = vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
        if (fullstendigePerioder.isEmpty() || pleietrengende.isEmpty()) {
            return new Systemdata(false, false);
        }
        var periode = mapTilPeriode(fullstendigePerioder);

        var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
        if (optAggregat.isEmpty()) {
            return new Systemdata(false, false);
        }
        var aggregat = optAggregat.get();
        var pleietrengendeAktørId = pleietrengende.get();
        var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).toList();

        var registrertForeldrerelasjon = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
        var registrertSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);

        return new Systemdata(registrertForeldrerelasjon, registrertSammeBosted);

    }

    private DatoIntervallEntitet mapTilPeriode(NavigableSet<DatoIntervallEntitet> perioder) {
        final var fom = perioder
            .stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();

        final var tom = perioder
            .stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public boolean skalHaAksjonspunkt(BehandlingReferanse referanse, LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, boolean medAlleGamleVurderingerPåNytt) {
        var omsorgenForVilkår = OmsorgenForVilkår.finnTjeneste(omsorgenForVilkårene, referanse.getFagsakYtelseType());
        return omsorgenForVilkår.skalHaAksjonspunkt(samletOmsorgenForTidslinje, medAlleGamleVurderingerPåNytt);
    }

    public static class Systemdata {
        private final boolean registrertForeldrerelasjon;
        private final boolean registrertSammeBosted;


        public Systemdata(boolean registrertForeldrerelasjon, boolean registrertSammeBosted) {
            this.registrertForeldrerelasjon = registrertForeldrerelasjon;
            this.registrertSammeBosted = registrertSammeBosted;
        }


        public boolean isRegistrertForeldrerelasjon() {
            return registrertForeldrerelasjon;
        }

        public boolean isRegistrertSammeBosted() {
            return registrertSammeBosted;
        }
    }
}
