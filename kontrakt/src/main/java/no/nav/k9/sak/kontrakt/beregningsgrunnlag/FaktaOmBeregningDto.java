package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.Collections;
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

    @JsonProperty(value = "andelerForFaktaOmBeregning")
    @Size(max = 200)
    @Valid
    private List<AndelForFaktaOmBeregningDto> andelerForFaktaOmBeregning = Collections.emptyList();

    @JsonProperty(value = "arbeidsforholdMedLønnsendringUtenIM")
    @Valid
    @Size(max = 200)
    private List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIM = Collections.emptyList();

    @JsonProperty(value = "arbeidstakerOgFrilanserISammeOrganisasjonListe")
    @Valid
    @Size(max = 200)
    private List<ATogFLISammeOrganisasjonDto> arbeidstakerOgFrilanserISammeOrganisasjonListe = Collections.emptyList();

    @JsonProperty(value = "avklarAktiviteter")
    @Valid
    private AvklarAktiviteterDto avklarAktiviteter;

    @JsonProperty(value = "besteberegningAndeler")
    @Valid
    @Size(max = 200)
    private List<TilstøtendeYtelseAndelDto> besteberegningAndeler = Collections.emptyList();

    @JsonProperty(value = "faktaOmBeregningTilfeller", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = Collections.emptyList();

    @JsonProperty(value = "frilansAndel")
    @Valid
    private FaktaOmBeregningAndelDto frilansAndel;

    @JsonProperty(value = "kortvarigeArbeidsforhold")
    @Valid
    @Size(max = 200)
    private List<KortvarigeArbeidsforholdDto> kortvarigeArbeidsforhold = Collections.emptyList();

    @JsonProperty(value = "kunYtelse")
    @Valid
    private KunYtelseDto kunYtelse;

    @JsonProperty(value = "refusjonskravSomKommerForSentListe")
    @Size(max = 200)
    @Valid
    private List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentListe = Collections.emptyList();

    @JsonProperty(value = "vurderBesteberegning")
    @Valid
    private VurderBesteberegningDto vurderBesteberegning;

    @JsonProperty(value = "vurderMilitaer")
    @Valid
    private VurderMilitærDto vurderMilitaer;

    @JsonProperty(value = "vurderMottarYtelse")
    @Valid
    private VurderMottarYtelseDto vurderMottarYtelse;

    public FaktaOmBeregningDto() {
        //
    }

    public List<AndelForFaktaOmBeregningDto> getAndelerForFaktaOmBeregning() {
        return Collections.unmodifiableList(andelerForFaktaOmBeregning);
    }

    public List<FaktaOmBeregningAndelDto> getArbeidsforholdMedLønnsendringUtenIM() {
        return Collections.unmodifiableList(arbeidsforholdMedLønnsendringUtenIM);
    }

    public List<ATogFLISammeOrganisasjonDto> getArbeidstakerOgFrilanserISammeOrganisasjonListe() {
        return Collections.unmodifiableList(arbeidstakerOgFrilanserISammeOrganisasjonListe);
    }

    public AvklarAktiviteterDto getAvklarAktiviteter() {
        return avklarAktiviteter;
    }

    public List<TilstøtendeYtelseAndelDto> getBesteberegningAndeler() {
        return Collections.unmodifiableList(besteberegningAndeler);
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return Collections.unmodifiableList(faktaOmBeregningTilfeller);
    }

    public FaktaOmBeregningAndelDto getFrilansAndel() {
        return frilansAndel;
    }

    public List<KortvarigeArbeidsforholdDto> getKortvarigeArbeidsforhold() {
        return Collections.unmodifiableList(kortvarigeArbeidsforhold);
    }

    public KunYtelseDto getKunYtelse() {
        return kunYtelse;
    }

    public List<RefusjonskravSomKommerForSentDto> getRefusjonskravSomKommerForSentListe() {
        return Collections.unmodifiableList(refusjonskravSomKommerForSentListe);
    }

    public VurderBesteberegningDto getVurderBesteberegning() {
        return vurderBesteberegning;
    }

    public VurderMilitærDto getVurderMilitaer() {
        return vurderMilitaer;
    }

    public VurderMottarYtelseDto getVurderMottarYtelse() {
        return vurderMottarYtelse;
    }

    public void setAndelerForFaktaOmBeregning(List<AndelForFaktaOmBeregningDto> andelerForFaktaOmBeregning) {
        this.andelerForFaktaOmBeregning = List.copyOf(andelerForFaktaOmBeregning);
    }

    public void setArbeidsforholdMedLønnsendringUtenIM(List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIM) {
        this.arbeidsforholdMedLønnsendringUtenIM = List.copyOf(arbeidsforholdMedLønnsendringUtenIM);
    }

    public void setArbeidstakerOgFrilanserISammeOrganisasjonListe(List<ATogFLISammeOrganisasjonDto> aTogFLISammeOrganisasjonListe) {
        this.arbeidstakerOgFrilanserISammeOrganisasjonListe = List.copyOf(aTogFLISammeOrganisasjonListe);
    }

    public void setAvklarAktiviteter(AvklarAktiviteterDto avklarAktiviteter) {
        this.avklarAktiviteter = avklarAktiviteter;
    }

    public void setBesteberegningAndeler(List<TilstøtendeYtelseAndelDto> besteberegningAndeler) {
        this.besteberegningAndeler = List.copyOf(besteberegningAndeler);
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = List.copyOf(faktaOmBeregningTilfeller);
    }

    public void setFrilansAndel(FaktaOmBeregningAndelDto frilansAndel) {
        this.frilansAndel = frilansAndel;
    }

    public void setKortvarigeArbeidsforhold(List<KortvarigeArbeidsforholdDto> kortvarigeArbeidsforhold) {
        this.kortvarigeArbeidsforhold = List.copyOf(kortvarigeArbeidsforhold);
    }

    public void setKunYtelse(KunYtelseDto kunYtelse) {
        this.kunYtelse = kunYtelse;
    }

    public void setRefusjonskravSomKommerForSentListe(List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentListe) {
        this.refusjonskravSomKommerForSentListe = List.copyOf(refusjonskravSomKommerForSentListe);
    }

    public void setVurderBesteberegning(VurderBesteberegningDto vurderBesteberegning) {
        this.vurderBesteberegning = vurderBesteberegning;
    }

    public void setVurderMilitaer(VurderMilitærDto vurderMilitaer) {
        this.vurderMilitaer = vurderMilitaer;
    }

    public void setVurderMottarYtelse(VurderMottarYtelseDto vurderMottarYtelse) {
        this.vurderMottarYtelse = vurderMottarYtelse;
    }

}
