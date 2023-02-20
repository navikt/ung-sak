package no.nav.k9.sak.domene.behandling.steg.kompletthet.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.TidligereEtterlysning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.Arbeidsgiver;

class EtterlysningInputTest {

    @Test
    void skal_filtrere_etter_rett_type() {
        var input = new EtterlysningInput(Map.of(),
            Map.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000")))),
            Map.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), List.of(new TidligereEtterlysning(DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, Arbeidsgiver.virksomhet("000000000")))));

        var filtrert = input.getRelevanteFiltrerteMangler(DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK);

        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0).harMangler()).isFalse();
    }
}
