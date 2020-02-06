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

    @JsonProperty(value = "vurderNyoppstartetFL")
    @Valid
    private VurderNyoppstartetFLDto vurderNyoppstartetFL;

    @JsonProperty(value = "vurderTidsbegrensetArbeidsforhold")
    @Valid
    private VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold;

    @JsonProperty(value = "vurderNyIArbeidslivet")
    @Valid
    private VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet;

    @JsonProperty(value = "fastsettMaanedsinntektFL")
    @Valid
    private FastsettMånedsinntektFLDto fastsettMaanedsinntektFL;

    @JsonProperty(value = "vurdertLonnsendring")
    @Valid
    private VurderLønnsendringDto vurdertLonnsendring;

    @JsonProperty(value = "fastsattUtenInntektsmelding")
    @Valid
    private FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding;

    @JsonProperty(value = "vurderATogFLiSammeOrganisasjon")
    @Valid
    private VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon;

    @JsonProperty(value = "besteberegningAndeler")
    @Valid
    private BesteberegningFødendeKvinneDto besteberegningAndeler;

    @JsonProperty(value = "faktaOmBeregningTilfeller")
    @Size(max=100)
    @Valid
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;

    @JsonProperty(value = "kunYtelseFordeling")
    @Valid
    private FastsettBgKunYtelseDto kunYtelseFordeling;

    @JsonProperty(value = "vurderEtterlønnSluttpakke")
    @Valid
    private VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke;

    @JsonProperty(value = "fastsettEtterlønnSluttpakke")
    @Valid
    private FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke;

    @JsonProperty(value = "mottarYtelse")
    @Valid
    private MottarYtelseDto mottarYtelse;

    @JsonProperty(value = "vurderMilitaer")
    @Valid
    private VurderMilitærDto vurderMilitaer;

    @JsonProperty(value = "refusjonskravGyldighet")
    @Valid
    @Size(max = 100)
    private List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet;

    protected FaktaBeregningLagreDto() {
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

    public List<RefusjonskravPrArbeidsgiverVurderingDto> getRefusjonskravGyldighet() {
        return refusjonskravGyldighet;
    }

    public void setRefusjonskravGyldighet(List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        this.refusjonskravGyldighet = refusjonskravGyldighet;
    }

    public VurderNyoppstartetFLDto getVurderNyoppstartetFL() {
        return vurderNyoppstartetFL;
    }

    public void setVurderNyoppstartetFL(VurderNyoppstartetFLDto vurderNyoppstartetFL) {
        this.vurderNyoppstartetFL = vurderNyoppstartetFL;
    }

    public VurderTidsbegrensetArbeidsforholdDto getVurderTidsbegrensetArbeidsforhold() {
        return vurderTidsbegrensetArbeidsforhold;
    }

    public void setVurderTidsbegrensetArbeidsforhold(VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        this.vurderTidsbegrensetArbeidsforhold = vurderTidsbegrensetArbeidsforhold;
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto getVurderNyIArbeidslivet() {
        return vurderNyIArbeidslivet;
    }

    public void setVurderNyIArbeidslivet(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        this.vurderNyIArbeidslivet = vurderNyIArbeidslivet;
    }

    public FastsettMånedsinntektFLDto getFastsettMaanedsinntektFL() {
        return fastsettMaanedsinntektFL;
    }

    public void setFastsettMaanedsinntektFL(FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        this.fastsettMaanedsinntektFL = fastsettMaanedsinntektFL;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public VurderLønnsendringDto getVurdertLonnsendring() {
        return vurdertLonnsendring;
    }

    public void setVurdertLonnsendring(VurderLønnsendringDto vurdertLonnsendring) {
        this.vurdertLonnsendring = vurdertLonnsendring;
    }

    public VurderATogFLiSammeOrganisasjonDto getVurderATogFLiSammeOrganisasjon() {
        return vurderATogFLiSammeOrganisasjon;
    }

    public void setVurderATogFLiSammeOrganisasjon(VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        this.vurderATogFLiSammeOrganisasjon = vurderATogFLiSammeOrganisasjon;
    }

    public BesteberegningFødendeKvinneDto getBesteberegningAndeler() {
        return besteberegningAndeler;
    }

    public void setBesteberegningAndeler(BesteberegningFødendeKvinneDto besteberegningAndeler) {
        this.besteberegningAndeler = besteberegningAndeler;
    }

    public FastsettBgKunYtelseDto getKunYtelseFordeling() {
        return kunYtelseFordeling;
    }

    public void setKunYtelseFordeling(FastsettBgKunYtelseDto kunYtelseFordeling) {
        this.kunYtelseFordeling = kunYtelseFordeling;
    }

    public VurderEtterlønnSluttpakkeDto getVurderEtterlønnSluttpakke() {
        return vurderEtterlønnSluttpakke;
    }

    public void setVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        this.vurderEtterlønnSluttpakke = vurderEtterlønnSluttpakke;
    }

    public FastsettEtterlønnSluttpakkeDto getFastsettEtterlønnSluttpakke() {
        return fastsettEtterlønnSluttpakke;
    }

    public void setFastsettEtterlønnSluttpakke(FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        this.fastsettEtterlønnSluttpakke = fastsettEtterlønnSluttpakke;
    }

    public void setMottarYtelse(MottarYtelseDto mottarYtelse) {
        this.mottarYtelse = mottarYtelse;
    }

    public MottarYtelseDto getMottarYtelse() {
        return mottarYtelse;
    }

    public FastsettMånedsinntektUtenInntektsmeldingDto getFastsattUtenInntektsmelding() {
        return fastsattUtenInntektsmelding;
    }

    public void setFastsattUtenInntektsmelding(FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        this.fastsattUtenInntektsmelding = fastsattUtenInntektsmelding;
    }

    public VurderMilitærDto getVurderMilitaer() {
        return vurderMilitaer;
    }

    public void setVurderMilitaer(VurderMilitærDto vurderMilitaer) {
        this.vurderMilitaer = vurderMilitaer;
    }
}
