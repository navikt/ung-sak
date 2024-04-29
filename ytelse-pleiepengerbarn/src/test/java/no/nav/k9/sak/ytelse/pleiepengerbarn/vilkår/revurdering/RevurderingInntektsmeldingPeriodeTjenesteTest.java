package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.DefaultVilkårUtleder;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.HarInntektsmeldingerRelevanteEndringerForPeriode;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class RevurderingInntektsmeldingPeriodeTjenesteTest {

    public static final LocalDate STP = LocalDate.now();
    public static final DatoIntervallEntitet VILKÅRSPERIODE = DatoIntervallEntitet.fra(STP, STP.plusDays(10));
    public static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("123456789");
    public static final JournalpostId SØKNAD_JP = new JournalpostId(892L);
    @Inject
    private HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode;
    @Inject
    private ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer;

    @Inject
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private RevurderingInntektsmeldingPeriodeTjeneste tjeneste;
    @Inject
    private EntityManager entityManager;

    @Inject
    private SøknadsperiodeRepository søknadsperiodeRepository;

    private OpptjeningRepository opptjeningRepository;


    @BeforeEach
    void setUp() {
        var behandlingRepository = new BehandlingRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository, new VilkårResultatRepository(entityManager));
        tjeneste = new RevurderingInntektsmeldingPeriodeTjeneste(
            new UnitTestLookupInstanceImpl<>(new DefaultVilkårUtleder()),
            harInntektsmeldingerRelevanteEndringerForPeriode,
            erEndringIRefusjonskravVurderer,
            behandlingRepository,
            søknadsperiodeTjeneste);
    }

    @Test
    void skal_returnere_tom_tidslinje_ved_ingen_inntektsmeldinger() {


        var originalBehandling = lagOriginalBehandling();

        var behandling = lagRevurdering(originalBehandling);
        lagIAYMedArbeid(behandling);
        lagIAYMedArbeid(originalBehandling);
        lagOpptjening(behandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagOpptjening(originalBehandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);;

        var tidslinje = tjeneste.utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse.fra(behandling), Set.of(), List.of(), List.of(VILKÅRSPERIODE));

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_tom_tidslinje_ved_en_im_mottatt_i_forrige_behandling() {
        var originalBehandling = lagOriginalBehandling();

        var behandling = lagRevurdering(originalBehandling);

        var journalpostId = 123L;
        var im = lagInntektsmelding(journalpostId, BigDecimal.TEN, null, "KANALREF");
        lagIAYMedArbeid(behandling);
        lagIAYMedArbeid(originalBehandling);
        lagOpptjening(behandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagOpptjening(originalBehandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);;

        var tidslinje = tjeneste.utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse.fra(behandling), Set.of(im), List.of(), List.of(VILKÅRSPERIODE));

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_hele_perioden_ved_ny_inntektsmelding_for_sak_uten_match_i_forrige_behandling() {
        var originalBehandling = lagOriginalBehandling();

        var behandling = lagRevurdering(originalBehandling);

        var journalpostId = 123L;
        var im = lagInntektsmelding(journalpostId, BigDecimal.TEN, null, "KANALREF");
        var mottattDokument = lagMottattDokument(behandling, journalpostId);
        entityManager.persist(mottattDokument);
        entityManager.flush();

        lagIAYMedArbeid(behandling);
        lagIAYMedArbeid(originalBehandling);
        lagOpptjening(behandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagOpptjening(originalBehandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var tidslinje = tjeneste.utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse.fra(behandling), Set.of(im), List.of(mottattDokument), List.of(VILKÅRSPERIODE));

        assertThat(tidslinje.isEmpty()).isFalse();
        var localDateIntervals = tidslinje.getLocalDateIntervals();
        assertThat(localDateIntervals.size()).isEqualTo(1);
        var intervall = localDateIntervals.iterator().next();
        assertThat(intervall.getFomDato()).isEqualTo(STP);
        assertThat(intervall.getTomDato()).isEqualTo(STP.plusDays(10));
    }

    @Test
    void skal_returnere_deler_av_perioden_ved_ny_inntektsmelding_for_sak_og_endring_i_refusjon() {
        var originalBehandling = lagOriginalBehandling();

        var behandling = lagRevurdering(originalBehandling);

        var journalpostIdOriginal = 1234L;
        var imOriginal = lagInntektsmelding(1234L, BigDecimal.TEN, null, "KANALREF1");
        var mottattDokumentOriginal = lagMottattDokument(originalBehandling, journalpostIdOriginal);
        entityManager.persist(mottattDokumentOriginal);

        var journalpostId = 123L;
        var im = lagInntektsmelding(journalpostId, BigDecimal.TEN, new Refusjon(BigDecimal.TEN, STP.plusDays(2)), "KANALREF2");
        var mottattDokument = lagMottattDokument(behandling, journalpostId);
        entityManager.persist(mottattDokument);

        entityManager.flush();

        lagIAYMedArbeid(behandling);
        lagIAYMedArbeid(originalBehandling);
        lagOpptjening(behandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagOpptjening(originalBehandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);


        var tidslinje = tjeneste.utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse.fra(behandling), Set.of(im, imOriginal), List.of(mottattDokument), List.of(VILKÅRSPERIODE));

        assertThat(tidslinje.isEmpty()).isFalse();
        var localDateIntervals = tidslinje.getLocalDateIntervals();
        assertThat(localDateIntervals.size()).isEqualTo(1);
        var intervall = localDateIntervals.iterator().next();
        assertThat(intervall.getFomDato()).isEqualTo(STP.plusDays(2));
        assertThat(intervall.getTomDato()).isEqualTo(STP.plusDays(10));
    }

    @Test
    void skal_returnere_hele_perioden_ved_ny_inntektsmelding_for_sak_og_endring_i_refusjon_og_inntekt() {
        var originalBehandling = lagOriginalBehandling();
        var behandling = lagRevurdering(originalBehandling);

        var journalpostIdOriginal = 1234L;
        var imOriginal = lagInntektsmelding(1234L, BigDecimal.TEN, null, "KANALREF1");
        var mottattDokumentOriginal = lagMottattDokument(originalBehandling, journalpostIdOriginal);
        entityManager.persist(mottattDokumentOriginal);

        var journalpostId = 123L;
        var im = lagInntektsmelding(journalpostId, BigDecimal.valueOf(100), new Refusjon(BigDecimal.valueOf(100), STP.plusDays(2)), "KANALREF2");
        var mottattDokument = lagMottattDokument(behandling, journalpostId);
        entityManager.persist(mottattDokument);
        entityManager.flush();

        lagIAYMedArbeid(behandling);
        lagIAYMedArbeid(originalBehandling);
        lagOpptjening(behandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagOpptjening(originalBehandling, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);


        var tidslinje = tjeneste.utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse.fra(behandling), Set.of(im, imOriginal), List.of(mottattDokument), List.of(VILKÅRSPERIODE));

        assertThat(tidslinje.isEmpty()).isFalse();
        var segments = tidslinje.toSegments();
        assertThat(segments.size()).isEqualTo(2);
        var iterator = segments.iterator();
        var segment1 = iterator.next();
        assertThat(segment1.getFom()).isEqualTo(STP);
        assertThat(segment1.getTom()).isEqualTo(STP.plusDays(1));

        var segment2 = iterator.next();
        assertThat(segment2.getFom()).isEqualTo(STP.plusDays(2));
        assertThat(segment2.getTom()).isEqualTo(STP.plusDays(10));
    }

    private void lagIAYMedArbeid(Behandling behandling) {
        var inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                .medAktørId(behandling.getAktørId())
                .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                    .medArbeidsgiver(ARBEIDSGIVER1)
                    .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                    .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(100), STP.plusDays(50))))));
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
    }

    private void lagOpptjening(Behandling behandling, OpptjeningAktivitetKlassifisering opptjeningAktivitetKlassifisering) {
        var opptjeningFom = RevurderingInntektsmeldingPeriodeTjenesteTest.STP.minusDays(29);
        var opptjeningTom = RevurderingInntektsmeldingPeriodeTjenesteTest.STP.minusDays(1);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningFom, opptjeningTom, true);
        opptjeningRepository.lagreOpptjeningResultat(behandling, RevurderingInntektsmeldingPeriodeTjenesteTest.STP, Period.of(0, 0, 28),
            List.of(new OpptjeningAktivitet(opptjeningFom, opptjeningTom,
                OpptjeningAktivitetType.ARBEID,
                opptjeningAktivitetKlassifisering,
                ARBEIDSGIVER1.getIdentifikator(),
                ReferanseType.ORG_NR)));
    }


    private static Inntektsmelding lagInntektsmelding(long journalpostId, BigDecimal inntekt, Refusjon refusjon, String kanalref) {
        var builder = InntektsmeldingBuilder.builder();
        builder.medArbeidsgiver(ARBEIDSGIVER1);
        builder.medBeløp(inntekt);
        builder.medStartDatoPermisjon(STP);
        builder.medJournalpostId(new JournalpostId(journalpostId));
        if (refusjon != null) {
            builder.leggTil(refusjon);
        }
        builder.medRefusjon(BigDecimal.ZERO);
        builder.medKanalreferanse(kanalref);
        return builder.build();
    }

    private static MottattDokument lagMottattDokument(Behandling behandling, long journalpostId) {
        var builder = new MottattDokument.Builder();
        builder.medBehandlingId(behandling.getId());
        builder.medJournalPostId(new JournalpostId(journalpostId));
        builder.medType(Brevkode.INNTEKTSMELDING);
        builder.medFagsakId(behandling.getFagsakId());
        builder.medStatus(DokumentStatus.GYLDIG);
        return builder.build();
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        var revurderingBuilder = TestScenarioBuilder.builderUtenSøknad(FagsakYtelseType.PSB);
        revurderingBuilder.medBehandlingType(BehandlingType.REVURDERING);
        revurderingBuilder.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        revurderingBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        revurderingBuilder.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        revurderingBuilder.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);
        var behandling = revurderingBuilder.lagre(entityManager);
        søknadsperiodeRepository.lagre(behandling.getId(), new Søknadsperioder(SØKNAD_JP, new Søknadsperiode(VILKÅRSPERIODE)));
        return behandling;
    }

    private Behandling lagOriginalBehandling() {
        var scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PSB);
        var søknadBuilder = scenarioBuilder.medSøknad();
        søknadBuilder.medSøknadsperiode(VILKÅRSPERIODE);
        scenarioBuilder.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        scenarioBuilder.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, Utfall.OPPFYLT, new Periode(VILKÅRSPERIODE.getFomDato(), VILKÅRSPERIODE.getTomDato()));
        var originalBehandling = scenarioBuilder.lagre(entityManager);


        var builder = new MottattDokument.Builder();
        builder.medBehandlingId(originalBehandling.getId());
        var søknadJp = SØKNAD_JP;
        builder.medJournalPostId(søknadJp);
        builder.medType(Brevkode.PLEIEPENGER_BARN_SOKNAD);
        builder.medFagsakId(originalBehandling.getFagsakId());
        builder.medStatus(DokumentStatus.GYLDIG);

        entityManager.persist(builder.build());
        entityManager.flush();

        søknadsperiodeRepository.lagre(originalBehandling.getId(), new Søknadsperioder(SØKNAD_JP, new Søknadsperiode(VILKÅRSPERIODE)));

        return originalBehandling;
    }

}
