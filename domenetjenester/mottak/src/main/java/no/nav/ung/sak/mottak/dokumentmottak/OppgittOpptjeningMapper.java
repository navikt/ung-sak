package no.nav.ung.sak.mottak.dokumentmottak;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.ung.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.ung.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.k9.søknad.felles.opptjening.AnnenAktivitet;
import no.nav.k9.søknad.felles.opptjening.Frilanser;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.opptjening.UtenlandskArbeidsforhold;
import no.nav.k9.søknad.felles.type.Organisasjonsnummer;
import no.nav.k9.søknad.felles.type.Periode;

@Dependent
public class OppgittOpptjeningMapper {

    @Inject
    OppgittOpptjeningMapper() {
    }

    public Optional<OppgittOpptjeningMottattRequest> mapRequest(Behandling behandling, MottattDokument dokument, OpptjeningAktivitet opptjeningAktiviteter) {

        if (opptjeningAktiviteter == null) {
            return Optional.empty();
        }
        var builder = OppgittOpptjeningBuilder.ny(UUID.randomUUID(), LocalDateTime.now());
        if (opptjeningAktiviteter.getSelvstendigNæringsdrivende() != null) {
            var snAktiviteter = opptjeningAktiviteter.getSelvstendigNæringsdrivende();
            var snBuilders = snAktiviteter.stream().map(this::mapEgenNæring).collect(Collectors.toList());
            snBuilders.forEach(builder::leggTilEgneNæringer);
        }
        if (opptjeningAktiviteter.getFrilanser() != null) {
            Frilanser frilanser = opptjeningAktiviteter.getFrilanser();
            DatoIntervallEntitet frilansperiode = frilanser.getSluttdato() == null ? DatoIntervallEntitet.fraOgMed(frilanser.getStartdato()) :
                DatoIntervallEntitet.fraOgMedTilOgMed(frilanser.getStartdato(), frilanser.getSluttdato());
            builder.leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                    .leggTilFrilansOppdrag(lagFrilansperiode(frilansperiode))
                .build());
        }
        if (opptjeningAktiviteter.getUtenlandskeArbeidsforhold() != null) {
            opptjeningAktiviteter.getUtenlandskeArbeidsforhold().forEach(arbforhold ->
                builder.leggTilOppgittArbeidsforhold(mapOppgittArbeidsforhold(arbforhold)));
        }
        if (opptjeningAktiviteter.getAndreAktiviteter() != null) {
            opptjeningAktiviteter.getAndreAktiviteter().forEach(aktivitet ->
                builder.leggTilAnnenAktivitet(mapAnnenAktivitet(aktivitet)));
        }
        builder.leggTilJournalpostId(dokument.getJournalpostId());
        builder.leggTilInnsendingstidspunkt(dokument.getInnsendingstidspunkt());

        return Optional.of(byggRequest(behandling, builder));
    }

    private OppgittFrilansoppdrag lagFrilansperiode(DatoIntervallEntitet frilansperiode) {
        return OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
            .medPeriode(frilansperiode)
            .build();
    }

    private List<EgenNæringBuilder> mapEgenNæring(SelvstendigNæringsdrivende sn) {
        Map.Entry<Periode, SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo> entry = getSnPeriodeInfo(sn);
        var periode = entry.getKey();
        var info = entry.getValue();
        var virksomhetTyper = info.getVirksomhetstyper();
        var orgnummer = sn.getOrganisasjonsnummer();
        return virksomhetTyper.stream().map(type -> mapNæringForVirksomhetType(periode, info, type, orgnummer)).toList();
    }

    private Map.Entry<Periode, SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo> getSnPeriodeInfo(SelvstendigNæringsdrivende sn) {
        if (sn.getPerioder().size() != 1) {
            throw new IllegalArgumentException("Søknad må ha eksakt én SN-periode. Størrelse var " + sn.getPerioder().size());
        }
        return sn.getPerioder().entrySet().iterator().next();
    }

    private EgenNæringBuilder mapNæringForVirksomhetType(no.nav.k9.søknad.felles.type.Periode periode,
                                                         no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo info,
                                                         no.nav.k9.søknad.felles.type.VirksomhetType type,
                                                         Organisasjonsnummer organisasjonsnummer) {
        var builder = EgenNæringBuilder.ny();
        builder.medVirksomhet(organisasjonsnummer != null ? new OrgNummer(organisasjonsnummer.getVerdi()) : null);
        builder.medPeriode(periode.getTilOgMed() != null
            ? DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed())
            : DatoIntervallEntitet.fraOgMed(periode.getFraOgMed()));
        builder.medBruttoInntekt(info.getBruttoInntekt());
        builder.medVirksomhetType(VirksomhetType.fraKode(type.getKode()));
        builder.medRegnskapsførerNavn(info.getRegnskapsførerNavn());
        builder.medRegnskapsførerTlf(info.getRegnskapsførerTlf());
        builder.medVarigEndring(info.getErVarigEndring());
        builder.medEndringDato(info.getEndringDato());
        builder.medBegrunnelse(info.getEndringBegrunnelse());
        builder.medNyoppstartet(info.getErNyoppstartet());
        builder.medNyIArbeidslivet(info.getErNyIArbeidslivet());
        return builder;
    }

    public OppgittOpptjeningMottattRequest byggRequest(Behandling behandling, OppgittOpptjeningBuilder builder) {
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandling.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(behandling.getAktørId(), null, behandling.getUuid()).mapTilDto(builder);
        var request = new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandling.getUuid(), aktør, ytelseType, oppgittOpptjening);
        return request;
    }

    private OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder mapOppgittArbeidsforhold(UtenlandskArbeidsforhold arbeidsforhold) {
        var arbeidsforholdBuilder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
            .medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD)
            .medUtenlandskVirksomhet(new OppgittUtenlandskVirksomhet(
                Landkoder.fraKode(arbeidsforhold.getLand().getLandkode()),
                arbeidsforhold.getArbeidsgiversnavn()))
            .medPeriode(DatoIntervallEntitet.fra(
                arbeidsforhold.getAnsettelsePeriode().getFraOgMed(),
                arbeidsforhold.getAnsettelsePeriode().getTilOgMed())
            );
        return arbeidsforholdBuilder;
    }

    private OppgittAnnenAktivitet mapAnnenAktivitet(AnnenAktivitet annenAktivitet) {
        var aktivitetPeriode = DatoIntervallEntitet.fra(annenAktivitet.getPeriode().getFraOgMed(), annenAktivitet.getPeriode().getTilOgMed());
        var arbeidType = ArbeidType.fraKode(annenAktivitet.getAnnenAktivitetType().getKode());
        return new OppgittAnnenAktivitet(aktivitetPeriode, arbeidType);
    }
}
