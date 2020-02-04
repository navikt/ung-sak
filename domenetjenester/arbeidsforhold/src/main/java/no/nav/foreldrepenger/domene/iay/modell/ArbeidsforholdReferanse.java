package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKeyComposer;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.api.IndexKey;

public class ArbeidsforholdReferanse extends BaseEntitet implements IndexKey, TraverseValue {

    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    private InternArbeidsforholdRef internReferanse;

    @ChangeTracked
    private EksternArbeidsforholdRef eksternReferanse;

    ArbeidsforholdReferanse() {
    }

    public ArbeidsforholdReferanse(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse, EksternArbeidsforholdRef eksternReferanse) {
        this.arbeidsgiver = arbeidsgiver;
        this.internReferanse = internReferanse != null ? internReferanse : InternArbeidsforholdRef.nullRef();
        this.eksternReferanse = eksternReferanse;
    }

    ArbeidsforholdReferanse(ArbeidsforholdReferanse arbeidsforholdInformasjonEntitet) {
        this(arbeidsforholdInformasjonEntitet.arbeidsgiver, arbeidsforholdInformasjonEntitet.internReferanse, arbeidsforholdInformasjonEntitet.eksternReferanse);
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { internReferanse, eksternReferanse };
        return IndexKeyComposer.createKey(keyParts);
    }

    public InternArbeidsforholdRef getInternReferanse() {
        return internReferanse;
    }

    public EksternArbeidsforholdRef getEksternReferanse() {
        return eksternReferanse;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof ArbeidsforholdReferanse))
            return false;
        ArbeidsforholdReferanse that = (ArbeidsforholdReferanse) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(internReferanse, that.internReferanse) &&
            Objects.equals(eksternReferanse, that.eksternReferanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, internReferanse, eksternReferanse);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdReferanseEntitet{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", internReferanse=" + internReferanse +
            ", eksternReferanse=" + eksternReferanse +
            '}';
    }
}
