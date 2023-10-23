package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseDeserializer;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseSerializer;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkHendelse {
    
    public static class View {
        public static class V1 {}
        public static class V2 extends V1 {}
    }

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
    @Size(max = 1000)
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
    @Size(max = 100000)
    @NotNull
    @Valid
    private List<StønadstatistikkPeriode> perioder;

    @JsonProperty(value = "relasjon", required = false)
    @Size(max = 100000)
    @Valid
    private List<StønadstatistikkRelasjonPeriode> relasjon;


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
                                    List<StønadstatistikkPeriode> perioder,
                                    List<StønadstatistikkRelasjonPeriode> relasjon) {
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
        this.relasjon = relasjon;
    }

    public static StønadstatistikkHendelse forOmsorgspenger(PersonIdent søker,
                                                            Saksnummer saksnummer,
                                                            String utbetalingsreferanse,
                                                            UUID behandlingUuid,
                                                            UUID forrigeBehandlingUuid,
                                                            LocalDateTime vedtakstidspunkt,
                                                            List<StønadstatistikkPeriode> perioder) {
        StønadstatistikkHendelse hendelse = new StønadstatistikkHendelse();
        hendelse.søker = søker;
        hendelse.ytelseType = FagsakYtelseType.OMSORGSPENGER;
        hendelse.saksnummer = saksnummer;
        hendelse.utbetalingsreferanse = utbetalingsreferanse;
        hendelse.behandlingUuid = behandlingUuid;
        hendelse.forrigeBehandlingUuid = forrigeBehandlingUuid;
        hendelse.vedtakstidspunkt = vedtakstidspunkt;
        hendelse.perioder = perioder;

        //ubrukte, sender tom liste etter ønske fra mottaker (stønadsstatistikk)
        hendelse.diagnosekoder = List.of();
        hendelse.relasjon = List.of();
        return hendelse;
    }


    @AssertTrue
    boolean isDetaljertInngangsvilkårBruktKunForOmsorgspenger() {
        return ytelseType == FagsakYtelseType.OMSORGSPENGER || perioder.stream().flatMap(p -> p.getInngangsvilkår().stream()).noneMatch(iv -> iv.getDetaljertUtfall() != null && !iv.getDetaljertUtfall().isEmpty());
    }

    @AssertFalse
    boolean isManglerFelterIPeriodeForPSB() {
        return ytelseType == FagsakYtelseType.PSB && perioder.stream().anyMatch(
            p -> p.getUttaksgrad() == null || p.getSøkersTapteArbeidstid() == null || p.getOppgittTilsyn() == null || p.getPleiebehov() == null || p.getGraderingMotTilsyn() == null
        );
    }

    @AssertFalse
    boolean isManglerFelterIPeriodeForPPN() {
        return ytelseType == FagsakYtelseType.PPN && perioder.stream().anyMatch(
            p -> p.getUttaksgrad() == null || p.getSøkersTapteArbeidstid() == null || p.getPleiebehov() == null
        );
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public PersonIdent getSøker() {
        return søker;
    }

    public PersonIdent getPleietrengende() {
        return pleietrengende;
    }

    public List<StønadstatistikkDiagnosekode> getDiagnosekoder() {
        return diagnosekoder;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public String getUtbetalingsreferanse() {
        return utbetalingsreferanse;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public UUID getForrigeBehandlingUuid() {
        return forrigeBehandlingUuid;
    }

    public LocalDateTime getVedtakstidspunkt() {
        return vedtakstidspunkt;
    }

    public List<StønadstatistikkPeriode> getPerioder() {
        return perioder;
    }

    public List<StønadstatistikkRelasjonPeriode> getRelasjon() {
        if (relasjon == null) {
            return List.of();
        }
        return relasjon;
    }
}
