package no.nav.ung.sak.oppgave;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.ung.sak.oppgave.oppgavedata.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EndretStartdatoOppgaveData.class, name = "BEKREFT_ENDRET_STARTDATO"),
    @JsonSubTypes.Type(value = EndretSluttdatoOppgaveData.class, name = "BEKREFT_ENDRET_SLUTTDATO"),
    @JsonSubTypes.Type(value = KontrollerRegisterInntektOppgaveData.class, name = "BEKREFT_AVVIK_REGISTERINNTEKT"),
    @JsonSubTypes.Type(value = InntektsrapporteringOppgaveData.class, name = "RAPPORTER_INNTEKT"),
    @JsonSubTypes.Type(value = SøkYtelseOppgaveData.class, name = "SØK_YTELSE"),
    @JsonSubTypes.Type(value = FjernetPeriodeOppgaveData.class, name = "BEKREFT_FJERNET_PERIODE"),
    @JsonSubTypes.Type(value = EndretPeriodeOppgaveData.class, name = "BEKREFT_ENDRET_PERIODE")
})
public abstract class OppgaveData {
}

