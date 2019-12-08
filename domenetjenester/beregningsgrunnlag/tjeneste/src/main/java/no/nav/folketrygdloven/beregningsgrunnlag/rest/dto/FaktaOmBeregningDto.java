package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;

public class FaktaOmBeregningDto {

    private List<KortvarigeArbeidsforholdDto> kortvarigeArbeidsforhold;
    private FaktaOmBeregningAndelDto frilansAndel;
    private KunYtelseDto kunYtelse;
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;
    private List<ATogFLISammeOrganisasjonDto> arbeidstakerOgFrilanserISammeOrganisasjonListe;
    private List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIM;
    private List<TilstøtendeYtelseAndelDto> besteberegningAndeler;
    private VurderMottarYtelseDto vurderMottarYtelse;
    private AvklarAktiviteterDto avklarAktiviteter;
    private VurderBesteberegningDto vurderBesteberegning;
    private List<AndelForFaktaOmBeregningDto> andelerForFaktaOmBeregning;
    private VurderMilitærDto vurderMilitaer;
    private List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentListe;



    public FaktaOmBeregningDto() {
        // Hibernate
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
