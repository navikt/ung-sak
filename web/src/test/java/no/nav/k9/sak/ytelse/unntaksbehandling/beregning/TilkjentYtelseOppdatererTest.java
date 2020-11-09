package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BekreftTilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class TilkjentYtelseOppdatererTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste = mock(BeregnFeriepengerTjeneste.class);

    private TilkjentYtelseOppdaterer oppdaterer;
    private Behandling behandling;

    @Before
    public void setUp() {
        oppdaterer = new TilkjentYtelseOppdaterer(repositoryProvider, new UnitTestLookupInstanceImpl<>(beregnFeriepengerTjeneste));

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void lagre_manuell_tilkjent_ytelse() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusDays(1);
        int dagsatsRefusjon = 100;
        int dagsatsBruker = 100;
        BigDecimal utbealingsgrad = BigDecimal.valueOf(100);
        ArbeidsgiverDto arbeidsgiver = new ArbeidsgiverDto("910909088", "GUI-910909088", "Bedriftsnavn");
        AktivitetStatus aktivitetStatus = AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
        Inntektskategori inntekskategori = Inntektskategori.ARBEIDSAVKLARINGSPENGER;

        var andelBruker = TilkjentYtelseAndelDto.build()
            .medErBrukerMottaker(true) //TODO: Fjernes
            .medRefusjon(dagsatsRefusjon)
            .medTilSoker(dagsatsBruker)
            .medAktivitetstatus(aktivitetStatus)
            .medInntektskategori(inntekskategori)
            .medArbeidsgiver(arbeidsgiver)
            .medUtbetalingsgrad(utbealingsgrad)
            .create();

        var ytelsePeriode = TilkjentYtelsePeriodeDto.build(fom, tom)
            .medAndeler(List.of(andelBruker))
            .create();
        var tilkjentYtelseDto = new TilkjentYtelseDto(List.of(ytelsePeriode));

        var dto = new BekreftTilkjentYtelseDto();
        dto.setTilkjentYtelseDto(tilkjentYtelseDto);

        // Act
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        var aggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId()).orElseThrow();
        assertThat(aggregat.getBgBeregningsresultat()).isNotNull();
        assertThat(aggregat.getBgBeregningsresultat().getBeregningsresultatPerioder()).hasSize(0);

        assertThat(aggregat.getOverstyrtBeregningsresultat()).isNotNull();
        assertThat(aggregat.getOverstyrtBeregningsresultat().getBeregningsresultatPerioder()).hasSize(1);
        assertThat(aggregat.getOverstyrtBeregningsresultat().getBeregningsresultatPerioder().get(0)
            .getBeregningsresultatAndelList()).hasSize(2);
    }
}
