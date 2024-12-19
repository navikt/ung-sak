package no.nav.ung.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProduksjonsstyringBehandlingOpprettetHendelse extends ProduksjonsstyringHendelse {
    public final String saksnummer;
    public final String ytelseType;
    public final String behandlingType;
    public final LocalDate behandlingstidFrist;
    public final Periode fagsakPeriode;
    public final AktørId søkersAktørId;


    @JsonCreator
    public ProduksjonsstyringBehandlingOpprettetHendelse(
        @JsonProperty("eksternId") UUID eksternId,
        @JsonProperty("hendelseTid") LocalDateTime hendelseTid,
        @JsonProperty("saksnummer") String saksnummer,
        @JsonProperty("ytelseType") FagsakYtelseType ytelseType,
        @JsonProperty("behandlingType") BehandlingType behandlingType,
        @JsonProperty("behandlingstidFrist") LocalDate behandlingstidFrist,
        @JsonProperty("fagsakPeriode") Periode fagsakPeriode,
        @JsonProperty("søkersAktørId") AktørId søkersAktørId
    ) {
        super(eksternId, hendelseTid, UngSakHendelseType.BEHANDLING_OPPRETTET);
        this.saksnummer = saksnummer;
        this.ytelseType = ytelseType.getKode();
        this.behandlingType = behandlingType.getKode();
        this.behandlingstidFrist = behandlingstidFrist;
        this.fagsakPeriode = fagsakPeriode;
        this.søkersAktørId = søkersAktørId;
    }
}
