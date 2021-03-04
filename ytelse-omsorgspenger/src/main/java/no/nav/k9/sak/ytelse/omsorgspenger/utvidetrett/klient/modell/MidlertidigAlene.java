package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class MidlertidigAlene implements UtvidetRett {

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private Saksnummer saksnummer;

    @JsonProperty(value = "behandlingId", required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;

    @Valid
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @JsonProperty(value = "tidspunkt", required = true)
    private ZonedDateTime tidspunkt;

    @Valid
    @NotNull
    @JsonProperty(value = "søker", required = true)
    private Person søker;

    @Valid
    @NotNull
    @JsonProperty(value = "annenForelder", required = true)
    private Person annenForelder;

    @Valid
    @NotNull
    @JsonProperty(value = "periode", required = true)
    private Periode periode;

    @Valid
    @NotNull
    @JsonProperty(value = "versjon", required = true)
    public final String versjon = "1.0.0";

    public MidlertidigAlene() {
    }

    @JsonCreator
    public MidlertidigAlene(@JsonProperty(value = "saksnummer", required = true) @Valid @NotNull Saksnummer saksnummer,
                            @JsonProperty(value = "behandlingId", required = true) @Valid @NotNull UUID behandlingUuid,
                            @JsonProperty(value = "tidspunkt", required = true) @Valid @NotNull ZonedDateTime tidspunkt,
                            @JsonProperty(value = "søker", required = true) @Valid @NotNull Person søker,
                            @JsonProperty(value = "annenForelder", required = true) @Valid @NotNull Person annenForelder,
                            @JsonProperty(value = "periode", required = true) @Valid @NotNull Periode periode) {
        this.saksnummer = saksnummer;
        this.behandlingUuid = behandlingUuid;
        this.tidspunkt = tidspunkt;
        this.søker = søker;
        this.annenForelder = annenForelder;
        this.periode = periode;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public MidlertidigAlene setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public MidlertidigAlene setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
        return this;
    }

    public ZonedDateTime getTidspunkt() {
        return tidspunkt;
    }

    public MidlertidigAlene setTidspunkt(ZonedDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
        return this;
    }

    public Person getSøker() {
        return søker;
    }

    public MidlertidigAlene setSøker(Person søker) {
        this.søker = søker;
        return this;
    }

    public MidlertidigAlene setAnnenForelder(Person annenForelder) {
        this.annenForelder = annenForelder;
        return this;
    }

    public Person getAnnenForelder() {
        return annenForelder;
    }

    public MidlertidigAlene setPeriode(Periode periode) {
        this.periode = periode;
        return this;
    }

    public Periode getPeriode() {
        return periode;
    }
}
