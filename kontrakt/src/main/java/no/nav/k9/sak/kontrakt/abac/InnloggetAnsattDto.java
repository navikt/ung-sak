package no.nav.k9.sak.kontrakt.abac;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InnloggetAnsattDto {

    public static class Builder {
        private String brukernavn;
        private Boolean kanBehandleKode6;
        private Boolean kanBehandleKode7;
        private Boolean kanBehandleKodeEgenAnsatt;
        private Boolean kanBeslutte;
        private Boolean kanOverstyre;
        private Boolean kanSaksbehandle;
        private Boolean kanVeilede;
        private String navn;
        private Boolean skalViseDetaljerteFeilmeldinger;

        Builder() {
            kanSaksbehandle = false;
            kanVeilede = false;
            kanBeslutte = false;
            kanOverstyre = false;
            kanBehandleKodeEgenAnsatt = false;
            kanBehandleKode6 = false;
            kanBehandleKode7 = false;
        }

        public InnloggetAnsattDto create() {
            return new InnloggetAnsattDto(brukernavn, navn, kanSaksbehandle, kanVeilede, kanBeslutte, kanOverstyre, kanBehandleKodeEgenAnsatt, kanBehandleKode6,
                kanBehandleKode7, skalViseDetaljerteFeilmeldinger);
        }

        public Builder setBrukernavn(String brukernavn) {
            this.brukernavn = brukernavn;
            return this;
        }

        public Builder setKanBehandleKode6(Boolean kanBehandleKode6) {
            this.kanBehandleKode6 = kanBehandleKode6;
            return this;
        }

        public Builder setKanBehandleKode7(Boolean kanBehandleKode7) {
            this.kanBehandleKode7 = kanBehandleKode7;
            return this;
        }

        public Builder setKanBehandleKodeEgenAnsatt(Boolean kanBehandleKodeEgenAnsatt) {
            this.kanBehandleKodeEgenAnsatt = kanBehandleKodeEgenAnsatt;
            return this;
        }

        public Builder setKanBeslutte(Boolean kanBeslutte) {
            this.kanBeslutte = kanBeslutte;
            return this;
        }

        public Builder setKanOverstyre(Boolean kanOverstyre) {
            this.kanOverstyre = kanOverstyre;
            return this;
        }

        public Builder setKanSaksbehandle(Boolean kanSaksbehandle) {
            this.kanSaksbehandle = kanSaksbehandle;
            return this;
        }

        public Builder setKanVeilede(Boolean kanVeilede) {
            this.kanVeilede = kanVeilede;
            return this;
        }

        public Builder setNavn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder skalViseDetaljerteFeilmeldinger(Boolean skalViseDetaljerteFeilmeldinger) {
            this.skalViseDetaljerteFeilmeldinger = skalViseDetaljerteFeilmeldinger;
            return this;
        }
    }

    @JsonProperty(value = "brukernavn", required = true)
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    private final String brukernavn;

    @JsonProperty(value = "funksjonellTid")
    @NotNull
    private final LocalDateTime funksjonellTid;

    @JsonProperty(value = "kanBehandleKode6")
    private final boolean kanBehandleKode6;

    @JsonProperty(value = "kanBehandleKode7")
    private final boolean kanBehandleKode7;

    @JsonProperty(value = "kanBehandleKodeEgenAnsatt")
    private final boolean kanBehandleKodeEgenAnsatt;

    @JsonProperty(value = "kanBeslutte")
    private final boolean kanBeslutte;

    @JsonProperty(value = "kanOverstyre")
    private final boolean kanOverstyre;

    @JsonProperty(value = "kanSaksbehandle")
    private final boolean kanSaksbehandle;

    @JsonProperty(value = "kanVeilede")
    private final boolean kanVeilede;

    @JsonProperty(value = "navn")
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private final String navn;

    @JsonProperty(value = "skalViseDetaljerteFeilmeldinger")
    private final boolean skalViseDetaljerteFeilmeldinger;

    private InnloggetAnsattDto(
                               String brukernavn,
                               String navn,
                               boolean kanSaksbehandle,
                               boolean kanVeilede,
                               boolean kanBeslutte,
                               boolean kanOverstyre,
                               boolean kanBehandleKodeEgenAnsatt,
                               boolean kanBehandleKode6,
                               boolean kanBehandleKode7,
                               boolean skalViseDetaljerteFeilmeldinger) {
        this.brukernavn = brukernavn;
        this.navn = navn;
        this.kanSaksbehandle = kanSaksbehandle;
        this.kanVeilede = kanVeilede;
        this.kanBeslutte = kanBeslutte;
        this.kanOverstyre = kanOverstyre;
        this.kanBehandleKodeEgenAnsatt = kanBehandleKodeEgenAnsatt;
        this.kanBehandleKode6 = kanBehandleKode6;
        this.kanBehandleKode7 = kanBehandleKode7;
        this.skalViseDetaljerteFeilmeldinger = skalViseDetaljerteFeilmeldinger;
        this.funksjonellTid = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBrukernavn() {
        return brukernavn;
    }

    public LocalDateTime getFunksjonellTid() {
        return funksjonellTid;
    }

    public boolean getKanBehandleKode6() {
        return kanBehandleKode6;
    }

    public boolean getKanBehandleKode7() {
        return kanBehandleKode7;
    }

    public boolean getKanBehandleKodeEgenAnsatt() {
        return kanBehandleKodeEgenAnsatt;
    }

    public boolean getKanBeslutte() {
        return kanBeslutte;
    }

    public boolean getKanOverstyre() {
        return kanOverstyre;
    }

    public boolean getKanSaksbehandle() {
        return kanSaksbehandle;
    }

    public boolean getKanVeilede() {
        return kanVeilede;
    }

    public String getNavn() {
        return navn;
    }

    public boolean getSkalViseDetaljerteFeilmeldinger() {
        return skalViseDetaljerteFeilmeldinger;
    }
}
