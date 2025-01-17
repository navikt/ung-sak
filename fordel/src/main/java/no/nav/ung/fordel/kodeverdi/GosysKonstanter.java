package no.nav.ung.fordel.kodeverdi;

public class GosysKonstanter {
    private GosysKonstanter() {}

    public enum TemaGruppe {
        FAMILIE("FMLI");
        final String dto;
        TemaGruppe(String dto) {
            this.dto = dto;
        }
        public String getKode() {
            return dto;
        }
    }
    public enum OppgaveType {
        GENERELL("GEN"),
        JOURNALFØRING("JFR"),
        VURDER_DOKUMENT("VUR");
        final String dto;
        OppgaveType(String dto) {
            this.dto = dto;
        }

        public String getKode() {
            return dto;
        }

        public static OppgaveType from(String s) {
            for (OppgaveType o : values()) {
                if (o.dto.equals(s)) {
                    return o;
                }
            }
            return null;
        }
    }
    public enum Prioritet {
        NORMAL("NORM");
        final String dto;
        Prioritet(String dto) {
            this.dto = dto;
        }
    }
    public enum Fagsaksystem {
        INFOTRYGD("IT00"),
        K9("FS39");
        final String dto;
        Fagsaksystem(String dto) {
            this.dto = dto;
        }

        public String getKode() {
            return dto;
        }

        public static Fagsaksystem from(String s) {
            for (Fagsaksystem f : values()) {
                if (f.dto.equals(s)) {
                    return f;
                }
            }
            return null;
        }
    }
    public enum JournalpostSystem {
        JOARK("AS36");
        final String dto;
        JournalpostSystem(String dto) {
            this.dto = dto;
        }
    }

    public enum Status {
        FERDIGSTILT("FERDIGSTILT"),
        FEILREGISTRERT("FEILREGISTRERT");
        final String dto;
        Status(String dto) {
            this.dto = dto;
        }
    }
}
