package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.historikk.HistorikkInnslagKonverter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VurderTilbaketrekkDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VurderTilbaketrekkOppdatererTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;

    private Behandling behandling;
    private HistorikkTjenesteAdapter historikkAdapter;
    private VurderTilbaketrekkOppdaterer vurderTilbaketrekkOppdaterer;
    private BeregningsresultatRepository beregningsresultatRepository;

    @SuppressWarnings("unused")
    private BeregningsresultatEntitet beregningsresultat;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        HistorikkInnslagKonverter historikkInnslagKonverter = new HistorikkInnslagKonverter();
        historikkAdapter = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(), historikkInnslagKonverter, null);
        vurderTilbaketrekkOppdaterer = new VurderTilbaketrekkOppdaterer(repositoryProvider, historikkAdapter);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        beregningsresultat = buildBeregningsresultatFP();
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_skal_utføres() {
        // Arrange
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", false);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().get()).isFalse();
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_ikke_skal_utføres() {
        // Arrange
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", true);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().get()).isTrue();
    }


    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
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

    private BeregningsresultatEntitet buildBeregningsresultatFP() {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        return beregningsresultat;
    }

}
