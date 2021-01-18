package no.nav.k9.sak.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

public class BeregningsresultatRepositoryImplTest {

    private static final String ORGNR = "55";

    private static final LocalDate DAGENSDATO = LocalDate.now().withMonth(2);

    @RegisterExtension
    public static final JpaExtension extension = new JpaExtension();

    private final EntityManager entityManager = extension.getEntityManager();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
    private Behandling behandling;
    private AktørId aktørId;

    private final BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(entityManager);

    @BeforeEach
    public void setup() {
        aktørId = AktørId.dummy();
        behandling = opprettBehandling();
    }

    private Behandling opprettBehandling() {
        return behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagreOgHentBeregningsresultatAggregat() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling -> assertThat(brKobling.getBgBeregningsresultat()).isSameAs(beregningsresultat));
    }

    @Test
    public void lagreOgHentUtbetBeregningsresultatAggregatNårUTBETEksisterer() {
        // Arrange
        var bgBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        var utbetBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO.plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultat);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultat);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling -> assertThat(brKobling.getUtbetBeregningsresultat()).isSameAs(utbetBeregningsresultat));
    }

    @Test
    public void lagreOgHenteBeregningsresultat() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatLest = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());

        assertThat(beregningsresultatLest).isEqualTo(Optional.of(beregningsresultat));
    }

    @Test
    public void lagreOgHenteEndeligBeregningsresultat() {
        // Arrange
        // bgBeregningsresultat kan bli overskrivet av utbetBeregningsresultat ved vurdering av tilbaketrekk
        var bgBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        var utbetBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO.plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultat);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultat);

        // Assert
        Long id = utbetBeregningsresultat.getId();
        assertThat(id).isNotNull();

        flushAndClear();
        Optional<BeregningsresultatEntitet> utbetBeregningsresultatLest = beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId());

        assertThat(utbetBeregningsresultatLest).isEqualTo(Optional.of(utbetBeregningsresultat));
    }

    @Test
    public void lagreOgHenteBeregningsresultatMedPrivatpersonSomArbeidsgiver() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), true);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatLest = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        assertThat(beregningsresultatLest).isEqualTo(Optional.of(beregningsresultat));
        assertThat(beregningsresultatLest).isPresent();
        Arbeidsgiver arbeidsgiver = beregningsresultatLest.get().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0).getArbeidsgiver().get();// NOSONAR
        assertThat(arbeidsgiver.getAktørId()).isEqualTo(aktørId);
        assertThat(arbeidsgiver.getIdentifikator()).isEqualTo(aktørId.getId());
    }

    @Test
    public void lagreBeregningsresultatOgUnderliggendeTabellerMedEndringsdatoLikDagensDato() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        var brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        flushAndClear();
        var beregningsresultatLest = hentEntitet(BeregningsresultatEntitet.class, brId);
        var brPeriodeLest = hentEntitet(BeregningsresultatPeriode.class, brPeriodeId);
        var brAndelLest = hentEntitet(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatLest.getId()).isNotNull();
        assertThat(beregningsresultatLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatLest.getEndringsdato()).isEqualTo(Optional.of(DAGENSDATO));
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    private <V> V hentEntitet(Class<V> cls, Long primaryKey) {
        return entityManager.find(cls, primaryKey);
    }

    @Test
    public void lagreBeregningsresultatOgUnderliggendeTabellerMedTomEndringsdato() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.empty(), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        var brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        flushAndClear();
        var beregningsresultatLest = hentEntitet(BeregningsresultatEntitet.class, brId);
        var brPeriodeLest = hentEntitet(BeregningsresultatPeriode.class, brPeriodeId);
        var brAndelLest = hentEntitet(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatLest.getId()).isNotNull();
        assertThat(beregningsresultatLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatLest.getEndringsdato()).isEmpty();
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    public void lagreBeregningsresultatOgFeriepenger() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        @SuppressWarnings("unused")
        var andel = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        flushAndClear();
        var hentetResultat = hentEntitet(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetResultat).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertBeregningsresultatPeriode(BeregningsresultatPeriode brPeriodeLest, BeregningsresultatAndel brAndelLest, BeregningsresultatPeriode brPeriodeExpected) {
        assertThat(brPeriodeLest).isEqualTo(brPeriodeExpected);
        assertThat(brPeriodeLest.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(brAndelLest).isEqualTo(brPeriodeExpected.getBeregningsresultatAndelList().get(0));
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeFom()).isEqualTo(brPeriodeExpected.getBeregningsresultatPeriodeFom());
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeTom()).isEqualTo(brPeriodeExpected.getBeregningsresultatPeriodeTom());
    }

    @Test
    public void toBehandlingerKanHaSammeBeregningsresultat() {
        // Arrange
        var behandling2 = opprettBehandling();
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        beregningsresultatRepository.lagre(behandling2, beregningsresultat);

        // Assert
        Optional<BeregningsresultatEntitet> beregningsresultat1 = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        Optional<BeregningsresultatEntitet> beregningsresultat2 = beregningsresultatRepository.hentBgBeregningsresultat(behandling2.getId());
        assertThat(beregningsresultat1).isPresent();
        assertThat(beregningsresultat2).isPresent();
        assertThat(beregningsresultat1).hasValueSatisfying(b -> assertThat(b).isSameAs(beregningsresultat2.get())); // NOSONAR
    }

    @Test
    public void slettBeregningsresultatOgKobling() {
        // Arrange
        var beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        Optional<BehandlingBeregningsresultatEntitet> koblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Act
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), behandlingRepository.taSkriveLås(behandling));

        // Assert
        var hentetBG = entityManager.find(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetBG).isNotNull();

        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        BeregningsresultatPeriode hentetBGPeriode = entityManager.find(BeregningsresultatPeriode.class, beregningsresultatPeriode.getId());
        assertThat(hentetBGPeriode).isNotNull();

        var beregningsresultatAndel = beregningsresultatPeriode.getBeregningsresultatAndelList().get(0);
        var hentetBRAndel = entityManager.find(BeregningsresultatAndel.class, beregningsresultatAndel.getId());
        assertThat(hentetBRAndel).isNotNull();

        Optional<BeregningsresultatEntitet> deaktivertBeregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> deaktivertKobling = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(deaktivertBeregningsresultat).isNotPresent();
        assertThat(deaktivertKobling).isNotPresent();
        assertThat(koblingOpt).hasValueSatisfying(kobling -> assertThat(kobling.erAktivt()).isFalse());
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, boolean medPrivatpersonArbeidsgiver) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(medPrivatpersonArbeidsgiver ? Arbeidsgiver.person(aktørId) : Arbeidsgiver.virksomhet(ORGNR))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medFeriepengerÅrsbeløp(new Beløp(BigDecimal.valueOf(999)))
            .buildFor(beregningsresultatPeriode);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(DAGENSDATO.minusDays(20), DAGENSDATO.minusDays(15))
            .build(beregningsresultat);
    }

    private BeregningsresultatEntitet buildBeregningsresultat(Optional<LocalDate> endringsdato, boolean medPrivatpersonArbeidsgiver) {
        var builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdato.ifPresent(builder::medEndringsdato);
        var beregningsresultat = builder.build();
        var brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode, medPrivatpersonArbeidsgiver);
        return beregningsresultat;
    }

}
