package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PermisjonDto {

    @JsonProperty(value = "permisjonFom", required = true)
    @NotNull
    private LocalDate permisjonFom;

    @JsonProperty(value = "permisjonTom", required = true)
    @NotNull
    private LocalDate permisjonTom;

    public PermisjonDto(LocalDate permisjonFom, LocalDate permisjonTom) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
    }

    PermisjonDto() {
        // Skjul default constructor
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PermisjonDto that = (PermisjonDto) o;
        return Objects.equals(permisjonFom, that.permisjonFom)
            && Objects.equals(permisjonTom, that.permisjonTom);
    }

    public LocalDate getPermisjonFom() {
        return permisjonFom;
    }

    public LocalDate getPermisjonTom() {
        return permisjonTom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permisjonFom, permisjonTom);
    }

    public void setPermisjonFom(LocalDate permisjonFom) {
        this.permisjonFom = permisjonFom;
    }

    public void setPermisjonTom(LocalDate permisjonTom) {
        this.permisjonTom = permisjonTom;
    }

}
