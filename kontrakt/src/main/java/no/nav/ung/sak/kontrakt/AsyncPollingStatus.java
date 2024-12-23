package no.nav.ung.sak.kontrakt;

import java.net.URI;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Asynk status returnert fra server ved long-polling jobs. Typisk flyt:
 * 1. Klient sender request, får HTTP 202 Accepted + Location (for status url)
 * 2. status url gir HTTP 200 + denne som output sålenge jobb ikke er ferdig
 * 3. status url gir HTTP 303 + Location for endelig svar når jobb er fedig.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AsyncPollingStatus {

    public enum Status {
        CANCELLED(418),
        COMPLETE(303),
        DELAYED(418),
        HALTED(418),
        PENDING(200);

        private int httpStatus;

        Status(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    @JsonProperty(value = "cancelUri")
    private URI cancelUri;

    @JsonProperty(value = "eta")
    private LocalDateTime eta;

    @JsonProperty(value = "location")
    private URI location;

    @JsonProperty(value = "message")
    private String message;

    @JsonProperty(value = "pollIntervalMillis")
    private Long pollIntervalMillis;

    @JsonProperty(value = "readOnly")
    private boolean readOnly;

    @JsonProperty(value = "status")
    private Status status;

    public AsyncPollingStatus(Status status) {
        this(status, null);
    }

    public AsyncPollingStatus(Status status, LocalDateTime eta, String message) {
        this(status, eta, message, null, null);
    }

    public AsyncPollingStatus(Status status, LocalDateTime eta, String message, URI cancelUri, Long pollIntervalMillis) {
        this.status = status;
        this.eta = eta;
        this.message = message;
        this.cancelUri = cancelUri;
        this.pollIntervalMillis = pollIntervalMillis;
        this.readOnly = status == Status.PENDING || status == Status.DELAYED || status == Status.HALTED;
    }

    public AsyncPollingStatus(Status status, String message) {
        this(status, message, 0L);
    }

    public AsyncPollingStatus(Status status, String message, long pollIntervalMillis) {
        this(status, null, message, null, pollIntervalMillis);
    }

    protected AsyncPollingStatus() {
    }

    public URI getCancelUri() {
        return cancelUri;
    }

    public LocalDateTime getEta() {
        return eta;
    }

    public URI getLocation() {
        // kan returneres også i tilfelle feil, for å kunne hente nåværende tilstand, uten hensyn til hva som ikke kan kjøres videre.
        return location;
    }

    public String getMessage() {
        return message;
    }

    public Long getPollIntervalMillis() {
        return pollIntervalMillis;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isPending() {
        return Status.PENDING.equals(getStatus());
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setLocation(URI uri) {
        this.location = uri;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "status=" + status
            + (message == null ? "" : ", message=" + message)
            + (eta == null ? "" : ", eta=" + eta)
            + (", readOnly=" + readOnly)
            + (pollIntervalMillis == null ? "" : ", pollIntervalMillis=" + pollIntervalMillis)
            + (location == null ? "" : ", location=" + location)
            + (cancelUri == null ? "" : ", cancelUri=" + cancelUri)
            + ">";
    }
}
