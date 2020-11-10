package no.nav.k9.sak.domene.vedtak.ekstern;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Instance;

import org.junit.Rule;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.arbeidsforhold.TemaUnderkategori;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.beregningsresultat.BeregningsresultatProvider;
import no.nav.k9.sak.ytelse.beregning.beregningsresultat.DefaultBeregningsresultatProvider;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class VurderOmArenaYtelseSkalOpphøreTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final Repository repository = repoRule.getRepository();
    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());

    private Instance<BeregningsresultatProvider> beregningsresultatProvidere = new UnitTestLookupInstanceImpl<>(new DefaultBeregningsresultatProvider(repositoryProvider));

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String SAK_ID = "1200095";
    private static Long MELDEKORTPERIODE = 14L;

    private final LocalDate stp = LocalDate.parse("2020-08-08");
    private final TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(AKTØR_ID);
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphør = new VurderOmArenaYtelseSkalOpphøre(
        behandlingRepository,
        iayTjeneste, behandlingVedtakRepository, null, beregningsresultatProvidere);

    private Behandling behandling;

    private BeregningsresultatEntitet.Builder beregningsresultatBuilder;
    private BeregningsresultatPeriode.Builder brPeriodebuilder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_teste_arena_ytelser_finnes_ikke() {
        // Arrange
        LocalDate vedtaksDato = stp.minusDays(7);
        LocalDate startDato = stp;
        byggScenarioUtenYtelseIArena();
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    // T1: Siste utbetalingsdato for ARENA-ytelse før vedtaksdato for foreldrepenger
    // T2: Første forventede utbetalingsdato for ARENA-ytelse etter vedtaksdato for foreldrepenger

    @Test
    public void skal_teste_startdato_før_T1() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        LocalDate meldekortT1 = stp.plusDays(2);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = stp.plusDays(36);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_før_T1_FEIL_3526() {
        // Arrange
        // Startdato før T1 , vedtaksdato etter T1
        LocalDate meldekortT1 = LocalDate.of(2018, 8, 12);
        LocalDate ytelseVedtakFOM = LocalDate.of(2018, 7, 29);
        LocalDate ytelseVedtakTOM = LocalDate.of(2018, 8, 26);
        LocalDate vedtaksDato = LocalDate.of(2018, 11, 14);
        LocalDate startDato = LocalDate.of(2018, 4, 6);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_etter_T2() {
        // Arrange
        // Startdato før T2, vedtaksdato etter T2
        LocalDate meldekortT1 = stp.minusDays(64);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(19);
        LocalDate vedtaksDato = stp.minusDays(49);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdato_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_etter_T1() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom T1 og (T1 + 8 dager)
        LocalDate meldekortT1 = stp.minusDays(5);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = stp.minusDays(4);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdato_mellom_T1_T2_vedtaksdato_mindre_enn_8_dager_før_T2() {
        // Arrange
        // startdato mellom T1 og T2, vedtaksdato mellom (T2 - 8 dager) og T2
        LocalDate meldekortT1 = stp.minusDays(8);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = stp.plusDays(4);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_Arena_ytelse_interval_før_vedtaksdato_fom_overlapper_() {
        // Arrange
        // Arena ytelser etter startdato men før vedtaksdato .
        LocalDate meldekortT1 = stp.plusDays(MELDEKORTPERIODE + 7);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE);
        LocalDate vedtaksDato = stp.plusDays(47);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_er_like_T1_og_T2_er_null() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato og sluttdato .
        LocalDate meldekortT1 = stp;
        LocalDate ytelseVedtakFOM = stp.minusDays(MELDEKORTPERIODE * 2);
        LocalDate ytelseVedtakTOM = stp.minusDays(1);
        LocalDate vedtaksDato = stp.plusDays(47);
        LocalDate startDato = stp.minusDays(1);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_vedtaksdato_er_like_T2() {
        // Arrange
        // Arena ytelser før vedtaksdato og mellom startdato og sluttdato .
        LocalDate meldekortT1 = stp.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = stp;
        LocalDate startDato = stp.plusDays(10);
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_teste_startdato_før_T1_og__overlapper_ikke_ARENA_ytelse() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato og sluttdato .
        LocalDate meldekortT1 = stp.plusDays(7);
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE);
        LocalDate ytelseVedtakTOM = stp.plusDays(MELDEKORTPERIODE * 2);
        LocalDate vedtaksDato = stp.plusDays(54);
        LocalDate startDato = stp;
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_teste_startdato_for_5910() {
        // Arrange
        // Arena ytelser før vedtaksdato og utenfor startdato og sluttdato .
        LocalDate meldekortT1 = stp.minusDays(5); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE); // 2019-01-21
        LocalDate ytelseVedtakTOM = stp.plusMonths(6);// 2019-10-01
        LocalDate vedtaksDato = stp.plusDays(7); // 2019-02-15
        LocalDate startDato = stp; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ytelse_avsluttet_før_stp_siste_meldekort_rett_etter() {
        // Arrange
        // Siste meldekort vil som regel komme rett etter perioden og med prosent <200
        LocalDate meldekortT1 = stp.plusDays(2); // 2019-02-03
        LocalDate ytelseVedtakFOM = stp.minusDays(MELDEKORTPERIODE * 2 - 1); // 2019-01-21
        LocalDate ytelseVedtakTOM = stp.minusDays(1);// 2019-10-01
        LocalDate vedtaksDato = stp.plusDays(7); // 2019-02-15
        LocalDate startDato = stp; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void vanlig_case_vedtak_før_start() {
        // Arrange
        // Gir arena nok tid til å avslutte løpende ytelse
        LocalDate meldekortT1 = stp.minusDays(16); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        LocalDate vedtaksDato = stp.minusDays(14); // 2019-02-15
        LocalDate startDato = stp; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void vanlig_case_vedtak_rett_før_start() {
        // Arrange
        // Potensielt for liten tid til å avslutte løpende ytelse
        LocalDate meldekortT1 = stp.minusDays(10); // 2019-02-03
        LocalDate ytelseVedtakFOM = meldekortT1.minusDays(MELDEKORTPERIODE * 8); // 2019-01-21
        LocalDate ytelseVedtakTOM = meldekortT1.plusDays(MELDEKORTPERIODE * 8);// 2019-10-01
        LocalDate vedtaksDato = stp.minusDays(4); // 2019-02-15
        LocalDate startDato = stp; // 2019-02-08
        byggScenario(ytelseVedtakFOM, ytelseVedtakTOM, meldekortT1, vedtaksDato, startDato, Fagsystem.ARENA);
        // Act
        boolean resultat = vurdereOmArenaYtelseSkalOpphør.vurderArenaYtelserOpphøres(behandling.getId(), behandling.getAktørId(), startDato, vedtaksDato);
        // Assert
        assertThat(resultat).isTrue();
    }

    private void byggScenarioUtenYtelseIArena() {
        byggScenario(stp, stp.plusDays(15), stp, stp, stp, Fagsystem.INFOTRYGD);
    }

    private void byggScenario(LocalDate ytelserFom,
                              LocalDate ytelserTom,
                              LocalDate t1,
                              LocalDate vedtaksdato,
                              LocalDate startdato,
                              Fagsystem fagsystem) {
        byggScenario(ytelserFom, ytelserTom, t1, vedtaksdato.atStartOfDay(),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusDays(90)), fagsystem);
    }

    private void byggScenario(LocalDate ytelserFom,
                              LocalDate ytelserTom,
                              LocalDate t1,
                              LocalDateTime vedtakstidspunkt,
                              DatoIntervallEntitet intervall,
                              Fagsystem fagsystem) {
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        behandling = lagre(scenario);

        // Legg til ytelse
        InntektArbeidYtelseAggregatBuilder aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØR_ID);
        aktørYtelseBuilder.leggTilYtelse(byggYtelser(ytelserFom, ytelserTom, t1, fagsystem));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        // Legg til beregningresultat
        beregningsresultatBuilder = BeregningsresultatEntitet.builder();
        brPeriodebuilder = BeregningsresultatPeriode.builder();
        BeregningsresultatEntitet beregningsresultat = byggBeregningsresultat(intervall.getFomDato(), intervall.getTomDato());
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Legg til behandling resultat
        repository.lagre(behandling);
        repository.flushAndClear();

        // Legg til vedtak
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medAnsvarligSaksbehandler("asdf").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));
    }

    private YtelseBuilder byggYtelser(LocalDate ytelserFom,
                                      LocalDate ytelserTom,
                                      LocalDate t1,
                                      Fagsystem fagsystem) {
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer(SAK_ID))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ytelserFom, ytelserTom))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(FagsakYtelseType.DAGPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT);
        byggYtelserAnvist(ytelserFom, ytelserTom, t1, ytelseBuilder).forEach(
            ytelseAnvist -> {
                ytelseBuilder.medYtelseAnvist(ytelseAnvist);
            });
        return ytelseBuilder;
    }

    private List<YtelseAnvist> byggYtelserAnvist(LocalDate yaFom,
                                                 @SuppressWarnings("unused") LocalDate yaTom,
                                                 LocalDate t1,
                                                 YtelseBuilder ytelseBuilder) {
        // Man må sende meldekort hver 2 uker.
        final long ytelseDagerMellomrom = 13;
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        LocalDate fom = yaFom;
        LocalDate tom = yaFom.plusDays(ytelseDagerMellomrom);
        do {
            YtelseAnvist ya = ytelseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100L))
                .medBeløp(BigDecimal.valueOf(30000L))
                .medDagsats(BigDecimal.valueOf(1000L))
                .build();
            ytelseAnvistList.add(ya);
            fom = tom.plusDays(1);
            tom = fom.plusDays(ytelseDagerMellomrom);
        } while (tom.isBefore(t1));

        return ytelseAnvistList;
    }

    private BeregningsresultatEntitet byggBeregningsresultat(LocalDate fom, LocalDate tom) {
        BeregningsresultatEntitet beregningsresultat = beregningsresultatBuilder
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        byggBeregningsresultatPeriode(beregningsresultat, fom, tom);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode byggBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat,
                                                                    LocalDate fom, LocalDate tom) {
        var brPeriode = brPeriodebuilder
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medDagsats(10)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(10)
            .medBrukerErMottaker(true)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .buildFor(brPeriode);
        return brPeriode;
    }
}
