package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.sak.kontrakt.søknad.ArbeidsgiverDto;
import no.nav.k9.sak.kontrakt.søknad.ManglendeVedleggDto;
import no.nav.k9.sak.kontrakt.søknad.OppgittTilknytningDto;
import no.nav.k9.sak.kontrakt.søknad.SøknadDto;
import no.nav.k9.sak.kontrakt.søknad.UtlandsoppholdDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;

@ApplicationScoped
public class SøknadDtoTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private MedlemTjeneste medlemTjeneste;

    protected SøknadDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             KompletthetsjekkerProvider kompletthetsjekkerProvider,
                             ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                             MedlemTjeneste medlemTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
        this.medlemTjeneste = medlemTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public Optional<SøknadDto> mapFra(Behandling behandling) {
        Optional<SøknadEntitet> søknadOpt = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            SøknadEntitet søknad = søknadOpt.get();
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            return lagSoknadDto(søknad, ref);
        }
        return Optional.empty();
    }

    private Optional<SøknadDto> lagSoknadDto(SøknadEntitet søknad, BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();

        var dto = new SøknadDto();
        dto.setMottattDato(søknad.getMottattDato());
        dto.setSoknadsdato(søknad.getSøknadsdato());

        // FIXME K9 sett korrekt startdato for ytelse
        dto.setOppgittStartdato(søknad.getSøknadsdato());

        dto.setTilleggsopplysninger(søknad.getTilleggsopplysninger());
        dto.setSpraakkode(søknad.getSpråkkode());
        dto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> {
            dto.setOppgittTilknytning(mapFra(ma.getOppgittTilknytning().orElse(null)));
        });

        dto.setManglendeVedlegg(genererManglendeVedlegg(ref));

        return Optional.of(dto);
    }

    private List<ManglendeVedleggDto> genererManglendeVedlegg(BehandlingReferanse ref) {
        Kompletthetsjekker kompletthetsjekker = kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.getFagsakYtelseType(), ref.getBehandlingType());
        final List<ManglendeVedlegg> alleManglendeVedlegg = kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref);
        final List<ManglendeVedlegg> vedleggSomIkkeKommer = kompletthetsjekker.utledAlleManglendeVedleggSomIkkeKommer(ref);

        // Fjerner slik at det ikke blir dobbelt opp, og for å markere korrekt hvilke som ikke vil komme
        alleManglendeVedlegg.removeIf(e -> vedleggSomIkkeKommer.stream().anyMatch(it -> it.getArbeidsgiver().equals(e.getArbeidsgiver())));
        alleManglendeVedlegg.addAll(vedleggSomIkkeKommer);

        return alleManglendeVedlegg.stream().map(this::mapTilManglendeVedleggDto).collect(Collectors.toList());
    }

    private ManglendeVedleggDto mapTilManglendeVedleggDto(ManglendeVedlegg mv) {
        final ManglendeVedleggDto dto = new ManglendeVedleggDto();
        dto.setDokumentType(mv.getDokumentType());
        if (mv.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING)) {
            dto.setArbeidsgiver(mapTilArbeidsgiverDto(mv.getArbeidsgiver()));
            dto.setBrukerHarSagtAtIkkeKommer(mv.getBrukerHarSagtAtIkkeKommer());
        }
        return dto;
    }

    private ArbeidsgiverDto mapTilArbeidsgiverDto(String arbeidsgiverIdent) {
        if (OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdent) || OrgNummer.erKunstig(arbeidsgiverIdent)) {
            return virksomhetArbeidsgiver(new OrgNummer(arbeidsgiverIdent));
        } else {
            return privatpersonArbeidsgiver(new AktørId(arbeidsgiverIdent));
        }
    }

    private ArbeidsgiverDto privatpersonArbeidsgiver(AktørId aktørId) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.person(aktørId));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setNavn(opplysninger.getNavn());
        dto.setFødselsdato(opplysninger.getFødselsdato());
        dto.setAktørId(aktørId);

        // Dette må gjøres for å ikke knekke frontend, kan fjernes når frontend er rettet.
        dto.setOrganisasjonsnummer(new OrgNummer(opplysninger.getIdentifikator()));

        return dto;
    }

    private ArbeidsgiverDto virksomhetArbeidsgiver(OrgNummer orgnr) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(orgnr));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setOrganisasjonsnummer(orgnr);
        dto.setNavn(opplysninger.getNavn());
        return dto;
    }

    private static OppgittTilknytningDto mapFra(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        if (oppgittTilknytning != null) {
            return new OppgittTilknytningDto(
                oppgittTilknytning.isOppholdNå(),
                oppgittTilknytning.isOppholdINorgeSistePeriode(),
                oppgittTilknytning.isOppholdINorgeNestePeriode(),
                mapFør(oppgittTilknytning.getOpphold()),
                mapEtter(oppgittTilknytning.getOpphold()));
        }
        return null;
    }

    private static List<UtlandsoppholdDto> mapFør(Set<MedlemskapOppgittLandOppholdEntitet> opphold) {
        return mapFraMedlemskapOppgttLandOpphold(opphold.stream()
            .filter(o -> o.isTidligereOpphold())
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .collect(Collectors.toList()));
    }

    private static List<UtlandsoppholdDto> mapEtter(Set<MedlemskapOppgittLandOppholdEntitet> utlandsopphold) {
        return mapFraMedlemskapOppgttLandOpphold(utlandsopphold.stream()
            .filter(o -> !o.isTidligereOpphold())
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .collect(Collectors.toList()));
    }

    public static List<UtlandsoppholdDto> mapFraMedlemskapOppgttLandOpphold(List<MedlemskapOppgittLandOppholdEntitet> utlandsoppholdList) {
        return utlandsoppholdList.stream()
            .map(utlandsopphold -> new UtlandsoppholdDto(
                utlandsopphold.getLand().getNavn(),
                utlandsopphold.getPeriodeFom(),
                utlandsopphold.getPeriodeTom()))
            .collect(Collectors.toList());
    }
}
