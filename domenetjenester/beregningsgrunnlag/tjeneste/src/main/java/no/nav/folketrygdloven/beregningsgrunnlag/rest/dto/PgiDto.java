package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;

public class PgiDto {
    private BigDecimal beløp;
    private Integer årstall;

    public PgiDto(BigDecimal beløp, Integer årstall) {
        this.beløp = beløp;
        this.årstall = årstall;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    public Integer getÅrstall() {
        return årstall;
    }
}
