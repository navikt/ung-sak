package no.nav.k9.sak.ytelse.ung.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsresultat.BekreftTilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TilkjentYtelseOppdatererTest {

    @Inject
    public EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste;
    private ArbeidsgiverValidator arbeidsgiverValidator;

    private TilkjentYtelseOppdaterer oppdaterer;
    private Behandling behandling;
    private HistorikkTjenesteAdapter historikkAdapter;

    @BeforeEach
    public void setUp() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        beregnFeriepengerTjeneste = mock(BeregnFeriepengerTjeneste.class);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        arbeidsgiverValidator = mock(ArbeidsgiverValidator.class);

        historikkAdapter = mock(HistorikkTjenesteAdapter.class);
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder();
        when(historikkAdapter.tekstBuilder()).thenReturn(historikkInnslagTekstBuilder);

        oppdaterer = new TilkjentYtelseOppdaterer(repositoryProvider, new UnitTestLookupInstanceImpl<>(beregnFeriepengerTjeneste), arbeidsgiverValidator, historikkAdapter);

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);

        // legg til et nytt vilkårsresultat
        final var vilkårResultatBuilder = Vilkårene.builder();
        final var vilkårResultat = vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.K9_VILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(LocalDate.now(), LocalDate.now().plusDays(7))
                .medUtfallOverstyrt(Utfall.OPPFYLT)
            )
        )
            .build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);

    }

    @Test
    public void lagre_manuell_tilkjent_ytelse() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusDays(1);
        int dagsatsRefusjon = 100;
        int dagsatsBruker = 100;
        BigDecimal utbealingsgrad = BigDecimal.valueOf(100);
        Inntektskategori inntekskategori = Inntektskategori.ARBEIDSAVKLARINGSPENGER;

        var andelBruker = TilkjentYtelseAndelDto.build()
            .medRefusjonsbeløp(dagsatsRefusjon)
            .medBeløpTilSøker(dagsatsBruker)
            .medInntektskategori(inntekskategori)
            .medArbeidsgiverOrgNr(new OrgNummer("910909088"))
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
        assertThat(aggregat.getBgBeregningsresultat().getBeregningsresultatPerioder()).hasSize(1);
        assertThat(aggregat.getBgBeregningsresultat().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()).hasSize(2);

        assertThat(aggregat.getBgBeregningsresultat().getBeregningsresultatPerioder()
            .stream().flatMap(e -> e.getBeregningsresultatAndelList().stream())
            .map(BeregningsresultatAndel::getAktivitetStatus)
        )
            .containsOnly(AktivitetStatus.ARBEIDSAVKLARINGSPENGER);


    }
}
