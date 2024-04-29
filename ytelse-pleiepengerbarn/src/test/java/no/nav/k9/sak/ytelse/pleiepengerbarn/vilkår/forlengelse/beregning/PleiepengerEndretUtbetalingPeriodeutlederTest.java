package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.util.Collections.emptyNavigableSet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.OverstyrUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.PleietrengendeRevurderingPerioderTjeneste;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PleiepengerEndretUtbetalingPeriodeutlederTest {

    public static final String ORGANISASJONSNUMMER = "123456789";
    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2024, 3, 14);
    public static final String JOURNALPOST_ID = "123567324234";
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    private Behandling originalBehandling;

    private SøknadsperiodeRepository søknadsperiodeRepository;

    @Inject
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    private PleiepengerEndretUtbetalingPeriodeutleder utleder;
    private MottatteDokumentRepository mottatteDokumentRepository;

    private final VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock();

    private final InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning = (sakInntektsmeldinger, vilkårsPeriode) -> sakInntektsmeldinger.stream().toList();

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private UttakNyeReglerRepository uttakNyeReglerRepository;

    private final PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste = mock(PleietrengendeRevurderingPerioderTjeneste.class);
    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        søknadsperiodeRepository = new SøknadsperiodeRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        uttakNyeReglerRepository = new UttakNyeReglerRepository(entityManager);
        var kompletthetForBeregningTjeneste = new KompletthetForBeregningTjeneste(
            new UnitTestLookupInstanceImpl<>(inntektsmeldingerRelevantForBeregning),
            null,
            null,
            null,
            null
        );
        var erEndringIRefusjonskravVurderer = new ErEndringIRefusjonskravVurderer(
            behandlingRepository, kompletthetForBeregningTjeneste,
            inntektArbeidYtelseTjeneste,
            mottatteDokumentRepository
        );
        var personopplysningRepository = new PersonopplysningRepository(entityManager);
        var personopplysningTjeneste = new PersonopplysningTjeneste(personopplysningRepository);

        utleder = new PleiepengerEndretUtbetalingPeriodeutleder(
            new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste),
            new ProsessTriggereRepository(entityManager),
            søknadsperiodeTjeneste,
            uttakNyeReglerRepository,
            personopplysningTjeneste,
            pleietrengendeRevurderingPerioderTjeneste,
            erEndringIRefusjonskravVurderer,
            new OverstyrUttakTjeneste(null, new OverstyrUttakRepository(entityManager), null, null));
        originalBehandling = opprettBehandling(SKJÆRINGSTIDSPUNKT);
        behandling = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));

        mottatteDokumentRepository.lagre(byggMottattDokument(behandling.getFagsakId()), DokumentStatus.GYLDIG);

        when(vilkårsPerioderTilVurderingTjeneste.utled(any(), any())).thenReturn(emptyNavigableSet());
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(any())).thenReturn(emptyNavigableSet());
        when(vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()).thenReturn(Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR));

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        personOpplysningSøker(informasjonBuilder);
        pleietrengende(informasjonBuilder, Optional.empty());

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);
        when(pleietrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(any(), any())).thenReturn(LocalDateTimeline.empty());
    }

    @Test
    void skal_returnere_tom_liste_dersom_nye_regler_dato_etter_periode() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;


        var tomDato = SKJÆRINGSTIDSPUNKT.plusDays(antallDager);

        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandling.getId(), tomDato.plusDays(1));

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, tomDato));

        assertThat(forlengelseperioder.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_periode_på_en_dag_dersom_dato_nye_uttaksregler_lik_tomdato() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        var tomDato = SKJÆRINGSTIDSPUNKT.plusDays(antallDager);

        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandling.getId(), tomDato);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, tomDato));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var first = forlengelseperioder.getFirst();
        assertThat(first.getFomDato()).isEqualTo(tomDato);
        assertThat(first.getTomDato()).isEqualTo(tomDato);
    }

    @Test
    void skal_returnere_hele_perioden_ved_dato_for_nye_regler_før_vilkårsperiode() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        var tomDato = SKJÆRINGSTIDSPUNKT.plusDays(antallDager);

        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandling.getId(), SKJÆRINGSTIDSPUNKT.minusDays(1));

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, tomDato));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var first = forlengelseperioder.getFirst();
        assertThat(first.getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(first.getTomDato()).isEqualTo(tomDato);
    }

    @Test
    void skal_returnere_perioden_fom_endring_av_dato_for_nye_regler() {
        var antallDager = 10;
        var tomDato = SKJÆRINGSTIDSPUNKT.plusDays(antallDager);

        uttakNyeReglerRepository.lagreDatoForNyeRegler(originalBehandling.getId(), SKJÆRINGSTIDSPUNKT);
        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandling.getId(), SKJÆRINGSTIDSPUNKT.plusDays(5));

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, tomDato));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var first = forlengelseperioder.getFirst();
        assertThat(first.getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(first.getTomDato()).isEqualTo(tomDato);
    }


    @Test
    void skal_gi_tom_periode_ved_ingen_endring() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_perioder_med_endring_i_refusjon() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        var builder = new MottattDokument.Builder();
        var journalPostId = new JournalpostId(1L);
        builder.medJournalPostId(journalPostId);
        builder.medBehandlingId(behandling.getId());
        builder.medFagsakId(fagsak.getId());
        builder.medType(Brevkode.INNTEKTSMELDING);
        mottatteDokumentRepository.lagre(builder.build(), DokumentStatus.GYLDIG);

        inntektArbeidYtelseTjeneste.lagreInntektsmeldinger(fagsak.getSaksnummer(), behandling.getId(), List.of(InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGANISASJONSNUMMER))
            .medRefusjon(BigDecimal.TEN)
            .medKanalreferanse("kanalref")
            .medJournalpostId(journalPostId)
            .medStartDatoPermisjon(SKJÆRINGSTIDSPUNKT)));


        when(vilkårsPerioderTilVurderingTjeneste.utled(any(), any())).thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(antallDager)))));
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(any())).thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(antallDager)))));

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(periode.getTomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
    }

    @Test
    void skal_returnere_relevante_søknadsperioder() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var dagerEtterSTPSøknadFom = 5;
        var antallDager = 10;

        var søknadsperiode = new Søknadsperiode(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom), SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
        var søknadsperioder = new Søknadsperioder(new JournalpostId(JOURNALPOST_ID), søknadsperiode);
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(søknadsperioder));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(mandagenFør(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom)));
        assertThat(periode.getTomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
    }


    @Test
    void skal_returnere_fom_dato_lik_stp_dersom_mandagen_før_ligger_utenfor_periode() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var dagerEtterSTPSøknadFom = 1;
        var antallDager = 10;

        var søknadsperiode = new Søknadsperiode(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom), SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
        var søknadsperioder = new Søknadsperioder(new JournalpostId(JOURNALPOST_ID), søknadsperiode);
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(søknadsperioder));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(periode.getTomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
    }


    @Test
    void skal_gi_en_periode_ved_endring_i_tilsyn_i_hele_perioden_bortsett_fra_helg() {
        var fom = SKJÆRINGSTIDSPUNKT.with(TemporalAdjusters.next(DayOfWeek.MONDAY));


        when(pleietrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(
            BehandlingReferanse.fra(behandling),
            Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR)
        )).thenReturn(
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, fom.plusDays(4), Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON)),
                new LocalDateSegment<>(fom.plusDays(7), fom.plusDays(11), Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            ))
        );


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(11)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(11));
    }

    //hull på 10 dager mellom stp1 og stp2 periodene inkl fom og tom
        /*
          beh1    |---|     |----|
          ben2         |---|
          res          |---------|
         */
    @Test
    void skal_inkludere_periode_uten_endring_hvis_kant_i_kant_med_tidligere_stp() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(5);

        var fomHull = tom1.plusDays(1);
        var tomHull = fomHull.plusDays(7);

        var stp2 = tomHull.plusDays(1);
        var tom2 = stp2.plusDays(20);


        when(pleietrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(
            BehandlingReferanse.fra(behandling),
            Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR)
        )).thenReturn(
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fomHull, tomHull, Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON)),
                new LocalDateSegment<>(stp2, tom2, Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            ))
        );


        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom1),
                DatoIntervallEntitet.fraOgMedTilOgMed(stp2, tom2)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom2));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(mandagenFør(fomHull));
        assertThat(periode.getTomDato()).isEqualTo(tom2);
    }

    //hull på 10 dager mellom stp1 og stp2 periodene inkl fom og tom
    //men tettes bare delvis
        /*
          beh1        |---|           |----|
          ben2               |---|
          resultat           |---|
         */
    @Test
    void skal_inkludere_periode_fra_endringsdato_og_til_slutten_av_perioden_hvis_ikke_kant_i_kant_fra_tidligere_stp() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(5);

        var fomHull = tom1.plusDays(3);
        var tomHull = fomHull.plusDays(4);

        var stp2 = tomHull.plusDays(4);
        var tom2 = stp2.plusDays(20);


        when(pleietrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(
            BehandlingReferanse.fra(behandling),
            Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR)
        )).thenReturn(
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fomHull, tomHull, Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            ))
        );

        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom1),
                DatoIntervallEntitet.fraOgMedTilOgMed(stp2, tom2)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom2));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(mandagenFør(fomHull));
        assertThat(periode.getTomDato()).isEqualTo(tom2);
    }

    @Test
    void skal_returnere_tom_liste_for_søknadsendringer_utenfor_periode() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var dagerEtterSTPSøknadFom = 5;
        var antallDager = 10;

        var søknadsperiode = new Søknadsperiode(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom), SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
        var søknadsperioder = new Søknadsperioder(new JournalpostId(JOURNALPOST_ID), søknadsperiode);
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(søknadsperioder));
        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, fom.plusDays(dagerEtterSTPSøknadFom - 4)));

        assertThat(forlengelseperioder.size()).isEqualTo(0);
    }

    private Behandling opprettBehandling(LocalDate skjæringstidspunkt) {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(),
            AktørId.dummy(),
            null,
            new Saksnummer("SAK"), skjæringstidspunkt, skjæringstidspunkt.plusDays(3));

        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }


    public static MottattDokument byggMottattDokument(Long fagsakId) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medMottattDato(LocalDate.now());
        builder.medType(Brevkode.PLEIEPENGER_BARN_SOKNAD);
        builder.medPayload("payload");
        builder.medFagsakId(fagsakId);
        builder.medJournalPostId(new JournalpostId(JOURNALPOST_ID));
        return builder.build();
    }

    private void pleietrengende(PersonInformasjonBuilder informasjonBuilder, Optional<LocalDate> dødsdato) {
        var aktørId = fagsak.getPleietrengendeAktørId();
        LocalDate fødselsdato = SKJÆRINGSTIDSPUNKT.minusYears(1);
        var personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(aktørId)
            .medNavn("Navn")
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medFødselsdato(fødselsdato)
            .medSivilstand(SivilstandType.UGIFT)
            .medRegion(Region.NORDEN);
        dødsdato.ifPresent(personopplysningBuilder::medDødsdato);
        informasjonBuilder.leggTil(personopplysningBuilder)
            .leggTil(informasjonBuilder
                .getPersonstatusBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("NOR").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND)
                .medAdresselinje1("Testadresse")
                .medLand("Sverige").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), Landkoder.NOR, Region.NORDEN));
    }

    private void personOpplysningSøker(PersonInformasjonBuilder informasjonBuilder) {
        var aktørId = behandling.getAktørId();
        LocalDate fødselsdato = SKJÆRINGSTIDSPUNKT.minusYears(20);
        informasjonBuilder.leggTil(
                informasjonBuilder.getPersonopplysningBuilder(aktørId)
                    .medNavn("Navn")
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medFødselsdato(fødselsdato)
                    .medSivilstand(SivilstandType.GIFT)
                    .medRegion(Region.NORDEN))
            .leggTil(informasjonBuilder
                .getPersonstatusBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato)).medPersonstatus(PersonstatusType.BOSA))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.BOSTEDSADRESSE)
                .medAdresselinje1("Testadresse")
                .medLand("NOR").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getAdresseBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND)
                .medAdresselinje1("Testadresse")
                .medLand("Sverige").medPostnummer("1234"))
            .leggTil(informasjonBuilder
                .getStatsborgerskapBuilder(aktørId, DatoIntervallEntitet.fraOgMed(fødselsdato), Landkoder.NOR, Region.NORDEN));
    }


    private static LocalDate mandagenFør(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }

}
