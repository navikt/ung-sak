package no.nav.k9.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PeriodeAndel {

    @ChangeTracked
    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    /**
     * antall timer per dag (og minutter). Hvis null antas hel arbeidsdag skal telles.
     */
    @ChangeTracked
    @JsonProperty(value = "varighetPerDag")
    @Valid
    private Duration varighetPerDag;

    @JsonCreator
    public PeriodeAndel(@JsonProperty(value = "periode", required = true) @NotNull Periode periode,
                        @JsonProperty(value = "varighetPerDag") Duration varighetPerDag) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.varighetPerDag = varighetPerDag;
    }

    public PeriodeAndel(LocalDate fom, LocalDate tom, Duration varighetPerDag) {
        this(new Periode(fom, tom), varighetPerDag);
    }

    public PeriodeAndel(LocalDate fom, LocalDate tom, BigDecimal timerPerDag) {
        this(new Periode(fom, tom), toDuration(timerPerDag));
    }

    /**
     * Antal hel dag skal telles med.
     */
    public PeriodeAndel(Periode periode) {
        this(periode, (Duration) null);
    }

    public PeriodeAndel(LocalDate fom, LocalDate tom) {
        this(new Periode(fom, tom), (Duration) null);
    }

    public PeriodeAndel(Periode periode, BigDecimal timerPerDag) {
        this(periode, toDuration(timerPerDag));
    }

    public static Duration toDuration(BigDecimal timerPerDag) {
        if (timerPerDag == null) {
            return null;
        }
        int timer = timerPerDag.intValue();
        BigDecimal andelAvTime = timerPerDag.subtract(BigDecimal.valueOf(timer));

        int minutterPerTime = 60;
        int minutterPerIntervall = 30;
        BigDecimal antallIntervaller = andelAvTime.multiply(BigDecimal.valueOf(minutterPerTime / minutterPerIntervall)).setScale(0, RoundingMode.HALF_UP);
        int minutterAvrundet = antallIntervaller.intValue() * minutterPerIntervall;
        return Duration.ofHours(timer).plusMinutes(minutterAvrundet);
    }

    public Periode getPeriode() {
        return periode;
    }

    public LocalDate getFom() {
        return periode.getFom();
    }

    public LocalDate getTom() {
        return periode.getTom();
    }

    public Duration getVarighetPerDag() {
        return varighetPerDag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof PeriodeAndel))
            return false;
        var other = (PeriodeAndel) obj;
        return Objects.equals(periode, other.periode)
            && Objects.equals(varighetPerDag, other.varighetPerDag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, varighetPerDag);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + periode + " =" + varighetPerDag + ">";
    }
}
