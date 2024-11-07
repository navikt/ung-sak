package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;

public class Yrkesaktivitet implements IndexKey, Comparable<Yrkesaktivitet> {

    private static final Logger log = LoggerFactory.getLogger(Yrkesaktivitet.class);

    @ChangeTracked
    private Set<AktivitetsAvtale> aktivitetsAvtale = new LinkedHashSet<>();

    @ChangeTracked
    private Set<Permisjon> permisjon = new LinkedHashSet<>();

    @ChangeTracked
    private String navnArbeidsgiverUtland;

    /**
     * Kan være privat eller virksomhet som arbeidsgiver. Dersom {@link #arbeidType} = 'NÆRING', er denne null.
     */
    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private ArbeidType arbeidType;

    Yrkesaktivitet() {
        // hibernate
    }

    public Yrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        var kopierFra = yrkesaktivitet; // NOSONAR
        this.arbeidType = kopierFra.getArbeidType();
        this.arbeidsgiver = kopierFra.getArbeidsgiver();
        this.arbeidsforholdRef = kopierFra.arbeidsforholdRef;
        this.navnArbeidsgiverUtland = kopierFra.getNavnArbeidsgiverUtland();

        // NB må aksessere felt her heller en getter siden getter filtrerer
        this.aktivitetsAvtale = kopierFra.aktivitetsAvtale.stream().map(aa -> {
            var aktivitetsAvtaleEntitet = new AktivitetsAvtale(aa);
            return aktivitetsAvtaleEntitet;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        this.permisjon = kopierFra.permisjon.stream().map(p -> {
            var permisjonEntitet = new Permisjon(p);
            permisjonEntitet.setYrkesaktivitet(this);
            return permisjonEntitet;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { arbeidsgiver, arbeidsforholdRef, arbeidType };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Kategorisering av aktivitet som er enten pensjonsgivende inntekt eller likestilt med pensjonsgivende inntekt
     * <p>
     * Fra aa-reg
     * <ul>
     * <li>{@link ArbeidType#ORDINÆRT_ARBEIDSFORHOLD}</li>
     * <li>{@link ArbeidType#MARITIMT_ARBEIDSFORHOLD}</li>
     * <li>{@link ArbeidType#FORENKLET_OPPGJØRSORDNING}</li>
     * </ul>
     * <p>
     * Fra inntektskomponenten
     * <ul>
     * <li>{@link ArbeidType#FRILANSER_OPPDRAGSTAKER_MED_MER}</li>
     * </ul>
     * <p>
     * De resterende kommer fra søknaden
     *
     * @return {@link ArbeidType}
     */
    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    void setArbeidType(ArbeidType arbeidType) {
        this.arbeidType = arbeidType;
    }

    /**
     * Unik identifikator for arbeidsforholdet til aktøren i bedriften.
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return {@code ArbeidsforholdRef.ref(null)} hvis ikke tilstede
     */
    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    void setArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
        this.arbeidsforholdRef = arbeidsforholdId != null && !InternArbeidsforholdRef.nullRef().equals(arbeidsforholdId) ? arbeidsforholdId : null;
    }

    /**
     * Identifiser om yrkesaktiviteten gjelder for arbeidsgiver og arbeidsforholdRef.
     *
     * @param arbeidsgiver en {@link Arbeidsgiver}
     * @param arbeidsforholdRef et {@link InternArbeidsforholdRef}
     * @return true hvis arbeidsgiver og arbeidsforholdRef macther
     */

    public boolean gjelderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        boolean gjelderForArbeidsgiver = Objects.equals(getArbeidsgiver(), arbeidsgiver);
        boolean gjelderFor = gjelderForArbeidsgiver && getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
        return gjelderFor;
    }

    /**
     * Liste over fremtidige / historiske permisjoner hos arbeidsgiver.
     * <p>
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return liste med permisjoner
     */
    public Collection<Permisjon> getPermisjon() {
        return Collections.unmodifiableSet(permisjon);
    }

    void leggTilPermisjon(Permisjon permisjon) {
        if (permisjon == null) {
            return;
        }
        this.permisjon.add(permisjon);
        permisjon.setYrkesaktivitet(this);
    }

    public Collection<AktivitetsAvtale> getAlleAktivitetsAvtaler() {
        return Collections.unmodifiableSet(aktivitetsAvtale);
    }

    public Collection<AktivitetsAvtale> getAnsettelsesPeriode() {
        return getAlleAktivitetsAvtaler()
            .stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .collect(Collectors.toList());
    }

    /**
     * Gir stillingsprosent hvis det finnes for den gitte dagen
     *
     * @param forespurtDato {@link LocalDate}
     * @return Stillingsprosent {@link Stillingsprosent}
     */
    public Optional<Stillingsprosent> getStillingsprosentFor(LocalDate forespurtDato) {
        var besteKandidat = finnNærmesteAvtale(forespurtDato);
        return besteKandidat.map(AktivitetsAvtale::getProsentsats);
    }

    private Optional<AktivitetsAvtale> finnNærmesteAvtale(LocalDate forespurtDato) {
        LinkedList<AktivitetsAvtale> avtaler = getAlleAktivitetsAvtaler()
            .stream()
            .filter(a -> !a.erAnsettelsesPeriode())
            .filter(a -> a.getPeriode().inkluderer(forespurtDato))
            .sorted(AktivitetsAvtale.COMPARATOR)
            .collect(Collectors.toCollection(LinkedList::new));

        if (avtaler.isEmpty()) {
            return Optional.empty();
        }
        var besteKandidat = avtaler.getLast();

        if (avtaler.size() > 1) {
            // kan skje når periodene er overlappende som følge av at Aareg/A-ordningen har gjenbrukt id på arbeidsforhold for endringer
            // plukker da siste avtale med lønnsendringsdato før forespurt dato (som ikke er ansettelsesavtale, og har overlappende periode)
            for (var av : avtaler) {
                if (av.getSisteLønnsendringsdato() != null && av.getSisteLønnsendringsdato().isBefore(forespurtDato)) {
                    besteKandidat = av;
                }
            }
            log.warn("Fant [{}] overlappende aktivitetsavtaler for dato [{}], {}, aktivitetsavtaler={}. Valgt idx={}", avtaler.size(), forespurtDato, this, avtaler, avtaler.indexOf(besteKandidat));
        }
        return Optional.of(besteKandidat);
    }

    void leggTilAktivitetsAvtale(AktivitetsAvtale avtale) {
        if (avtale == null) {
            return;
        }
        this.aktivitetsAvtale.add(avtale);
    }

    /**
     * Arbeidsgiver
     * <p>
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return {@link Arbeidsgiver}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    /**
     * Navn på utenlands arbeidsgiver
     *
     * @return Navn
     */
    public String getNavnArbeidsgiverUtland() {
        return navnArbeidsgiverUtland;
    }

    void setNavnArbeidsgiverUtland(String navnArbeidsgiverUtland) {
        this.navnArbeidsgiverUtland = navnArbeidsgiverUtland;
    }

    public boolean erArbeidsforhold() {
        return ArbeidType.AA_REGISTER_TYPER.contains(arbeidType);
    }

    void tilbakestillPermisjon() {
        permisjon.clear();
    }

    void tilbakestillAvtaler() {
        aktivitetsAvtale.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Yrkesaktivitet)) {
            return false;
        }
        Yrkesaktivitet other = (Yrkesaktivitet) obj;
        return Objects.equals(this.getArbeidsforholdRef(), other.getArbeidsforholdRef()) &&
            Objects.equals(this.getNavnArbeidsgiverUtland(), other.getNavnArbeidsgiverUtland()) &&
            Objects.equals(this.getArbeidType(), other.getArbeidType()) &&
            Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getArbeidsforholdRef(), getNavnArbeidsgiverUtland(), getArbeidType(), getArbeidsgiver());
    }

    @Override
    public String toString() {
        return "YrkesaktivitetEntitet{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", arbeidType=" + arbeidType +
            '}';
    }

    void fjernPeriode(DatoIntervallEntitet aktivitetsPeriode) {
        aktivitetsAvtale.removeIf(aa -> aa.matcherPeriode(aktivitetsPeriode));
    }

    @Override
    public int compareTo(Yrkesaktivitet o) {
        return this.getIndexKey().compareTo(o.getIndexKey());
    }

}
