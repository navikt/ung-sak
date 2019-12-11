package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.ATogFLISammeOrganisasjonDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.AktivitetTomDatoMappingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.AndelMedBeløpDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.AvklarAktiviteterDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.KortvarigeArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.KunYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.TilstøtendeYtelseAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.VurderBesteberegningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

@SuppressWarnings("unchecked")
public class FaktaOmBeregningDtoTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repoRule.getEntityManager());

    private FaktaOmBeregningDtoTjeneste faktaOmBeregningDtoTjeneste;
    private TestScenarioBuilder scenario;
    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setUp() {
        scenario = TestScenarioBuilder.nyttScenario();
        Instance<FaktaOmBeregningTilfelleDtoTjeneste> tjenesteInstances = mock(Instance.class);
        List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester = new ArrayList<>();
        tjenester.add(lagDtoTjenesteMock(setBesteberegingAndelConsumer()));
        tjenester.add(lagDtoTjenesteMock(setFrilansAndelConsumer()));
        tjenester.add(lagDtoTjenesteMock(atflSammeOrgConsumer()));
        tjenester.add(lagDtoTjenesteMock(kunYtelseConsumer()));
        tjenester.add(lagDtoTjenesteMock(kortvarigeArbeidsforholdConsumer()));
        tjenester.add(lagDtoTjenesteMock(vurderLønnsendringConsumer()));
        tjenester.add(lagDtoTjenesteMock(vurderBesteberegningConsumer()));
        when(tjenesteInstances.iterator()).thenReturn(tjenester.iterator());
        when(tjenesteInstances.stream()).thenReturn(tjenester.stream());
        AndelerForFaktaOmBeregningTjeneste andelerForFaktaOmBeregningTjeneste = mock(AndelerForFaktaOmBeregningTjeneste.class);
        faktaOmBeregningDtoTjeneste = new FaktaOmBeregningDtoTjeneste(beregningsgrunnlagRepository,
            tjenesteInstances, lagAvklarAktiviteterDtoTjenesteMock(), andelerForFaktaOmBeregningTjeneste);
    }

    @Test
    public void skal_kalle_dto_tjenester() {
        List<FaktaOmBeregningTilfelle> tilfeller = List.of(
            FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL,
            FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON,
            FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE,
            FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD,
            FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING,
            FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE,
            FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING);
        lagBehandlingMedBgMedTilfeller(tilfeller);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(getBeregningsgrunnlagGrunnlag());
        Optional<FaktaOmBeregningDto> dto = faktaOmBeregningDtoTjeneste.lagDto(input);
        assertThat(dto.orElseThrow().getBesteberegningAndeler()).hasSize(1);
        assertThat(dto.get().getFrilansAndel().getAndelsnr()).isEqualTo(1);
        assertThat(dto.get().getArbeidstakerOgFrilanserISammeOrganisasjonListe()).hasSize(1);
        assertThat(dto.get().getKunYtelse().getAndeler()).hasSize(1);
        assertThat(dto.get().getKortvarigeArbeidsforhold()).hasSize(1);
        assertThat(dto.get().getArbeidsforholdMedLønnsendringUtenIM()).hasSize(1);
    }

    @Test
    public void skal_lage_fakta_om_beregning_dto_når_man_har_tilfeller_i_fakta_om_beregning() {
        List<FaktaOmBeregningTilfelle> tilfeller = Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
        lagBehandlingMedBgMedTilfeller(tilfeller);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(getBeregningsgrunnlagGrunnlag());

        Optional<FaktaOmBeregningDto> dto = faktaOmBeregningDtoTjeneste.lagDto(input);
        assertThat(dto.isPresent()).isTrue();
    }

    private BeregningsgrunnlagGrunnlagEntitet getBeregningsgrunnlagGrunnlag() {
        return repositoryProvider.getBeregningsgrunnlagRepository().hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId()).orElseThrow();
    }

    @Test
    public void skal_lage_fakta_om_beregning_dto_med_avklar_aktiviterer() {
        lagBehandlingKunSkjæringstidspunkt();
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(getBeregningsgrunnlagGrunnlag());
        Optional<FaktaOmBeregningDto> dto = faktaOmBeregningDtoTjeneste
            .lagDto(input);
        assertThat(dto).isPresent();
        assertThat(dto.orElseThrow().getAvklarAktiviteter().getAktiviteterTomDatoMapping()).isNotNull();
    }

    private void lagBehandlingMedBgMedTilfeller(List<FaktaOmBeregningTilfelle> tilfeller) {
        lagBeregningsgrunnlag(scenario, tilfeller);
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private void lagBehandlingKunSkjæringstidspunkt() {
        scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .build();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(TestScenarioBuilder scenario, List<FaktaOmBeregningTilfelle> tilfeller) {
        return scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .medGrunnbeløp(BigDecimal.valueOf(90000))
            .leggTilFaktaOmBeregningTilfeller(tilfeller)
            .build();
    }

    private AvklarAktiviteterDtoTjeneste lagAvklarAktiviteterDtoTjenesteMock() {
        return new AvklarAktiviteterDtoTjeneste() {
            @Override
            public void lagAvklarAktiviteterDto(LocalDate skjæringstidspunkt,
                                                BeregningAktivitetAggregatEntitet registerAktivitetAggregat,
                                                Optional<BeregningAktivitetAggregatEntitet> saksbehandletAktivitetAggregat,
                                                Optional<BeregningAktivitetAggregatEntitet> forrigeRegisterAggregat,
                                                Optional<BeregningAktivitetAggregatEntitet> forrigeSaksbehandletAggregat,
                                                Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon,
                                                FaktaOmBeregningDto faktaOmBeregningDto) {
                AvklarAktiviteterDto avklarAktiviteterDto = new AvklarAktiviteterDto();
                AktivitetTomDatoMappingDto aktivitetTomDatoMappingDto = new AktivitetTomDatoMappingDto();
                avklarAktiviteterDto.setAktiviteterTomDatoMapping(List.of(aktivitetTomDatoMappingDto));
                faktaOmBeregningDto.setAvklarAktiviteter(avklarAktiviteterDto);
            }
        };
    }

    private FaktaOmBeregningTilfelleDtoTjeneste lagDtoTjenesteMock(Consumer<FaktaOmBeregningDto> dtoConsumer) {
        return (input, forrigeGrunnlagOpt, faktaOmBeregningDto) -> dtoConsumer.accept(faktaOmBeregningDto);
    }

    private Consumer<FaktaOmBeregningDto> setBesteberegingAndelConsumer() {
        return (dto) -> {
            TilstøtendeYtelseAndelDto andel = new TilstøtendeYtelseAndelDto();
            dto.setBesteberegningAndeler(Collections.singletonList(andel));
        };
    }

    private Consumer<FaktaOmBeregningDto> setFrilansAndelConsumer() {
        return (dto) -> {
            FaktaOmBeregningAndelDto andel = new FaktaOmBeregningAndelDto();
            andel.setAndelsnr(1L);
            dto.setFrilansAndel(andel);
        };
    }

    private Consumer<FaktaOmBeregningDto> atflSammeOrgConsumer() {
        return (dto) -> {
            ATogFLISammeOrganisasjonDto atflSammeOrgDto = new ATogFLISammeOrganisasjonDto();
            atflSammeOrgDto.setAndelsnr(1L);
            dto.setArbeidstakerOgFrilanserISammeOrganisasjonListe(Collections.singletonList(atflSammeOrgDto));
        };
    }

    private Consumer<FaktaOmBeregningDto> kunYtelseConsumer() {
        return (dto) -> {
            KunYtelseDto kunYtelseDto = new KunYtelseDto();
            AndelMedBeløpDto brukersAndelDto = new AndelMedBeløpDto();
            brukersAndelDto.setAndelsnr(1L);
            kunYtelseDto.setAndeler(Collections.singletonList(brukersAndelDto));
            dto.setKunYtelse(kunYtelseDto);
        };
    }

    private Consumer<FaktaOmBeregningDto> kortvarigeArbeidsforholdConsumer() {
        return (dto) -> {
            KortvarigeArbeidsforholdDto kortvarigeArbeidsforholdDto = new KortvarigeArbeidsforholdDto();
            kortvarigeArbeidsforholdDto.setErTidsbegrensetArbeidsforhold(true);
            dto.setKortvarigeArbeidsforhold(Collections.singletonList(kortvarigeArbeidsforholdDto));
        };
    }

    private Consumer<FaktaOmBeregningDto> vurderLønnsendringConsumer() {
        return (dto) -> {
            FaktaOmBeregningAndelDto andelDto = new FaktaOmBeregningAndelDto();
            andelDto.setAndelsnr(1L);
            dto.setArbeidsforholdMedLønnsendringUtenIM(Collections.singletonList(andelDto));
        };
    }

    private Consumer<FaktaOmBeregningDto> vurderBesteberegningConsumer() {
        return (dto) -> {
            AndelMedBeløpDto andelDto = new AndelMedBeløpDto();
            andelDto.setAndelsnr(1L);
            VurderBesteberegningDto vurderBesteberegning = new VurderBesteberegningDto();
            vurderBesteberegning.setSkalHaBesteberegning(true);
            dto.setVurderBesteberegning(vurderBesteberegning);
        };
    }

}
