package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@JsonPropertyOrder({ "saksnummer", "behandlingId", "andrePartersSaker", "søker", "barn", "søknadsperioder", "lovbestemtFerie", "arbeid", "tilsynsbehov", "medlemskap" })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttaksplanRequest {

    @JsonProperty(value = "andrePartersSaker")
    @Valid
    private List<AndrePartSak> andrePartersSaker = new ArrayList<>();

    @JsonProperty(value = "arbeid")
    @Valid
    private List<UttakArbeid> arbeid = new ArrayList<>();

    @JsonAlias("pleietrengende")
    @JsonProperty(value = "barn", required = true)
    @NotNull
    @Valid
    private Person barn;

    @JsonProperty(value = "behandlingId", required = true)
    @Valid
    @NotNull
    private UUID behandlingId;

    @JsonProperty(value = "medlemskap")
    @Valid
    private NavigableMap<Periode, UttakMedlemskap> medlemskap = Collections.emptyNavigableMap();

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "søker", required = true)
    @NotNull
    @Valid
    private Person søker;

    @JsonProperty(value = "søknadsperioder", required = true)
    @Valid
    @NotNull
    @Size(min = 1)
    private List<Periode> søknadsperioder = new ArrayList<Periode>();

    @JsonProperty(value = "lovbestemtFerie", required = true)
    @Valid
    private List<LovbestemtFerie> lovbestemtFerie = new ArrayList<>();

    @JsonProperty(value = "tilsynsbehov")
    @Valid
    private NavigableMap<Periode, UttakTilsynsbehov> tilsynsbehov = Collections.emptyNavigableMap();

    public List<AndrePartSak> getAndrePartersSaker() {
        return Collections.unmodifiableList(andrePartersSaker);
    }

    public List<UttakArbeid> getArbeid() {
        return arbeid;
    }

    public Person getBarn() {
        return barn;
    }

    public UUID getBehandlingId() {
        return behandlingId;
    }

    public NavigableMap<Periode, UttakMedlemskap> getMedlemskap() {
        return medlemskap;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Person getSøker() {
        return søker;
    }

    public List<Periode> getSøknadsperioder() {
        return søknadsperioder;
    }

    public NavigableMap<Periode, UttakTilsynsbehov> getTilsynsbehov() {
        return Collections.unmodifiableNavigableMap(tilsynsbehov);
    }

    public void setLovbestemtFerie(List<LovbestemtFerie> lovbestemtFerie) {
        if (lovbestemtFerie != null) {
            this.lovbestemtFerie = new ArrayList<LovbestemtFerie>(lovbestemtFerie);
            Collections.sort(this.lovbestemtFerie);
        }
    }

    public void setAndrePartersSaker(List<AndrePartSak> andrePartersSaker) {
        if (andrePartersSaker != null) {
            this.andrePartersSaker = new ArrayList<>(andrePartersSaker);
            Collections.sort(this.andrePartersSaker);
        }
    }

    @JsonSetter("arbeid")
    public void setArbeid(List<UttakArbeid> arbeid) {
        if (arbeid != null) {
            this.arbeid = new ArrayList<>(arbeid);
            Collections.sort(this.arbeid);
        }
    }

    public void setBarn(Person barn) {
        this.barn = barn;
    }

    public void setBehandlingId(UUID behandlingId) {
        this.behandlingId = behandlingId;
    }

    @JsonSetter("medlemskap")
    public void setMedlemskap(Map<Periode, UttakMedlemskap> medlemskap) {
        this.medlemskap = medlemskap == null ? null : new TreeMap<>(medlemskap);
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void setSøker(Person søker) {
        this.søker = søker;
    }

    public void setSøknadsperioder(List<Periode> søknadsperioder) {
        if (søknadsperioder == null) {
            this.søknadsperioder = Collections.emptyList();
        } else {
            this.søknadsperioder = new ArrayList<>(søknadsperioder);
            Collections.sort(this.søknadsperioder);
        }
    }

    @JsonSetter("tilsynsbehov")
    public void setTilsynsbehov(Map<Periode, UttakTilsynsbehov> tilsynsbehov) {
        this.tilsynsbehov = tilsynsbehov == null ? Collections.emptyNavigableMap() : new TreeMap<>(tilsynsbehov);
    }

}
