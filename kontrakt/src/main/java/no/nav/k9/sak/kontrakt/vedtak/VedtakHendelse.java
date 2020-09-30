package no.nav.k9.sak.kontrakt.vedtak;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.typer.AktørId;

public class VedtakHendelse {

    @NotNull
    @JsonProperty("behandlingId")
    private UUID behandlingId;

    @NotNull
    @JsonProperty("type")
    private FagsakYtelseType type;

    @Pattern(
        regexp = "^(-?[1-9]|[a-z0])[a-z0-9_:-]*$",
        flags = {Pattern.Flag.CASE_INSENSITIVE}
    )
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
    private VedtakResultatType vedtakResultatType;

    @NotNull
    @Valid
    @JsonProperty("vedtattTidspunkt")
    private LocalDateTime vedtattTidspunkt;

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

    public FagsakYtelseType getType() {
        return type;
    }

    public void setType(FagsakYtelseType type) {
        this.type = type;
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
}
