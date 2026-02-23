package no.nav.ung.sak.oppgave;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;
import no.nav.ung.sak.typer.AktørId;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ColumnTransformer;
import no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.oppgave.søkytelse.SøkYtelseOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode.EndretPeriodeOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato.EndretSluttdatoOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato.EndretStartdatoOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode.FjernetPeriodeOppgaveDataEntitet;
import no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgaveDataEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "BrukerdialogOppgave")
@Table(name = "BD_OPPGAVE")
public class BrukerdialogOppgaveEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "oppgavereferanse", nullable = false, updatable = false, unique = true)
    private UUID oppgavereferanse;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveStatus status = OppgaveStatus.ULØST;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private OppgaveType oppgaveType;


    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Column(name = "løst_dato")
    private LocalDateTime løstDato; // NOSONAR

    @Column(name = "åpnet_dato")
    private LocalDateTime åpnetDato; // NOSONAR

    @Column(name = "lukket_dato")
    private LocalDateTime lukketDato; // NOSONAR

    @Convert(converter = OppgaveBekreftelseConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "bekreftelse", columnDefinition = "jsonb")
    private BekreftelseDTO bekreftelse;

    @Any
    @AnyDiscriminator(DiscriminatorType.STRING)
    @AnyDiscriminatorValues({
        @AnyDiscriminatorValue(discriminator = "BEKREFT_ENDRET_STARTDATO",      entity = EndretStartdatoOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "BEKREFT_ENDRET_SLUTTDATO",      entity = EndretSluttdatoOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "BEKREFT_FJERNET_PERIODE",       entity = FjernetPeriodeOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "BEKREFT_ENDRET_PERIODE",        entity = EndretPeriodeOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "BEKREFT_AVVIK_REGISTERINNTEKT", entity = KontrollerRegisterinntektOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "RAPPORTER_INNTEKT",             entity = InntektsrapporteringOppgaveDataEntitet.class),
        @AnyDiscriminatorValue(discriminator = "SØK_YTELSE",                   entity = SøkYtelseOppgaveDataEntitet.class),
    })
    @AnyKeyJavaClass(Long.class)
    @Column(name = "type", insertable = false, updatable = false, nullable = false)
    @JoinColumn(name = "oppgave_data_id")
    @SuppressWarnings("JpaAttributeTypeInspection") // @Any er ikke støttet av IntelliJs JPA-inspeksjon
    private OppgaveDataEntitet oppgaveData;

    protected BrukerdialogOppgaveEntitet() {
        // For JPA
    }

    public BrukerdialogOppgaveEntitet(UUID oppgavereferanse,
                                      OppgaveType oppgaveType,
                                      AktørId aktørId,
                                      LocalDateTime fristTid) {
        this.oppgavereferanse = oppgavereferanse;
        this.oppgaveType = oppgaveType;
        this.aktørId = aktørId;
        this.fristTid = fristTid;
    }

    /**
     * Konstruktør for migrering av oppgave fra annen applikasjon.
     * Brukes når alle felter inkludert status og datoer skal settes.
     */
    public BrukerdialogOppgaveEntitet(UUID oppgavereferanse,
                                      OppgaveType oppgaveType,
                                      AktørId aktørId,
                                      BekreftelseDTO bekreftelse,
                                      OppgaveStatus status,
                                      LocalDateTime fristTid,
                                      LocalDateTime løstDato,
                                      LocalDateTime åpnetDato,
                                      LocalDateTime lukketDato) {
        this.oppgavereferanse = oppgavereferanse;
        this.oppgaveType = oppgaveType;
        this.aktørId = aktørId;
        this.bekreftelse = bekreftelse;
        this.status = status;
        this.fristTid = fristTid;
        this.løstDato = løstDato;
        this.åpnetDato = åpnetDato;
        this.lukketDato = lukketDato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public UUID getOppgavereferanse() {
        return oppgavereferanse;
    }

    public OppgaveStatus getStatus() {
        return status;
    }

    public OppgaveType getOppgaveType() {
        return oppgaveType;
    }


    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    protected void setStatus(OppgaveStatus status) {
        this.status = status;
    }

    public void setLøstDato(LocalDateTime løstDato) {
        this.løstDato = løstDato;
    }

    public LocalDateTime getLøstDato() {
        return løstDato;
    }

    public void setÅpnetDato(LocalDateTime åpnetDato) {
        this.åpnetDato = åpnetDato;
    }

    public LocalDateTime getÅpnetDato() {
        return åpnetDato;
    }

    public void setLukketDato(LocalDateTime lukketDato) {
        this.lukketDato = lukketDato;
    }

    public LocalDateTime getLukketDato() {
        return lukketDato;
    }

    public BekreftelseDTO getBekreftelse() {
        return bekreftelse;
    }

    public void setBekreftelse(BekreftelseDTO bekreftelse) {
        this.bekreftelse = bekreftelse;
    }

    Long getId() {
        return id;
    }

    public OppgaveDataEntitet getOppgaveData() {
        return oppgaveData;
    }

    public void setOppgaveData(OppgaveDataEntitet oppgaveData) {
        this.oppgaveData = oppgaveData;
    }
}
