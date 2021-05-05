package no.nav.k9.sak.kontrakt.vedtak;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VedtakHendelse {

    @NotNull
    @JsonProperty("behandlingId")
    private UUID behandlingId;

    @NotNull
    @JsonProperty("behandlingType")
    private BehandlingType behandlingType;

    @NotNull
    @JsonProperty("fagsakYtelseType")
    private FagsakYtelseType fagsakYtelseType;

    @Pattern(regexp = "^(-?[1-9]|[a-z0])[a-z0-9_:-]*$", flags = { Pattern.Flag.CASE_INSENSITIVE })
    @JsonProperty("saksnummer")
    private String saksnummer;

    @NotNull
    @JsonProperty("status")
    private FagsakStatus status;

    @JsonProperty("fagsystem")
    @NotNull
    private Fagsystem fagsystem;

    @NotNull
    @Valid
    @JsonProperty("aktør")
    private AktørId aktør;

    @NotNull
    @JsonProperty("vedtakResultatType")
    private VedtakResultatType vedtakResultatType;

    @NotNull
    @JsonProperty("behandlingResultatType")
    private BehandlingResultatType behandlingResultatType;

    @NotNull
    @Valid
    @JsonProperty("vedtattTidspunkt")
    private LocalDateTime vedtattTidspunkt;

    /** @NotNull produseres alltid, men er nullable for kompatiblitet med eldre versjoner */
    @Valid
    @JsonProperty(value = "fagsakPeriode", required = false)
    private Periode fagsakPeriode;

    /** (Optional) AktørId for angitt pleietrengende (barn, eller nærstående). */
    @Valid
    @JsonProperty(value = "pleietrengendeAktørId")
    private AktørId pleietrengendeAktørId;

    /** (Optional) AktørId for angitt relatert annen part (eks. annen forelder. */
    @Valid
    @JsonProperty(value = "relatertPartAktørId", required = false)
    private AktørId relatertPartAktørId;

    public Periode getFagsakPeriode() {
        return fagsakPeriode;
    }

    public void setFagsakPeriode(Periode fagsakPeriode) {
        this.fagsakPeriode = fagsakPeriode;
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public void setPleietrengendeAktørId(AktørId pleietrengendeAktørId) {
        this.pleietrengendeAktørId = pleietrengendeAktørId;
    }

    public AktørId getRelatertPartAktørId() {
        return relatertPartAktørId;
    }

    public void setRelatertPartAktørId(AktørId relatertPartAktørId) {
        this.relatertPartAktørId = relatertPartAktørId;
    }

    public AktørId getAktør() {
        return aktør;
    }

    public void setAktør(AktørId aktør) {
        this.aktør = aktør;
    }

    public LocalDateTime getVedtattTidspunkt() {
        return vedtattTidspunkt;
    }

    public void setVedtattTidspunkt(LocalDateTime vedtattTidspunkt) {
        this.vedtattTidspunkt = vedtattTidspunkt;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public void setFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public UUID getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(UUID behandlingId) {
        this.behandlingId = behandlingId;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public Fagsystem getFagsystem() {
        return fagsystem;
    }

    public void setFagsystem(Fagsystem fagsystem) {
        this.fagsystem = fagsystem;
    }

    public VedtakResultatType getVedtakResultatType() {
        return vedtakResultatType;
    }

    public void setVedtakResultat(VedtakResultatType vedtakResultatType) {
        this.vedtakResultatType = vedtakResultatType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
    }

    public void setVedtakResultatType(VedtakResultatType vedtakResultatType) {
        this.vedtakResultatType = vedtakResultatType;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public void setBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        this.behandlingResultatType = behandlingResultatType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        VedtakHendelse that = (VedtakHendelse) o;
        return behandlingId.equals(that.behandlingId) &&
            behandlingType == that.behandlingType &&
            fagsakYtelseType == that.fagsakYtelseType &&
            saksnummer.equals(that.saksnummer) &&
            status == that.status &&
            fagsystem == that.fagsystem &&
            aktør.equals(that.aktør) &&
            vedtakResultatType == that.vedtakResultatType &&
            behandlingResultatType == that.behandlingResultatType &&
            vedtattTidspunkt.equals(that.vedtattTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            behandlingId,
            behandlingType,
            fagsakYtelseType,
            saksnummer,
            status,
            fagsystem,
            aktør,
            vedtakResultatType,
            behandlingResultatType,
            vedtattTidspunkt);
    }

    @Override
    public String toString() {
        return "VedtakHendelse{" +
            "behandlingId=" + behandlingId +
            ", behandlingType=" + behandlingType +
            ", fagsakYtelseType=" + fagsakYtelseType +
            ", saksnummer=" + saksnummer +
            ", status=" + status +
            ", fagsystem=" + fagsystem +
            ", vedtakResultatType=" + vedtakResultatType +
            ", behandlingResultatType=" + behandlingResultatType +
            ", vedtattTidspunkt=" + vedtattTidspunkt +
            '}';
    }
}
