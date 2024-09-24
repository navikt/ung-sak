package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public enum ManuellRevurderingSteg {

    ENDRET_FORDELING(BehandlingÅrsakType.RE_ENDRET_FORDELING);

    ManuellRevurderingSteg(BehandlingÅrsakType type) {
        this.type = type;
    }

    @JsonProperty("type")
    private BehandlingÅrsakType type;

    public BehandlingÅrsakType getType() {
        return type;
    }

    @JsonCreator
    public static ManuellRevurderingSteg fromJson(@JsonProperty("type") BehandlingÅrsakType type) {
        for (ManuellRevurderingSteg steg : ManuellRevurderingSteg.values()) {
            if (steg.type.equals(type)) {
                return steg;
            }
        }
        throw new IllegalArgumentException("Ukjent behandlingsårsaktype: " + type);
    }
}
