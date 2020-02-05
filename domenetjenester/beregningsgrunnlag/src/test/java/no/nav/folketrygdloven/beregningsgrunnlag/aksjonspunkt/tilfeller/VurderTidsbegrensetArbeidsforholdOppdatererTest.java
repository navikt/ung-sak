package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderFaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderteArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class VurderTidsbegrensetArbeidsforholdOppdatererTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());


    @Inject
    private FaktaOmBeregningTilfellerOppdaterer faktaOmBeregningTilfellerOppdaterer;



    private List<VurderteArbeidsforholdDto> tidsbestemteArbeidsforhold;
    private final long FØRSTE_ANDELSNR = 1L;
    private final long ANDRE_ANDELSNR = 2L;
    private final long TREDJE_ANDELSNR = 3L;
    private final LocalDate FOM = LocalDate.now().minusDays(100);
    private final LocalDate TOM = LocalDate.now();
    private final List<VirksomhetEntitet> virksomheter = new ArrayList<>();
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Before
    public void setup() {
        virksomheter.add(new VirksomhetEntitet.Builder()
                .medOrgnr("123")
                .medNavn("VirksomhetNavn1")
                .oppdatertOpplysningerNå()
                .build());
        virksomheter.add(new VirksomhetEntitet.Builder()
                .medOrgnr("456")
                .medNavn("VirksomhetNavn2")
                .oppdatertOpplysningerNå()
                .build());
        virksomheter.add(new VirksomhetEntitet.Builder()
                .medOrgnr("789")
                .medNavn("VirksomhetNavn3")
                .oppdatertOpplysningerNå()
                .build());
        virksomheter.forEach(v -> repositoryProvider.getVirksomhetRepository().lagre(v));
        tidsbestemteArbeidsforhold = lagFastsatteAndelerListe();


    }

    private List<VurderteArbeidsforholdDto> lagFastsatteAndelerListe() {

        VurderteArbeidsforholdDto førsteForhold = new VurderteArbeidsforholdDto(
            FØRSTE_ANDELSNR,
            true,
            null
        );

        VurderteArbeidsforholdDto andreForhold = new VurderteArbeidsforholdDto(
            ANDRE_ANDELSNR,
            false,
            null
        );

        VurderteArbeidsforholdDto tredjeForhold = new VurderteArbeidsforholdDto(
            TREDJE_ANDELSNR,
            true,
            null
        );

        return new ArrayList<>(List.of(førsteForhold, andreForhold, tredjeForhold));
    }


    @Test
    public void skal_markere_korrekte_andeler_som_tidsbegrenset() {
        //Arrange
        lagBehandlingMedBeregningsgrunnlag();

        //Dto
        var vurderFaktaOmBeregningDto = new VurderFaktaOmBeregningDto("begrunnelse",
            new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD),
            new VurderTidsbegrensetArbeidsforholdDto( tidsbestemteArbeidsforhold)));


        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlag.dypKopi();
        faktaOmBeregningTilfellerOppdaterer.oppdater(vurderFaktaOmBeregningDto.getFakta(), null, nyttBeregningsgrunnlag, Optional.empty());

        //Assert
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler.get(0).getBgAndelArbeidsforhold().get().getErTidsbegrensetArbeidsforhold()).isTrue();
        assertThat(andeler.get(1).getBgAndelArbeidsforhold().get().getErTidsbegrensetArbeidsforhold()).isFalse();
        assertThat(andeler.get(2).getBgAndelArbeidsforhold().get().getErTidsbegrensetArbeidsforhold()).isTrue();
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Virksomhet virksomhet) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

    private void lagBehandlingMedBeregningsgrunnlag() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .build();

        BeregningsgrunnlagPeriode periode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag,
            FOM, TOM);
        buildBgPrStatusOgAndel(periode, virksomheter.get(0));
        buildBgPrStatusOgAndel(periode, virksomheter.get(1));
        buildBgPrStatusOgAndel(periode, virksomheter.get(2));

        scenario.lagre(repositoryProvider);
    }
}
