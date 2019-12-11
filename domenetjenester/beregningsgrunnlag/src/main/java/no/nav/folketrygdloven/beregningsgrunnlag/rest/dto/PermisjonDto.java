package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.time.LocalDate;
import java.util.Objects;

public class PermisjonDto {

    private LocalDate permisjonFom;
    private LocalDate permisjonTom;

    PermisjonDto(){
        // Skjul default constructor
    }

    public PermisjonDto(LocalDate permisjonFom, LocalDate permisjonTom) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
    }

    public LocalDate getPermisjonFom() {
        return permisjonFom;
    }

    public void setPermisjonFom(LocalDate permisjonFom) {
        this.permisjonFom = permisjonFom;
    }

    public LocalDate getPermisjonTom() {
        return permisjonTom;
    }

    public void setPermisjonTom(LocalDate permisjonTom) {
        this.permisjonTom = permisjonTom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermisjonDto that = (PermisjonDto) o;
        return Objects.equals(permisjonFom, that.permisjonFom)
            && Objects.equals(permisjonTom, that.permisjonTom);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(permisjonFom, permisjonTom);
    }

}
