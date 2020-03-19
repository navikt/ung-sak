package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.medlem.api.FinnMedlemRequest;
import no.nav.k9.sak.domene.medlem.api.Medlemskapsperiode;
import no.nav.k9.sak.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.k9.sak.domene.medlem.impl.MedlemResultat;
import no.nav.k9.sak.kontrakt.medlem.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.k9.sak.kontrakt.medlem.EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class MedlemTjeneste {

    private static Map<MedlemResultat, AksjonspunktDefinisjon> mapMedlemResulatTilAkDef = new EnumMap<>(MedlemResultat.class);

    static {
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT);
    }

    private MedlemskapRepository medlemskapRepository;
    private HentMedlemskapFraRegister hentMedlemskapFraRegister;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerTjeneste;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;

    MedlemTjeneste() {
        // CDI
    }

    @Inject
    public MedlemTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          HentMedlemskapFraRegister hentMedlemskapFraRegister,
                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                          PersonopplysningTjeneste personopplysningTjeneste,
                          UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerForMedlemskapTjeneste,
                          VurderMedlemskapTjeneste vurderMedlemskapTjeneste) {
        this.hentMedlemskapFraRegister = hentMedlemskapFraRegister;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.utledVurderingsdatoerTjeneste = utledVurderingsdatoerForMedlemskapTjeneste;
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
    }

    /**
     * Finn medlemskapsperioder i MEDL2 register for en person.
     *
     * @param finnMedlemRequest Inneholder fødselsnummer, start-/slutt- dato for søket, og behandling-/fagsak- ID.
     * @return Liste av medlemsperioder funnet
     */
    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest) {
        return hentMedlemskapFraRegister.finnMedlemskapPerioder(finnMedlemRequest);
    }

    public Optional<MedlemskapAggregat> hentMedlemskap(Long behandlingId) {
        return medlemskapRepository.hentMedlemskap(behandlingId);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = medlemskapRepository.hentIdPåAktivMedlemskap(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(MedlemskapAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(MedlemskapAggregat.class));
    }

    public Set<AksjonspunktDefinisjon> utledAksjonspunkterForVurderingsDato(BehandlingReferanse ref, LocalDate dato) {
        final var medlemResultat = vurderMedlemskapTjeneste.vurderMedlemskap(ref, dato);
        return medlemResultat.stream().map(mr -> mapMedlemResulatTilAkDef.get(mr)).collect(Collectors.toSet());
    }

    /**
     * Sjekker endringer i personopplysninger som tilsier at bruker 'ikke er'/'skal miste' medlemskap.
     * Sjekker statsborgerskap (kun mht endring i {@link Region}, ikke land),
     * {@link PersonstatusType}, og {@link AdresseType}
     * for intervall { max(seneste vedtatte medlemskapsperiode, skjæringstidspunkt), nå}.
     * <p>
     * Metoden gjelder revurdering foreldrepenger
     */
    // TODO Diamant (Denne gjelder kun revurdering og foreldrepenger, bør eksponeres som egen tjeneste for behandling type BT004)
    public EndringsresultatPersonopplysningerForMedlemskap søkerHarEndringerIPersonopplysninger(Behandling revurderingBehandling) {

        EndringsresultatPersonopplysningerForMedlemskap.Builder builder = EndringsresultatPersonopplysningerForMedlemskap.builder();
        if (revurderingBehandling.erRevurdering()) {
            AktørId aktørId = revurderingBehandling.getAktørId();
            Long behandlingId = revurderingBehandling.getId();
            DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMedTilOgMed(finnStartdato(revurderingBehandling), LocalDate.now());
            Optional<PersonopplysningerAggregat> historikkAggregat = personopplysningTjeneste
                .hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, intervall);

            historikkAggregat.ifPresent(historikk -> {
                sjekkEndringer(historikk.getStatsborgerskapFor(aktørId).stream()
                        .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getStatsborgerskap(), e.getPeriode())), builder,
                    EndretAttributt.StatsborgerskapRegion);

                sjekkEndringer(historikk.getPersonstatuserFor(aktørId).stream()
                    .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getPersonstatus(), e.getPeriode())), builder, EndretAttributt.Personstatus);

                sjekkEndringer(historikk.getAdresserFor(aktørId).stream()
                    .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getAdresseType(), e.getPeriode())), builder, EndretAttributt.Adresse);
            });
        }
        return builder.build();
    }

    public Map<LocalDate, VurderMedlemskap> utledVurderingspunkterMedAksjonspunkt(BehandlingReferanse ref) {
        final Map<LocalDate, Set<VurderingsÅrsak>> vurderingsdatoer = utledVurderingsdatoerTjeneste.finnVurderingsdatoerMedÅrsak(ref.getBehandlingId());
        final HashMap<LocalDate, VurderMedlemskap> map = new HashMap<>();
        for (Map.Entry<LocalDate, Set<VurderingsÅrsak>> entry : vurderingsdatoer.entrySet()) {
            LocalDate vurderingsdato = entry.getKey();
            final Set<MedlemResultat> vurderinger = vurderMedlemskapTjeneste.vurderMedlemskap(ref, vurderingsdato);
            if (!vurderinger.isEmpty()) {
                map.put(vurderingsdato, mapTilVurderMeldemspa(vurderinger, entry.getValue()));
            }
        }
        return map;
    }

    private VurderMedlemskap mapTilVurderMeldemspa(Set<MedlemResultat> vurderinger, Set<VurderingsÅrsak> vurderingsÅrsaks) {
        final Set<AksjonspunktDefinisjon> aksjonspunkter = vurderinger.stream()
            .map(vu -> Optional.ofNullable(mapMedlemResulatTilAkDef.get(vu)).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return new VurderMedlemskap(aksjonspunkter, vurderingsÅrsaks);
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        Objects.requireNonNull(idDiff.getGrunnlagId1(), "kan ikke diffe når id1 ikke er oppgitt");
        Objects.requireNonNull(idDiff.getGrunnlagId2(), "kan ikke diffe når id2 ikke er oppgitt");

        return medlemskapRepository.diffResultat((Long) idDiff.getGrunnlagId1(), (Long) idDiff.getGrunnlagId2(), kunSporedeEndringer);
    }

    /**
     * Sjekker både medlemskapsvilkåret og løpende medlemskapsvilkår
     * Tar hensyn til overstyring
     *
     * @param behandling
     * @return opphørsdato
     */
    public Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Long behandlingId) {
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isEmpty()) {
            return Optional.empty();
        }
        Optional<Vilkår> medlemskapsvilkåret = vilkårene.get()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        if (medlemskapsvilkåret.isPresent()) {
            Vilkår medlem = medlemskapsvilkåret.get();
            final var ikkeGodkjentePerioder = medlem.getPerioder()
                .stream()
                .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
                .collect(Collectors.toList());
            if (!ikkeGodkjentePerioder.isEmpty()) {
                return ikkeGodkjentePerioder.stream()
                    .sorted(Comparator.comparing(VilkårPeriode::getPeriode))
                    .map(VilkårPeriode::getPeriode)
                    .map(DatoIntervallEntitet::getFomDato)
                    .findFirst();
            }
        }
        return Optional.empty();
    }

    private <T extends Kodeverdi> void sjekkEndringer(Stream<ElementMedGyldighetsintervallWrapper<T>> elementer,
                                                      EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        List<ElementMedGyldighetsintervallWrapper<T>> endringer = elementer
            .sorted(Comparator.comparing(ElementMedGyldighetsintervallWrapper::sortPeriode))
            .distinct().collect(Collectors.toList());

        leggTilEndringer(endringer, builder, endretAttributt);
    }

    private <T extends Kodeverdi> void leggTilEndringer(List<ElementMedGyldighetsintervallWrapper<T>> endringer,
                                                        EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        if (endringer != null && endringer.size() > 1) {
            for (int i = 0; i < endringer.size() - 1; i++) {
                String endretFra = endringer.get(i).element.getNavn();
                String endretTil = endringer.get(i + 1).element.getNavn();
                DatoIntervallEntitet periode = endringer.get(i + 1).gylidghetsintervall;
                builder.leggTilEndring(endretAttributt, new Periode(periode.getFomDato(), periode.getTomDato()), endretFra, endretTil);
            }
        }
    }

    private LocalDate finnStartdato(Behandling behandling) {
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        if (vilkårene.isEmpty()) {
            return LocalDate.now();
        }
        Optional<Vilkår> medlemskapsvilkåret = vilkårene.get()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        LocalDate startDato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
        if (medlemskapsvilkåret.isPresent()) {
            LocalDate date = medlemskapsvilkåret.get()
                .getPerioder()
                .stream()
                .map(VilkårPeriode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .max(LocalDate::compareTo)
                .orElse(startDato);

            if (startDato.isBefore(date)) {
                startDato = date;
            }
        }

        return startDato.isAfter(LocalDate.now()) ? LocalDate.now() : startDato;
    }

    public Tuple<Utfall, Avslagsårsak> utledVilkårUtfall(Behandling revurdering) {
        final var vilkårene = vilkårResultatRepository.hent(revurdering.getId());
        Optional<Vilkår> medlemOpt = vilkårene
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        if (medlemOpt.isPresent()) {
            Vilkår medlem = medlemOpt.get();
            final var ikkeGodkjentePerioder = medlem.getPerioder()
                .stream()
                .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
                .sorted(Comparator.comparing(VilkårPeriode::getPeriode))
                .collect(Collectors.toList());

            if (ikkeGodkjentePerioder.isEmpty()) {
                return new Tuple<>(Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
            }
            final var førstePeriodeMedIkkeOppfylt = ikkeGodkjentePerioder.stream().findFirst().get();

            if (førstePeriodeMedIkkeOppfylt.getGjeldendeUtfall().equals(Utfall.IKKE_OPPFYLT) && !førstePeriodeMedIkkeOppfylt.getErOverstyrt()) {
                return new Tuple<>(Utfall.IKKE_OPPFYLT, Avslagsårsak.fraKode(førstePeriodeMedIkkeOppfylt.getMerknad().getKode()));
            } else if (førstePeriodeMedIkkeOppfylt.getGjeldendeUtfall().equals(Utfall.IKKE_OPPFYLT) && førstePeriodeMedIkkeOppfylt.getErOverstyrt()) {
                Avslagsårsak avslagsårsak = førstePeriodeMedIkkeOppfylt.getAvslagsårsak();
                return new Tuple<>(Utfall.IKKE_OPPFYLT, avslagsårsak);
            }
        }
        throw new IllegalStateException("Kan ikke utlede vilkår utfall type når medlemskapsvilkåret ikke finnes");
    }

    private static final class ElementMedGyldighetsintervallWrapper<T> {
        private final T element;
        private final DatoIntervallEntitet gylidghetsintervall;

        private ElementMedGyldighetsintervallWrapper(T element, DatoIntervallEntitet gylidghetsintervall) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(gylidghetsintervall);
            this.element = element;
            this.gylidghetsintervall = gylidghetsintervall;
        }

        private static Long sortPeriode(ElementMedGyldighetsintervallWrapper<?> e) {
            return e.gylidghetsintervall.getFomDato().toEpochDay();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (obj instanceof ElementMedGyldighetsintervallWrapper<?>) {
                ElementMedGyldighetsintervallWrapper<?> other = (ElementMedGyldighetsintervallWrapper<?>) obj;
                return element.equals(other.element);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(element, gylidghetsintervall);
        }
    }
}
