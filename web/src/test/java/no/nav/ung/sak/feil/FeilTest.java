package no.nav.ung.sak.feil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.junit.jupiter.api.Test;

import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public class FeilTest {
    private final List<String> duplikatFeil = new ArrayList<>();
    private final List<String> prefiksFeil = new ArrayList<>();
    private final Map<String, AnnotationInstance> unikeKoder = new HashMap<>(1000);

    @Test
    public void test_Feil_annotation_deklarasjoner() {
        List<AnnotationInstance> annotationInstances = new IndexFeil().getAnnotationInstances(TekniskFeil.class, IntegrasjonFeil.class, FunksjonellFeil.class);

        for (AnnotationInstance ai : annotationInstances) {
            String feilkode = ai.value("feilkode").asString();
            verifiserFeilPrefiks(ai, feilkode);
            verifiserDuplikatFeil(ai, feilkode);
        }

        assertThat(unikeKoder).isNotEmpty();
        assertThat(prefiksFeil).isEmpty();
        assertThat(duplikatFeil).isEmpty();

    }

    private void verifiserDuplikatFeil(AnnotationInstance ai, String feilkode) {
        AnnotationInstance prev = unikeKoder.put(feilkode, ai);
        if (prev != null && !equalsTarget(prev, ai)) {
            duplikatFeil.add(String.format("2 Metoder har samme feilkode[%s] : %s, %s", feilkode, ai.target().asMethod().name(),
                prev.target().asMethod().name()));
        }
    }

    private void verifiserFeilPrefiks(AnnotationInstance ai, String feilkode) {
        String regex = "[A-Z0-9]+(-[A-Z0-9]+)*";
        if (!(feilkode.matches(regex))) {
            prefiksFeil
                .add(String.format("Metode %s har feilkode '%s' som ikke matcher regex: '%s'", ai.target().asMethod().name(), feilkode, regex));
        }
    }

    private boolean equalsTarget(AnnotationInstance prev, AnnotationInstance ai) {
        AnnotationTarget pt = prev.target();
        AnnotationTarget ait = ai.target();
        return pt.kind() == ait.kind()
            && Objects.equals(pt.asMethod().name(), ait.asMethod().name())
            && Objects.equals(pt.asMethod().declaringClass().name(), ait.asMethod().declaringClass().name());
    }
}
