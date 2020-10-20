package no.nav.k9.sak.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class BeregningsresultatRepositoryImplTest {

    private static final String ORGNR = "55";

    private static final LocalDate DAGENSDATO = LocalDate.now();

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final Repository repository = repoRule.getRepository();

    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());
    private Behandling behandling;
    private AktørId aktørId;

    private final BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(repoRule.getEntityManager());

    @Before
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
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling ->
            assertThat(brKobling.getBgBeregningsresultat()).isSameAs(beregningsresultat)
        );
    }

    @Test
    public void lagreOgHentUtbetBeregningsresultatAggregatNårUTBETEksisterer() {
        // Arrange
        BeregningsresultatEntitet bgBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        BeregningsresultatEntitet utbetBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO.plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultat);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultat);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling ->
            assertThat(brKobling.getUtbetBeregningsresultat()).isSameAs(utbetBeregningsresultat)
        );
    }

    @Test
    public void lagreOgHenteBeregningsresultat() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatLest = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());

        assertThat(beregningsresultatLest).isEqualTo(Optional.of(beregningsresultat));
    }

    @Test
    public void lagreOgHenteUtbetBeregningsresultat() {
        // Arrange
        BeregningsresultatEntitet bgBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        BeregningsresultatEntitet utbetBeregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO.plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultat);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultat);

        // Assert
        Long id = utbetBeregningsresultat.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> utbetBeregningsresultatLest = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());

        assertThat(utbetBeregningsresultatLest).isEqualTo(Optional.of(utbetBeregningsresultat));
    }

    @Test
    public void lagreOgHenteBeregningsresultatMedPrivatpersonSomArbeidsgiver() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), true);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatLest = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        assertThat(beregningsresultatLest).isEqualTo(Optional.of(beregningsresultat));
        assertThat(beregningsresultatLest).isPresent();
        Arbeidsgiver arbeidsgiver = beregningsresultatLest.get().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0).getArbeidsgiver().get();//NOSONAR
        assertThat(arbeidsgiver.getAktørId()).isEqualTo(aktørId);
        assertThat(arbeidsgiver.getIdentifikator()).isEqualTo(aktørId.getId());
    }

    @Test
    public void lagreBeregningsresultatOgUnderliggendeTabellerMedEndringsdatoLikDagensDato() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        BeregningsresultatPeriode brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        repository.flushAndClear();
        BeregningsresultatEntitet beregningsresultatLest = repository.hent(BeregningsresultatEntitet.class, brId);
        BeregningsresultatPeriode brPeriodeLest = repository.hent(BeregningsresultatPeriode.class, brPeriodeId);
        BeregningsresultatAndel brAndelLest = repository.hent(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatLest.getId()).isNotNull();
        assertThat(beregningsresultatLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatLest.getEndringsdato()).isEqualTo(Optional.of(DAGENSDATO));
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    public void lagreBeregningsresultatOgUnderliggendeTabellerMedTomEndringsdato() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.empty(), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        BeregningsresultatPeriode brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        repository.flushAndClear();
        BeregningsresultatEntitet beregningsresultatLest = repository.hent(BeregningsresultatEntitet.class, brId);
        BeregningsresultatPeriode brPeriodeLest = repository.hent(BeregningsresultatPeriode.class, brPeriodeId);
        BeregningsresultatAndel brAndelLest = repository.hent(BeregningsresultatAndel.class, brAndelId);

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
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        BeregningsresultatAndel andel = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0);
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(LocalDate.now().withMonth(12).withDayOfMonth(31))
            .medÅrsbeløp(300L)
            .buildFor(andel);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        repository.flushAndClear();
        BeregningsresultatEntitet hentetResultat = repository.hent(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetResultat).isNotNull();
        assertThat(hentetResultat.getBeregningsresultatFeriepengerPrÅrListe()).isNotEmpty();
        assertThat(hentetResultat.getBeregningsresultatFeriepengerPrÅrListe()).allSatisfy(this::assertFeriepenger);
    }

    private void assertFeriepenger(BeregningsresultatFeriepengerPrÅr prÅr) {
        assertThat(prÅr).satisfies(val -> {
            assertThat(val.getBeregningsresultatAndel()).isNotNull();
            assertThat(val.getOpptjeningsår()).isNotNull();
            assertThat(val.getÅrsbeløp()).isNotNull();
        });
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
        Behandling behandling2 = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        beregningsresultatRepository.lagre(behandling2, beregningsresultat);

        // Assert
        Optional<BeregningsresultatEntitet> beregningsresultat1 = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        Optional<BeregningsresultatEntitet> beregningsresultat2 = beregningsresultatRepository.hentBgBeregningsresultat(behandling2.getId());
        assertThat(beregningsresultat1).isPresent();
        assertThat(beregningsresultat2).isPresent();
        assertThat(beregningsresultat1).hasValueSatisfying(b -> assertThat(b).isSameAs(beregningsresultat2.get())); //NOSONAR
    }

    @Test
    public void slettBeregningsresultatOgKobling() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultat(Optional.of(DAGENSDATO), false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        Optional<BehandlingBeregningsresultatEntitet> koblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Act
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), behandlingRepository.taSkriveLås(behandling));

        //Assert
        BeregningsresultatEntitet hentetBG = repoRule.getEntityManager().find(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetBG).isNotNull();

        BeregningsresultatPeriode beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        BeregningsresultatPeriode hentetBGPeriode = repoRule.getEntityManager().find(BeregningsresultatPeriode.class, beregningsresultatPeriode.getId());
        assertThat(hentetBGPeriode).isNotNull();

        BeregningsresultatAndel beregningsresultatAndel = beregningsresultatPeriode.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel hentetBRAndel = repoRule.getEntityManager().find(BeregningsresultatAndel.class, beregningsresultatAndel.getId());
        assertThat(hentetBRAndel).isNotNull();

        Optional<BeregningsresultatEntitet> deaktivertBeregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> deaktivertKobling = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(deaktivertBeregningsresultat).isNotPresent();
        assertThat(deaktivertKobling).isNotPresent();
        assertThat(koblingOpt).hasValueSatisfying(kobling ->
            assertThat(kobling.erAktivt()).isFalse());
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
            .buildFor(beregningsresultatPeriode);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .build(beregningsresultat);
    }

    private BeregningsresultatEntitet buildBeregningsresultat(Optional<LocalDate> endringsdato, boolean medPrivatpersonArbeidsgiver) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdato.ifPresent(builder::medEndringsdato);
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode, medPrivatpersonArbeidsgiver);
        return beregningsresultat;
    }

}
