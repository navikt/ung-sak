package no.nav.ung.sak.kontrakt.mottak;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class JournalpostMottakOpprettSakDto extends JournalpostMottakDto {

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    public JournalpostMottakOpprettSakDto(
        String aktørId,
        Periode periode,
        Saksnummer saksnummer,
        JournalpostId journalpostId,
        FagsakYtelseType ytelseType,
        Brevkode type,
        LocalDateTime forsendelseMottattTidspunkt,
        String payloadRawString
    ) {
        super(saksnummer, journalpostId, ytelseType, type, forsendelseMottattTidspunkt, payloadRawString);
        this.aktørId = aktørId;
        this.periode = periode;

    }

    protected JournalpostMottakOpprettSakDto() {
        super();
    }

    public Periode getPeriode() {
        return periode;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.AKTØR_ID)
    public String getAktørId() {
        return aktørId;
    }

}
