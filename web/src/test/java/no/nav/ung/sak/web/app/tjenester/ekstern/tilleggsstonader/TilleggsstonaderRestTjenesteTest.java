package no.nav.ung.sak.web.app.tjenester.ekstern.tilleggsstonader;

import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPeriode;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPerioderRequest;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerPerioderTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.perioder.InnvilgedePerioder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TilleggsstonaderRestTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);
    private static final String KONSUMENT = "dev-gcp:tilleggsstonader:tilleggsstonader-integrasjoner";

    private final AktørTjeneste aktørTjeneste = mock(AktørTjeneste.class);
    private final AktivitetspengerPerioderTjeneste perioderTjeneste = mock(AktivitetspengerPerioderTjeneste.class);
    private final TilleggsstonaderRestTjeneste tjeneste = new TilleggsstonaderRestTjeneste(aktørTjeneste, perioderTjeneste);

    private final PersonIdent ident = new PersonIdent("12345678901");
    private final Set<AktørId> aktørIder = Set.of(AktørId.dummy());
    private final AktivitetspengerPerioderRequest request = new AktivitetspengerPerioderRequest(ident, FOM, TOM);

    @BeforeEach
    void setUp() {
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(ident)).thenReturn(aktørIder);
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(KONSUMENT);
    }

    @AfterEach
    void tearDown() {
        SubjectHandlerUtils.reset();
        SubjectHandlerUtils.unsetSubjectHandler();
        MDC.clear();
    }

    @Test
    void skal_svare_med_perioder_og_legge_konsument_endepunkt_og_saksnummer_i_mdc() {
        var periode = new Periode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 30));
        when(perioderTjeneste.hentInnvilgedeOverlappendePerioder(aktørIder, new Periode(FOM, TOM)))
            .thenReturn(new InnvilgedePerioder(List.of(periode), Set.of(new Saksnummer("SAK1"))));

        var resultat = tjeneste.hentAktivitetspengerPerioder(request);

        assertThat(resultat.perioder()).containsExactly(new AktivitetspengerPeriode(periode.getFom(), periode.getTom()));
        assertThat(MDC.get("prosess_konsument")).isEqualTo(KONSUMENT);
        assertThat(MDC.get("prosess_endepunkt")).isEqualTo("/ekstern/tilleggsstonader/aktivitetspenger/perioder");
        assertThat(MDC.get("prosess_saksnummer")).isEqualTo("SAK1");
    }

    @Test
    void skal_ikke_legge_saksnummer_i_mdc_uten_treff() {
        when(perioderTjeneste.hentInnvilgedeOverlappendePerioder(aktørIder, new Periode(FOM, TOM)))
            .thenReturn(new InnvilgedePerioder(List.of(), Set.of()));

        var resultat = tjeneste.hentAktivitetspengerPerioder(request);

        assertThat(resultat.perioder()).isEmpty();
        assertThat(MDC.get("prosess_konsument")).isEqualTo(KONSUMENT);
        assertThat(MDC.get("prosess_saksnummer")).isNull();
    }
}
