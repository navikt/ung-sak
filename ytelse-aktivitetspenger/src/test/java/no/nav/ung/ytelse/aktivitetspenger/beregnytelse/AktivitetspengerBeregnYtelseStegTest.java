package no.nav.ung.ytelse.aktivitetspenger.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektTjeneste;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.Kontrollresultat;
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
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AktivitetspengerBeregnYtelseStegTest {

    private static final Long BEHANDLING_ID = 1L;
    private static final LocalDate FOM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 7, 31);

    @Test
    void skal_ikke_tilkjenne_første_måned_før_inntekten_er_kontrollert() {
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(FOM);
        var satser = AktivitetspengerBrevScenarioerUtils.lagSatserTidslinje(
            scenario.satsperioder().mapValue(AktivitetspengerSatsPeriode::satsGrunnlag), scenario.beregningsgrunnlag());
        var grunnlagRepository = mock(AktivitetspengerGrunnlagRepository.class);
        var grunnlag = mock(AktivitetspengerGrunnlag.class);
        when(grunnlagRepository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
        when(grunnlag.hentAktivitetspengerSatsTidslinje()).thenReturn(satser);
        var tilkjentRepository = mock(TilkjentYtelseRepository.class);
        when(tilkjentRepository.hentKontrollerInntektTidslinje(BEHANDLING_ID)).thenReturn(LocalDateTimeline.empty());
        var perioder = mock(MånedsvisTidslinjeUtleder.class);
        when(perioder.finnMånedsvisPeriodisertePerioder(BEHANDLING_ID)).thenReturn(new LocalDateTimeline<>(FOM, TOM, YearMonth.from(FOM)));
        var steg = new AktivitetspengerBeregnYtelseSteg(grunnlagRepository, tilkjentRepository, perioder);
        var behandling = Behandling.forFørstegangssøknad(Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, AktørId.dummy())).build();

        steg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(BEHANDLING_ID)));

        ArgumentCaptor<LocalDateTimeline<TilkjentYtelseVerdi>> resultat = ArgumentCaptor.captor();
        verify(tilkjentRepository).lagre(eq(BEHANDLING_ID), resultat.capture(), anyString(), anyString());
        assertThat(resultat.getValue().isEmpty()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = InntektYtelseType.class, names = {"SYKEPENGER", "OMSORGSPENGER", "PLEIEPENGER", "OPPLÆRINGSPENGER"})
    void skal_avkorte_mot_arbeidsinntekt_og_ytelse_etter_bekreftelse_uten_uttalelse(InntektYtelseType ytelseType) {
        // Arrange
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(FOM);
        var satser = AktivitetspengerBrevScenarioerUtils.lagSatserTidslinje(
            scenario.satsperioder().mapValue(AktivitetspengerSatsPeriode::satsGrunnlag), scenario.beregningsgrunnlag());
        var uredusertBeløp = scenario.tilkjentYtelsePerioder().segmenter().getFirst().getValue().uredusertBeløp();
        var behandling = Behandling.forFørstegangssøknad(Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, AktørId.dummy())).build();
        var behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
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
        when(iayTjeneste.finnGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(iayGrunnlag));
        when(iayTjeneste.hentGrunnlagForGrunnlagId(BEHANDLING_ID, iayGrunnlag.getEksternReferanse())).thenReturn(iayGrunnlag);
        var mapper = new RapportertInntektMapper(iayTjeneste, behandlingRepository, new RegisterinntektKilde(true));
        var relevantTidslinje = new LocalDateTimeline<>(FOM, TOM, true);
        var rapporterteInntekter = mapper.mapAlleGjeldendeRegisterOgBrukersInntekter(BEHANDLING_ID, relevantTidslinje);
        var etterlysninger = mapper.finnRegisterinntekterForEtterlysninger(BEHANDLING_ID, List.of(
            new InntektskontrollEtterlysningsPeriode(relevantTidslinje.getLocalDateIntervals().getFirst(),
                new InntektskontrollEtterlysningInfo(EtterlysningStatus.MOTTATT_SVAR, false),
                iayGrunnlag.getEksternReferanse())));
        var tilkjentYtelseRepository = mock(TilkjentYtelseRepository.class);
        var kontrollTjeneste = new KontrollerteInntektperioderTjeneste(tilkjentYtelseRepository, mock(RelevanteKontrollperioderUtleder.class));

        // Act
        var kontrollresultat = new KontrollerInntektTjeneste(BigDecimal.valueOf(15)).utførKontroll(
            new KontrollerInntektInput(relevantTidslinje, relevantTidslinje, rapporterteInntekter, etterlysninger));
        assertThat(kontrollresultat.segmenter()).allSatisfy(segment ->
            assertThat(segment.getValue().type()).isEqualTo(KontrollResultatType.FERDIG_KONTROLLERT));
        kontrollTjeneste.opprettKontrollerteInntekterPerioderFraBruker(BEHANDLING_ID,
            kontrollresultat.mapValue(Kontrollresultat::inntektsresultat), rapporterteInntekter, "input", "sporing");
        ArgumentCaptor<List<KontrollertInntektPeriode>> perioderCaptor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagreKontrollertePerioder(eq(BEHANDLING_ID), perioderCaptor.capture(), anyString(), anyString());
        var kontrollertPeriode = perioderCaptor.getValue().getFirst();
        when(tilkjentYtelseRepository.hentKontrollerInntektTidslinje(BEHANDLING_ID)).thenReturn(
            new LocalDateTimeline<>(FOM, TOM, new KontrollerteInntekter(kontrollertPeriode.getInntekt(), kontrollertPeriode.getYtelse())));

        var grunnlagRepository = mock(AktivitetspengerGrunnlagRepository.class);
        var grunnlag = mock(AktivitetspengerGrunnlag.class);
        when(grunnlagRepository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
        when(grunnlag.hentAktivitetspengerSatsTidslinje()).thenReturn(satser);
        var månedsvisUtleder = mock(MånedsvisTidslinjeUtleder.class);
        when(månedsvisUtleder.finnMånedsvisPeriodisertePerioder(BEHANDLING_ID)).thenReturn(
            new LocalDateTimeline<>(FOM, TOM, YearMonth.of(2026, 7)));
        var steg = new AktivitetspengerBeregnYtelseSteg(grunnlagRepository, tilkjentYtelseRepository, månedsvisUtleder);
        steg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(BEHANDLING_ID)));

        // Assert
        assertThat(kontrollertPeriode.getErManueltVurdert()).isFalse();
        assertThat(kontrollertPeriode.getKilde()).isEqualTo(KontrollertInntektKilde.REGISTER);
        assertThat(kontrollertPeriode.getInntekt()).isEqualByComparingTo("5000");
        assertThat(kontrollertPeriode.getYtelse()).isEqualByComparingTo("3000");
        ArgumentCaptor<LocalDateTimeline<TilkjentYtelseVerdi>> ytelseCaptor = ArgumentCaptor.captor();
        verify(tilkjentYtelseRepository).lagre(eq(BEHANDLING_ID), ytelseCaptor.capture(), anyString(), anyString());
        var verdi = ytelseCaptor.getValue().segmenter().getFirst().getValue();
        assertThat(verdi.reduksjon()).isEqualByComparingTo("5280");
        assertThat(verdi.uredusertBeløp()).isEqualByComparingTo(uredusertBeløp);
        assertThat(verdi.redusertBeløp()).isEqualByComparingTo(uredusertBeløp.subtract(new BigDecimal("5280")));
    }
}
