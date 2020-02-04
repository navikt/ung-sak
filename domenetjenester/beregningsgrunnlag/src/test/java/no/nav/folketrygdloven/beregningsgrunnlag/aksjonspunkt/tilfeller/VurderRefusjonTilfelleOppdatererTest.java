package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class VurderRefusjonTilfelleOppdatererTest {
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet("973861778");
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private VurderRefusjonTilfelleOppdaterer vurderRefusjonTilfelleOppdaterer;
    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste = mock(InntektsmeldingMedRefusjonTjeneste.class);
    public TestScenarioBuilder scenario;
    private BehandlingReferanse referanse;

    @Before
    public void setUp() {
        vurderRefusjonTilfelleOppdaterer = new VurderRefusjonTilfelleOppdaterer(beregningsgrunnlagRepository, inntektsmeldingMedRefusjonTjeneste);
        this.scenario = TestScenarioBuilder.nyttScenario();
        this.referanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse() {
        // Arrange
        LocalDate førsteMuligDatoMedRefusjonFørAksjonspunkt = lagArbeidsgiverSøktForSent();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, førsteMuligDatoMedRefusjonFørAksjonspunkt);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse() {
        // Arrange
        lagArbeidsgiverSøktForSent();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, SKJÆRINGSTIDSPUNKT);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_false() {
        // Arrange
        LocalDate førsteMuligDatoMedRefusjonFørAksjonspunkt = lagArbeidsgiverSøktForSent();
        lagBeregningsgrunnlagMedOverstyring(førsteMuligDatoMedRefusjonFørAksjonspunkt);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, SKJÆRINGSTIDSPUNKT);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_true() {
        // Arrange
        lagArbeidsgiverSøktForSent();
        lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, SKJÆRINGSTIDSPUNKT);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_ikkje_gyldig() {
        // Arrange
        LocalDate førsteMuligDatoMedRefusjonFørAksjonspunkt = lagArbeidsgiverSøktForSent();
        lagBeregningsgrunnlagMedOverstyring(førsteMuligDatoMedRefusjonFørAksjonspunkt);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, førsteMuligDatoMedRefusjonFørAksjonspunkt);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_gyldig() {
        // Arrange
        LocalDate førsteMuligDatoMedRefusjonFørAksjonspunkt = lagArbeidsgiverSøktForSent();
        lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        vurderRefusjonTilfelleOppdaterer.oppdater(dto, referanse, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty());

        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.getId()).orElseThrow();

        // Assert
        assertOverstyringAvRefusjon(nyttGrunnlag, førsteMuligDatoMedRefusjonFørAksjonspunkt);
    }

    private void assertOverstyringAvRefusjon(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag, LocalDate førsteMuligeDato) {
        assertThat(nyttGrunnlag.getRefusjonOverstyringer()).isPresent();
        BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringer = nyttGrunnlag.getRefusjonOverstyringer().get();
        List<BeregningRefusjonOverstyringEntitet> overstyringer = beregningRefusjonOverstyringer.getRefusjonOverstyringer();
        assertThat(overstyringer.size()).isEqualTo(1);
        assertThat(overstyringer.get(0).getArbeidsgiver()).isEqualTo(VIRKSOMHET);
        assertThat(overstyringer.get(0).getFørsteMuligeRefusjonFom()).isEqualTo(førsteMuligeDato);
    }

    private FaktaBeregningLagreDto lagDto(boolean skalUtvideGyldighet) {
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT));

        RefusjonskravPrArbeidsgiverVurderingDto ref1 = new RefusjonskravPrArbeidsgiverVurderingDto();
        ref1.setArbeidsgiverId(VIRKSOMHET.getIdentifikator());
        ref1.setSkalUtvideGyldighet(skalUtvideGyldighet);
        dto.setRefusjonskravGyldighet(List.of(ref1));
        return dto;
    }

    private LocalDate lagArbeidsgiverSøktForSent() {
        HashSet<Arbeidsgiver> arbeidsgivereSomHarSøktForSent = new HashSet<>();
        LocalDate førsteMuligDatoMedRefusjonFørAksjonspunkt = SKJÆRINGSTIDSPUNKT.plusMonths(1);
        when(inntektsmeldingMedRefusjonTjeneste.finnFørsteLovligeDatoForRefusjonFørOverstyring(referanse, VIRKSOMHET))
            .thenReturn(Optional.of(førsteMuligDatoMedRefusjonFørAksjonspunkt));
        arbeidsgivereSomHarSøktForSent.add(VIRKSOMHET);
        when(inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(any(), any(), any()))
            .thenReturn(arbeidsgivereSomHarSøktForSent);
        return førsteMuligDatoMedRefusjonFørAksjonspunkt;
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlagMedOverstyring(LocalDate dato) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        beregningsgrunnlagRepository.lagre(referanse.getId(), BeregningRefusjonOverstyringerEntitet.builder()
            .leggTilOverstyring(new BeregningRefusjonOverstyringEntitet(VIRKSOMHET, dato)).build());
        return beregningsgrunnlagRepository.lagre(referanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        return beregningsgrunnlagRepository.lagre(referanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }
}
