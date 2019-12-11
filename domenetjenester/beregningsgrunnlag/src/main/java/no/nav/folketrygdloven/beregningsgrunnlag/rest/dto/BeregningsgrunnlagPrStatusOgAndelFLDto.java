package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

public class BeregningsgrunnlagPrStatusOgAndelFLDto extends BeregningsgrunnlagPrStatusOgAndelDto {
    private Boolean erNyoppstartet;

    public BeregningsgrunnlagPrStatusOgAndelFLDto() {
        super();
        // trengs for deserialisering av JSON
    }

    public Boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public void setErNyoppstartet(Boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

}
