package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderMilitærDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FaktaOmBeregningDto {

    @JsonProperty(value = "kortvarigeArbeidsforhold")
    @Valid
    @Size(max = 200)
    private List<KortvarigeArbeidsforholdDto> kortvarigeArbeidsforhold;

    @JsonProperty(value = "frilansAndel")
    @Valid
    private FaktaOmBeregningAndelDto frilansAndel;

    @JsonProperty(value = "kunYtelse")
    @Valid
    private KunYtelseDto kunYtelse;

    @JsonProperty(value = "faktaOmBeregningTilfeller", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;

    @JsonProperty(value = "arbeidstakerOgFrilanserISammeOrganisasjonListe")
    @Valid
    @Size(max = 200)
    private List<ATogFLISammeOrganisasjonDto> arbeidstakerOgFrilanserISammeOrganisasjonListe;

    @JsonProperty(value = "arbeidsforholdMedLønnsendringUtenIM")
    @Valid
    @Size(max = 200)
    private List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIM;

    @JsonProperty(value = "besteberegningAndeler")
    @Valid
    @Size(max = 200)
    private List<TilstøtendeYtelseAndelDto> besteberegningAndeler;

    @JsonProperty(value = "vurderMottarYtelse")
    @Valid
    private VurderMottarYtelseDto vurderMottarYtelse;

    @JsonProperty(value = "avklarAktiviteter")
    @Valid
    private AvklarAktiviteterDto avklarAktiviteter;

    @JsonProperty(value = "vurderBesteberegning")
    @Valid
    private VurderBesteberegningDto vurderBesteberegning;

    @JsonProperty(value = "andelerForFaktaOmBeregning")
    @Size(max = 200)
    @Valid
    private List<AndelForFaktaOmBeregningDto> andelerForFaktaOmBeregning;
    
    @JsonProperty(value = "vurderMilitaer")
    @Valid
    private VurderMilitærDto vurderMilitaer;

    @JsonProperty(value = "refusjonskravSomKommerForSentListe")
    @Size(max = 200)
    @Valid
    private List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentListe;

    public FaktaOmBeregningDto() {
        //
    }

    public List<RefusjonskravSomKommerForSentDto> getRefusjonskravSomKommerForSentListe() {
        return refusjonskravSomKommerForSentListe;
    }

    public void setRefusjonskravSomKommerForSentListe(List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentListe) {
        this.refusjonskravSomKommerForSentListe = refusjonskravSomKommerForSentListe;
    }

    public List<AndelForFaktaOmBeregningDto> getAndelerForFaktaOmBeregning() {
        return andelerForFaktaOmBeregning;
    }

    public void setAndelerForFaktaOmBeregning(List<AndelForFaktaOmBeregningDto> andelerForFaktaOmBeregning) {
        this.andelerForFaktaOmBeregning = andelerForFaktaOmBeregning;
    }

    public VurderBesteberegningDto getVurderBesteberegning() {
        return vurderBesteberegning;
    }

    public void setVurderBesteberegning(VurderBesteberegningDto vurderBesteberegning) {
        this.vurderBesteberegning = vurderBesteberegning;
    }

    public KunYtelseDto getKunYtelse() {
        return kunYtelse;
    }

    public void setKunYtelse(KunYtelseDto kunYtelse) {
        this.kunYtelse = kunYtelse;
    }

    public List<KortvarigeArbeidsforholdDto> getKortvarigeArbeidsforhold() {
        return kortvarigeArbeidsforhold;
    }

    public void setKortvarigeArbeidsforhold(List<KortvarigeArbeidsforholdDto> kortvarigeArbeidsforhold) {
        this.kortvarigeArbeidsforhold = kortvarigeArbeidsforhold;
    }

    public FaktaOmBeregningAndelDto getFrilansAndel() {
        return frilansAndel;
    }

    public void setFrilansAndel(FaktaOmBeregningAndelDto frilansAndel) {
        this.frilansAndel = frilansAndel;
    }

    public List<FaktaOmBeregningAndelDto> getArbeidsforholdMedLønnsendringUtenIM() {
        return arbeidsforholdMedLønnsendringUtenIM;
    }

    public void setArbeidsforholdMedLønnsendringUtenIM(List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIM) {
        this.arbeidsforholdMedLønnsendringUtenIM = arbeidsforholdMedLønnsendringUtenIM;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }

    public List<ATogFLISammeOrganisasjonDto> getArbeidstakerOgFrilanserISammeOrganisasjonListe() {
        return arbeidstakerOgFrilanserISammeOrganisasjonListe;
    }

    public void setArbeidstakerOgFrilanserISammeOrganisasjonListe(List<ATogFLISammeOrganisasjonDto> aTogFLISammeOrganisasjonListe) {
        this.arbeidstakerOgFrilanserISammeOrganisasjonListe = aTogFLISammeOrganisasjonListe;
    }

    public List<TilstøtendeYtelseAndelDto> getBesteberegningAndeler() {
        return besteberegningAndeler;
    }

    public void setBesteberegningAndeler(List<TilstøtendeYtelseAndelDto> besteberegningAndeler) {
        this.besteberegningAndeler = besteberegningAndeler;
    }

    public VurderMottarYtelseDto getVurderMottarYtelse() {
        return vurderMottarYtelse;
    }

    public void setVurderMottarYtelse(VurderMottarYtelseDto vurderMottarYtelse) {
        this.vurderMottarYtelse = vurderMottarYtelse;
    }

    public AvklarAktiviteterDto getAvklarAktiviteter() {
        return avklarAktiviteter;
    }

    public void setAvklarAktiviteter(AvklarAktiviteterDto avklarAktiviteter) {
        this.avklarAktiviteter = avklarAktiviteter;
    }

    public VurderMilitærDto getVurderMilitaer() {
        return vurderMilitaer;
    }

    public void setVurderMilitaer(VurderMilitærDto vurderMilitaer) {
        this.vurderMilitaer = vurderMilitaer;
    }

}
