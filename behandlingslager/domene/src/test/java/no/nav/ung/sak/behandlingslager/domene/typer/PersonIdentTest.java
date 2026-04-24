package no.nav.ung.sak.behandlingslager.domene.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.ung.sak.typer.PersonIdent;

public class PersonIdentTest {

    @Test
    public void gyldigFoedselsnummer_Fnr() {
        String fnr = "17420373147";
        boolean gyldig = PersonIdent.erGyldigFnr(fnr);
        assertThat(gyldig).isEqualTo(true);

        assertThat(new PersonIdent(fnr).erDnr()).isFalse();
    }

    @Test
    public void gyldigFoedselsnummer_Dnr() {
        String dnr = "53488838241";
        boolean gyldig = PersonIdent.erGyldigFnr(dnr);
        assertThat(gyldig).isEqualTo(true);

        assertThat(new PersonIdent(dnr).erDnr()).isTrue();
    }

    @Test
    public void ugyldigFoedselsnummer() {
        String foedselsnummer = "31048518434";
        boolean gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isEqualTo(false);

        foedselsnummer = "9999999999";
        gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isEqualTo(false);
    }

    @Test
    void test_personnr() throws Exception {
        String fnr = "24420167209";
        boolean gyldig = PersonIdent.erGyldigFnr(fnr);
        assertThat(gyldig).isEqualTo(true);

        PersonIdent personIdent = new PersonIdent(fnr);
        assertThat(personIdent.erDnr()).isFalse();
        assertThat(personIdent.erAktørId()).isFalse();
        assertThat(personIdent.erNorskIdent()).isTrue();
        assertThat(personIdent.erFdatNummer()).isFalse();
        assertThat(personIdent.erFnr()).isTrue();
    }
}
