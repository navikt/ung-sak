package no.nav.ung.fordel.repo;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

@Entity(name = "ProduksjonsstyringOppgaveEntitet")
@Table(name = "PRODUKSJONSSTYRING_OPPGAVE")
public class ProduksjonsstyringOppgaveEntitet {

    @Id
    @Column(name = "journalpost_id", nullable = false, updatable = false)
    private String journalpostId;

    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    @Column(name = "ytelse_type", nullable = false)
    private String ytelseType;

    @Column(name = "fagsak_system", nullable = false)
    private String fagsakSystem;

    @Column(name = "oppgave_type", nullable = false)
    private String oppgaveType;

    @Column(name = "oppgave_id")
    private String oppgaveId;

    @Column(name = "beskrivelse")
    private String beskrivelse;

    @Column(name = "behandlingstema")
    private String behandlingstema;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt; // NOSONAR

    private ProduksjonsstyringOppgaveEntitet() {
        // for jpa/hibernate proxy
    }

    private ProduksjonsstyringOppgaveEntitet(String journalpostId,
                                             String aktørId,
                                             FagsakYtelseType ytelseType,
                                             BehandlingTema behandlingstema,
                                             String beskrivelse,
                                             GosysKonstanter.Fagsaksystem fagsakSystem,
                                             GosysKonstanter.OppgaveType oppgaveType) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.aktørId = aktørId;
        this.fagsakSystem = (fagsakSystem == null) ? null : fagsakSystem.getKode();
        this.oppgaveType = Objects.requireNonNull(oppgaveType, "oppgaveType").getKode();
        this.ytelseType = ytelseType.getKode();
        this.beskrivelse = beskrivelse;
        this.behandlingstema = behandlingstema == null ? null : behandlingstema.getKode();
    }

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    public void setOppgaveId(String oppgaveId) {
        if (this.oppgaveId != null) {
            throw new IllegalStateException("har allerede satt oppgaveId [" + this.oppgaveId + "], kan ikke overstyre til [" + oppgaveId + "]");
        }
        this.oppgaveId = oppgaveId;
    }

    public static class Builder {
        private String journalpostId;
        private String aktørId;
        private FagsakYtelseType ytelseType;
        private String beskrivelse;
        private GosysKonstanter.Fagsaksystem fagsaksystem;
        private GosysKonstanter.OppgaveType oppgaveType;
        private BehandlingTema behandlingstema;

        public Builder withJournalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder withAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }


        public Builder withYtelseType(FagsakYtelseType ytelseType) {
            this.ytelseType = ytelseType;
            return this;
        }

        public Builder withBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder withFagsaksystem(GosysKonstanter.Fagsaksystem fagsaksystem) {
            this.fagsaksystem = fagsaksystem;
            return this;
        }

        public Builder withOppgaveType(GosysKonstanter.OppgaveType oppgaveType) {
            this.oppgaveType = oppgaveType;
            return this;
        }

        public Builder withBehandlingTema(BehandlingTema behandlingTema) {
            this.behandlingstema = behandlingTema;
            return this;
        }

        public ProduksjonsstyringOppgaveEntitet build() {
            return new ProduksjonsstyringOppgaveEntitet(
                journalpostId,
                aktørId,
                ytelseType,
                behandlingstema,
                beskrivelse,
                fagsaksystem, oppgaveType);
        }

    }

}
