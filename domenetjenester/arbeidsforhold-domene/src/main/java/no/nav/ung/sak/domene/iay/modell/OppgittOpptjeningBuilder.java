package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.OrgNummer;

public class OppgittOpptjeningBuilder {

    private final OppgittOpptjening kladd;

    private OppgittOpptjeningBuilder(OppgittOpptjening kladd) {
        this.kladd = kladd;
    }

    public static OppgittOpptjeningBuilder ny() {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(UUID.randomUUID()));
    }

    public static OppgittOpptjeningBuilder nyFraEksisterende(OppgittOpptjening eksisterende, UUID eksternReferanse, LocalDateTime tidspunkt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksisterende, eksternReferanse, tidspunkt));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, OffsetDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
    }

    public static OppgittOpptjeningBuilder oppdater(UUID eksternReferanse, OffsetDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
    }

    public OppgittOpptjeningBuilder medJournalpostId(JournalpostId journalpostId) {
        this.kladd.setJournalpostId(journalpostId);
        return this;
    }

    public OppgittOpptjeningBuilder medInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.kladd.setInnsendingstidspunkt(innsendingstidspunkt);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
        this.kladd.leggTilAnnenAktivitet(annenAktivitet);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilFrilansOpplysninger(OppgittFrilans frilans) {
        this.kladd.setFrilans(frilans);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilOppgittArbeidsforhold(OppgittArbeidsforholdBuilder builder) {
        this.kladd.leggTilOppgittArbeidsforhold(builder.build());
        return this;
    }

    public OppgittOpptjeningBuilder leggTilOppgittArbeidsforhold(List<OppgittArbeidsforholdBuilder> builder) {
        builder.forEach(builders -> this.kladd.leggTilOppgittArbeidsforhold(builders.build()));
        return this;
    }

    public OppgittFrilansBuilder getFrilansBuilder() {
        return this.kladd.getFrilans().isPresent()
            ? OppgittFrilansBuilder.fraEksisterende(kladd.getFrilans().get())
            : OppgittFrilansBuilder.ny();
    }

    public OppgittOpptjening build() {
        return kladd;
    }

    public void leggTilJournalpostId(JournalpostId journalpostId) {
        this.kladd.setJournalpostId(journalpostId);
    }

    public void leggTilInnsendingstidspunkt(LocalDateTime tidspunkt) {
        this.kladd.setInnsendingstidspunkt(tidspunkt);
    }

    public static class OppgittFrilansBuilder {

        private final OppgittFrilans kladd;

        private OppgittFrilansBuilder(OppgittFrilans kladd) {
            this.kladd = kladd;
        }

        public static OppgittFrilansBuilder ny() {
            return new OppgittFrilansBuilder(new OppgittFrilans());
        }

        public static OppgittFrilansBuilder fraEksisterende(OppgittFrilans oppgittFrilans) {
            return new OppgittFrilansBuilder(new OppgittFrilans(oppgittFrilans));
        }

        public OppgittFrilansBuilder medFrilansOppdrag(List<OppgittFrilansoppdrag> frilansoppdrag) {
            frilansoppdrag.forEach(this::leggTilFrilansOppdrag);
            return this;
        }

        public OppgittFrilansBuilder leggTilFrilansOppdrag(OppgittFrilansoppdrag frilansoppdrag) {
            if (this.kladd.getFrilansoppdrag().isEmpty()) {
                this.kladd.setFrilansoppdrag(new ArrayList<>());
            }
            this.kladd.leggTilFrilansoppdrag(frilansoppdrag);
            return this;
        }

        public OppgittFrilansBuilder medHarInntektFraFosterhjem(Boolean harInntektFraFosterhjem) {
            this.kladd.setHarInntektFraFosterhjem(harInntektFraFosterhjem);
            return this;
        }

        public OppgittFrilansBuilder medErNyoppstartet(Boolean erNyoppstartet) {
            this.kladd.setErNyoppstartet(erNyoppstartet);
            return this;
        }

        public OppgittFrilansBuilder medHarNærRelasjon(Boolean harNærRelasjon) {
            this.kladd.setHarNærRelasjon(harNærRelasjon);
            return this;
        }

        public OppgittFrilans build() {
            return kladd;
        }
    }

    public static class OppgittFrilansOppdragBuilder {
        private final OppgittFrilansoppdrag kladd;

        private OppgittFrilansOppdragBuilder(OppgittFrilansoppdrag kladd) {
            this.kladd = kladd;
        }

        public static OppgittFrilansOppdragBuilder ny() {
            return new OppgittFrilansOppdragBuilder(new OppgittFrilansoppdrag());
        }

        public OppgittFrilansOppdragBuilder medPeriode(DatoIntervallEntitet periode) {
            this.kladd.setPeriode(periode);
            return this;
        }

        public OppgittFrilansOppdragBuilder medInntekt(BigDecimal inntekt) {
            this.kladd.setInntekt(inntekt);
            return this;
        }

        public OppgittFrilansOppdragBuilder medOppdragsgiver(String oppdragsgiver) {
            this.kladd.setOppdragsgiver(oppdragsgiver);
            return this;
        }

        public OppgittFrilansoppdrag build() {
            return this.kladd;
        }
    }

    public static class OppgittArbeidsforholdBuilder {
        private OppgittArbeidsforhold entitet;

        private OppgittArbeidsforholdBuilder(OppgittArbeidsforhold entitet) {
            this.entitet = entitet;
        }

        public static OppgittArbeidsforholdBuilder ny() {
            return new OppgittArbeidsforholdBuilder(new OppgittArbeidsforhold());
        }

        public static OppgittArbeidsforholdBuilder fraEksisterende(OppgittArbeidsforhold kopierFra) {
            return new OppgittArbeidsforholdBuilder(new OppgittArbeidsforhold(kopierFra));
        }

        public OppgittArbeidsforholdBuilder medPeriode(DatoIntervallEntitet periode) {
            this.entitet.setPeriode(periode);
            return this;
        }

        public OppgittArbeidsforholdBuilder medInntekt(BigDecimal inntekt) {
            this.entitet.setInntekt(inntekt);
            return this;
        }

        public OppgittArbeidsforholdBuilder medArbeidType(ArbeidType arbeidType) {
            this.entitet.setArbeidType(arbeidType);
            return this;
        }

        public OppgittArbeidsforhold build() {
            return entitet;
        }
    }
}
