package no.nav.k9.sak.kontrakt.søknad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OppgittTilknytningDto {

    @JsonProperty(value = "oppholdNorgeNa")
    private boolean oppholdNorgeNa;

    @JsonProperty(value = "oppholdSistePeriode")
    private boolean oppholdSistePeriode;

    @JsonProperty(value = "oppholdNestePeriode")
    private boolean oppholdNestePeriode;

    @JsonProperty(value = "utlandsoppholdFor")
    @Valid
    @Size(max = 50)
    private List<UtlandsoppholdDto> utlandsoppholdFor = new ArrayList<>();

    @JsonProperty(value = "utlandsoppholdEtter")
    @Valid
    @Size(max = 50)
    private List<UtlandsoppholdDto> utlandsoppholdEtter = new ArrayList<>();

    protected OppgittTilknytningDto() {
        // trengs for deserialisering av JSON
    }

    public OppgittTilknytningDto(boolean oppholdNorgeNa,
                                 boolean oppholdSistePeriode,
                                 boolean oppholdNestePeriode,
                                 List<UtlandsoppholdDto> utlandsoppholdFor,
                                 List<UtlandsoppholdDto> utlandsoppholdEtter) {

        this.oppholdNorgeNa = oppholdNorgeNa;
        this.oppholdSistePeriode = oppholdSistePeriode;
        this.oppholdNestePeriode = oppholdNestePeriode;
        this.utlandsoppholdFor = List.copyOf(utlandsoppholdFor);
        this.utlandsoppholdEtter = List.copyOf(utlandsoppholdEtter);
    }

    public boolean isOppholdNorgeNa() {
        return oppholdNorgeNa;
    }

    public boolean isOppholdSistePeriode() {
        return oppholdSistePeriode;
    }

    public boolean isOppholdNestePeriode() {
        return oppholdNestePeriode;
    }

    public List<UtlandsoppholdDto> getUtlandsoppholdFor() {
        return Collections.unmodifiableList(utlandsoppholdFor);
    }

    public List<UtlandsoppholdDto> getUtlandsoppholdEtter() {
        return Collections.unmodifiableList(utlandsoppholdEtter);
    }
}
