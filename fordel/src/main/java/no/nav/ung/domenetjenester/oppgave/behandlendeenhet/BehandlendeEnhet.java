package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import java.util.Objects;

public class BehandlendeEnhet {
    public final String nummer;
    public final String navn;

    public BehandlendeEnhet(String nummer, String navn) {
        this.nummer = nummer;
        this.navn = navn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BehandlendeEnhet that = (BehandlendeEnhet) o;

        return Objects.equals(nummer, that.nummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nummer);
    }

    @Override
    public String toString() {
        return "BehandlendeEnhet{" +
                "nummer='" + nummer + '\'' +
                ", navn='" + navn + '\'' +
                '}';
    }
}
