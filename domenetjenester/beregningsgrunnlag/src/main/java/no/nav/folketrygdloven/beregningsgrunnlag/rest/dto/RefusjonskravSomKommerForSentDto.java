package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

public class RefusjonskravSomKommerForSentDto {

    private String arbeidsgiverId;
    private String arbeidsgiverVisningsnavn;
    private Boolean erRefusjonskravGyldig;

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public String getArbeidsgiverVisningsnavn() {
        return arbeidsgiverVisningsnavn;
    }

    public void setArbeidsgiverVisningsnavn(String arbeidsgiverVisningsnavn) {
        this.arbeidsgiverVisningsnavn = arbeidsgiverVisningsnavn;
    }

    public Boolean getErRefusjonskravGyldig() {
        return erRefusjonskravGyldig;
    }

    public void setErRefusjonskravGyldig(Boolean erRefusjonskravGyldig) {
        this.erRefusjonskravGyldig = erRefusjonskravGyldig;
    }
}
