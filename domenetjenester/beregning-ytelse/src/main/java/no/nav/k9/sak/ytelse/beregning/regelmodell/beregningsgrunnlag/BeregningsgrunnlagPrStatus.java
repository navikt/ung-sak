package no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BeregningsgrunnlagPrStatus {
    private AktivitetStatus aktivitetStatus;
    private List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold = new ArrayList<>();
    private BigDecimal redusertBrukersAndelPrÅr;
    private Inntektskategori inntektskategori;

    BeregningsgrunnlagPrStatus() {
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public List<BeregningsgrunnlagPrArbeidsforhold> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public BigDecimal getRedusertBrukersAndelPrÅr() {
        return redusertBrukersAndelPrÅr;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsgrunnlagPrStatus mal;

        public Builder() {
            mal = new BeregningsgrunnlagPrStatus();
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            mal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagPrArbeidsforhold) {
            mal.arbeidsforhold.add(beregningsgrunnlagPrArbeidsforhold);
            return this;
        }

        public Builder medRedusertBrukersAndelPrÅr(BigDecimal redusertBrukersAndelPrÅr) {
            mal.redusertBrukersAndelPrÅr = redusertBrukersAndelPrÅr;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            mal.inntektskategori = inntektskategori;
            return this;
        }

        public BeregningsgrunnlagPrStatus build() {
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.aktivitetStatus, "aktivitetStatus");
            if(AktivitetStatus.ATFL!=mal.aktivitetStatus) {
                Objects.requireNonNull(mal.inntektskategori, "inntektskategori");
            } else {
                Objects.requireNonNull(mal.arbeidsforhold, "arbeidsforhold");
            }
        }

        public Builder medArbeidsforhold(List<Arbeidsforhold> arbeidsforhold) {
            if (arbeidsforhold != null) {
                arbeidsforhold.forEach(af -> mal.arbeidsforhold.add(BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(af).build()));
            }
            return this;
        }
    }
}
