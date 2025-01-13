package no.nav.ung.fordel.kodeverdi;

public class GosysKonstanter {
    private GosysKonstanter() {}

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

}
