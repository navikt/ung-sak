package no.nav.k9.sak.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OrganisasjonsNummerValidatorTest {

    @Test
    public void erGyldig() {
        assertThat(OrganisasjonsNummerValidator.erGyldig("910909088")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("974760673")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("123123341")).isFalse();

        assertThat(OrganisasjonsNummerValidator.erGyldig("812157892")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("812400452")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("980003698")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("974600897")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("974600951")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("987920823")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("987921226")).isTrue();

        assertThat(OrganisasjonsNummerValidator.erGyldig("980018350")).isTrue();
        assertThat(OrganisasjonsNummerValidator.erGyldig("974600250")).isTrue();

        // kunstig org for saksbehandlers endringer.
        assertThat(OrganisasjonsNummerValidator.erGyldig(OrgNummer.KUNSTIG_ORG)).isFalse();

        assertThat(OrganisasjonsNummerValidator.erGyldig("1")).isFalse();
    }
}
