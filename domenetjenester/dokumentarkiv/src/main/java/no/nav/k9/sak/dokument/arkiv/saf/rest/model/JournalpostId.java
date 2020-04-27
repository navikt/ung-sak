package no.nav.k9.sak.dokument.arkiv.saf.rest.model;

import java.util.Comparator;
import java.util.Objects;

public class JournalpostId extends ValueWrapper implements Comparable<JournalpostId> {
    public JournalpostId(String value) {
        super(value);
    }
    @Override
    public int compareTo(JournalpostId journalpostId) {
        return Objects.compare(this, journalpostId, Comparator.comparing(a -> a.value));
    }

}
