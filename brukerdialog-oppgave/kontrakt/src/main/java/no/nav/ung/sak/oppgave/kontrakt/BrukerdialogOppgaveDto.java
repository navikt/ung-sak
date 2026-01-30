package no.nav.ung.sak.oppgave.kontrakt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class BrukerdialogOppgaveDto {

    @JsonProperty(value = "oppgavereferanse", required = true)
    @NotNull
    @Valid
    private UUID oppgavereferanse;

    @JsonProperty(value = "status", required = true)
    @NotNull
    @Valid
    private String status;

    @JsonProperty(value = "type")
    @Valid
    private String type;

    @JsonProperty(value = "data", required = true)
    @NotNull
    @Valid
    private Object data;

    @JsonProperty(value = "fristTid")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fristTid;

    @JsonProperty(value = "opprettetTidspunkt", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime opprettetTidspunkt;

    @JsonProperty(value = "løstDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime løstDato;

    @JsonProperty(value = "åpnetDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime åpnetDato;

    @JsonProperty(value = "lukketDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lukketDato;

    public BrukerdialogOppgaveDto() {
        // Jackson
    }

    public BrukerdialogOppgaveDto(UUID oppgavereferanse, String status, String type, Object data,
                                   LocalDateTime fristTid, LocalDateTime opprettetTidspunkt,
                                   LocalDateTime løstDato, LocalDateTime åpnetDato, LocalDateTime lukketDato) {
        this.oppgavereferanse = oppgavereferanse;
        this.status = status;
        this.type = type;
        this.data = data;
        this.fristTid = fristTid;
        this.opprettetTidspunkt = opprettetTidspunkt;
        this.løstDato = løstDato;
        this.åpnetDato = åpnetDato;
        this.lukketDato = lukketDato;
    }

    public UUID getOppgavereferanse() {
        return oppgavereferanse;
    }

    public void setOppgavereferanse(UUID oppgavereferanse) {
        this.oppgavereferanse = oppgavereferanse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public LocalDateTime getLøstDato() {
        return løstDato;
    }

    public void setLøstDato(LocalDateTime løstDato) {
        this.løstDato = løstDato;
    }

    public LocalDateTime getÅpnetDato() {
        return åpnetDato;
    }

    public void setÅpnetDato(LocalDateTime åpnetDato) {
        this.åpnetDato = åpnetDato;
    }

    public LocalDateTime getLukketDato() {
        return lukketDato;
    }

    public void setLukketDato(LocalDateTime lukketDato) {
        this.lukketDato = lukketDato;
    }
}

