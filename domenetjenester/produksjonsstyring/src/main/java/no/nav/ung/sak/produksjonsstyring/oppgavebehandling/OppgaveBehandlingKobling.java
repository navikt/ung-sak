package no.nav.ung.sak.produksjonsstyring.oppgavebehandling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.typer.Saksnummer;

@Entity(name = "OppgaveBehandlingKobling")
@Table(name = "OPPGAVE_BEHANDLING_KOBLING")
public class OppgaveBehandlingKobling extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPGAVE_BEHANDLING_KOBLING")
    private Long id;

    @Convert(converter = OppgaveÅrsakKodeverdiConverter.class)
    @Column(name="oppgave_aarsak", nullable = false)
    private OppgaveÅrsak oppgaveÅrsak;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    /**
     * Offisielt tildelt saksnummer fra GSAK.
     */
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false, updatable = false)))
    private Saksnummer saksnummer;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "ferdigstilt")
    private Boolean ferdigstilt = false;

    @Column(name = "ferdigstilt_av")
    private String ferdigstiltAv;

    @Column(name = "ferdigstilt_tid")
    private LocalDateTime ferdigstiltTid;

    OppgaveBehandlingKobling() {
        // Hibernate
    }

    public OppgaveBehandlingKobling(OppgaveÅrsak oppgaveÅrsak, String oppgaveId, Saksnummer saksnummer, Behandling behandling) {
        this.setOppgaveÅrsak(oppgaveÅrsak);
        this.oppgaveId = oppgaveId;
        this.saksnummer = saksnummer;
        this.behandling = behandling;
    }

    public Long getId() {
        return id;
    }

    //GSAK-oppgave-id
    public String getOppgaveId() {
        return oppgaveId;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void ferdigstillOppgave(String ferdigstiltAv) {
        this.ferdigstiltTid = LocalDateTime.now();
        this.ferdigstiltAv = ferdigstiltAv;
        this.ferdigstilt = true;
    }

    public Boolean isFerdigstilt() {
        return ferdigstilt;
    }

    public void setFerdigstilt(Boolean ferdigstilt) {
        this.ferdigstilt = ferdigstilt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof OppgaveBehandlingKobling)) {
            return false;
        }
        OppgaveBehandlingKobling other = (OppgaveBehandlingKobling) obj;
        return Objects.equals(getOppgaveId(), other.getOppgaveId())
            && Objects.equals(getOppgaveÅrsak(), other.getOppgaveÅrsak())
            && Objects.equals(behandling, other.behandling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgaveId, getOppgaveÅrsak(), behandling);
    }

    public OppgaveÅrsak getOppgaveÅrsak() {
        return Objects.equals(oppgaveÅrsak, OppgaveÅrsak.UDEFINERT) ? null : oppgaveÅrsak;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public String getFerdigstiltAv() {
        return ferdigstiltAv;
    }

    private void setOppgaveÅrsak(OppgaveÅrsak oppgaveÅrsak) {
        this.oppgaveÅrsak = oppgaveÅrsak == null ? OppgaveÅrsak.UDEFINERT : oppgaveÅrsak;
    }

    public static Optional<OppgaveBehandlingKobling> getAktivOppgaveMedÅrsak(OppgaveÅrsak årsak, List<OppgaveBehandlingKobling> oppgaver) {
        return oppgaver
            .stream()
            .filter(oppgave -> !oppgave.isFerdigstilt())
            .filter(oppgave -> årsak.equals(oppgave.getOppgaveÅrsak()))
            .findFirst();
    }
}


