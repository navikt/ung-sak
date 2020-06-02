package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

/**
 * Endringsresultat i personopplysninger for medlemskap
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class EndringsresultatPersonopplysningerForMedlemskap {

    /** @deprecated bruk settere i stedet. */
    @Deprecated(forRemoval = true)
    public static class Builder {
        EndringsresultatPersonopplysningerForMedlemskap kladd = new EndringsresultatPersonopplysningerForMedlemskap();

        private Builder() {
        }

        public EndringsresultatPersonopplysningerForMedlemskap build() {
            return kladd;
        }

        public Builder leggTilEndring(EndretAttributt endretAttributt, Periode periode, String endretFra, String endretTil) {
            Endring endring = new Endring(endretAttributt, periode, endretFra, endretTil);
            kladd.endringer.add(endring);
            kladd.oppdaterGjeldendeFra();
            return this;
        }
    }

    public enum EndretAttributt {
        Adresse, Personstatus, StatsborgerskapRegion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
    public static class Endring {

        @JsonProperty(value = "endretAttributt")
        private EndretAttributt endretAttributt;

        @JsonProperty(value = "erEndret")
        private boolean erEndret;

        @JsonProperty(value = "periode")
        private Periode periode;

        @JsonProperty(value = "endretFra")
        @Size(max = 4000)
        @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        String endretFra;

        @JsonProperty(value = "endretTil")
        @Size(max = 4000)
        @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        String endretTil;

        private Endring(EndretAttributt endretAttributt, Periode periode, String endretFra, String endretTil) {
            Objects.requireNonNull(endretAttributt);
            Objects.requireNonNull(endretFra);
            Objects.requireNonNull(endretTil);
            Objects.requireNonNull(periode);

            if (!endretFra.trim().equalsIgnoreCase(endretTil.trim())) {
                this.erEndret = true;
            }
            this.endretAttributt = endretAttributt;
            this.endretFra = endretFra;
            this.endretTil = endretTil;
            this.periode = periode;
        }

        Endring() {
            //
        }

        public EndretAttributt getEndretAttributt() {
            return endretAttributt;
        }

        public String getEndretFra() {
            return endretFra;
        }

        public String getEndretTil() {
            return endretTil;
        }

        public Periode getPeriode() {
            return periode;
        }

        public boolean isErEndret() {
            return erEndret;
        }
    }

    @JsonProperty(value = "endringer")
    @Size(max = 100)
    @Valid
    private List<Endring> endringer = new ArrayList<>();

    @JsonProperty(value = "gjeldendeFra")
    @Valid
    private Optional<LocalDate> gjeldendeFra;

    public EndringsresultatPersonopplysningerForMedlemskap() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Endring> getEndredeAttributter() {
        return endringer.stream().filter(Endring::isErEndret).collect(Collectors.toList());
    }

    public List<Endring> getEndringer() {
        return endringer;
    }

    /**
     * @return er satt hvis det er endringer
     */
    public Optional<LocalDate> getGjeldendeFra() {
        return gjeldendeFra;
    }

    public boolean harEndringer() {
        return endringer.stream().anyMatch(Endring::isErEndret);
    }

    public void setEndringer(List<Endring> endringer) {
        this.endringer = endringer;
        oppdaterGjeldendeFra();
    }

    public void setGjeldendeFra(Optional<LocalDate> gjeldendeFra) {
        this.gjeldendeFra = gjeldendeFra;
    }

    private void oppdaterGjeldendeFra() {
        this.gjeldendeFra = getEndredeAttributter().stream()
            .map(e -> e.getPeriode().getFom())
            .min(LocalDate::compareTo);
    }
}
