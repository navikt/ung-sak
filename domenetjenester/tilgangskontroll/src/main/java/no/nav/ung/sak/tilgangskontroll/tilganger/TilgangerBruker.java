package no.nav.ung.sak.tilgangskontroll.tilganger;

import java.util.Objects;

public class TilgangerBruker {
    private String brukernavn;
    private boolean kanBehandleKode6;
    private boolean kanBehandleKode7;
    private boolean kanBehandleEgenAnsatt;
    private boolean kanBeslutte;
    private boolean kanOverstyre;
    private boolean kanSaksbehandle;
    private boolean kanVeilede;
    private boolean kanDrifte;

    private TilgangerBruker() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBrukernavn() {
        return brukernavn;
    }

    public boolean kanBehandleKode6() {
        return kanBehandleKode6;
    }

    public boolean kanBehandleKode7() {
        return kanBehandleKode7;
    }

    public boolean kanBehandleEgenAnsatt() {
        return kanBehandleEgenAnsatt;
    }

    public boolean kanBeslutte() {
        return kanBeslutte;
    }

    public boolean kanOverstyre() {
        return kanOverstyre;
    }

    public boolean kanSaksbehandle() {
        return kanSaksbehandle;
    }

    public boolean kanVeilede() {
        return kanVeilede;
    }

    public boolean kanDrifte() {
        return kanDrifte;
    }

    @Override
    public String toString() {
        return "TilgangerBruker{" +
            "brukernavn='" + brukernavn + '\'' +
            ", kanBehandleKode6=" + kanBehandleKode6 +
            ", kanBehandleKode7=" + kanBehandleKode7 +
            ", kanBehandleEgenAnsatt=" + kanBehandleEgenAnsatt +
            ", kanBeslutte=" + kanBeslutte +
            ", kanOverstyre=" + kanOverstyre +
            ", kanSaksbehandle=" + kanSaksbehandle +
            ", kanVeilede=" + kanVeilede +
            ", kanDrifte=" + kanDrifte +
            '}';
    }

    public static class Builder {
        private TilgangerBruker kladd = new TilgangerBruker();

        public Builder medBrukernavn(String brukernavn) {
            kladd.brukernavn = brukernavn;
            return this;
        }

        public Builder medKanBehandleKode6(boolean kanBehandleKode6) {
            kladd.kanBehandleKode6 = kanBehandleKode6;
            return this;
        }

        public Builder medKanBehandleKode7(boolean kanBehandleKode7) {
            kladd.kanBehandleKode7 = kanBehandleKode7;
            return this;
        }

        public Builder medKanBehandleEgenAnsatt(boolean kanBehandleEgenAnsatt) {
            kladd.kanBehandleEgenAnsatt = kanBehandleEgenAnsatt;
            return this;
        }

        public Builder medKanBeslutte(boolean kanBeslutte) {
            kladd.kanBeslutte = kanBeslutte;
            return this;
        }

        public Builder medKanOverstyre(boolean kanOverstyre) {
            kladd.kanOverstyre = kanOverstyre;
            return this;
        }

        public Builder medKanSaksbehandle(boolean kanSaksbehandle) {
            kladd.kanSaksbehandle = kanSaksbehandle;
            return this;
        }

        public Builder medKanVeilede(boolean kanVeilede) {
            kladd.kanVeilede = kanVeilede;
            return this;
        }

        public Builder medKanDrifte(boolean kanDrifte) {
            kladd.kanDrifte = kanDrifte;
            return this;
        }

        public TilgangerBruker build() {
            try {
                Objects.requireNonNull(kladd.brukernavn, "brukernavn");
                return kladd;
            } finally {
                kladd = null;
            }
        }


    }
}
