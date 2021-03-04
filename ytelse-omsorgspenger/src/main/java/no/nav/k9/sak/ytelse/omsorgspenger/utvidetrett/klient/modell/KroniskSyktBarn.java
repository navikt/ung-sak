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

import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class KroniskSyktBarn implements UtvidetRett {

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
    @JsonProperty(value = "barn", required = true)
    private Person barn;

    public KroniskSyktBarn() {
    }

    @JsonCreator
    public KroniskSyktBarn(@JsonProperty(value = "saksnummer", required = true) @Valid @NotNull Saksnummer saksnummer,
                            @JsonProperty(value = "behandlingId", required = true) @Valid @NotNull UUID behandlingUuid,
                            @JsonProperty(value = "tidspunkt", required = true) @Valid @NotNull ZonedDateTime tidspunkt,
                            @JsonProperty(value = "søker", required = true) @Valid @NotNull Person søker,
                            @JsonProperty(value = "barn", required = true) @Valid @NotNull Person barn) {
        this.saksnummer = saksnummer;
        this.behandlingUuid = behandlingUuid;
        this.tidspunkt = tidspunkt;
        this.søker = søker;
        this.barn = barn;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public KroniskSyktBarn setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public KroniskSyktBarn setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
        return this;
    }

    public ZonedDateTime getTidspunkt() {
        return tidspunkt;
    }

    public KroniskSyktBarn setTidspunkt(ZonedDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
        return this;
    }

    public Person getSøker() {
        return søker;
    }

    public KroniskSyktBarn setSøker(Person søker) {
        this.søker = søker;
        return this;
    }

    public KroniskSyktBarn setBarn(Person barn) {
        this.barn = barn;
        return this;
    }

    public Person getBarn() {
        return barn;
    }
}
