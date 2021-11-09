package no.nav.k9.sak.hendelse.stønadstatistikk.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseDeserializer;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseSerializer;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkHendelse {

    @JsonProperty(value = "ytelseType", required = true)
    @JsonDeserialize(using = PlainYtelseDeserializer.class)
    @JsonSerialize(using = PlainYtelseSerializer.class)
    @NotNull
    @Valid
    private FagsakYtelseType ytelseType;
    
    @JsonProperty(value = "søker", required = true)
    @NotNull
    @Valid
    private PersonIdent søker;
    
    @JsonProperty(value = "pleietrengende", required = false)
    @Valid
    private PersonIdent pleietrengende;
    
    @JsonProperty(value = "diagnosekoder", required = true)
    @Size(max=1000)
    @NotNull
    @Valid
    private List<StønadstatistikkDiagnosekode> diagnosekoder;
    
    @JsonProperty(value = "saksnummer", required = false)
    @Valid
    private Saksnummer saksnummer;
    
    @JsonProperty(value = "utbetalingsreferanse", required = true)
    @NotNull
    @Valid
    private String utbetalingsreferanse;
    
    @JsonProperty(value = "behandlingUuid", required = false)
    @Valid
    private UUID behandlingUuid;
    
    @JsonProperty(value = "forrigeBehandlingUuid", required = false)
    @Valid
    private UUID forrigeBehandlingUuid;
    
    @JsonProperty(value = "vedtakstidspunkt", required = true)
    @NotNull
    @Valid
    private LocalDateTime vedtakstidspunkt;
    
    @JsonProperty(value = "perioder", required = true)
    @Size(max=100000)
    @NotNull
    @Valid
    private List<StønadstatistikkPeriode> perioder;

    
    public StønadstatistikkHendelse() {
        
    }
    
    public StønadstatistikkHendelse(FagsakYtelseType ytelseType,
            PersonIdent søker,
            PersonIdent pleietrengende,
            List<StønadstatistikkDiagnosekode> diagnosekoder,
            Saksnummer saksnummer,
            String utbetalingsreferanse,
            UUID behandlingUuid,
            UUID forrigeBehandlingUuid,
            LocalDateTime vedtakstidspunkt,
            List<StønadstatistikkPeriode> perioder) {
        this.ytelseType = ytelseType;
        this.søker = søker;
        this.pleietrengende = pleietrengende;
        this.diagnosekoder = diagnosekoder;
        this.saksnummer = saksnummer;
        this.utbetalingsreferanse = utbetalingsreferanse;
        this.behandlingUuid = behandlingUuid;
        this.forrigeBehandlingUuid = forrigeBehandlingUuid;
        this.vedtakstidspunkt = vedtakstidspunkt;
        this.perioder = perioder;
    }
}