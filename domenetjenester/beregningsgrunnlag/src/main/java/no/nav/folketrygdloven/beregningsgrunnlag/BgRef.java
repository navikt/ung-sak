package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Beregningsgrunnlag referanse + Skjæringstidspunkt. For intern referanser til beregningsgrunnlag i denne modulen. */
public class BgRef implements Comparable<BgRef> {
    private UUID ref;
    private LocalDate stp;
    private boolean erGenerert;

    public BgRef(UUID bgRef, LocalDate stp) {
        this.ref = bgRef == null ? UUID.randomUUID() : bgRef;
        this.stp = stp;
        if (bgRef == null) {
            erGenerert = true;
        }
    }

    /** genererer ny ref for angitt skjæringstidspunkt. */
    public BgRef(LocalDate stp) {
        this(null, stp);
    }

    public LocalDate getStp() {
        return stp;
    }

    public UUID getRef() {
        return ref;
    }

    public boolean erGenerertReferanse() {
        return erGenerert;
    }

    @Override
    public int compareTo(BgRef o) {
        // sorterer å skjæringstidspunkt her, ikke ref
        int comp = (o == null) ? 1 : stp.compareTo(o.getStp());
        return comp == 0 ? getRef().compareTo(o.getRef()) : comp;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<stp=" + stp + ", ref=" + ref + ">";
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !obj.getClass().equals(this.getClass()))
            return false;
        var other = (BgRef) obj;
        return Objects.equals(ref, other.getRef());
    }

    public static List<UUID> getRefs(Collection<BgRef> bgReferanser) {
        return bgReferanser.stream().map(BgRef::getRef).sorted().distinct().collect(Collectors.toList());
    }

    public static List<LocalDate> getStps(Collection<BgRef> bgReferanser) {
        return bgReferanser.stream().map(BgRef::getStp).sorted().distinct().collect(Collectors.toList());
    }

}