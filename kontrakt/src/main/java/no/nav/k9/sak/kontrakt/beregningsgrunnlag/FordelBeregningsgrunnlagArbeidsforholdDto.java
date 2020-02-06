package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagArbeidsforholdDto extends BeregningsgrunnlagArbeidsforholdDto {

    @JsonProperty(value = "periodreMedGraderingEllerRefusjon", required = true)
    @Valid
    @Size(max = 200)
    private List<GraderingEllerRefusjonDto> perioderMedGraderingEllerRefusjon = new ArrayList<>();

    @JsonProperty(value = "permisjon")
    @Valid
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        FordelBeregningsgrunnlagArbeidsforholdDto that = (FordelBeregningsgrunnlagArbeidsforholdDto) o;
        return Objects.equals(perioderMedGraderingEllerRefusjon, that.perioderMedGraderingEllerRefusjon)
            && Objects.equals(permisjon, that.permisjon);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), perioderMedGraderingEllerRefusjon);
    }
}
