package no.nav.ung.fordel.repo;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "FrisinnSøknadEntitet")
@Table(name = "FRISINN_SOKNAD_TASK_PROPS")
public class FrisinnSøknadTaskPropsEntitet {

    public enum Status {
        UBEHANDLET("UBEHANDLET"),
        FERDIG("FERDIG");

        private final String dbKode;

        private Status(String dbKode) {
            this.dbKode = dbKode;
        }

        public String getDbKode() {
            return dbKode;
        }
    }

    @Id
    @Column(name = "journalpost_id", nullable = false)
    private String journalpostId;

    @Column(name = "aktoer_id", nullable = false, updatable = false)
    private String aktørId;

    @Column(name = "forsendelse_mottatt", nullable = false, updatable = false)
    private LocalDate forsendelseMottatt;

    @Column(name = "status")
    private String status;


    private FrisinnSøknadTaskPropsEntitet() {

    }

    private FrisinnSøknadTaskPropsEntitet(String journalpostId, String aktørId, LocalDate forsendelseMottatt, String status) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
        this.forsendelseMottatt = Objects.requireNonNull(forsendelseMottatt, "forsendelseMottatt");
        this.status = Objects.requireNonNull(status, "status");
    }


    public static class Builder {
        private String journalpostId;
        private String aktørId;
        private LocalDate forsendelseMottatt;

        public Builder withJournalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder withAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder withForsendelseMottatt(LocalDate forsendelseMottatt) {
            this.forsendelseMottatt = forsendelseMottatt;
            return this;
        }

        public FrisinnSøknadTaskPropsEntitet build() {
            return new FrisinnSøknadTaskPropsEntitet(journalpostId, aktørId, forsendelseMottatt, Status.UBEHANDLET.getDbKode());
        }
    }

}
