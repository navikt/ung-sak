package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FordelBeregningsgrunnlagArbeidsforholdDto extends BeregningsgrunnlagArbeidsforholdDto {

    private List<GraderingEllerRefusjonDto> perioderMedGraderingEllerRefusjon = new ArrayList<>();
    private PermisjonDto permisjon;

    public void leggTilPeriodeMedGraderingEllerRefusjon(GraderingEllerRefusjonDto periodeMedGraderingEllerRefusjon) {
        this.perioderMedGraderingEllerRefusjon.add(periodeMedGraderingEllerRefusjon);
    }

    public List<GraderingEllerRefusjonDto> getPerioderMedGraderingEllerRefusjon() {
        return perioderMedGraderingEllerRefusjon;
    }

    public void setPerioderMedGraderingEllerRefusjon(List<GraderingEllerRefusjonDto> perioderMedGraderingEllerRefusjon) {
        this.perioderMedGraderingEllerRefusjon = perioderMedGraderingEllerRefusjon;
    }

    public PermisjonDto getPermisjon() {
        return permisjon;
    }

    public void setPermisjon(PermisjonDto permisjon) {
        this.permisjon = permisjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FordelBeregningsgrunnlagArbeidsforholdDto that = (FordelBeregningsgrunnlagArbeidsforholdDto) o;
        return Objects.equals(perioderMedGraderingEllerRefusjon, that.perioderMedGraderingEllerRefusjon)
            && Objects.equals(permisjon, that.permisjon);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), perioderMedGraderingEllerRefusjon);
    }
}
