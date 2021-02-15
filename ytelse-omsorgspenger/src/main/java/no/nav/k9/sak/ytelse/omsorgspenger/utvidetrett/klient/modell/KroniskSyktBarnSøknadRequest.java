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
public class KroniskSyktBarnSøknadRequest {

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
    @JsonProperty(value = "søknadMottatt", required = true)
    private ZonedDateTime søknadMottatt;

    @Valid
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @JsonProperty(value = "tidspunkt", required = true)
    private ZonedDateTime tidspunkt;

    @Valid
    @NotNull
    @JsonProperty(value = "søker", required = true)
    private Søker søker;

    @Valid
    @NotNull
    @JsonProperty(value = "barn", required = true)
    private Barn barn;

    public KroniskSyktBarnSøknadRequest() {
    }

    @JsonCreator
    public KroniskSyktBarnSøknadRequest(@JsonProperty(value = "saksnummer", required = true) @Valid @NotNull Saksnummer saksnummer,
                                        @JsonProperty(value = "behandlingId", required = true) @Valid @NotNull UUID behandlingUuid,
                                        @JsonProperty(value = "søknadMottatt", required = true) @Valid @NotNull ZonedDateTime søknadMottatt,
                                        @JsonProperty(value = "tidspunkt", required = true) @Valid @NotNull ZonedDateTime tidspunkt,
                                        @JsonProperty(value = "søker", required = true) @Valid @NotNull Søker søker,
                                        @JsonProperty(value = "barn", required = true) @Valid @NotNull Barn barn) {
        this.saksnummer = saksnummer;
        this.behandlingUuid = behandlingUuid;
        this.søknadMottatt = søknadMottatt;
        this.tidspunkt = tidspunkt;
        this.søker = søker;
        this.barn = barn;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public KroniskSyktBarnSøknadRequest setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public KroniskSyktBarnSøknadRequest setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
        return this;
    }

    public ZonedDateTime getSøknadMottatt() {
        return søknadMottatt;
    }

    public KroniskSyktBarnSøknadRequest setSøknadMottatt(ZonedDateTime søknadMottatt) {
        this.søknadMottatt = søknadMottatt;
        return this;
    }

    public ZonedDateTime getTidspunkt() {
        return tidspunkt;
    }

    public KroniskSyktBarnSøknadRequest setTidspunkt(ZonedDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
        return this;
    }

    public Søker getSøker() {
        return søker;
    }

    public KroniskSyktBarnSøknadRequest setSøker(Søker søker) {
        this.søker = søker;
        return this;
    }

    public KroniskSyktBarnSøknadRequest setBarn(Barn barn) {
        this.barn = barn;
        return this;
    }
}
