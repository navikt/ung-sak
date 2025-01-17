package no.nav.ung.fordel.repo;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "InntektsmeldingEntitet")
@Table(name = "INNTEKTSMELDING_TASK_PROPS")
public class InntektsmeldingTaskPropsEntitet {

    public enum Status {
        UBEHANDLET("UBEHANDLET"),
        TIL_OPPGAVE("TIL_OPPGAVE"),
        PROSESSERER("PROSESSERER"),
        TIL_SAK("TIL_SAK");

        private final String dbKode;

        private Status(String dbKode) {
            this.dbKode = dbKode;
        }

        public String getDbKode() {
            return dbKode;
        }
    }

    @Id
    @Column(name = "journalpost_id")
    private String journalpostId;

    @Column(name = "aktoer_id")
    private String aktørId;

    @Column(name = "ar_referanse")
    private String arReferanse;

    @Column(name = "arbeidsgiver")
    private String arbeidsgiver;

    @Column(name = "status")
    private String status;


    private InntektsmeldingTaskPropsEntitet() {

    }

    private InntektsmeldingTaskPropsEntitet(String journalpostId, String aktørId, String arReferanse, String arbeidsgiver, String status) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
        this.arReferanse = Objects.requireNonNull(arReferanse, "arReferanse");
        this.arbeidsgiver = Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        this.status = Objects.requireNonNull(status, "status");
    }


    public static class Builder {
        private String journalpostId;
        private String aktørId;
        private String arReferanse;
        private String arbeidsgiver;

        public Builder withJournalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder withAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder withArReferanse(String arReferanse) {
            this.arReferanse = arReferanse;
            return this;
        }

        public Builder withArbeidsgiver(String arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public InntektsmeldingTaskPropsEntitet build() {
            return new InntektsmeldingTaskPropsEntitet(journalpostId, aktørId, arReferanse, arbeidsgiver, Status.UBEHANDLET.getDbKode());
        }

    }

}
