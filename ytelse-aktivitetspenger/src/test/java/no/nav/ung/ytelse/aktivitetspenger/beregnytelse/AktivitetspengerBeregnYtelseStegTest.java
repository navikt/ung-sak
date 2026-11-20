package no.nav.ung.ytelse.aktivitetspenger.beregnytelse;

import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.domene.iay.modell.VersjonType;
import no.nav.ung.sak.kontroll.InntektskontrollEtterlysningInfo;
import no.nav.ung.sak.kontroll.InntektskontrollEtterlysningsPeriode;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.kontroll.RegisterinntektKilde;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektTjeneste;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.Kontrollresultat;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerInntektskontrollFritakUtleder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerBeregnYtelseStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 7, 31);

    @Inject
    private EntityManager entityManager;

    @Test
    void skal_ikke_tilkjenne_første_måned_før_inntekten_er_kontrollert() {
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(FOM);
        var repositories = repositories();
        var behandling = lagreScenario(scenario, repositories);
        var månedsvisUtleder = mock(MånedsvisTidslinjeUtleder.class);
        when(månedsvisUtleder.finnMånedsvisPeriodisertePerioder(behandling.getId()))
            .thenReturn(new LocalDateTimeline<>(FOM, TOM, YearMonth.from(FOM)));
        var steg = new AktivitetspengerBeregnYtelseSteg(
            repositories.aktivitetspengerGrunnlagRepository(),
            repositories.tilkjentYtelseRepository(),
            månedsvisUtleder,
            new AktivitetspengerInntektskontrollFritakUtleder());

        steg.utførSteg(kontekst(behandling));

        assertThat(repositories.tilkjentYtelseRepository().hentTidslinje(behandling.getId()).isEmpty()).isTrue();
    }

    @Test
    void skal_avkorte_mot_arbeidsinntekt_og_sykepenger_etter_bekreftelse_uten_uttalelse() {
        skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType.SYKEPENGER);
    }

    @Test
    void skal_avkorte_mot_arbeidsinntekt_og_omsorgspenger_etter_bekreftelse_uten_uttalelse() {
        skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType.OMSORGSPENGER);
    }

    @Test
    void skal_avkorte_mot_arbeidsinntekt_og_pleiepenger_etter_bekreftelse_uten_uttalelse() {
        skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType.PLEIEPENGER);
    }

    @Test
    void skal_avkorte_mot_arbeidsinntekt_og_opplæringspenger_etter_bekreftelse_uten_uttalelse() {
        skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType.OPPLÆRINGSPENGER);
    }

    private void skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType ytelseType) {
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(FOM);
        var repositories = repositories();
        var behandling = lagreScenario(scenario, repositories);
        var behandlingId = behandling.getId();
        var uredusertBeløp = scenario.tilkjentYtelsePerioder().segmenter().getFirst().getValue().uredusertBeløp();
        var iayTjeneste = mock(InntektArbeidYtelseTjeneste.class);
        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var inntekter = register.getInntekterBuilder();
        inntekter.leggTilInntekt(InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_AKTIVITETSPENGER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("973000000"))
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medPeriode(FOM, TOM)
                .medInntektspostType(InntektspostType.LØNN)
                .medBeløp(BigDecimal.valueOf(5000))));
        inntekter.leggTilInntekt(InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_AKTIVITETSPENGER)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medPeriode(FOM, TOM)
                .medInntektspostType(InntektspostType.YTELSE)
                .medInntektYtelse(ytelseType)
                .medBeløp(BigDecimal.valueOf(3000))));
        register.leggTilInntekter(inntekter);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(register).build();
        when(iayTjeneste.finnGrunnlag(behandlingId)).thenReturn(Optional.of(iayGrunnlag));
        when(iayTjeneste.hentGrunnlagForGrunnlagId(behandlingId, iayGrunnlag.getEksternReferanse())).thenReturn(iayGrunnlag);
        var mapper = new RapportertInntektMapper(iayTjeneste, repositories.repositoryProvider().getBehandlingRepository(), new RegisterinntektKilde(true));
        var relevantTidslinje = new LocalDateTimeline<>(FOM, TOM, true);
        var rapporterteInntekter = mapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandlingId, relevantTidslinje);
        var etterlysninger = mapper.finnRegisterinntekterForEtterlysninger(behandlingId, List.of(
            new InntektskontrollEtterlysningsPeriode(relevantTidslinje.getLocalDateIntervals().getFirst(),
                new InntektskontrollEtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false),
                iayGrunnlag.getEksternReferanse())));
        var kontrollTjeneste = new KontrollerteInntektperioderTjeneste(
            repositories.tilkjentYtelseRepository(), mock(RelevanteKontrollperioderUtleder.class));

        var kontrollresultat = new KontrollerInntektTjeneste(BigDecimal.valueOf(15)).utførKontroll(
            new KontrollerInntektInput(relevantTidslinje, relevantTidslinje, rapporterteInntekter, etterlysninger));
        assertThat(kontrollresultat.segmenter()).allSatisfy(segment ->
            assertThat(segment.getValue().type()).isEqualTo(KontrollResultatType.FERDIG_KONTROLLERT));
        kontrollTjeneste.opprettKontrollerteInntekterPerioderFraBruker(behandlingId,
            kontrollresultat.mapValue(Kontrollresultat::inntektsresultat), rapporterteInntekter, "input", "sporing");
        var kontrollertPeriode = repositories.tilkjentYtelseRepository()
            .hentKontrollertInntektPerioder(behandlingId).orElseThrow().getPerioder().getFirst();

        var månedsvisUtleder = mock(MånedsvisTidslinjeUtleder.class);
        when(månedsvisUtleder.finnMånedsvisPeriodisertePerioder(behandlingId))
            .thenReturn(new LocalDateTimeline<>(FOM, TOM, YearMonth.of(2026, 7)));
        var steg = new AktivitetspengerBeregnYtelseSteg(
            repositories.aktivitetspengerGrunnlagRepository(),
            repositories.tilkjentYtelseRepository(),
            månedsvisUtleder,
            new AktivitetspengerInntektskontrollFritakUtleder());

        steg.utførSteg(kontekst(behandling));

        assertThat(kontrollertPeriode.getErManueltVurdert()).isFalse();
        assertThat(kontrollertPeriode.getKilde()).isEqualTo(KontrollertInntektKilde.REGISTER);
        assertThat(kontrollertPeriode.getInntekt()).isEqualByComparingTo("5000");
        assertThat(kontrollertPeriode.getYtelse()).isEqualByComparingTo("3000");
        var verdi = repositories.tilkjentYtelseRepository().hentTidslinje(behandlingId).segmenter().getFirst().getValue();
        assertThat(verdi.reduksjon()).isEqualByComparingTo("5280");
        assertThat(verdi.uredusertBeløp()).isEqualByComparingTo(uredusertBeløp);
        assertThat(verdi.redusertBeløp()).isEqualByComparingTo(uredusertBeløp.subtract(new BigDecimal("5280")));
    }

    private AktivitetspengerTestRepositories repositories() {
        return AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null);
    }

    private static Behandling lagreScenario(
        AktivitetspengerTestScenario scenario,
        AktivitetspengerTestRepositories repositories) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(scenario.søknadsperioder().getFirst().getFom())
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);
    }

    private static BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            new BehandlingLås(behandling.getId()));
    }
}
