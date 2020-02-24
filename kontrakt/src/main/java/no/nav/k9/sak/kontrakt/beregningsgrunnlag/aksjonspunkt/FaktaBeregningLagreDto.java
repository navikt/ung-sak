package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FaktaBeregningLagreDto {

    @JsonProperty(value = "besteberegningAndeler")
    @Valid
    private BesteberegningFødendeKvinneDto besteberegningAndeler;

    @JsonProperty(value = "faktaOmBeregningTilfeller")
    @Size(max=100)
    @Valid
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;

    @JsonProperty(value = "fastsattUtenInntektsmelding")
    @Valid
    private FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding;

    @JsonProperty(value = "fastsettEtterlønnSluttpakke")
    @Valid
    private FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke;

    @JsonProperty(value = "fastsettMaanedsinntektFL")
    @Valid
    private FastsettMånedsinntektFLDto fastsettMaanedsinntektFL;

    @JsonProperty(value = "kunYtelseFordeling")
    @Valid
    private FastsettBgKunYtelseDto kunYtelseFordeling;

    @JsonProperty(value = "mottarYtelse")
    @Valid
    private MottarYtelseDto mottarYtelse;

    @JsonProperty(value = "refusjonskravGyldighet")
    @Valid
    @Size(max = 100)
    private List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet;

    @JsonProperty(value = "vurderATogFLiSammeOrganisasjon")
    @Valid
    private VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon;

    @JsonProperty(value = "vurderEtterlønnSluttpakke")
    @Valid
    private VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke;

    @JsonProperty(value = "vurderMilitaer")
    @Valid
    private VurderMilitærDto vurderMilitaer;

    @JsonProperty(value = "vurderNyIArbeidslivet")
    @Valid
    private VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet;

    @JsonProperty(value = "vurderNyoppstartetFL")
    @Valid
    private VurderNyoppstartetFLDto vurderNyoppstartetFL;

    @JsonProperty(value = "vurderTidsbegrensetArbeidsforhold")
    @Valid
    private VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold;

    @JsonProperty(value = "vurdertLonnsendring")
    @Valid
    private VurderLønnsendringDto vurdertLonnsendring;

    public FaktaBeregningLagreDto() {
        //
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FastsettBgKunYtelseDto kunYtelseFordeling) { // NOSONAR
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
        this.kunYtelseFordeling = kunYtelseFordeling;
    }

    public FaktaBeregningLagreDto(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                  VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) { // NOSONAR
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
        this.vurderTidsbegrensetArbeidsforhold = vurderTidsbegrensetArbeidsforhold;
    }

    public BesteberegningFødendeKvinneDto getBesteberegningAndeler() {
        return besteberegningAndeler;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public FastsettMånedsinntektUtenInntektsmeldingDto getFastsattUtenInntektsmelding() {
        return fastsattUtenInntektsmelding;
    }

    public FastsettEtterlønnSluttpakkeDto getFastsettEtterlønnSluttpakke() {
        return fastsettEtterlønnSluttpakke;
    }

    public FastsettMånedsinntektFLDto getFastsettMaanedsinntektFL() {
        return fastsettMaanedsinntektFL;
    }

    public FastsettBgKunYtelseDto getKunYtelseFordeling() {
        return kunYtelseFordeling;
    }

    public MottarYtelseDto getMottarYtelse() {
        return mottarYtelse;
    }

    public List<RefusjonskravPrArbeidsgiverVurderingDto> getRefusjonskravGyldighet() {
        return refusjonskravGyldighet;
    }

    public VurderATogFLiSammeOrganisasjonDto getVurderATogFLiSammeOrganisasjon() {
        return vurderATogFLiSammeOrganisasjon;
    }

    public VurderEtterlønnSluttpakkeDto getVurderEtterlønnSluttpakke() {
        return vurderEtterlønnSluttpakke;
    }

    public VurderMilitærDto getVurderMilitaer() {
        return vurderMilitaer;
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto getVurderNyIArbeidslivet() {
        return vurderNyIArbeidslivet;
    }

    public VurderNyoppstartetFLDto getVurderNyoppstartetFL() {
        return vurderNyoppstartetFL;
    }

    public VurderTidsbegrensetArbeidsforholdDto getVurderTidsbegrensetArbeidsforhold() {
        return vurderTidsbegrensetArbeidsforhold;
    }

    public VurderLønnsendringDto getVurdertLonnsendring() {
        return vurdertLonnsendring;
    }

    public void setBesteberegningAndeler(BesteberegningFødendeKvinneDto besteberegningAndeler) {
        this.besteberegningAndeler = besteberegningAndeler;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public void setFastsattUtenInntektsmelding(FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        this.fastsattUtenInntektsmelding = fastsattUtenInntektsmelding;
    }

    public void setFastsettEtterlønnSluttpakke(FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        this.fastsettEtterlønnSluttpakke = fastsettEtterlønnSluttpakke;
    }

    public void setFastsettMaanedsinntektFL(FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        this.fastsettMaanedsinntektFL = fastsettMaanedsinntektFL;
    }

    public void setKunYtelseFordeling(FastsettBgKunYtelseDto kunYtelseFordeling) {
        this.kunYtelseFordeling = kunYtelseFordeling;
    }

    public void setMottarYtelse(MottarYtelseDto mottarYtelse) {
        this.mottarYtelse = mottarYtelse;
    }

    public void setRefusjonskravGyldighet(List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        this.refusjonskravGyldighet = refusjonskravGyldighet;
    }

    public void setVurderATogFLiSammeOrganisasjon(VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        this.vurderATogFLiSammeOrganisasjon = vurderATogFLiSammeOrganisasjon;
    }

    public void setVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        this.vurderEtterlønnSluttpakke = vurderEtterlønnSluttpakke;
    }

    public void setVurderMilitaer(VurderMilitærDto vurderMilitaer) {
        this.vurderMilitaer = vurderMilitaer;
    }

    public void setVurderNyIArbeidslivet(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        this.vurderNyIArbeidslivet = vurderNyIArbeidslivet;
    }

    public void setVurderNyoppstartetFL(VurderNyoppstartetFLDto vurderNyoppstartetFL) {
        this.vurderNyoppstartetFL = vurderNyoppstartetFL;
    }

    public void setVurderTidsbegrensetArbeidsforhold(VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        this.vurderTidsbegrensetArbeidsforhold = vurderTidsbegrensetArbeidsforhold;
    }

    public void setVurdertLonnsendring(VurderLønnsendringDto vurdertLonnsendring) {
        this.vurdertLonnsendring = vurdertLonnsendring;
    }
}
