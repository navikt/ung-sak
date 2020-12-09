package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OMPVurderSøknadsfristTjenesteTest {

    private OMPVurderSøknadsfristTjeneste tjeneste = new OMPVurderSøknadsfristTjeneste();

    @Test
    void asdf() {
        Map<Søknad, Set<SøktPeriode>> map = Map.of(new Søknad(new JournalpostId(123L), LocalDateTime.now()),
            Set.of(new SøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(12), LocalDate.now()), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nyRef())));

        Map<Søknad, Set<VurdertSøktPeriode>> søknadSetMap = tjeneste.vurderSøknadsfrist(map);

        assertThat(søknadSetMap).isNull();
    }
}
