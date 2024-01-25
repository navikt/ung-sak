package no.nav.k9.sak.kontrakt.mottak;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class JournalpostMottakOpprettSakDto extends JournalpostMottakDto {

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    @JsonProperty(value = "pleietrengendeAktørId")
    @Digits(integer = 19, fraction = 0)
    private String pleietrengendeAktørId;

    @JsonProperty(value = "relatertPersonAktørId")
    @Digits(integer = 19, fraction = 0)
    private String relatertPersonAktørId;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    public JournalpostMottakOpprettSakDto(
        String aktørId,
        String pleietrengendeAktørId,
        String relatertPersonAktørId,
        Periode periode,
        Saksnummer saksnummer,
        JournalpostId journalpostId,
        FagsakYtelseType ytelseType,
        String kanalReferanse,
        Brevkode type,
        LocalDateTime forsendelseMottattTidspunkt,
        String payloadRawString
    ) {
        super(saksnummer, journalpostId, ytelseType, kanalReferanse, type, forsendelseMottattTidspunkt, payloadRawString);
        this.aktørId = aktørId;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.relatertPersonAktørId = relatertPersonAktørId;
        this.periode = periode;

    }

    protected JournalpostMottakOpprettSakDto() {
        super();
    }

    public Periode getPeriode() {
        return periode;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktørId() {
        return aktørId;
    }

    public String getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public String getRelatertPersonAktørId() {
        return relatertPersonAktørId;
    }
}
